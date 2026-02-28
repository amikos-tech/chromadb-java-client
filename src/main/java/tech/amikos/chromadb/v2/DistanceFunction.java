package tech.amikos.chromadb.v2;

/** HNSW distance metric. */
public enum DistanceFunction {
    COSINE("cosine"),
    L2("l2"),
    IP("ip");

    private final String value;

    DistanceFunction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
