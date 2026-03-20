package tech.amikos.chromadb.embeddings;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.*;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.v2.ChromaException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class DefaultEmbeddingFunctionTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private String originalUrl;

    @Before
    public void saveOriginalUrl() {
        originalUrl = DefaultEmbeddingFunction.modelDownloadUrl;
    }

    @After
    public void restoreOriginalUrl() {
        DefaultEmbeddingFunction.modelDownloadUrl = originalUrl;
    }

    @Test
    public void testDefaultTimeoutConstant() {
        assertEquals(300, DefaultEmbeddingFunction.DEFAULT_DOWNLOAD_TIMEOUT_SECONDS);
    }

    @Test
    public void testModelCacheDirIsStable() {
        String expected = System.getProperty("user.home")
            + "/.cache/chroma/onnx_models/all-MiniLM-L6-v2";
        assertEquals(expected, DefaultEmbeddingFunction.MODEL_CACHE_DIR.toString());
    }

    @Test
    public void testConstructorAcceptsCustomTimeout() {
        // Verify the overloaded constructor exists and is callable.
        // If model is cached, this succeeds; if not, it attempts download.
        boolean modelCached = DefaultEmbeddingFunction.MODEL_CACHE_DIR
            .resolve("onnx").resolve("model.onnx").toFile().exists();
        Assume.assumeTrue("Model not cached; skipping constructor test", modelCached);
        try {
            DefaultEmbeddingFunction ef = new DefaultEmbeddingFunction(60);
            assertNotNull(ef);
        } catch (EFException e) {
            fail("Constructor with timeout should succeed when model is cached: " + e.getMessage());
        }
    }

    @Test
    public void testConcurrentConstructionThreadSafety() throws Exception {
        boolean modelCached = DefaultEmbeddingFunction.MODEL_CACHE_DIR
            .resolve("onnx").resolve("model.onnx").toFile().exists();
        Assume.assumeTrue("Model not cached; skipping concurrency test", modelCached);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<DefaultEmbeddingFunction>> futures = new ArrayList<Future<DefaultEmbeddingFunction>>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<DefaultEmbeddingFunction>() {
                @Override
                public DefaultEmbeddingFunction call() throws Exception {
                    return new DefaultEmbeddingFunction();
                }
            }));
        }
        executor.shutdown();
        assertTrue("Threads did not complete in time", executor.awaitTermination(60, TimeUnit.SECONDS));

        int successCount = 0;
        for (Future<DefaultEmbeddingFunction> future : futures) {
            try {
                assertNotNull(future.get());
                successCount++;
            } catch (ExecutionException e) {
                fail("Concurrent construction threw: " + e.getCause().getMessage());
            }
        }
        assertEquals("All threads should succeed", threadCount, successCount);
    }

    @Test
    public void testNonRetryable404ThrowsChromaException() {
        // Point download URL at WireMock
        DefaultEmbeddingFunction.modelDownloadUrl = wireMock.url("/model.tar.gz");

        stubFor(get(urlEqualTo("/model.tar.gz"))
            .willReturn(aResponse().withStatus(404).withBody("Not Found")));

        // Temporarily move model file aside if cached
        Path modelOnnx = DefaultEmbeddingFunction.MODEL_CACHE_DIR.resolve("onnx").resolve("model.onnx");
        boolean wasCached = modelOnnx.toFile().exists();
        Path backup = null;
        if (wasCached) {
            backup = modelOnnx.resolveSibling("model.onnx.bak");
            modelOnnx.toFile().renameTo(backup.toFile());
        }
        try {
            new DefaultEmbeddingFunction(5);
            fail("Expected EFException wrapping ChromaException for 404");
        } catch (EFException e) {
            assertTrue("Cause should be ChromaException", e.getCause() instanceof ChromaException);
            String msg = e.getCause().getMessage();
            assertTrue("Message should mention 404", msg.contains("404"));
            assertTrue("Message should be non-retryable", msg.contains("non-retryable"));
        } finally {
            if (wasCached && backup != null) {
                backup.toFile().renameTo(modelOnnx.toFile());
            }
        }
        // Verify only 1 request (no retry for 404)
        verify(1, getRequestedFor(urlEqualTo("/model.tar.gz")));
    }

    @Test
    public void testRetryableServerErrorRetriesThenThrowsChromaException() {
        DefaultEmbeddingFunction.modelDownloadUrl = wireMock.url("/model.tar.gz");

        stubFor(get(urlEqualTo("/model.tar.gz"))
            .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        Path modelOnnx = DefaultEmbeddingFunction.MODEL_CACHE_DIR.resolve("onnx").resolve("model.onnx");
        boolean wasCached = modelOnnx.toFile().exists();
        Path backup = null;
        if (wasCached) {
            backup = modelOnnx.resolveSibling("model.onnx.bak");
            modelOnnx.toFile().renameTo(backup.toFile());
        }
        try {
            new DefaultEmbeddingFunction(5);
            fail("Expected EFException wrapping ChromaException for retried 503");
        } catch (EFException e) {
            assertTrue("Cause should be ChromaException", e.getCause() instanceof ChromaException);
            String msg = e.getCause().getMessage();
            assertTrue("Message should mention failed after 2 attempts",
                msg.contains("model download failed after 2 attempts"));
            assertTrue("Message should include download URL",
                msg.contains(wireMock.url("/model.tar.gz")));
        } finally {
            if (wasCached && backup != null) {
                backup.toFile().renameTo(modelOnnx.toFile());
            }
        }
        // Verify exactly 2 requests (1 original + 1 retry)
        verify(2, getRequestedFor(urlEqualTo("/model.tar.gz")));
    }

    @Test
    public void testTimeoutThrowsChromaExceptionWithActionableMessage() {
        DefaultEmbeddingFunction.modelDownloadUrl = wireMock.url("/model.tar.gz");

        // Stub with a fixed delay longer than the timeout
        stubFor(get(urlEqualTo("/model.tar.gz"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(10000)  // 10 seconds delay
                .withBody("slow")));

        Path modelOnnx = DefaultEmbeddingFunction.MODEL_CACHE_DIR.resolve("onnx").resolve("model.onnx");
        boolean wasCached = modelOnnx.toFile().exists();
        Path backup = null;
        if (wasCached) {
            backup = modelOnnx.resolveSibling("model.onnx.bak");
            modelOnnx.toFile().renameTo(backup.toFile());
        }
        try {
            // Use 1-second timeout to force timeout quickly
            new DefaultEmbeddingFunction(1);
            fail("Expected EFException wrapping ChromaException for timeout");
        } catch (EFException e) {
            assertTrue("Cause should be ChromaException", e.getCause() instanceof ChromaException);
            String msg = e.getCause().getMessage();
            // Timeout is retryable, so message should say "failed after 2 attempts"
            assertTrue("Message should mention failed after 2 attempts or timed out",
                msg.contains("model download failed after 2 attempts")
                || msg.contains("timed out"));
        } finally {
            if (wasCached && backup != null) {
                backup.toFile().renameTo(modelOnnx.toFile());
            }
        }
    }
}
