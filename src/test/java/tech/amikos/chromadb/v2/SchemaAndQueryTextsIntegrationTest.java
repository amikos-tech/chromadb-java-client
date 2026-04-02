package tech.amikos.chromadb.v2;

import org.junit.Test;
import org.junit.Assume;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaAndQueryTextsIntegrationTest extends AbstractChromaIntegrationTest {

    private static EmbeddingFunction fixedEmbeddingFunction(final float[] embedding) {
        return new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) throws EFException {
                return new Embedding(embedding);
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) throws EFException {
                List<Embedding> out = new ArrayList<Embedding>(documents.size());
                for (int i = 0; i < documents.size(); i++) {
                    out.add(new Embedding(embedding));
                }
                return out;
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) throws EFException {
                List<Embedding> out = new ArrayList<Embedding>(documents.length);
                for (int i = 0; i < documents.length; i++) {
                    out.add(new Embedding(embedding));
                }
                return out;
            }

            @Override
            public List<Embedding> embedQueries(List<String> queries) throws EFException {
                return embedDocuments(queries);
            }

            @Override
            public List<Embedding> embedQueries(String[] queries) throws EFException {
                return embedQueries(Arrays.asList(queries));
            }
        };
    }

    private static String uniqueCollectionName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    public void testCreateCollectionWithTopLevelSchemaRoundTrip() {
        assumeCloudChroma();
        String templateName = uniqueCollectionName("schema_template_");
        Collection template = client.createCollection(templateName);
        assertNotNull(template.getSchema());
        Schema schema = template.getSchema();

        String name = uniqueCollectionName("schema_top_");
        client.createCollection(name, CreateCollectionOptions.builder()
                .schema(schema)
                .build());
        Collection fetched = client.getCollection(name);

        assertNotNull(fetched.getSchema());
        assertNotNull(fetched.getSchema().getKey(Schema.EMBEDDING_KEY));
    }

    @Test
    public void testCreateCollectionWithConfigurationSchemaCompatibilityRoundTrip() {
        assumeCloudChroma();
        String templateName = uniqueCollectionName("schema_cfg_template_");
        Collection template = client.createCollection(templateName);
        assertNotNull(template.getSchema());
        Schema schema = template.getSchema();

        CollectionConfiguration configuration = CollectionConfiguration.builder()
                .schema(schema)
                .build();

        String name = uniqueCollectionName("schema_cfg_");
        client.createCollection(name, CreateCollectionOptions.builder()
                .configuration(configuration)
                .build());
        Collection fetched = client.getCollection(name);

        assertNotNull(fetched.getSchema());
        assertNotNull(fetched.getSchema().getKey(Schema.EMBEDDING_KEY));
    }

    @Test
    public void testConfigurationSchemaRoundTripPreservesEmptyDefaultsObject() {
        assumeCloudChroma();
        String templateName = uniqueCollectionName("schema_cfg_defaults_template_");
        Collection template = client.createCollection(templateName);
        assertNotNull(template.getSchema());

        Schema templateSchema = template.getSchema();
        Schema schema = Schema.builder()
                .defaults(ValueTypes.builder().build())
                .keys(templateSchema.getKeys())
                .cmek(templateSchema.getCmek())
                .build();

        CollectionConfiguration configuration = CollectionConfiguration.builder()
                .schema(schema)
                .build();

        String name = uniqueCollectionName("schema_cfg_defaults_");
        client.createCollection(name, CreateCollectionOptions.builder()
                .configuration(configuration)
                .build());

        Collection fetched = client.getCollection(name);
        assertNotNull(fetched.getSchema());
        assertNotNull(fetched.getSchema().getDefaults());
        // Chroma may normalize an empty defaults object into explicit default entries.
        assertNotNull(fetched.getSchema().getDefaults().getString());
    }

    @Test
    public void testQueryTextsWithExplicitCreateOptionsEmbeddingFunction() {
        String name = uniqueCollectionName("query_texts_create_");
        Collection col = client.createCollection(name, CreateCollectionOptions.builder()
                .embeddingFunction(fixedEmbeddingFunction(new float[]{1.0f, 0.0f, 0.0f}))
                .build());

        col.add()
                .ids("left", "right")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f}
                )
                .documents("left doc", "right doc")
                .execute();

        QueryResult result = col.query()
                .queryTexts("left side")
                .nResults(1)
                .execute();

        assertEquals(1, result.getIds().size());
        assertEquals(1, result.getIds().get(0).size());
        assertEquals("left", result.getIds().get(0).get(0));
    }

    @Test
    public void testQueryTextsWithExplicitGetCollectionEmbeddingFunction() {
        String name = uniqueCollectionName("query_texts_get_");
        Collection created = client.createCollection(name);
        created.add()
                .ids("left", "right")
                .embeddings(
                        new float[]{1.0f, 0.0f, 0.0f},
                        new float[]{0.0f, 1.0f, 0.0f}
                )
                .execute();

        Collection fetched = client.getCollection(
                name,
                fixedEmbeddingFunction(new float[]{0.0f, 1.0f, 0.0f})
        );
        QueryResult result = fetched.query()
                .queryTexts("right side")
                .nResults(1)
                .execute();

        assertEquals(1, result.getIds().size());
        assertEquals(1, result.getIds().get(0).size());
        assertEquals("right", result.getIds().get(0).get(0));
    }

    @Test
    public void testQueryTextsFromConfiguredProviderWithoutCredentialsFailsPredictably() {
        assumeCloudChroma();
        String name = uniqueCollectionName("query_texts_openai_cfg_");
        EmbeddingFunctionSpec spec = EmbeddingFunctionSpec.builder()
                .type("known")
                .name("openai")
                .config(Collections.<String, Object>singletonMap(
                        "api_key_env_var",
                        "CHROMA_INTEGRATION_NON_EXISTENT_OPENAI_KEY_ENV"
                ))
                .build();

        Collection col;
        try {
            col = client.createCollection(name, CreateCollectionOptions.builder()
                    .configuration(CollectionConfiguration.builder()
                            .embeddingFunction(spec)
                            .build())
                    .build());
        } catch (ChromaClientException e) {
            assertTrue(e.getStatusCode() >= 400);
            assertTrue(e.getStatusCode() < 500);
            assertNotNull(e.getMessage());
            assertFalse(e.getMessage().trim().isEmpty());
            return;
        }

        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue("Expected error about provider 'openai', got: " + e.getMessage(),
                    e.getMessage().contains("openai"));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testModifyConfigurationRejectsSpannUpdateWhenSchemaUsesHnsw() {
        assumeCloudChroma();
        Collection template = client.createCollection(uniqueCollectionName("schema_conflict_template_"));
        assertNotNull(template.getSchema());
        Schema schema = template.getSchema();

        boolean schemaHasHnsw = schemaHasHnsw(schema);
        boolean schemaHasSpann = schemaHasSpann(schema);
        Assume.assumeTrue(
                "Skipping because server schema does not expose explicit hnsw/spann index config",
                schemaHasHnsw ^ schemaHasSpann
        );

        String name = uniqueCollectionName("schema_conflict_");
        Collection col = client.createCollection(name, CreateCollectionOptions.builder().schema(schema).build());

        try {
            if (schemaHasHnsw) {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .spannSearchNprobe(32)
                        .build());
            } else {
                col.modifyConfiguration(UpdateCollectionConfiguration.builder()
                        .hnswSearchEf(128)
                        .build());
            }
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot switch collection index parameters between HNSW and SPANN"));
        }
    }

    private static boolean schemaHasHnsw(Schema schema) {
        ValueTypes embedding = schema.getKey(Schema.EMBEDDING_KEY);
        if (embedding == null || embedding.getFloatList() == null) {
            return false;
        }
        VectorIndexType vectorIndex = embedding.getFloatList().getVectorIndex();
        if (vectorIndex == null || vectorIndex.getConfig() == null) {
            return false;
        }
        return vectorIndex.getConfig().getHnsw() != null;
    }

    private static boolean schemaHasSpann(Schema schema) {
        ValueTypes embedding = schema.getKey(Schema.EMBEDDING_KEY);
        if (embedding == null || embedding.getFloatList() == null) {
            return false;
        }
        VectorIndexType vectorIndex = embedding.getFloatList().getVectorIndex();
        if (vectorIndex == null || vectorIndex.getConfig() == null) {
            return false;
        }
        return vectorIndex.getConfig().getSpann() != null;
    }
}
