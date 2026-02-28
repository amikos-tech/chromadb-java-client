package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GetResultImpl implements GetResult {

    private final List<String> ids;
    private final List<String> documents;
    private final List<Map<String, Object>> metadatas;
    private final List<float[]> embeddings;
    private final List<String> uris;

    private GetResultImpl(List<String> ids, List<String> documents,
                          List<Map<String, Object>> metadatas,
                          List<float[]> embeddings, List<String> uris) {
        this.ids = immutableList(ids);
        this.documents = immutableList(documents);
        this.metadatas = immutableMetadataList(metadatas);
        this.embeddings = immutableEmbeddings(embeddings);
        this.uris = immutableList(uris);
    }

    static GetResultImpl from(ChromaDtos.GetResponse dto) {
        if (dto.ids == null) {
            throw new ChromaDeserializationException(
                    "Server returned get result without required ids field",
                    200
            );
        }
        return new GetResultImpl(
                dto.ids,
                dto.documents,
                dto.metadatas,
                ChromaDtos.toFloatArrays(dto.embeddings),
                dto.uris
        );
    }

    @Override
    public List<String> getIds() {
        return ids;
    }

    @Override
    public List<String> getDocuments() {
        return documents;
    }

    @Override
    public List<Map<String, Object>> getMetadatas() {
        return metadatas;
    }

    @Override
    public List<float[]> getEmbeddings() {
        return embeddings;
    }

    @Override
    public List<String> getUris() {
        return uris;
    }

    private static <T> List<T> immutableList(List<T> list) {
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(new ArrayList<T>(list));
    }

    private static List<Map<String, Object>> immutableMetadataList(List<Map<String, Object>> metadatas) {
        if (metadatas == null) {
            return null;
        }
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>(metadatas.size());
        for (Map<String, Object> metadata : metadatas) {
            copy.add(metadata == null ? null : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata)));
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<float[]> immutableEmbeddings(List<float[]> embeddings) {
        if (embeddings == null) {
            return null;
        }
        List<float[]> copy = new ArrayList<float[]>(embeddings.size());
        for (float[] embedding : embeddings) {
            copy.add(embedding == null ? null : Arrays.copyOf(embedding, embedding.length));
        }
        return Collections.unmodifiableList(copy);
    }
}
