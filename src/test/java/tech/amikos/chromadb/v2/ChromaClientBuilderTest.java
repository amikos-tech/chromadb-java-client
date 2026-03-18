package tech.amikos.chromadb.v2;

import org.junit.Test;
import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
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
                .tenant(Tenant.of("t"))
                .tenant("t-string")
                .database(Database.of("d"))
                .database("d-string")
                .timeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(10))
                .defaultHeaders(Collections.<String, String>emptyMap())
                .logger(ChromaLogger.noop())
                .build();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testBuilderApiKeyUsesTokenAuth() throws Exception {
        ChromaClient.Builder builder = ChromaClient.builder().apiKey("key");
        Field authProviderField = ChromaClient.Builder.class.getDeclaredField("authProvider");
        authProviderField.setAccessible(true);

        assertTrue(authProviderField.get(builder) instanceof TokenAuth);
    }

    @Test
    public void testBuilderRejectsSecondAuthSetterAuthThenApiKey() {
        try {
            ChromaClient.builder()
                    .auth(TokenAuth.of("tok"))
                    .apiKey("other");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("exactly one auth strategy"));
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testBuilderRejectsSecondAuthSetterApiKeyThenAuth() {
        try {
            ChromaClient.builder()
                    .apiKey("tok")
                    .auth(BasicAuth.of("user", "pass"));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("exactly one auth strategy"));
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testCloudBuilderFluentChaining() {
        ChromaClient.CloudBuilder builder = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .timeout(Duration.ofSeconds(30))
                .logger(ChromaLogger.noop());
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

    @Test
    public void testCloudBuilderLoggerIsStoredInApiClient() throws Exception {
        ChromaLogger logger = new StubLogger();
        Client client = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .logger(logger)
                .build();
        try {
            Field apiClientField = client.getClass().getDeclaredField("apiClient");
            apiClientField.setAccessible(true);
            Object apiClient = apiClientField.get(client);

            Field loggerField = ChromaApiClient.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            Object storedLogger = loggerField.get(apiClient);

            assertSame(logger, storedLogger);
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
    public void testBuilderRejectsAuthorizationInDefaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer stale");

        try {
            ChromaClient.builder().defaultHeaders(headers);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testBuilderRejectsXChromaTokenInDefaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("x-chroma-token", "stale");

        try {
            ChromaClient.builder().defaultHeaders(headers);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testBuildRejectsConflictingAuthHeaderInjectedIntoBuilderState() throws Exception {
        ChromaClient.Builder builder = ChromaClient.builder();
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer stale");

        Field field = ChromaClient.Builder.class.getDeclaredField("defaultHeaders");
        field.setAccessible(true);
        field.set(builder, headers);

        try {
            builder.build();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth(...)"));
        }
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

    @Test
    public void testCloudBuilderRejectsSecondApiKeySetterCall() {
        try {
            ChromaClient.cloud()
                    .apiKey("key-one")
                    .apiKey("key-two");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("exactly one auth strategy"));
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testBuilderLoggerIsStoredInApiClient() throws Exception {
        ChromaLogger logger = new StubLogger();
        Client client = ChromaClient.builder()
                .baseUrl("http://localhost:8000")
                .logger(logger)
                .build();
        try {
            Field apiClientField = client.getClass().getDeclaredField("apiClient");
            apiClientField.setAccessible(true);
            Object apiClient = apiClientField.get(client);

            Field loggerField = ChromaApiClient.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            Object storedLogger = loggerField.get(apiClient);

            assertSame(logger, storedLogger);
        } finally {
            client.close();
        }
    }

    @Test
    public void testBuilderUsesProvidedHttpClientWithoutTakingOwnership() throws Exception {
        OkHttpClient provided = new OkHttpClient();
        Client client = ChromaClient.builder()
                .httpClient(provided)
                .build();
        try {
            Field apiClientField = client.getClass().getDeclaredField("apiClient");
            apiClientField.setAccessible(true);
            Object apiClient = apiClientField.get(client);

            Field httpClientField = ChromaApiClient.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            Object storedClient = httpClientField.get(apiClient);
            assertSame(provided, storedClient);
        } finally {
            client.close();
        }

        assertFalse(provided.dispatcher().executorService().isShutdown());
        provided.dispatcher().executorService().shutdown();
        provided.connectionPool().evictAll();
    }

    @Test
    public void testBuilderOwnedHttpClientIsShutdownOnClose() throws Exception {
        Client client = ChromaClient.builder().build();
        Field apiClientField = client.getClass().getDeclaredField("apiClient");
        apiClientField.setAccessible(true);
        Object apiClient = apiClientField.get(client);

        Field ownsField = ChromaApiClient.class.getDeclaredField("ownsHttpClient");
        ownsField.setAccessible(true);
        assertEquals(Boolean.TRUE, ownsField.get(apiClient));

        Field httpClientField = ChromaApiClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        OkHttpClient owned = (OkHttpClient) httpClientField.get(apiClient);
        assertFalse(owned.dispatcher().executorService().isShutdown());

        client.close();

        assertTrue(owned.dispatcher().executorService().isShutdown());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsHttpClientWithTimeoutOptions() {
        ChromaClient.builder()
                .httpClient(new OkHttpClient())
                .readTimeout(Duration.ofSeconds(1))
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsHttpClientWithInsecureTlsOptions() {
        ChromaClient.builder()
                .httpClient(new OkHttpClient())
                .insecure(true)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsSslCertWithInsecureTls() {
        ChromaClient.builder()
                .sslCert(Paths.get("does-not-matter.pem"))
                .insecure(true)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsMissingSslCertFile() {
        ChromaClient.builder()
                .sslCert(Paths.get("definitely-not-present-cert.pem"))
                .build();
    }

    @Test
    public void testBuilderTenantAndDatabaseFromEnv() throws Exception {
        String envVar = firstNonBlankEnvVar();
        String expected = System.getenv(envVar).trim();
        ChromaClient.Builder builder = ChromaClient.builder()
                .tenantFromEnv(envVar)
                .databaseFromEnv(envVar);

        Field tenantField = ChromaClient.Builder.class.getDeclaredField("tenant");
        tenantField.setAccessible(true);
        Field databaseField = ChromaClient.Builder.class.getDeclaredField("database");
        databaseField.setAccessible(true);

        Tenant tenant = (Tenant) tenantField.get(builder);
        Database database = (Database) databaseField.get(builder);

        assertEquals(expected, tenant.getName());
        assertEquals(expected, database.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderTenantFromMissingEnvThrows() {
        ChromaClient.builder().tenantFromEnv("CHROMA_TEST_MISSING_ENV_" + System.nanoTime());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderDatabaseFromMissingEnvThrows() {
        ChromaClient.builder().databaseFromEnv("CHROMA_TEST_MISSING_ENV_" + System.nanoTime());
    }

    private static String firstNonBlankEnvVar() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No non-blank environment variable available for test");
    }

    private static final class StubLogger implements ChromaLogger {
        @Override
        public void debug(String event, Map<String, Object> fields) {}

        @Override
        public void info(String event, Map<String, Object> fields) {}

        @Override
        public void warn(String event, Map<String, Object> fields) {}

        @Override
        public void error(String event, Map<String, Object> fields, Throwable throwable) {}
    }
}
