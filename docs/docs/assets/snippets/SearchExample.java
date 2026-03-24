import tech.amikos.chromadb.v2.*;

// --8<-- [start:knn-text]
// KNN search by text (server-side embedding via collection's embedding function)
SearchResult result = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("search term").limit(10))
                        .build()
        )
        .execute();
// --8<-- [end:knn-text]

// --8<-- [start:knn-embedding]
// KNN search by pre-computed embedding vector
SearchResult byEmbedding = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryEmbedding(new float[]{0.1f, 0.2f, 0.3f}).limit(5))
                        .build()
        )
        .execute();
// --8<-- [end:knn-embedding]

// --8<-- [start:knn-with-filter]
// KNN with metadata filter and field projection
SearchResult filtered = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("machine learning").limit(10))
                        .where(Where.eq("category", "tech"))
                        .select(Select.ID, Select.DOCUMENT, Select.SCORE, Select.METADATA)
                        .build()
        )
        .execute();
// --8<-- [end:knn-with-filter]

// --8<-- [start:rrf]
// RRF fuses multiple KNN sub-rankings into a single ranked list
SearchResult rrfResult = collection.search()
        .searches(
                Search.builder()
                        .rrf(Rrf.builder()
                                .rank(Knn.queryText("term1"), 1.0)
                                .rank(Knn.queryText("term2"), 0.5)
                                .k(60)
                                .build())
                        .build()
        )
        .execute();
// --8<-- [end:rrf]

// --8<-- [start:group-by]
// Group results by a metadata key, capping 5 results per group
SearchResult grouped = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("topic").limit(50))
                        .groupBy(GroupBy.builder()
                                .key("category")
                                .maxK(5)
                                .minK(1)
                                .build())
                        .build()
        )
        .execute();
// --8<-- [end:group-by]

// --8<-- [start:read-level]
// INDEX_AND_WAL includes recently written records not yet indexed
SearchResult fresh = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("recent data").limit(10))
                        .build()
        )
        .readLevel(ReadLevel.INDEX_AND_WAL)
        .execute();

// INDEX_ONLY reads only from the persisted index (faster, potentially stale)
SearchResult fast = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("historical data").limit(10))
                        .build()
        )
        .readLevel(ReadLevel.INDEX_ONLY)
        .execute();
// --8<-- [end:read-level]

// --8<-- [start:search-result]
SearchResult sr = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("example").limit(3))
                        .select(Select.ID, Select.DOCUMENT, Select.SCORE)
                        .build()
        )
        .execute();

// Row-based access for the first search input
for (SearchResultRow row : sr.rows(0)) {
    System.out.println(row.getId() + " score=" + row.getScore()
            + " doc=" + row.getDocument());
}
// --8<-- [end:search-result]

// --8<-- [start:batch-search]
// Multiple searches in one call — each produces an independent result group
SearchResult batch = collection.search()
        .searches(
                Search.builder()
                        .knn(Knn.queryText("first query").limit(5))
                        .build(),
                Search.builder()
                        .knn(Knn.queryText("second query").limit(5))
                        .build()
        )
        .execute();

// Access results by search input index
for (SearchResultRow row : batch.rows(0)) {
    System.out.println("Query 1: " + row.getId());
}
for (SearchResultRow row : batch.rows(1)) {
    System.out.println("Query 2: " + row.getId());
}
// --8<-- [end:batch-search]
