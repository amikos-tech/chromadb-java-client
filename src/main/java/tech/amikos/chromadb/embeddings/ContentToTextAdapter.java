package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.Content;

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
public class ContentToTextAdapter implements EmbeddingFunction {

    private final ContentEmbeddingFunction wrapped;

    public ContentToTextAdapter(ContentEmbeddingFunction wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped content embedding function must not be null");
        }
        this.wrapped = wrapped;
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        return wrapped.embedContent(Content.text(query));
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        List<Content> contents = new ArrayList<Content>(documents.size());
        for (String doc : documents) {
            contents.add(Content.text(doc));
        }
        return wrapped.embedContents(contents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }
}
