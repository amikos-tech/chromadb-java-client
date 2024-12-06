package tech.amikos.chromadb.api;

public class TextID implements ID   {
    private String id;

    public TextID(String id) {
        this.id = id;
    }


    @Override
    public String asString() {
        return id;
    }

    @Override
    public byte[] asBytes() {
        return id.getBytes();
    }
}
