package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for Search API DTOs: Knn, Rrf, Search, GroupBy, ReadLevel, and wire-format
 * serialization via ChromaDtos helper methods.
 */
@SuppressWarnings("unchecked")
public class SearchApiUnitTest {

    // ========== KNN tests (SEARCH-01) ==========

    @Test
    public void testKnnQueryText() {
        Knn knn = Knn.queryText("headphones");
        assertEquals("headphones", knn.getQuery());
        assertEquals("#embedding", knn.getKey());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        assertTrue("should have 'knn' key", map.containsKey("knn"));
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        assertEquals("headphones", inner.get("query"));
        assertEquals("#embedding", inner.get("key"));
    }

    @Test
    public void testKnnQueryEmbedding() {
        float[] emb = {0.1f, 0.2f};
        Knn knn = Knn.queryEmbedding(emb);
        assertTrue("query should be float[]", knn.getQuery() instanceof float[]);
        assertEquals("#embedding", knn.getKey());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        Object query = inner.get("query");
        assertTrue("serialized query should be a List", query instanceof List);
        List<Float> queryList = (List<Float>) query;
        assertEquals(2, queryList.size());
        assertEquals(0.1f, queryList.get(0), 1e-6f);
        assertEquals(0.2f, queryList.get(1), 1e-6f);
    }

    @Test
    public void testKnnQuerySparseVector() {
        SparseVector sv = SparseVector.of(new int[]{1, 5}, new float[]{0.3f, 0.7f});
        Knn knn = Knn.querySparseVector(sv);
        assertTrue("query should be SparseVector", knn.getQuery() instanceof SparseVector);
        // key defaults to null for sparse
        assertNull("key should be null for sparse vector knn", knn.getKey());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        Object query = inner.get("query");
        assertTrue("serialized sparse query should be a Map", query instanceof Map);
        Map<String, Object> svMap = (Map<String, Object>) query;
        List<Integer> indices = (List<Integer>) svMap.get("indices");
        List<Float> values = (List<Float>) svMap.get("values");
        assertNotNull(indices);
        assertNotNull(values);
        assertEquals(Integer.valueOf(1), indices.get(0));
        assertEquals(Integer.valueOf(5), indices.get(1));
        assertEquals(0.3f, values.get(0), 1e-6f);
        assertEquals(0.7f, values.get(1), 1e-6f);
    }

    @Test
    public void testKnnWithLimit() {
        Knn knn = Knn.queryText("test").limit(10);
        assertEquals(Integer.valueOf(10), knn.getLimit());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        assertEquals(10, inner.get("limit"));
    }

    @Test
    public void testKnnWithReturnRank() {
        Knn knn = Knn.queryText("test").returnRank(true);
        assertTrue(knn.isReturnRank());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        assertEquals(Boolean.TRUE, inner.get("return_rank"));
    }

    @Test
    public void testKnnReturnRankFalseByDefault() {
        Knn knn = Knn.queryText("test");
        assertFalse("returnRank should default to false", knn.isReturnRank());

        Map<String, Object> map = ChromaDtos.buildKnnRankMap(knn);
        Map<String, Object> inner = (Map<String, Object>) map.get("knn");
        assertFalse("return_rank should not appear in map when false", inner.containsKey("return_rank"));
    }

    @Test
    public void testKnnImmutability() {
        Knn original = Knn.queryText("test");
        Knn withLimit = original.limit(5);
        // original should be unchanged
        assertNull("original limit should still be null", original.getLimit());
        assertEquals(Integer.valueOf(5), withLimit.getLimit());
    }

    // ========== RRF tests (SEARCH-02) ==========

