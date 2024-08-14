package tech.amikos.chromadb;

import java.util.List;

public interface EmbeddingFunction {

    Embedding embedQuery(String query) throws EFException;

    List<Embedding> embedDocuments(List<String> documents) throws EFException;

    List<Embedding> embedDocuments(String[] documents) throws EFException;

}
