package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/** Result from a get operation. Flat lists of matching records. */
public interface GetResult {

    List<String> getIds();

    List<String> getDocuments();

    List<Map<String, Object>> getMetadatas();

    List<float[]> getEmbeddings();
}
