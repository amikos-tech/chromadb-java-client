package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Nyquist validation tests for Phase 2: API Coverage Completion.
 *
 * These tests verify the four Phase 2 success criteria through observable
 * user-facing behavior rather than structural inspection. Each test maps
 * directly to a success criterion from the phase definition.
 *
 * SC-1: User can perform tenant/database lifecycle operations with typed request/response contracts.
 * SC-2: User can create and manage collections with schema/config/CMEK data preserved round-trip.
 * SC-3: User can execute add/get/query/update/upsert/delete with full filter/include/queryTexts behavior.
 * SC-4: Contract tests prove response mapping for the covered API surface.
 */
public class Phase02ValidationTest {

    private static final String TENANTS_PATH = "/api/v2/tenants";
    private static final String DATABASES_PATH = "/api/v2/tenants/default_tenant/databases";
    private static final String COLLECTIONS_PATH = "/api/v2/tenants/default_tenant/databases/default_database/collections";

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private Client client;

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    private Client newClient() {
        client = ChromaClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        return client;
    }

    // ========================================================================
    // SC-1: Tenant/database lifecycle with typed request/response contracts
    // ========================================================================

    @Test
    public void testUserCanCreateTenantAndGetTypedResult() {
        stubFor(post(urlEqualTo(TENANTS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_tenant\"}")));

        Tenant tenant = newClient().createTenant("my_tenant");

        assertNotNull("createTenant must return a non-null Tenant", tenant);
        assertEquals("Tenant name must match server response",
                Tenant.of("my_tenant"), tenant);
    }

    @Test
    public void testUserCanCreateDatabaseAndGetTypedResult() {
        stubFor(post(urlEqualTo(DATABASES_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_db\",\"tenant\":\"default_tenant\"}")));

        Database db = newClient().createDatabase("my_db");

        assertNotNull("createDatabase must return a non-null Database", db);
        assertEquals("Database name must match server response",
                Database.of("my_db"), db);
    }

