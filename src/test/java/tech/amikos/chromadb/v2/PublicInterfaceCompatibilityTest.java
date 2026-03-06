package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.lang.reflect.Method;

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
}
