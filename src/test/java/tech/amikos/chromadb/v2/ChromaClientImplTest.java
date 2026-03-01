package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class ChromaClientImplTest {

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

    private Client newClient(String tenant, String database) {
        client = ChromaClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .tenant(tenant)
                .database(database)
                .build();
        return client;
    }

    // --- heartbeat ---

    @Test
    public void testHeartbeat() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\":12345}")));

        Client c = newClient();
        String result = c.heartbeat();
        assertEquals("12345", result);
    }

    @Test
    public void testHeartbeatMissingRequiredFieldThrows() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"other\":1}")));

        try {
            newClient().heartbeat();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("nanosecond heartbeat"));
        }
    }

    // --- version ---

    @Test
    public void testVersion() {
        stubFor(get(urlEqualTo("/api/v2/version"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("\"1.0.0\"")));

        Client c = newClient();
        assertEquals("1.0.0", c.version());
    }

    // --- preFlight ---

    @Test
    public void testPreFlight() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"max_batch_size\":123,\"supports_base64_encoding\":true}")));

        Client c = newClient();
        PreFlightInfo info = c.preFlight();
        assertEquals(123, info.getMaxBatchSize());
        assertEquals(Boolean.TRUE, info.getSupportsBase64Encoding());
    }

    @Test
    public void testPreFlightWithoutSupportsBase64Encoding() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"max_batch_size\":64}")));

        Client c = newClient();
        PreFlightInfo info = c.preFlight();
        assertEquals(64, info.getMaxBatchSize());
        assertNull(info.getSupportsBase64Encoding());
    }

    @Test
    public void testPreFlightWithSupportsBase64EncodingFalse() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"max_batch_size\":64,\"supports_base64_encoding\":false}")));

        Client c = newClient();
        PreFlightInfo info = c.preFlight();
        assertEquals(64, info.getMaxBatchSize());
        assertEquals(Boolean.FALSE, info.getSupportsBase64Encoding());
        assertFalse(info.supportsBase64Encoding());
    }

    @Test
    public void testPreFlightMissingMaxBatchSizeThrows() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"supports_base64_encoding\":true}")));

        try {
            newClient().preFlight();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("max_batch_size"));
        }
    }

    @Test
    public void testPreFlightNegativeMaxBatchSizeThrows() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"max_batch_size\":-1,\"supports_base64_encoding\":false}")));

        try {
            newClient().preFlight();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("max_batch_size"));
        }
    }

    @Test
    public void testPreFlightZeroMaxBatchSizeThrows() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"max_batch_size\":0,\"supports_base64_encoding\":false}")));

        try {
            newClient().preFlight();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("max_batch_size"));
        }
    }

    // --- getIdentity ---

    @Test
    public void testGetIdentity() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"user-1\",\"tenant\":\"tenant-a\",\"databases\":[\"db1\",\"db2\"]}")));

        Client c = newClient();
        Identity identity = c.getIdentity();
        assertEquals("user-1", identity.getUserId());
        assertEquals("tenant-a", identity.getTenant());
        assertEquals(Arrays.asList("db1", "db2"), identity.getDatabases());
    }

    @Test
    public void testGetIdentityMissingUserIdThrows() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"tenant\":\"tenant-a\",\"databases\":[\"db1\"]}")));

        try {
            newClient().getIdentity();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("identity.user_id"));
        }
    }

    @Test
    public void testGetIdentityBlankUserIdThrows() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"  \",\"tenant\":\"tenant-a\",\"databases\":[\"db1\"]}")));

        try {
            newClient().getIdentity();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("identity.user_id"));
        }
    }

    @Test
    public void testGetIdentityMissingTenantThrows() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"user-1\",\"databases\":[\"db1\"]}")));

        try {
            newClient().getIdentity();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("identity.tenant"));
        }
    }

    @Test
    public void testGetIdentityMissingDatabasesThrows() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"user-1\",\"tenant\":\"tenant-a\"}")));

        try {
            newClient().getIdentity();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("identity.databases"));
        }
    }

    @Test
    public void testGetIdentityBlankDatabaseEntryThrows() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"user-1\",\"tenant\":\"tenant-a\",\"databases\":[\"db1\",\"  \"]}")));

        try {
            newClient().getIdentity();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("identity.databases[1]"));
        }
    }

    @Test
    public void testGetIdentityWithEmptyDatabasesList() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"user_id\":\"user-1\",\"tenant\":\"tenant-a\",\"databases\":[]}")));

        Identity identity = newClient().getIdentity();
        assertNotNull(identity);
        assertNotNull(identity.getDatabases());
        assertTrue(identity.getDatabases().isEmpty());
    }

    @Test(expected = ChromaUnauthorizedException.class)
    public void testGetIdentityUnauthorized() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized\"}")));

        newClient().getIdentity();
    }

    // --- reset ---

    @Test
    public void testReset() {
        stubFor(post(urlEqualTo("/api/v2/reset"))
                .willReturn(aResponse().withStatus(200)));

        Client c = newClient();
        c.reset();

        verify(postRequestedFor(urlEqualTo("/api/v2/reset")));
    }

    // --- createTenant ---

    @Test
    public void testCreateTenant() {
        stubFor(post(urlEqualTo("/api/v2/tenants"))
                .withRequestBody(equalToJson("{\"name\":\"my_tenant\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_tenant\"}")));

        Client c = newClient();
        Tenant tenant = c.createTenant("my_tenant");
        assertEquals(Tenant.of("my_tenant"), tenant);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateTenantRejectsNullName() {
        newClient().createTenant(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTenantRejectsBlankName() {
        newClient().createTenant("   ");
    }

    @Test(expected = ChromaConflictException.class)
    public void testCreateTenantConflict() {
        stubFor(post(urlEqualTo("/api/v2/tenants"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"already exists\"}")));

        newClient().createTenant("my_tenant");
    }

    // --- getTenant ---

    @Test
    public void testGetTenant() {
        stubFor(get(urlEqualTo("/api/v2/tenants/my_tenant"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_tenant\"}")));

        Client c = newClient();
        Tenant tenant = c.getTenant("my_tenant");
        assertEquals(Tenant.of("my_tenant"), tenant);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTenantRejectsBlankName() {
        newClient().getTenant("   ");
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testGetTenantNotFound() {
        stubFor(get(urlEqualTo("/api/v2/tenants/unknown"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        newClient().getTenant("unknown");
    }

    @Test
    public void testGetTenantMissingNameThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/my_tenant"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":null}")));

        try {
            newClient().getTenant("my_tenant");
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("tenant.name"));
        }
    }

    // --- createDatabase ---

    @Test
    public void testCreateDatabase() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases"))
                .withRequestBody(equalToJson("{\"name\":\"my_db\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_db\",\"tenant\":\"default_tenant\"}")));

        Client c = newClient();
        Database db = c.createDatabase("my_db");
        assertEquals(Database.of("my_db"), db);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateDatabaseRejectsNullName() {
        newClient().createDatabase(null);
    }

    @Test(expected = ChromaConflictException.class)
    public void testCreateDatabaseConflict() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"already exists\"}")));

        newClient().createDatabase("my_db");
    }

    // --- getDatabase ---

    @Test
    public void testGetDatabase() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/my_db"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_db\",\"tenant\":\"default_tenant\"}")));

        Client c = newClient();
        Database db = c.getDatabase("my_db");
        assertEquals(Database.of("my_db"), db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDatabaseRejectsBlankName() {
        newClient().getDatabase("  ");
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testGetDatabaseNotFound() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/unknown"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        newClient().getDatabase("unknown");
    }

    @Test
    public void testGetDatabaseBlankNameThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/my_db"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"   \",\"tenant\":\"default_tenant\"}")));

        try {
            newClient().getDatabase("my_db");
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("database.name"));
        }
    }

    // --- listDatabases ---

    @Test
    public void testListDatabases() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"db1\",\"tenant\":\"default_tenant\"},{\"name\":\"db2\",\"tenant\":\"default_tenant\"}]")));

        Client c = newClient();
        List<Database> dbs = c.listDatabases();
        assertEquals(2, dbs.size());
        assertEquals(Database.of("db1"), dbs.get(0));
        assertEquals(Database.of("db2"), dbs.get(1));
    }

    @Test
    public void testListDatabasesWithMissingNameThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"db1\",\"tenant\":\"default_tenant\"},{\"tenant\":\"default_tenant\"}]")));

        try {
            newClient().listDatabases();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("database.name"));
        }
    }

    // --- deleteDatabase ---

    @Test
    public void testDeleteDatabase() {
        stubFor(delete(urlEqualTo("/api/v2/tenants/default_tenant/databases/my_db"))
                .willReturn(aResponse().withStatus(200)));

        Client c = newClient();
        c.deleteDatabase("my_db");

        verify(deleteRequestedFor(urlEqualTo("/api/v2/tenants/default_tenant/databases/my_db")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteDatabaseRejectsBlankName() {
        newClient().deleteDatabase(" ");
    }

    // --- createCollection ---

    @Test
    public void testCreateCollection() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("test_col")))
                .withRequestBody(matchingJsonPath("$.get_or_create", equalTo("false")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient();
        Collection col = c.createCollection("test_col");
        assertNotNull(col);
        assertEquals("col-id-1", col.getId());
        assertEquals("test_col", col.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateCollectionRejectsNullName() {
        newClient().createCollection(null);
    }

    @Test(expected = ChromaConflictException.class)
    public void testCreateCollectionConflict() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"already exists\"}")));

        newClient().createCollection("test_col");
    }

    @Test
    public void testCreateCollectionWithOptions() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("test_col")))
                .withRequestBody(matchingJsonPath("$.metadata.key", equalTo("val")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"metadata\":{\"key\":\"val\"}}")));

        Client c = newClient();
        java.util.Map<String, Object> meta = new java.util.HashMap<String, Object>();
        meta.put("key", "val");
        Collection col = c.createCollection("test_col",
                CreateCollectionOptions.withMetadata(meta));
        assertNotNull(col);
        assertEquals("val", col.getMetadata().get("key"));
    }

    @Test
    public void testCreateCollectionWithConfigurationOptions() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("cfg_col")))
                .withRequestBody(matchingJsonPath("$.configuration['hnsw:space']", equalTo("cosine")))
                .withRequestBody(matchingJsonPath("$.configuration['hnsw:M']", equalTo("16")))
                .withRequestBody(matchingJsonPath("$.configuration['hnsw:construction_ef']", equalTo("200")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-cfg-1\",\"name\":\"cfg_col\",\"configuration_json\":{\"hnsw:space\":\"cosine\",\"hnsw:M\":16,\"hnsw:construction_ef\":200}}")));

        Client c = newClient();
        CollectionConfiguration cfg = CollectionConfiguration.builder()
                .space(DistanceFunction.COSINE)
                .hnswM(16)
                .hnswConstructionEf(200)
                .build();
        Collection col = c.createCollection(
                "cfg_col",
                CreateCollectionOptions.builder().configuration(cfg).build());

        assertNotNull(col.getConfiguration());
        assertEquals(DistanceFunction.COSINE, col.getConfiguration().getSpace());
        assertEquals(Integer.valueOf(16), col.getConfiguration().getHnswM());
        assertEquals(Integer.valueOf(200), col.getConfiguration().getHnswConstructionEf());
    }

    // --- getCollection ---

    @Test
    public void testGetCollection() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient();
        Collection col = c.getCollection("test_col");
        assertEquals("col-id-1", col.getId());
        assertEquals("test_col", col.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetCollectionRejectsBlankName() {
        newClient().getCollection(" ");
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testGetCollectionNotFound() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/unknown"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        newClient().getCollection("unknown");
    }

    @Test
    public void testGetCollectionMissingIdThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"test_col\"}")));

        try {
            newClient().getCollection("test_col");
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("collection.id"));
        }
    }

    // --- getOrCreateCollection ---

    @Test
    public void testGetOrCreateCollection() {
        stubFor(post(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("test_col")))
                .withRequestBody(matchingJsonPath("$.get_or_create", equalTo("true")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient();
        Collection col = c.getOrCreateCollection("test_col");
        assertNotNull(col);
        assertEquals("col-id-1", col.getId());
    }

    // --- listCollections ---

    @Test
    public void testListCollections() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"id1\",\"name\":\"col1\"},{\"id\":\"id2\",\"name\":\"col2\"}]")));

        Client c = newClient();
        List<Collection> cols = c.listCollections();
        assertEquals(2, cols.size());
        assertEquals("col1", cols.get(0).getName());
        assertEquals("col2", cols.get(1).getName());
    }

    @Test
    public void testListCollectionsWithLimitOffset() {
        stubFor(get(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withQueryParam("limit", equalTo("10"))
                .withQueryParam("offset", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"id1\",\"name\":\"col1\"}]")));

        Client c = newClient();
        List<Collection> cols = c.listCollections(10, 5);
        assertEquals(1, cols.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListCollectionsRejectsNegativeLimit() {
        newClient().listCollections(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListCollectionsRejectsNegativeOffset() {
        newClient().listCollections(1, -1);
    }

    @Test
    public void testListCollectionsMissingNameThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"id1\"}]")));

        try {
            newClient().listCollections();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("collection.name"));
        }
    }

    @Test
    public void testListCollectionsWithNullEntryThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"id1\",\"name\":\"col1\"},null]")));

        try {
            newClient().listCollections();
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("null entry at index 1"));
        }
    }

    // --- deleteCollection ---

    @Test
    public void testDeleteCollection() {
        stubFor(delete(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col"))
                .willReturn(aResponse().withStatus(200)));

        Client c = newClient();
        c.deleteCollection("test_col");

        verify(deleteRequestedFor(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteCollectionRejectsBlankName() {
        newClient().deleteCollection("  ");
    }

    @Test(expected = ChromaNotFoundException.class)
    public void testDeleteCollectionNotFound() {
        stubFor(delete(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/unknown"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\"}")));

        newClient().deleteCollection("unknown");
    }

    // --- countCollections ---

    @Test
    public void testCountCollections() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections_count"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("5")));

        Client c = newClient();
        assertEquals(5, c.countCollections());
    }

    // --- session context switching ---

    @Test
    public void testCurrentTenantAndDatabaseDefaults() {
        Client c = newClient();
        assertEquals(Tenant.defaultTenant(), c.currentTenant());
        assertEquals(Database.defaultDatabase(), c.currentDatabase());
    }

    @Test
    public void testUseDatabaseSwitchesDatabaseOnly() {
        Client c = newClient("tenant_a", "db_a");
        c.useDatabase(Database.of("db_b"));

        assertEquals(Tenant.of("tenant_a"), c.currentTenant());
        assertEquals(Database.of("db_b"), c.currentDatabase());
    }

    @Test
    public void testUseTenantSwitchesTenantAndResetsDatabase() {
        Client c = newClient("tenant_a", "db_a");
        c.useTenant(Tenant.of("tenant_b"));

        assertEquals(Tenant.of("tenant_b"), c.currentTenant());
        assertEquals(Database.defaultDatabase(), c.currentDatabase());
    }

    @Test(expected = NullPointerException.class)
    public void testUseTenantRejectsNull() {
        newClient().useTenant(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUseDatabaseRejectsNull() {
        newClient().useDatabase(null);
    }

    @Test
    public void testCollectionOpsUseSwitchedTenantAndDatabase() {
        stubFor(post(urlEqualTo("/api/v2/tenants/tenant_switched/databases/db_switched/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient();
        c.useTenant(Tenant.of("tenant_switched"));
        c.useDatabase(Database.of("db_switched"));

        Collection col = c.createCollection("test_col");
        assertEquals(Tenant.of("tenant_switched"), col.getTenant());
        assertEquals(Database.of("db_switched"), col.getDatabase());
    }

    @Test
    public void testUseTenantResetsDatabaseForCollectionOperations() {
        stubFor(post(urlEqualTo("/api/v2/tenants/tenant_switched/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient("tenant_a", "db_a");
        c.useTenant(Tenant.of("tenant_switched"));

        Collection col = c.createCollection("test_col");
        assertEquals(Tenant.of("tenant_switched"), col.getTenant());
        assertEquals(Database.defaultDatabase(), col.getDatabase());
    }

    @Test
    public void testDatabaseOpsUseSwitchedTenant() {
        stubFor(post(urlEqualTo("/api/v2/tenants/tenant_switched/databases"))
                .withRequestBody(equalToJson("{\"name\":\"db_new\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"db_new\",\"tenant\":\"tenant_switched\"}")));

        Client c = newClient();
        c.useTenant(Tenant.of("tenant_switched"));

        Database db = c.createDatabase("db_new");
        assertEquals(Database.of("db_new"), db);
    }

    // --- close ---

    @Test
    public void testClose() {
        Client c = newClient();
        c.close();
        // Double close should be safe
        c.close();
    }

    // --- custom tenant/database ---

    @Test
    public void testCustomTenantDatabase() {
        stubFor(post(urlEqualTo("/api/v2/tenants/my_tenant/databases/my_db/collections"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\"}")));

        Client c = newClient("my_tenant", "my_db");
        Collection col = c.createCollection("test_col");
        assertNotNull(col);
        assertEquals(Tenant.of("my_tenant"), col.getTenant());
        assertEquals(Database.of("my_db"), col.getDatabase());
    }

    // --- collection metadata parsing ---

    @Test
    public void testCollectionWithFullMetadata() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"metadata\":{\"key\":\"val\"},\"dimension\":128}")));

        Client c = newClient();
        Collection col = c.getCollection("test_col");
        assertEquals("val", col.getMetadata().get("key"));
        assertEquals(Integer.valueOf(128), col.getDimension());
    }

    @Test
    public void testGetCollectionInvalidConfigurationSpaceThrows() {
        stubFor(get(urlEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/test_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-id-1\",\"name\":\"test_col\",\"configuration_json\":{\"hnsw:space\":123}}")));

        try {
            newClient().getCollection("test_col");
            fail("Expected ChromaDeserializationException");
        } catch (ChromaDeserializationException e) {
            assertTrue(e.getMessage().contains("hnsw:space"));
        }
    }
}
