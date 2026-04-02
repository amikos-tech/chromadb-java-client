package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.Content;
import tech.amikos.chromadb.embeddings.content.Modality;
import tech.amikos.chromadb.embeddings.content.Part;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that wraps a text-only {@link EmbeddingFunction} as a
 * {@link ContentEmbeddingFunction}.
 *
 * <p>For each {@link Content}, extracts the first text {@link Part} and delegates to the
 * wrapped embedding function. Throws {@link EFException} if any content has no text part.</p>
 */
public final class TextEmbeddingAdapter implements ContentEmbeddingFunction {

    private final EmbeddingFunction wrapped;

    public TextEmbeddingAdapter(EmbeddingFunction wrapped) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped embedding function must not be null");
        }
        this.wrapped = wrapped;
    }

    @Override
    public List<Embedding> embedContents(List<Content> contents) throws EFException {
        if (contents == null) {
            throw new IllegalArgumentException("contents must not be null");
        }
        List<String> texts = new ArrayList<String>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            if (content == null) {
                throw new IllegalArgumentException("content at index " + i + " must not be null");
            }
            String text = extractText(content);
            if (text == null) {
                throw new EFException("Content has no text part");
            }
            texts.add(text);
        }
        return wrapped.embedDocuments(texts);
    }

    private String extractText(Content content) throws EFException {
        for (Part part : content.getParts()) {
            if (part.getModality() == Modality.TEXT) {
                if (part.getText() == null) {
                    throw new EFException("Content text part must not be null");
                }
                return part.getText();
            }
        }
        return null;
    }
}
