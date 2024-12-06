package tech.amikos.chromadb.api;

import java.util.List;

public interface Embedding {
    Float[] asArray();
    Integer dimensions();
    List<Float> asList();
}