    @Test
    public void testCreateTenantFallsBackToRequestNameWhenServerOmitsName() {
        stubFor(post(urlEqualTo(TENANTS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        Tenant tenant = newClient().createTenant("fallback_tenant");

        assertEquals("When server omits name, create must fall back to request name",
                Tenant.of("fallback_tenant"), tenant);
    }

    @Test
    public void testCreateDatabaseFallsBackToRequestNameWhenServerReturnsBlank() {
        stubFor(post(urlEqualTo(DATABASES_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"  \",\"tenant\":\"default_tenant\"}")));

        Database db = newClient().createDatabase("fallback_db");

        assertEquals("When server returns blank name, create must fall back to request name",
                Database.of("fallback_db"), db);
    }

    @Test
    public void testGetTenantRejectsServerPayloadWithMissingName() {
        stubFor(get(urlEqualTo(TENANTS_PATH + "/bad_tenant"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        try {
            newClient().getTenant("bad_tenant");
            fail("getTenant must reject server payload with missing name");
        } catch (ChromaDeserializationException e) {
            assertTrue("Error message must reference the invalid field",
                    e.getMessage().contains("tenant.name") || e.getMessage().contains("invalid"));
        }
    }

    @Test
    public void testListDatabasesRejectsEntryWithBlankName() {
        stubFor(get(urlMatching(DATABASES_PATH + ".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"  \",\"tenant\":\"default_tenant\"}]")));

        try {
            newClient().listDatabases();
            fail("listDatabases must reject entries with blank names");
        } catch (ChromaDeserializationException e) {
            assertTrue("Error message must indicate deserialization failure",
                    e.getMessage() != null && !e.getMessage().isEmpty());
        }
    }

    @Test
    public void testUserCanDeleteDatabase() {
        stubFor(delete(urlEqualTo(DATABASES_PATH + "/doomed_db"))
                .willReturn(aResponse().withStatus(200)));

        // Should not throw
        newClient().deleteDatabase("doomed_db");

        verify(deleteRequestedFor(urlEqualTo(DATABASES_PATH + "/doomed_db")));
    }

    // ========================================================================
    // SC-2: Collection schema/config/CMEK data preserved round-trip
    // ========================================================================

    @Test
    public void testUnknownConfigKeysPreservedThroughParseSerializeRoundTrip() {
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("hnsw:M", Integer.valueOf(16));
        input.put("vendor:futuristic_key", "preserve_me");

        CollectionConfiguration parsed = ChromaDtos.parseConfiguration(input);
        Map<String, Object> serialized = ChromaDtos.toConfigurationMap(parsed);

        assertEquals("Known hnsw:M must survive round-trip",
                Integer.valueOf(16), serialized.get("hnsw:M"));
        assertEquals("Unknown vendor key must be preserved",
                "preserve_me", serialized.get("vendor:futuristic_key"));
    }

    @Test
    public void testUnknownSchemaKeysAndCmekProvidersPreservedThroughRoundTrip() {
        Map<String, Object> schemaMap = new LinkedHashMap<String, Object>();
        schemaMap.put("new_schema_field", "keep");
        Map<String, Object> cmek = new LinkedHashMap<String, Object>();
        cmek.put("gcp", "projects/p/locations/l/keyRings/r/cryptoKeys/k");
        cmek.put("aws", "arn:aws:kms:us-east-1:123456789012:key/abc");
        schemaMap.put("cmek", cmek);

        Schema parsed = ChromaDtos.parseSchema(schemaMap);
        Map<String, Object> serialized = ChromaDtos.toSchemaMap(parsed);

        assertEquals("Unknown schema root key must survive round-trip",
                "keep", serialized.get("new_schema_field"));

        @SuppressWarnings("unchecked")
        Map<String, Object> serializedCmek = (Map<String, Object>) serialized.get("cmek");
        assertNotNull("CMEK section must be preserved", serializedCmek);
        assertEquals("Known GCP CMEK provider must be preserved",
                "projects/p/locations/l/keyRings/r/cryptoKeys/k", serializedCmek.get("gcp"));
        assertEquals("Unknown AWS CMEK provider must be preserved",
                "arn:aws:kms:us-east-1:123456789012:key/abc", serializedCmek.get("aws"));
    }

    @Test
    public void testTypedFieldsOverrideConflictingPassthroughOnSerialization() {
        CollectionConfiguration.Builder builder = CollectionConfiguration.builder()
                .hnswM(24);
        Map<String, Object> passthrough = new LinkedHashMap<String, Object>();
        passthrough.put("hnsw:M", Integer.valueOf(999));
        passthrough.put("custom:extra", "keep");
        builder.passthrough(passthrough);

        Map<String, Object> serialized = ChromaDtos.toConfigurationMap(builder.build());

        assertEquals("Typed hnsw:M must win over conflicting passthrough value",
                Integer.valueOf(24), serialized.get("hnsw:M"));
        assertEquals("Non-conflicting passthrough key must be preserved",
                "keep", serialized.get("custom:extra"));
    }

    @Test
    public void testTopLevelSchemaIsCanonicalWhenBothSchemaLocationsPresent() {
        String topLevelSchemaJson = "{\"defaults\":{\"string\":{\"fts_index\":{\"enabled\":true}}}}";
        String configSchemaJson = "{\"defaults\":{\"string\":{\"fts_index\":{\"enabled\":false}}}}";

        String collectionPayload = "{\"id\":\"coll-id\",\"name\":\"schema_test\","
                + "\"schema\":" + topLevelSchemaJson + ","
                + "\"configuration_json\":{\"schema\":" + configSchemaJson + "}}";

        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/schema_test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(collectionPayload)));

        Collection coll = newClient().getCollection("schema_test");
        Schema schema = coll.getSchema();

        assertNotNull("Collection must have a resolved schema", schema);
        assertNotNull("Schema must have defaults", schema.getDefaults());
        assertNotNull("Schema defaults must have string type", schema.getDefaults().getString());
        assertNotNull("String type must have fts_index", schema.getDefaults().getString().getFtsIndex());
        assertTrue("Top-level schema fts_index.enabled=true must be canonical (not config schema's false)",
                schema.getDefaults().getString().getFtsIndex().isEnabled());
    }

    // ========================================================================
    // SC-3: Full record operations with filter/include/queryTexts behavior
    // ========================================================================

    @Test
    public void testUserCanAddRecordsWithEmbeddingsAndDocuments() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Collection coll = newClient().createCollection("test_col");
        coll.add()
                .ids("id1", "id2")
                .embeddings(new float[]{0.1f, 0.2f}, new float[]{0.3f, 0.4f})
                .documents("doc1", "doc2")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/add"))
                .withRequestBody(matchingJsonPath("$.ids[0]", equalTo("id1")))
                .withRequestBody(matchingJsonPath("$.ids[1]", equalTo("id2"))));
    }

    @Test
    public void testUserCanQueryWithEmbeddingsAndGetResults() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\",\"id2\"]]}")));

        Collection coll = newClient().createCollection("test_col");
        QueryResult result = coll.query()
                .queryEmbeddings(new float[]{0.1f, 0.2f})
                .nResults(5)
                .execute();

        assertNotNull("Query must return a result", result);
        assertEquals("Query must return expected IDs",
                Arrays.asList("id1", "id2"), result.getIds().get(0));
    }

    @Test
    public void testMixedQueryInputsAcceptedWithEmbeddingsAuthoritative() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.5")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"embed-wins\"]]}")));

        Collection coll = newClient().createCollection("test_col");

        // Set queryTexts first, then queryEmbeddings -- embeddings must win
        QueryResult result = coll.query()
                .queryTexts("some text")
                .queryEmbeddings(new float[]{0.5f, 0.6f})
                .execute();

        assertEquals("Explicit embeddings must be authoritative when both inputs set",
                "embed-wins", result.getIds().get(0).get(0));
    }

    @Test
    public void testOmittedIncludeDoesNotForceClientDefaults() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Collection coll = newClient().createCollection("test_col");
        coll.query()
                .queryEmbeddings(new float[]{1.0f})
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .withRequestBody(notMatching("(?s).*\"include\"\\s*:.*")));
    }

    @Test
    public void testExplicitIncludeForwardedExactly() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Collection coll = newClient().createCollection("test_col");
        coll.query()
                .queryEmbeddings(new float[]{1.0f})
                .include(Include.DOCUMENTS, Include.DISTANCES)
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .withRequestBody(matching(
                        "(?s).*\"include\"\\s*:\\s*\\[\\s*\"documents\"\\s*,\\s*\"distances\"\\s*\\].*")));
    }

    @Test
    public void testUserCanGetRecordsByIds() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/get"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[\"id1\"]}")));

        Collection coll = newClient().createCollection("test_col");
        GetResult result = coll.get()
                .ids("id1")
                .execute();

        assertNotNull("Get must return a result", result);
        assertEquals("id1", result.getIds().get(0));
    }

    @Test
    public void testUserCanUpdateRecords() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Collection coll = newClient().createCollection("test_col");
        coll.update()
                .ids("id1")
                .embeddings(new float[]{0.9f, 0.8f})
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/update"))
                .withRequestBody(matchingJsonPath("$.ids[0]", equalTo("id1"))));
    }

    @Test
    public void testUserCanUpsertRecords() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Collection coll = newClient().createCollection("test_col");
        coll.upsert()
                .ids("id1")
                .embeddings(new float[]{0.7f, 0.6f})
                .documents("updated doc")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/upsert"))
                .withRequestBody(matchingJsonPath("$.ids[0]", equalTo("id1"))));
    }

    @Test
    public void testUserCanDeleteRecords() {
        stubCreateCollection();
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"id1\"]")));

        Collection coll = newClient().createCollection("test_col");
        coll.delete()
                .ids("id1")
                .execute();

        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/delete"))
                .withRequestBody(matchingJsonPath("$.ids[0]", equalTo("id1"))));
    }

    // ========================================================================
    // SC-4: Contract tests prove response mapping for covered API surface
    // ========================================================================

    @Test
    public void testTenantResponseMapsToTypedObject() {
        stubFor(post(urlEqualTo(TENANTS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"server_tenant\"}")));

        Tenant tenant = newClient().createTenant("ignored_request");

        // The response name "server_tenant" must take precedence over request name
        assertEquals("Response mapping must extract typed Tenant from server JSON",
                "server_tenant", tenant.getName());
    }

    @Test
    public void testDatabaseResponseMapsToTypedObject() {
        stubFor(post(urlEqualTo(DATABASES_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"server_db\",\"tenant\":\"default_tenant\"}")));

        Database db = newClient().createDatabase("ignored_request");

        assertEquals("Response mapping must extract typed Database from server JSON",
                "server_db", db.getName());
    }

    @Test
    public void testCollectionResponseMapsConfigurationAndMetadata() {
        String body = "{\"id\":\"coll-123\",\"name\":\"my_col\","
                + "\"metadata\":{\"source\":\"test\"},"
                + "\"configuration_json\":{\"hnsw:M\":32,\"hnsw:search_ef\":100}}";

        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/my_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        Collection coll = newClient().getCollection("my_col");

        assertEquals("coll-123", coll.getId());
        assertEquals("my_col", coll.getName());
        assertNotNull("Metadata must be mapped", coll.getMetadata());
        assertEquals("test", coll.getMetadata().get("source"));
        assertNotNull("Configuration must be mapped", coll.getConfiguration());
        assertEquals(Integer.valueOf(32), coll.getConfiguration().getHnswM());
        assertEquals(Integer.valueOf(100), coll.getConfiguration().getHnswSearchEf());
    }

    @Test
    public void testQueryResponseMapsIdsAndDistances() {
        stubCreateCollection();
        String queryResponse = "{"
                + "\"ids\":[[\"q1\",\"q2\"]],"
                + "\"distances\":[[0.1,0.2]],"
                + "\"documents\":[[\"doc1\",\"doc2\"]]"
                + "}";

        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-id-1/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(queryResponse)));

        Collection coll = newClient().createCollection("test_col");
        QueryResult result = coll.query()
                .queryEmbeddings(new float[]{1.0f})
                .include(Include.DOCUMENTS, Include.DISTANCES)
                .execute();

        assertNotNull(result);
        assertEquals(1, result.getIds().size());
        assertEquals(Arrays.asList("q1", "q2"), result.getIds().get(0));
        assertNotNull("Distances must be mapped", result.getDistances());
        assertNotNull("Documents must be mapped", result.getDocuments());
        assertEquals("doc1", result.getDocuments().get(0).get(0));
    }

    // ========================================================================
    // Parity matrix gate (structural check)
    // ========================================================================

    @Test
    public void testParityMatrixTargetExistsInMakefile() throws Exception {
        java.io.File makefile = new java.io.File(
                System.getProperty("user.dir"), "Makefile");
        assertTrue("Makefile must exist at project root", makefile.exists());

        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(makefile));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            reader.close();
        }
        String content = sb.toString();

        assertTrue("Makefile must contain test-phase-02-parity target",
                content.contains("test-phase-02-parity:"));
        assertTrue("Parity target must test against Chroma 1.5.5",
                content.contains("CHROMA_VERSION=1.5.5"));
        assertTrue("Parity target must test against Chroma 1.3.7",
                content.contains("CHROMA_VERSION=1.3.7"));
    }

    // ========================================================================
    // Helper: stub collection creation
    // ========================================================================

    private void stubCreateCollection() {
        stubFor(post(urlEqualTo(COLLECTIONS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));
    }
}
