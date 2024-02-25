package tech.amikos.chromadb;

public enum HnswDistanceFunction {
    L2("l2"),
    IP("ip"),

    COSINE("cosine");

    private final String value;

    HnswDistanceFunction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
