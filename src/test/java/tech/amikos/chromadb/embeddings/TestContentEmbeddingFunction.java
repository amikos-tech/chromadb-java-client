package tech.amikos.chromadb.embeddings;

import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.BinarySource;
import tech.amikos.chromadb.embeddings.content.Content;
import tech.amikos.chromadb.embeddings.content.Part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestContentEmbeddingFunction {

    @Test
    public void testDefaultEmbedContentDelegatesToEmbedContents() throws Exception {
        ContentEmbeddingFunction ef = new ContentEmbeddingFunction() {
            @Override
            public List<Embedding> embedContents(List<Content> contents) {
                List<Embedding> result = new ArrayList<Embedding>();
                for (Content c : contents) {
                    result.add(Embedding.fromArray(new float[]{1.0f}));
                }
                return result;
            }
        };

        Embedding result = ef.embedContent(Content.text("hi"));
        assertNotNull(result);
        assertEquals(1, result.getDimensions());
    }

    @Test
    public void testFromTextOnlyWrapsEmbeddingFunction() throws Exception {
        EmbeddingFunction textEf = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) {
                return Embedding.fromArray(new float[]{2.0f, 3.0f});
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) {
                List<Embedding> result = new ArrayList<Embedding>();
                for (String d : documents) {
                    result.add(Embedding.fromArray(new float[]{2.0f, 3.0f}));
                }
                return result;
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) {
                return embedDocuments(Arrays.asList(documents));
            }
        };

        ContentEmbeddingFunction cef = ContentEmbeddingFunction.fromTextOnly(textEf);
        Embedding result = cef.embedContent(Content.text("hello"));
        assertNotNull(result);
        assertEquals(2, result.getDimensions());
        assertArrayEquals(new float[]{2.0f, 3.0f}, result.asArray(), 0.001f);
    }

    @Test(expected = EFException.class)
    public void testTextEmbeddingAdapterThrowsForNoTextPart() throws Exception {
        EmbeddingFunction dummyEf = new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) { return null; }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) { return null; }

            @Override
            public List<Embedding> embedDocuments(String[] documents) { return null; }
        };

        TextEmbeddingAdapter adapter = new TextEmbeddingAdapter(dummyEf);
        Content imageOnly = Content.builder()
                .part(Part.image(BinarySource.fromUrl("http://x")))
                .build();
        adapter.embedContents(Collections.singletonList(imageOnly));
    }

    @Test
    public void testContentToTextAdapterEmbedQuery() throws Exception {
        ContentEmbeddingFunction cef = new ContentEmbeddingFunction() {
            @Override
            public List<Embedding> embedContents(List<Content> contents) {
                List<Embedding> result = new ArrayList<Embedding>();
                for (Content c : contents) {
                    result.add(Embedding.fromArray(new float[]{4.0f}));
                }
                return result;
            }
        };

        ContentToTextAdapter adapter = new ContentToTextAdapter(cef);
        Embedding result = adapter.embedQuery("test");
        assertNotNull(result);
        assertEquals(1, result.getDimensions());
    }

    @Test
    public void testContentToTextAdapterEmbedDocumentsList() throws Exception {
        ContentEmbeddingFunction cef = new ContentEmbeddingFunction() {
            @Override
            public List<Embedding> embedContents(List<Content> contents) {
                List<Embedding> result = new ArrayList<Embedding>();
                for (Content c : contents) {
                    result.add(Embedding.fromArray(new float[]{5.0f}));
                }
                return result;
            }
        };

        ContentToTextAdapter adapter = new ContentToTextAdapter(cef);
        List<Embedding> results = adapter.embedDocuments(Arrays.asList("a", "b"));
        assertEquals(2, results.size());
    }

    @Test
    public void testContentToTextAdapterEmbedDocumentsArray() throws Exception {
        ContentEmbeddingFunction cef = new ContentEmbeddingFunction() {
            @Override
            public List<Embedding> embedContents(List<Content> contents) {
                List<Embedding> result = new ArrayList<Embedding>();
                for (Content c : contents) {
                    result.add(Embedding.fromArray(new float[]{6.0f}));
                }
                return result;
            }
        };

        ContentToTextAdapter adapter = new ContentToTextAdapter(cef);
        List<Embedding> results = adapter.embedDocuments(new String[]{"a", "b"});
        assertEquals(2, results.size());
    }
}
