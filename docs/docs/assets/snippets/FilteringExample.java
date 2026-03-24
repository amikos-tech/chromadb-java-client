import tech.amikos.chromadb.v2.*;

import java.util.Arrays;
import java.util.List;

// --8<-- [start:metadata-eq]
// Equality filter on string metadata
Where byType = Where.eq("type", "scientist");

// Equality filter on integer metadata
Where byCount = Where.eq("count", 42);
// --8<-- [end:metadata-eq]

// --8<-- [start:metadata-comparison]
// Greater than
Where expensive = Where.gt("price", 10.0f);

// Greater than or equal
Where atLeastTen = Where.gte("price", 10.0f);

// Less than
Where cheap = Where.lt("price", 5.0f);

// Less than or equal
Where atMostFive = Where.lte("price", 5.0f);
// --8<-- [end:metadata-comparison]

// --8<-- [start:metadata-ne]
// Not equal
Where notDeleted = Where.ne("status", "deleted");
// --8<-- [end:metadata-ne]

// --8<-- [start:metadata-in]
// Set inclusion
Where inCategories = Where.in("category", "electronics", "books");

// Set exclusion
Where notArchived = Where.nin("status", "archived", "deleted");
// --8<-- [end:metadata-in]

// --8<-- [start:metadata-contains]
// Array containment: metadata value (array) contains this element
Where hasTag = Where.contains("tags", "urgent");

// Array non-containment
Where noSpam = Where.notContains("tags", "spam");
// --8<-- [end:metadata-contains]

// --8<-- [start:id-filter]
// Filter by specific IDs (alternative to ids() on the builder)
Where specificIds = Where.idIn("id-1", "id-2", "id-3");

// Exclude specific IDs
Where excludeIds = Where.idNotIn("id-bad-1", "id-bad-2");
// --8<-- [end:id-filter]

// --8<-- [start:document-inline]
// Inline document filter (Cloud-oriented — may be rejected by local Chroma)
Where docContains = Where.documentContains("search term");
Where docNotContains = Where.documentNotContains("spam");
// --8<-- [end:document-inline]

// --8<-- [start:logical]
// AND: both conditions must match
Where both = Where.and(
        Where.eq("type", "A"),
        Where.gt("score", 0.5f)
);

// OR: either condition must match
Where either = Where.or(
        Where.eq("status", "active"),
        Where.eq("status", "pending")
);

// Chained combinator style
Where chained = Where.eq("type", "A").and(Where.gt("score", 0.5f));
// --8<-- [end:logical]

// --8<-- [start:where-document]
// Document content filter (local-compatible path)
WhereDocument contains = WhereDocument.contains("important topic");
WhereDocument notContains = WhereDocument.notContains("irrelevant");
WhereDocument matchesRegex = WhereDocument.regex("^Hello.*");
WhereDocument noRegex = WhereDocument.notRegex(".*spam.*");
// --8<-- [end:where-document]

// --8<-- [start:where-document-logical]
// AND: document must contain "hello" and not contain "bye"
WhereDocument docAnd = WhereDocument.and(
        WhereDocument.contains("hello"),
        WhereDocument.notContains("bye")
);

// OR: document matches either condition
WhereDocument docOr = WhereDocument.or(
        WhereDocument.contains("java"),
        WhereDocument.contains("python")
);
// --8<-- [end:where-document-logical]

// --8<-- [start:query-with-filters]
QueryResult results = collection.query()
        .queryTexts("machine learning")
        .nResults(10)
        .where(Where.and(
                Where.eq("category", "tech"),
                Where.gte("year", 2020)
        ))
        .whereDocument(WhereDocument.contains("neural"))
        .include(Include.DOCUMENTS, Include.METADATAS, Include.DISTANCES)
        .execute();
// --8<-- [end:query-with-filters]
