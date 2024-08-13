package tech.amikos.chromadb;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Embedding {
    private final float[] embedding;

    public Embedding(float[] embeddings) {
        this.embedding = embeddings;
    }

    public Embedding(List<? extends Number> embedding) {
        this.embedding = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            //TODO what if embeddings are integers?
            this.embedding[i] = embedding.get(i).floatValue();
        }
    }


    public List<Float> asList() {
        return IntStream.range(0, embedding.length)
                .mapToObj(i -> embedding[i])
                .collect(Collectors.toList());

    }

    public int getDimensions() {
        return embedding.length;
    }

    public float[] asArray() {
        return embedding;
    }

    public static Embedding fromList(List<Float> embedding) {
        return new Embedding(embedding);
    }

    public static Embedding fromArray(float[] embedding) {
        return new Embedding(embedding);
    }
}
