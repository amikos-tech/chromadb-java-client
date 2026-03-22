package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    private final boolean grouped;

    private final AtomicReferenceArray<ResultGroup<SearchResultRow>> cachedRows;

    private SearchResultImpl(List<List<String>> ids, List<List<String>> documents,
                             List<List<Map<String, Object>>> metadatas,
                             List<List<float[]>> embeddings, List<List<Double>> scores,
                             boolean grouped) {
        this.ids = immutableNestedList(ids);
        this.documents = immutableNestedList(documents);
        this.metadatas = immutableNestedMetadata(metadatas);
        this.embeddings = immutableNestedEmbeddings(embeddings);
        this.scores = immutableNestedList(scores);
        this.grouped = grouped;
        this.cachedRows = new AtomicReferenceArray<ResultGroup<SearchResultRow>>(this.ids.size());
    }

    static SearchResultImpl from(ChromaDtos.SearchResponse dto, boolean grouped) {
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
                dto.scores,
                grouped
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
        ResultGroup<SearchResultRow> r = cachedRows.get(searchIndex);
        if (r == null) {
            List<String> colIds = ids.get(searchIndex);
            List<SearchResultRow> result = new ArrayList<SearchResultRow>(colIds.size());
            for (int i = 0; i < colIds.size(); i++) {
                Float score = null;
                if (scores != null) {
                    List<Double> rowScores = scores.get(searchIndex);
                    if (rowScores != null && rowScores.get(i) != null) {
                        score = rowScores.get(i).floatValue();
                    }
                }
                List<String> docList = documents == null ? null : documents.get(searchIndex);
                List<Map<String, Object>> metaList = metadatas == null ? null : metadatas.get(searchIndex);
                List<float[]> embList = embeddings == null ? null : embeddings.get(searchIndex);
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
    public List<SearchResultGroup> groups(int searchIndex) {
        if (!grouped) {
            throw new IllegalStateException(
                    "Search result is not grouped — use rows(searchIndex) instead");
        }
        // Each result row is returned as a single-element group with key=null.
        // Group key extraction depends on server response format; refined in integration tests.
        ResultGroup<SearchResultRow> rowGroup = rows(searchIndex);
        List<SearchResultGroup> groups = new ArrayList<SearchResultGroup>(rowGroup.size());
        for (int i = 0; i < rowGroup.size(); i++) {
            final SearchResultRow row = rowGroup.get(i);
            List<SearchResultRow> singleRow = Collections.singletonList(row);
            groups.add(new SearchResultGroupImpl(null,
                    new ResultGroupImpl<SearchResultRow>(singleRow)));
        }
        return Collections.unmodifiableList(groups);
    }

    @Override
    public boolean isGrouped() {
        return grouped;
    }

    @Override
    public int groupCount() {
        return ids.size();
    }

    @Override
    public Stream<ResultGroup<SearchResultRow>> stream() {
        return IntStream.range(0, ids.size()).mapToObj(this::rows);
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
                innerCopy.add(metadata == null
                        ? null
                        : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata)));
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
