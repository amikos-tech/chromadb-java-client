package tech.amikos.chromadb.v2;

import org.junit.Test;
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
            assertTrue(e.getMessage().contains("Failed to initialize embedding function provider 'openai'"));
            assertNotNull(e.getCause());
        }
    }
}
