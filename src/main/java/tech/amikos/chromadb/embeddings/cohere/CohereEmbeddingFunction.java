package tech.amikos.chromadb.embeddings.cohere;

import org.jetbrains.annotations.NotNull;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;

import java.util.List;
import java.util.stream.Collectors;

public class CohereEmbeddingFunction implements EmbeddingFunction {

    private final CohereClient client;

    public CohereEmbeddingFunction(String cohereAPIKey) {
        this.client = new CohereClient(cohereAPIKey);

    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(new String[]{query}));
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(@NotNull List<String> documents) throws EFException{
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(documents.toArray(new String[0])));
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException{
        CreateEmbeddingResponse response = client.createEmbedding(new CreateEmbeddingRequest().texts(documents));
        return response.getEmbeddings().stream().map(Embedding::new).collect(Collectors.toList());
    }
}
