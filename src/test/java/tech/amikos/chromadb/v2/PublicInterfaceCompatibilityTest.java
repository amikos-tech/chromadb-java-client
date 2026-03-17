package tech.amikos.chromadb.v2;

import org.junit.Test;
import okhttp3.OkHttpClient;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PublicInterfaceCompatibilityTest {

    @Test
    public void testClientGetCollectionWithEmbeddingFunctionIsDefaultMethod() throws Exception {
        Method method = Client.class.getMethod(
                "getCollection",
                String.class,
                tech.amikos.chromadb.embeddings.EmbeddingFunction.class
        );
        assertTrue(method.isDefault());
    }

    @Test
    public void testCollectionGetSchemaIsDefaultMethod() throws Exception {
        Method method = Collection.class.getMethod("getSchema");
        assertTrue(method.isDefault());
    }

    @Test
    public void testAddBuilderHasIdGeneratorMethod() throws Exception {
        Method method = Collection.AddBuilder.class.getMethod("idGenerator", IdGenerator.class);
        assertEquals(Collection.AddBuilder.class, method.getReturnType());
    }

    @Test
    public void testUpsertBuilderHasIdGeneratorMethod() throws Exception {
        Method method = Collection.UpsertBuilder.class.getMethod("idGenerator", IdGenerator.class);
        assertEquals(Collection.UpsertBuilder.class, method.getReturnType());
    }

    @Test
    public void testChromaClientBuilderHasSslCertMethod() throws Exception {
        Method method = ChromaClient.Builder.class.getMethod("sslCert", Path.class);
        assertEquals(ChromaClient.Builder.class, method.getReturnType());
    }

    @Test
    public void testChromaClientBuilderHasInsecureMethod() throws Exception {
        Method method = ChromaClient.Builder.class.getMethod("insecure", boolean.class);
        assertEquals(ChromaClient.Builder.class, method.getReturnType());
    }

    @Test
    public void testChromaClientBuilderHasHttpClientMethod() throws Exception {
        Method method = ChromaClient.Builder.class.getMethod("httpClient", OkHttpClient.class);
        assertEquals(ChromaClient.Builder.class, method.getReturnType());
    }

    @Test
    public void testChromaClientBuilderHasLoggerMethod() throws Exception {
        Method method = ChromaClient.Builder.class.getMethod("logger", ChromaLogger.class);
        assertEquals(ChromaClient.Builder.class, method.getReturnType());
    }

    @Test
    public void testChromaClientBuilderHasEnvMethods() throws Exception {
        Method tenantMethod = ChromaClient.Builder.class.getMethod("tenantFromEnv", String.class);
        Method databaseMethod = ChromaClient.Builder.class.getMethod("databaseFromEnv", String.class);
        Method bothMethod = ChromaClient.Builder.class.getMethod("tenantAndDatabaseFromEnv");

        assertEquals(ChromaClient.Builder.class, tenantMethod.getReturnType());
        assertEquals(ChromaClient.Builder.class, databaseMethod.getReturnType());
        assertEquals(ChromaClient.Builder.class, bothMethod.getReturnType());
    }

    @Test
    public void testCloudBuilderHasLoggerMethod() throws Exception {
        Method method = ChromaClient.CloudBuilder.class.getMethod("logger", ChromaLogger.class);
        assertEquals(ChromaClient.CloudBuilder.class, method.getReturnType());
    }
}
