package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.content.Content;

import java.util.Collections;
import java.util.List;

/**
 * Embedding function interface for content-based (multimodal) embeddings.
 *
 * <p>Unlike {@link EmbeddingFunction} which only accepts text strings, this interface
 * accepts {@link Content} objects that can contain text, images, audio, or video parts.
 * Providers like Gemini or CLIP that support multimodal input implement this interface
 * directly.</p>
 *
 * <p>Use {@link #fromTextOnly(EmbeddingFunction)} to wrap any text-only embedding function
 * as a {@code ContentEmbeddingFunction}.</p>
 */
public interface ContentEmbeddingFunction {

    /**
     * Embeds multiple content objects into dense vectors.
     *
     * @param contents the content objects to embed
     * @return a list of embeddings, one per content object
     * @throws EFException if embedding fails
     */
    List<Embedding> embedContents(List<Content> contents) throws EFException;

    /**
     * Embeds a single content object into a dense vector.
     *
     * <p>Default implementation delegates to {@link #embedContents(List)}.</p>
     *
     * @param content the content to embed
     * @return the embedding
     * @throws EFException if embedding fails
     */
    default Embedding embedContent(Content content) throws EFException {
        List<Embedding> embeddings = embedContents(Collections.singletonList(content));
        if (embeddings == null || embeddings.isEmpty()) {
            throw new EFException("embedContents returned no embeddings");
        }
        return embeddings.get(0);
    }

    /**
     * Wraps a text-only {@link EmbeddingFunction} as a {@code ContentEmbeddingFunction}.
     *
     * <p>The returned adapter extracts text parts from each {@link Content} and delegates
     * to the wrapped function.</p>
     *
     * @param ef the text-only embedding function to wrap
     * @return a content embedding function that delegates to the text-only function
     */
    static ContentEmbeddingFunction fromTextOnly(EmbeddingFunction ef) {
        return new TextEmbeddingAdapter(ef);
    }
}
