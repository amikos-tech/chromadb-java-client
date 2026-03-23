package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class QueryResultImpl implements QueryResult {

    private final List<List<String>> ids;
    private final List<List<String>> documents;
    private final List<List<Map<String, Object>>> metadatas;
    private final List<List<float[]>> embeddings;
    private final List<List<Float>> distances;
    private final List<List<String>> uris;

    private final AtomicReferenceArray<ResultGroup<QueryResultRow>> cachedRows;

    private QueryResultImpl(List<List<String>> ids, List<List<String>> documents,
                            List<List<Map<String, Object>>> metadatas,
                            List<List<float[]>> embeddings, List<List<Float>> distances,
                            List<List<String>> uris) {
        this.ids = ImmutableCopyUtils.nestedList(ids);
        this.documents = ImmutableCopyUtils.nestedList(documents);
        this.metadatas = ImmutableCopyUtils.nestedMetadata(metadatas);
        this.embeddings = ImmutableCopyUtils.nestedEmbeddings(embeddings);
        this.distances = ImmutableCopyUtils.nestedList(distances);
        this.uris = ImmutableCopyUtils.nestedList(uris);
        this.cachedRows = new AtomicReferenceArray<ResultGroup<QueryResultRow>>(this.ids.size());
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
        ResultGroup<QueryResultRow> r = cachedRows.get(queryIndex); // throws IOOBE if bad index
        if (r == null) {
            List<String> colIds = ids.get(queryIndex);
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
            r = new ResultGroupImpl<QueryResultRow>(result);
            cachedRows.compareAndSet(queryIndex, null, r);
            r = cachedRows.get(queryIndex);
        }
        return r;
    }

    @Override
    public int groupCount() {
        return ids.size();
    }

    @Override
    public Stream<ResultGroup<QueryResultRow>> stream() {
        return IntStream.range(0, ids.size()).mapToObj(this::rows);
    }

}