    @Test
    public void testRrfDtoStructure() {
        Knn knn1 = Knn.queryText("wireless audio");
        Knn knn2 = Knn.queryText("noise cancelling headphones");
        Rrf rrf = Rrf.builder()
                .rank(knn1, 0.7)
                .rank(knn2, 0.3)
                .k(60)
                .build();

        Map<String, Object> map = ChromaDtos.buildRrfRankMap(rrf);
        assertTrue("should have 'rrf' key", map.containsKey("rrf"));
        Map<String, Object> rrfMap = (Map<String, Object>) map.get("rrf");
        List<Map<String, Object>> ranks = (List<Map<String, Object>>) rrfMap.get("ranks");
        assertNotNull(ranks);
        assertEquals("should have 2 ranks", 2, ranks.size());
        assertEquals(60, rrfMap.get("k"));

        Map<String, Object> rank0 = ranks.get(0);
        assertEquals(0.7, (Double) rank0.get("weight"), 1e-9);
        assertTrue("rank entry should have 'rank' key containing knn map",
                ((Map<String, Object>) rank0.get("rank")).containsKey("knn"));
    }

    @Test
    public void testRrfAutoSetsReturnRank() {
        Knn knn = Knn.queryText("test");
        assertFalse("returnRank should be false before adding to Rrf", knn.isReturnRank());

        Rrf rrf = Rrf.builder().rank(knn, 1.0).build();
        // The inner Knn stored in Rrf should have returnRank=true
        Rrf.RankWithWeight rw = rrf.getRanks().get(0);
        assertTrue("Rrf.Builder.rank() should auto-set returnRank=true", rw.getKnn().isReturnRank());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRrfEmptyRanksThrows() {
        Rrf.builder().k(60).build();
    }

    @Test
    public void testRrfDefaultK() {
        Knn knn = Knn.queryText("test");
        Rrf rrf = Rrf.builder().rank(knn, 1.0).build();
        assertEquals("default k should be 60", 60, rrf.getK());
    }

    // ========== Search builder tests ==========

    @Test
    public void testSearchWithKnn() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder().knn(knn).build();
        assertNotNull("knn should not be null", search.getKnn());
        assertNull("rrf should be null when knn is set", search.getRrf());
    }

    @Test
    public void testSearchWithRrf() {
        Knn knn = Knn.queryText("test");
        Rrf rrf = Rrf.builder().rank(knn, 1.0).build();
        Search search = Search.builder().rrf(rrf).build();
        assertNotNull("rrf should not be null", search.getRrf());
        assertNull("knn should be null when rrf is set", search.getKnn());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSearchRequiresRank() {
        Search.builder().limit(5).build();
    }

    @Test
    public void testSearchWithSelectProjection() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder()
                .knn(knn)
                .select(Select.ID, Select.SCORE, Select.key("title"))
                .build();
        List<Select> sel = search.getSelect();
        assertNotNull(sel);
        assertEquals(3, sel.size());
        assertEquals("#id", sel.get(0).getKey());
        assertEquals("#score", sel.get(1).getKey());
        assertEquals("title", sel.get(2).getKey());
    }

