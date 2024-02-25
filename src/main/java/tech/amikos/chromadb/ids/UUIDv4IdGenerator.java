package tech.amikos.chromadb.ids;

public class UUIDv4IdGenerator implements IdGenerator {
    @Override
    public String generateForDocument(String document) {
        return java.util.UUID.randomUUID().toString();
    }
}
