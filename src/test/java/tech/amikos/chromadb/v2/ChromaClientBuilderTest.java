package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ChromaClientBuilderTest {

    @Test
    public void testBuilderCreatesClient() {
        Client client = ChromaClient.builder()
                .baseUrl("http://localhost:1234")
                .build();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testBuilderCreatesClientWithDefaults() {
        Client client = ChromaClient.builder().build();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testBuilderFluentChaining() {
        Client client = ChromaClient.builder()
                .baseUrl("http://localhost:8000")
                .auth(TokenAuth.of("tok"))
                .apiKey("key")
                .tenant(Tenant.of("t"))
                .tenant("t-string")
                .database(Database.of("d"))
                .database("d-string")
                .timeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .defaultHeaders(Collections.<String, String>emptyMap())
                .build();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testCloudBuilderFluentChaining() {
        ChromaClient.CloudBuilder builder = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .timeout(Duration.ofSeconds(30));
        assertNotNull(builder);
    }

    @Test(expected = IllegalStateException.class)
    public void testCloudBuilderRequiresApiKey() {
        ChromaClient.cloud()
                .tenant("t")
                .database("d")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCloudBuilderRequiresTenant() {
        ChromaClient.cloud()
                .apiKey("key")
                .database("d")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testCloudBuilderRequiresDatabase() {
        ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .build();
    }

    @Test
    public void testCloudBuilderCreatesClient() {
        Client client = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .build();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testCloudBuilderUsesChromaTokenAuth() throws Exception {
        Client client = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .build();
        try {
            Field apiClientField = client.getClass().getDeclaredField("apiClient");
            apiClientField.setAccessible(true);
            Object apiClient = apiClientField.get(client);

            Field authProviderField = ChromaApiClient.class.getDeclaredField("authProvider");
            authProviderField.setAccessible(true);
            Object authProvider = authProviderField.get(apiClient);

            assertTrue(authProvider instanceof ChromaTokenAuth);
        } finally {
            client.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCloudBuilderRejectsBlankApiKey() {
        ChromaClient.cloud().apiKey("  ");
    }

    @Test(expected = NullPointerException.class)
    public void testCloudBuilderRejectsNullApiKey() {
        ChromaClient.cloud().apiKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCloudBuilderRejectsNullTenant() {
        ChromaClient.cloud().tenant(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCloudBuilderRejectsBlankTenant() {
        ChromaClient.cloud().tenant("  ");
    }

    @Test(expected = NullPointerException.class)
    public void testCloudBuilderRejectsNullDatabase() {
        ChromaClient.cloud().database(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCloudBuilderRejectsBlankDatabase() {
        ChromaClient.cloud().database("  ");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuilderDefaultHeadersDefensiveCopy() throws Exception {
        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("X-Test", "one");

        ChromaClient.Builder builder = ChromaClient.builder().defaultHeaders(input);
        input.put("X-Test", "two");

        Field field = ChromaClient.Builder.class.getDeclaredField("defaultHeaders");
        field.setAccessible(true);
        Map<String, String> stored = (Map<String, String>) field.get(builder);

        assertNotSame(input, stored);
        assertEquals("one", stored.get("X-Test"));
    }

    @Test
    public void testCloudBuilderNormalizesWhitespaceInTenantAndDatabase() throws Exception {
        ChromaClient.CloudBuilder builder = ChromaClient.cloud()
                .apiKey("key")
                .tenant("  tenant-a  ")
                .database("  db-a  ");

        Field tenantField = ChromaClient.CloudBuilder.class.getDeclaredField("tenant");
        tenantField.setAccessible(true);
        Field databaseField = ChromaClient.CloudBuilder.class.getDeclaredField("database");
        databaseField.setAccessible(true);

        assertEquals("tenant-a", tenantField.get(builder));
        assertEquals("db-a", databaseField.get(builder));
    }
}
