package tech.amikos.chromadb.embeddings.ollama;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.WithParam;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestOllamaEmbeddings {
    static GenericContainer ollamaContainer;

    @BeforeClass
    public static void setupChromaDB() throws Exception {
        try {
            ollamaContainer = new GenericContainer("ollama/ollama:latest")
                    .withExposedPorts(11434)
                    .waitingFor(Wait.forHttp("/api/version").forStatusCode(200));
            ollamaContainer.start();
            ollamaContainer.waitingFor(Wait.forHttp("/api/version").forStatusCode(200));
            System.setProperty("OLLAMA_URL", "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434) + "/api/embed");
            ollamaContainer.execInContainer("ollama", "pull", "nomic-embed-text");
        } catch (Exception e) {
            System.err.println("Ollama container failed to start");
            throw e;
        }
    }

    @AfterClass
    public static void teardownChromaDB() {
        if (ollamaContainer != null) {
            ollamaContainer.stop();
        }
    }

    @Test
    public void testOllamaEmbedDocuments() throws EFException {
        OllamaEmbeddingFunction ef = new OllamaEmbeddingFunction(WithParam.baseAPI(System.getProperty("OLLAMA_URL")));
        List<Embedding> results = ef.embedDocuments(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, I am Jane and I am an ML researcher."));
        assertEquals(2, results.size());
        assertEquals(768, results.get(0).getDimensions());
    }

    @Test
    public void testOllamaEmbedQuery() throws EFException {
        OllamaEmbeddingFunction ef = new OllamaEmbeddingFunction(WithParam.baseAPI(System.getProperty("OLLAMA_URL")));
        Embedding results = ef.embedQuery("Hello, I am Jane and I am an ML researcher.");
        assertNotNull(results);
        assertEquals(768, results.getDimensions());
    }


}
