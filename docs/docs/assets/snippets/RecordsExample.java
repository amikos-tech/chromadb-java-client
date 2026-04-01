import tech.amikos.chromadb.v2.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// --8<-- [start:add-docs]
Map<String, Object> meta1 = new HashMap<String, Object>();
meta1.put("type", "scientist");

Map<String, Object> meta2 = new HashMap<String, Object>();
meta2.put("type", "spy");

collection.add()
        .documents("Hello, my name is John. I am a Data Scientist.",
                   "Hello, my name is Bond. I am a Spy.")
        .metadatas(Arrays.<Map<String, Object>>asList(meta1, meta2))
        .ids("id-1", "id-2")
        .execute();
// --8<-- [end:add-docs]

// --8<-- [start:add-embeddings]
collection.add()
        .embeddings(new float[]{0.1f, 0.2f, 0.3f}, new float[]{0.4f, 0.5f, 0.6f})
        .ids("embed-1", "embed-2")
        .execute();
// --8<-- [end:add-embeddings]

// --8<-- [start:query-text]
QueryResult result = collection.query()
        .queryTexts("Who is the spy?")
        .nResults(5)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();
// --8<-- [end:query-text]

// --8<-- [start:query-filter]
QueryResult filtered = collection.query()
        .queryTexts("scientist")
        .nResults(5)
        .where(Where.eq("type", "scientist"))
        .execute();
// --8<-- [end:query-filter]

// --8<-- [start:query-embedding]
QueryResult byEmbedding = collection.query()
        .queryEmbeddings(new float[]{0.1f, 0.2f, 0.3f})
        .nResults(3)
        .execute();
// --8<-- [end:query-embedding]

// --8<-- [start:get]
GetResult all = collection.get()
        .include(Include.DOCUMENTS, Include.METADATAS)
        .execute();

GetResult byIds = collection.get()
        .ids("id-1", "id-2")
        .execute();
// --8<-- [end:get]

// --8<-- [start:update]
collection.update()
        .ids("id-1")
        .documents("Updated document text")
        .execute();
// --8<-- [end:update]

// --8<-- [start:upsert]
collection.upsert()
        .ids("id-3")
        .documents("New or updated document")
        .execute();
// --8<-- [end:upsert]

// --8<-- [start:delete]
collection.delete()
        .ids("id-1", "id-2")
        .execute();
// --8<-- [end:delete]

// --8<-- [start:count]
int count = collection.count();
// --8<-- [end:count]

// --8<-- [start:row-access]
QueryResult qr = collection.query()
        .queryTexts("example")
        .nResults(3)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();

// Row-based iteration
for (ResultRow row : qr.rows(0)) {
    System.out.println(row.getId() + ": " + row.getDocument());
}

// Index access
QueryResultRow first = (QueryResultRow) qr.rows(0).get(0);
System.out.println("Distance: " + first.getDistance());
// --8<-- [end:row-access]
