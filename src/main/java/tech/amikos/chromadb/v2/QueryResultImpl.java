package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class QueryResultImpl implements QueryResult {

    private final List<List<String>> ids;
    private final List<List<String>> documents;
    private final List<List<Map<String, Object>>> metadatas;
    private final List<List<float[]>> embeddings;
    private final List<List<Float>> distances;
    private final List<List<String>> uris;

    private QueryResultImpl(List<List<String>> ids, List<List<String>> documents,
                            List<List<Map<String, Object>>> metadatas,
                            List<List<float[]>> embeddings, List<List<Float>> distances,
                            List<List<String>> uris) {
        this.ids = immutableNestedList(ids);
        this.documents = immutableNestedList(documents);
        this.metadatas = immutableNestedMetadata(metadatas);
        this.embeddings = immutableNestedEmbeddings(embeddings);
        this.distances = immutableNestedList(distances);
        this.uris = immutableNestedList(uris);
    }

    static QueryResultImpl from(ChromaDtos.QueryResponse dto) {
        if (dto.ids == null) {
            throw new ChromaDeserializationException(
                    "Server returned query result without required ids field",
                    200
            );
        }
        List<List<float[]>> embeddings = null;
        if (dto.embeddings != null) {
            embeddings = new ArrayList<List<float[]>>(dto.embeddings.size());
            for (List<List<Float>> inner : dto.embeddings) {
                embeddings.add(ChromaDtos.toFloatArrays(inner));
            }
        }
        return new QueryResultImpl(
                dto.ids,
                dto.documents,
                dto.metadatas,
                embeddings,
                dto.distances,
                dto.uris
        );
    }

    @Override
    public List<List<String>> getIds() {
        return ids;
    }

    @Override
    public List<List<String>> getDocuments() {
        return documents;
    }

    @Override
    public List<List<Map<String, Object>>> getMetadatas() {
        return metadatas;
    }

    @Override
    public List<List<float[]>> getEmbeddings() {
        return embeddings;
    }

    @Override
    public List<List<Float>> getDistances() {
        return distances;
    }

    @Override
    public List<List<String>> getUris() {
        return uris;
    }

    @Override
    public ResultGroup<QueryResultRow> rows(int queryIndex) {
        List<String> colIds = ids.get(queryIndex); // throws IOOBE if bad index
        List<QueryResultRow> result = new ArrayList<QueryResultRow>(colIds.size());
        for (int i = 0; i < colIds.size(); i++) {
            result.add(new QueryResultRowImpl(
                    colIds.get(i),
                    documents  == null ? null : documents.get(queryIndex).get(i),
                    metadatas  == null ? null : metadatas.get(queryIndex).get(i),
                    embeddings == null ? null : embeddings.get(queryIndex).get(i),
                    uris       == null ? null : uris.get(queryIndex).get(i),
                    distances  == null ? null : distances.get(queryIndex).get(i)
            ));
        }
        return new ResultGroupImpl<QueryResultRow>(result);
    }

    @Override
    public int groupCount() {
        return ids.size();
    }

    @Override
    public Stream<ResultGroup<QueryResultRow>> stream() {
        return IntStream.range(0, ids.size()).mapToObj(new IntFunction<ResultGroup<QueryResultRow>>() {
            @Override
            public ResultGroup<QueryResultRow> apply(int i) {
                return rows(i);
            }
        });
    }

    private static <T> List<List<T>> immutableNestedList(List<List<T>> source) {
        if (source == null) {
            return null;
        }
        List<List<T>> outer = new ArrayList<List<T>>(source.size());
        for (List<T> inner : source) {
            if (inner == null) {
                outer.add(null);
            } else {
                outer.add(Collections.unmodifiableList(new ArrayList<T>(inner)));
            }
        }
        return Collections.unmodifiableList(outer);
    }

    private static List<List<Map<String, Object>>> immutableNestedMetadata(List<List<Map<String, Object>>> source) {
        if (source == null) {
            return null;
        }
        List<List<Map<String, Object>>> outer = new ArrayList<List<Map<String, Object>>>(source.size());
        for (List<Map<String, Object>> inner : source) {
            if (inner == null) {
                outer.add(null);
                continue;
            }
            List<Map<String, Object>> innerCopy = new ArrayList<Map<String, Object>>(inner.size());
            for (Map<String, Object> metadata : inner) {
                innerCopy.add(metadata == null ? null : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata)));
            }
            outer.add(Collections.unmodifiableList(innerCopy));
        }
        return Collections.unmodifiableList(outer);
    }

    private static List<List<float[]>> immutableNestedEmbeddings(List<List<float[]>> source) {
        if (source == null) {
            return null;
        }
        List<List<float[]>> outer = new ArrayList<List<float[]>>(source.size());
        for (List<float[]> inner : source) {
            if (inner == null) {
                outer.add(null);
                continue;
            }
            List<float[]> innerCopy = new ArrayList<float[]>(inner.size());
            for (float[] embedding : inner) {
                innerCopy.add(embedding == null ? null : Arrays.copyOf(embedding, embedding.length));
            }
            outer.add(Collections.unmodifiableList(innerCopy));
        }
        return Collections.unmodifiableList(outer);
    }
}
