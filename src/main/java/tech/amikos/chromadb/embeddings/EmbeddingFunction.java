package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;

import java.util.List;

public interface EmbeddingFunction {

    Embedding embedQuery(String query) throws EFException;

    List<Embedding> embedDocuments(List<String> documents) throws EFException;

    List<Embedding> embedDocuments(String[] documents) throws EFException;
}
