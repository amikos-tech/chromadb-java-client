import tech.amikos.chromadb.v2.*;

import java.util.Collections;

public class SchemaExample {

    public static void main(String[] args) {
        Client client = ChromaClient.builder()
                .baseUrl(System.getenv("CHROMA_URL"))
                .build();

        // --8<-- [start:basic-schema]
        Schema schema = Schema.builder()
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        // --8<-- [end:basic-schema]

        // --8<-- [start:with-ef-spec]
        Schema schemaWithEf = Schema.builder()
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .embeddingFunction(EmbeddingFunctionSpec.builder()
                                                        .type("known")
                                                        .name("openai")
                                                        .config(Collections.<String, Object>singletonMap(
                                                                "api_key_env_var", "OPENAI_API_KEY"))
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        // --8<-- [end:with-ef-spec]

        // --8<-- [start:cmek]
        Schema schemaWithCmek = Schema.builder()
                .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                        .floatList(FloatListValueType.builder()
                                .vectorIndex(VectorIndexType.builder()
                                        .config(VectorIndexConfig.builder()
                                                .space(DistanceFunction.COSINE)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .cmek(Cmek.gcpKms(
                        "projects/my-project/locations/us-central1/keyRings/my-keyring/cryptoKeys/my-key"))
                .build();
        // --8<-- [end:cmek]

        // --8<-- [start:create-with-schema]
        Collection collection = client.getOrCreateCollection(
                "my-schema-collection",
                CreateCollectionOptions.builder()
                        .schema(schema)
                        .build()
        );
        // --8<-- [end:create-with-schema]

        // --8<-- [start:hnsw-config]
        CollectionConfiguration config = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(16)
                .hnswConstructionEf(100)
                .hnswSearchEf(50)
                .build();

        Collection hnswCollection = client.getOrCreateCollection(
                "my-hnsw-collection",
                CreateCollectionOptions.builder()
                        .configuration(config)
                        .build()
        );
        // --8<-- [end:hnsw-config]
    }
}
