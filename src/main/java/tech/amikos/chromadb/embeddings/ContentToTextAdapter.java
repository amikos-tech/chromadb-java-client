package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.Content;
import tech.amikos.chromadb.embeddings.content.Intent;
import tech.amikos.chromadb.embeddings.content.Part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adapter that wraps a {@link ContentEmbeddingFunction} as a text-only
 * {@link EmbeddingFunction}.
 *
 * <p>Converts each input string to {@link Content#text(String)} and delegates to the
 * wrapped content embedding function.</p>
 */
public final class ContentToTextAdapter implements EmbeddingFunction {

    private final ContentEmbeddingFunction wrapped;

    public ContentToTextAdapter(ContentEmbeddingFunction wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped content embedding function must not be null");
        }
        this.wrapped = wrapped;
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        return wrapped.embedContent(Content.builder()
                .part(Part.text(query))
                .intent(Intent.RETRIEVAL_QUERY)
                .build());
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        if (documents == null) {
            throw new IllegalArgumentException("documents must not be null");
        }
        List<Content> contents = new ArrayList<Content>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.get(i);
            if (doc == null) {
                throw new IllegalArgumentException("document at index " + i + " must not be null");
            }
            contents.add(Content.builder()
                    .part(Part.text(doc))
                    .intent(Intent.RETRIEVAL_DOCUMENT)
                    .build());
        }
        return wrapped.embedContents(contents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        if (documents == null) {
            return embedDocuments((List<String>) null);
        }
        return embedDocuments(Arrays.asList(documents));
    }
}
