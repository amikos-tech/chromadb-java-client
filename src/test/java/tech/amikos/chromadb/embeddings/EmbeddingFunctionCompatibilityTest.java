package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmbeddingFunctionCompatibilityTest {

    @Test
    public void testDefaultEmbedQueriesListDelegatesToEmbedDocumentsList() throws EFException {
        final StringListHolder captured = new StringListHolder();
        EmbeddingFunction function = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                return new Embedding(new float[]{0.0f});
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                captured.value = documents;
                return Collections.singletonList(new Embedding(new float[]{1.0f}));
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                return embedDocuments(Arrays.asList(documents));
            }
        };

        List<Embedding> result = function.embedQueries(Arrays.asList("q1", "q2"));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(captured.value);
        assertEquals(Arrays.asList("q1", "q2"), captured.value);
    }

    @Test
    public void testDefaultEmbedQueriesArrayDelegatesToEmbedQueriesListDefault() throws EFException {
        final int[] listCalls = new int[]{0};
        EmbeddingFunction function = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                return new Embedding(new float[]{0.0f});
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                listCalls[0]++;
                return Collections.singletonList(new Embedding(new float[]{documents.size()}));
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                return embedDocuments(Arrays.asList(documents));
            }
        };

        List<Embedding> result = function.embedQueries(new String[]{"q1"});
        assertEquals(1, listCalls[0]);
        assertNotNull(result);
        assertEquals(1.0f, result.get(0).asArray()[0], 0.0f);
    }

    private static final class StringListHolder {
        private List<String> value;
    }
}
