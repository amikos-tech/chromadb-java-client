package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/** Result from a query operation. Contains per-query-text result groups. */
public interface QueryResult {

    List<List<String>> getIds();

    List<List<String>> getDocuments();

    List<List<Map<String, Object>>> getMetadatas();

    List<List<float[]>> getEmbeddings();

    List<List<Float>> getDistances();
}
