package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Package-private immutable implementation of {@link SearchResult}.
 *
 * <p>Supports both column-oriented and row-oriented access patterns. Rows are lazily constructed
 * and cached per search index using an {@link AtomicReferenceArray}.</p>
 */
final class SearchResultImpl implements SearchResult {

    private final List<List<String>> ids;
    private final List<List<String>> documents;
    private final List<List<Map<String, Object>>> metadatas;
    private final List<List<float[]>> embeddings;
    private final List<List<Double>> scores;

    private final AtomicReferenceArray<ResultGroup<SearchResultRow>> cachedRows;

    private SearchResultImpl(List<List<String>> ids, List<List<String>> documents,
                             List<List<Map<String, Object>>> metadatas,
                             List<List<float[]>> embeddings, List<List<Double>> scores) {
        this.ids = ImmutableCopyUtils.nestedList(ids);
        this.documents = ImmutableCopyUtils.nestedList(documents);
        this.metadatas = ImmutableCopyUtils.nestedMetadata(metadatas);
        this.embeddings = ImmutableCopyUtils.nestedEmbeddings(embeddings);
        this.scores = ImmutableCopyUtils.nestedList(scores);
        this.cachedRows = new AtomicReferenceArray<ResultGroup<SearchResultRow>>(this.ids.size());
    }

    static SearchResultImpl from(ChromaDtos.SearchResponse dto) {
        if (dto == null) {
            throw new ChromaDeserializationException(
                    "Server returned an empty search response payload",
                    200
            );
        }
        if (dto.ids == null) {
            throw new ChromaDeserializationException(
                    "Server returned search result without required ids field",
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
        return new SearchResultImpl(
                dto.ids,
                dto.documents,
                dto.metadatas,
                embeddings,
                dto.scores
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
    public List<List<Double>> getScores() {
        return scores;
    }

    @Override
    public ResultGroup<SearchResultRow> rows(int searchIndex) {
        checkSearchIndex(searchIndex);
        ResultGroup<SearchResultRow> r = cachedRows.get(searchIndex);
        if (r == null) {
            List<String> colIds = ids.get(searchIndex);
            List<Double> rowScores = scores == null ? null : scores.get(searchIndex);
            List<String> docList = documents == null ? null : documents.get(searchIndex);
            List<Map<String, Object>> metaList = metadatas == null ? null : metadatas.get(searchIndex);
            List<float[]> embList = embeddings == null ? null : embeddings.get(searchIndex);

            List<SearchResultRow> result = new ArrayList<SearchResultRow>(colIds.size());
            for (int i = 0; i < colIds.size(); i++) {
                Double score = (rowScores != null && rowScores.get(i) != null)
                        ? rowScores.get(i) : null;
                result.add(new SearchResultRowImpl(
                        colIds.get(i),
                        docList == null ? null : docList.get(i),
                        metaList == null ? null : metaList.get(i),
                        embList == null ? null : embList.get(i),
                        null,
                        score
                ));
            }
            r = new ResultGroupImpl<SearchResultRow>(result);
            cachedRows.compareAndSet(searchIndex, null, r);
            r = cachedRows.get(searchIndex);
        }
        return r;
    }

    @Override
    public int searchCount() {
        return ids.size();
    }

    @Override
    public Stream<ResultGroup<SearchResultRow>> stream() {
        return IntStream.range(0, ids.size()).mapToObj(this::rows);
    }

    private void checkSearchIndex(int searchIndex) {
        if (searchIndex < 0 || searchIndex >= ids.size()) {
            throw new IndexOutOfBoundsException(
                    "searchIndex " + searchIndex + " out of range [0, " + ids.size() + ")");
        }
    }
}
