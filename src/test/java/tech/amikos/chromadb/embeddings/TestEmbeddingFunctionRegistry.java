package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.bm25.BM25EmbeddingFunction;
import tech.amikos.chromadb.embeddings.content.Content;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.EmbeddingFunctionSpec;
import tech.amikos.chromadb.v2.SparseVector;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TestEmbeddingFunctionRegistry {

    private static EmbeddingFunctionSpec spec(String name) {
        return EmbeddingFunctionSpec.builder().name(name).type("known").build();
    }

    private static EmbeddingFunctionSpec specWithConfig(String name, Map<String, Object> config) {
        return EmbeddingFunctionSpec.builder().name(name).type("known").config(config).build();
    }

    @Test
    public void testGetDefaultIsSingleton() {
        assertSame(EmbeddingFunctionRegistry.getDefault(), EmbeddingFunctionRegistry.getDefault());
    }

    @Test
    public void testResolveOpenAI() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("api_key", "test-key");
        EmbeddingFunction ef = EmbeddingFunctionRegistry.getDefault().resolveDense(specWithConfig("openai", config));
        assertNotNull(ef);
        assertTrue(ef instanceof OpenAIEmbeddingFunction);
    }

    @Test
    public void testResolveCohere() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("api_key", "test-key");
        EmbeddingFunction ef = EmbeddingFunctionRegistry.getDefault().resolveDense(specWithConfig("cohere", config));
        assertNotNull(ef);
    }

    @Test
    public void testResolveBM25() {
        SparseEmbeddingFunction sf = EmbeddingFunctionRegistry.getDefault().resolveSparse(spec("chroma_bm25"));
        assertNotNull(sf);
        assertTrue(sf instanceof BM25EmbeddingFunction);
    }

    @Test
    public void testResolveBM25Alias() {
        SparseEmbeddingFunction sf = EmbeddingFunctionRegistry.getDefault().resolveSparse(spec("bm25"));
        assertNotNull(sf);
        assertTrue(sf instanceof BM25EmbeddingFunction);
    }

    @Test
    public void testResolveContentFallbackToDense() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("api_key", "test-key");
        ContentEmbeddingFunction cef = EmbeddingFunctionRegistry.getDefault().resolveContent(specWithConfig("openai", config));
        assertNotNull(cef);
    }

    @Test
    public void testResolveNullReturnsNull() {
        assertNull(EmbeddingFunctionRegistry.getDefault().resolveDense(null));
    }

    @Test(expected = ChromaException.class)
    public void testResolveUnknownThrows() {
        EmbeddingFunctionRegistry.getDefault().resolveDense(spec("nonexistent"));
    }

    @Test
    public void testCustomRegistryRegisterAndResolve() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final EmbeddingFunction mockEf = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) { return null; }
            @Override
            public List<Embedding> embedDocuments(List<String> documents) { return null; }
            @Override
            public List<Embedding> embedDocuments(String[] documents) { return null; }
        };
        registry.registerDense("my_custom", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                return mockEf;
            }
        });
        EmbeddingFunction resolved = registry.resolveDense(spec("my_custom"));
        assertSame(mockEf, resolved);
    }

    @Test
    public void testCustomRegistrySparseRegisterAndResolve() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final SparseEmbeddingFunction mockSf = new SparseEmbeddingFunction() {
            @Override
            public SparseVector embedQuery(String query) { return null; }
            @Override
            public List<SparseVector> embedDocuments(List<String> documents) { return null; }
        };
        registry.registerSparse("my_sparse", new EmbeddingFunctionRegistry.SparseFactory() {
            @Override
            public SparseEmbeddingFunction create(Map<String, Object> config) {
                return mockSf;
            }
        });
        SparseEmbeddingFunction resolved = registry.resolveSparse(spec("my_sparse"));
        assertSame(mockSf, resolved);
    }

    @Test
    public void testCustomRegistryContentRegisterAndResolve() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final ContentEmbeddingFunction mockCef = new ContentEmbeddingFunction() {
            @Override
            public List<Embedding> embedContents(List<Content> contents) { return null; }
        };
        registry.registerContent("my_content", new EmbeddingFunctionRegistry.ContentFactory() {
            @Override
            public ContentEmbeddingFunction create(Map<String, Object> config) {
                return mockCef;
            }
        });
        ContentEmbeddingFunction resolved = registry.resolveContent(spec("my_content"));
        assertSame(mockCef, resolved);
    }

    @Test
    public void testThreadSafety() throws Exception {
        final EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final AtomicInteger errors = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                        String name = "provider_" + idx;
                        registry.registerDense(name, new EmbeddingFunctionRegistry.DenseFactory() {
                            @Override
                            public EmbeddingFunction create(Map<String, Object> config) {
                                return new EmbeddingFunction() {
                                    @Override
                                    public Embedding embedQuery(String query) { return null; }
                                    @Override
                                    public List<Embedding> embedDocuments(List<String> documents) { return null; }
                                    @Override
                                    public List<Embedding> embedDocuments(String[] documents) { return null; }
                                };
                            }
                        });
                        EmbeddingFunction ef = registry.resolveDense(spec(name));
                        if (ef == null) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        assertEquals("No errors expected during concurrent register/resolve", 0, errors.get());
    }

    @Test
    public void testContentFallbackChain() throws EFException {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final Embedding dummyEmbedding = new Embedding(new float[]{1.0f, 2.0f, 3.0f});
        registry.registerDense("test_ef", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                return new EmbeddingFunction() {
                    @Override
                    public Embedding embedQuery(String query) { return dummyEmbedding; }
                    @Override
                    public List<Embedding> embedDocuments(List<String> documents) {
                        return Collections.singletonList(dummyEmbedding);
                    }
                    @Override
                    public List<Embedding> embedDocuments(String[] documents) {
                        return Collections.singletonList(dummyEmbedding);
                    }
                };
            }
        });
        ContentEmbeddingFunction cef = registry.resolveContent(spec("test_ef"));
        assertNotNull(cef);
        // Verify the adapter works - embedding a text content should succeed
        Embedding result = cef.embedContent(Content.text("hello"));
        assertNotNull(result);
        assertEquals(3, result.getDimensions());
    }

    @Test
    public void testResolveNullSparseReturnsNull() {
        assertNull(EmbeddingFunctionRegistry.getDefault().resolveSparse(null));
    }

    @Test
    public void testResolveNullContentReturnsNull() {
        assertNull(EmbeddingFunctionRegistry.getDefault().resolveContent(null));
    }

    @Test
    public void testCustomRegistryResolvesCaseInsensitiveName() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        final EmbeddingFunction mockEf = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) { return null; }
            @Override
            public List<Embedding> embedDocuments(List<String> documents) { return null; }
            @Override
            public List<Embedding> embedDocuments(String[] documents) { return null; }
        };

        registry.registerDense("MyProvider", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                return mockEf;
            }
        });

        assertSame(mockEf, registry.resolveDense(spec("MYPROVIDER")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDenseRejectsNullName() {
        new EmbeddingFunctionRegistry().registerDense(null, new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                return null;
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterSparseRejectsNullFactory() {
        new EmbeddingFunctionRegistry().registerSparse("provider", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterContentRejectsBlankName() {
        new EmbeddingFunctionRegistry().registerContent("   ", new EmbeddingFunctionRegistry.ContentFactory() {
            @Override
            public ContentEmbeddingFunction create(Map<String, Object> config) {
                return null;
            }
        });
    }

    @Test
    public void testResolveDenseWrapsEFException() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        registry.registerDense("broken", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                throw new EFException("bad dense config");
            }
        });

        try {
            registry.resolveDense(spec("broken"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to create dense provider 'broken'"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testResolveDenseWrapsRuntimeException() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        registry.registerDense("broken", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                throw new IllegalStateException("boom");
            }
        });

        try {
            registry.resolveDense(spec("broken"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to create dense provider 'broken'"));
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    public void testResolveContentWrapsRuntimeExceptionFromContentFactory() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        registry.registerContent("broken", new EmbeddingFunctionRegistry.ContentFactory() {
            @Override
            public ContentEmbeddingFunction create(Map<String, Object> config) {
                throw new IllegalStateException("boom");
            }
        });

        try {
            registry.resolveContent(spec("broken"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("Failed to create content provider 'broken'"));
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    public void testResolveContentWrapsRuntimeExceptionFromDenseFallback() {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        registry.registerDense("broken", new EmbeddingFunctionRegistry.DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) {
                throw new IllegalStateException("boom");
            }
        });

        try {
            registry.resolveContent(spec("broken"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("via dense fallback"));
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveDenseReportsUnavailableOptionalProvider() throws Exception {
        EmbeddingFunctionRegistry registry = new EmbeddingFunctionRegistry();
        Field field = EmbeddingFunctionRegistry.class.getDeclaredField("unavailableDenseProviders");
        field.setAccessible(true);
        Map<String, String> unavailable = (Map<String, String>) field.get(registry);
        unavailable.put("google_genai", "requires optional dependency com.google.genai:google-genai on the classpath");

        try {
            registry.resolveDense(spec("google_genai"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("unavailable"));
            assertTrue(e.getMessage().contains("google-genai"));
        }
    }
}