    @Test
    public void testSearchWithSelectAll() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder().knn(knn).selectAll().build();
        List<Select> sel = search.getSelect();
        assertNotNull(sel);
        assertEquals("selectAll should include all 5 standard fields", 5, sel.size());
    }

    @Test
    public void testSearchWithGroupBy() {
        Knn knn = Knn.queryText("test");
        GroupBy groupBy = GroupBy.builder().key("category").minK(1).maxK(3).build();
        Search search = Search.builder().knn(knn).groupBy(groupBy).build();
        assertNotNull("groupBy should not be null", search.getGroupBy());
        assertEquals("category", search.getGroupBy().getKey());
    }

    // ========== Wire format via buildSearchItemMap (SEARCH-01, SEARCH-03, SEARCH-04) ==========

    @Test
    public void testBuildSearchItemMapKnn() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder().knn(knn).build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, null);
        assertTrue("item should have 'rank' key", item.containsKey("rank"));
        Map<String, Object> rank = (Map<String, Object>) item.get("rank");
        assertTrue("rank should contain 'knn'", rank.containsKey("knn"));
    }

    @Test
    public void testBuildSearchItemMapWithFilter() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder()
                .knn(knn)
                .where(Where.eq("color", "red"))
                .build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, null);
        assertTrue("should use 'filter' key (not 'where')", item.containsKey("filter"));
        assertFalse("should NOT use 'where' key", item.containsKey("where"));
        Map<String, Object> filter = (Map<String, Object>) item.get("filter");
        assertNotNull(filter);
        assertTrue("filter should have 'color' key", filter.containsKey("color"));
    }

    @Test
    public void testBuildSearchItemMapMergesGlobalFilter() {
        Knn knn = Knn.queryText("test");
        Where perSearch = Where.eq("color", "red");
        Where global = Where.eq("brand", "sony");
        Search search = Search.builder().knn(knn).where(perSearch).build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, global);
        Map<String, Object> filter = (Map<String, Object>) item.get("filter");
        assertNotNull(filter);
        assertTrue("merged filter should contain per-search key", filter.containsKey("color"));
        assertTrue("merged filter should contain global key", filter.containsKey("brand"));
    }

    @Test
    public void testBuildSearchItemMapSelect() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder()
                .knn(knn)
                .select(Select.ID, Select.SCORE)
                .build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, null);
        assertTrue("should have 'select' key", item.containsKey("select"));
        Map<String, Object> sel = (Map<String, Object>) item.get("select");
        List<String> keys = (List<String>) sel.get("keys");
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("#id"));
        assertTrue(keys.contains("#score"));
    }

    @Test
    public void testBuildSearchItemMapLimitOffset() {
        Knn knn = Knn.queryText("test");
        Search search = Search.builder().knn(knn).limit(5).offset(10).build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, null);
        assertTrue("should have 'limit' key", item.containsKey("limit"));
        Map<String, Object> page = (Map<String, Object>) item.get("limit");
        assertEquals(5, page.get("limit"));
        assertEquals(10, page.get("offset"));
    }

    @Test
    public void testBuildSearchItemMapGroupBy() {
        Knn knn = Knn.queryText("test");
        GroupBy groupBy = GroupBy.builder().key("category").minK(1).maxK(3).build();
        Search search = Search.builder().knn(knn).groupBy(groupBy).build();
        Map<String, Object> item = ChromaDtos.buildSearchItemMap(search, null);
        assertTrue("should have 'group_by' key", item.containsKey("group_by"));
        Map<String, Object> gb = (Map<String, Object>) item.get("group_by");
        assertEquals("category", gb.get("key"));
        assertEquals(1, gb.get("min_k"));
        assertEquals(3, gb.get("max_k"));
    }

    // ========== ReadLevel tests (SEARCH-04) ==========

    @Test
    public void testReadLevelWireValues() {
        assertEquals("index_and_wal", ReadLevel.INDEX_AND_WAL.getValue());
        assertEquals("index_only", ReadLevel.INDEX_ONLY.getValue());
    }

    @Test
    public void testReadLevelFromValue() {
        assertEquals(ReadLevel.INDEX_AND_WAL, ReadLevel.fromValue("index_and_wal"));
        assertEquals(ReadLevel.INDEX_ONLY, ReadLevel.fromValue("index_only"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadLevelFromValueUnknownThrows() {
        ReadLevel.fromValue("unknown_level");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadLevelFromValueNullThrows() {
        ReadLevel.fromValue(null);
    }

    // ========== GroupBy tests (SEARCH-04) ==========

    @Test
    public void testGroupByBuilder() {
        GroupBy gb = GroupBy.builder().key("category").minK(1).maxK(3).build();
        assertEquals("category", gb.getKey());
        assertEquals(Integer.valueOf(1), gb.getMinK());
        assertEquals(Integer.valueOf(3), gb.getMaxK());
    }

    @Test
    public void testGroupByOptionalFields() {
        GroupBy gb = GroupBy.builder().key("tag").build();
        assertEquals("tag", gb.getKey());
        assertNull("minK should be null when not set", gb.getMinK());
        assertNull("maxK should be null when not set", gb.getMaxK());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupByNullKeyThrows() {
        GroupBy.builder().build();
    }
}
