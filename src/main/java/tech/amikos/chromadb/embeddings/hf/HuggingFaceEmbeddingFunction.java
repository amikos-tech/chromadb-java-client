package tech.amikos.chromadb.embeddings.hf;


import org.jetbrains.annotations.NotNull;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;

import java.util.List;
import java.util.stream.Collectors;

public class HuggingFaceEmbeddingFunction implements EmbeddingFunction {
    private final String hfAPIKey;
    private final HuggingFaceClient client;

    public HuggingFaceEmbeddingFunction(String hfAPIKey) {
        this.hfAPIKey = hfAPIKey;
        this.client = new HuggingFaceClient(this.hfAPIKey);
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        CreateEmbeddingResponse response = this.client.createEmbedding(new CreateEmbeddingRequest().inputs(new String[]{query}));
        return new Embedding(response.getEmbeddings().get(0));
    }

    @Override
    public List<Embedding> embedDocuments(@NotNull List<String> documents) throws EFException {
        CreateEmbeddingResponse response = this.client.createEmbedding(new CreateEmbeddingRequest().inputs(documents.toArray(new String[0])));
        return response.getEmbeddings().stream().map(Embedding::fromList).collect(Collectors.toList());
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        CreateEmbeddingResponse response = this.client.createEmbedding(new CreateEmbeddingRequest().inputs(documents));
        return response.getEmbeddings().stream().map(Embedding::fromList).collect(Collectors.toList());
    }
}
