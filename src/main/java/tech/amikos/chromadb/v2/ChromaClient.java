package tech.amikos.chromadb.v2;

import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Entry point for creating ChromaDB clients.
 *
 * <pre>{@code
 * // Self-hosted
 * Client client = ChromaClient.builder()
 *     .baseUrl("http://localhost:8000")
 *     .build();
 *
 * // Chroma Cloud
 * Client client = ChromaClient.cloud()
 *     .apiKey("your-key")
 *     .tenant("my-tenant")
 *     .database("my-db")
 *     .build();
 * }</pre>
 */
public final class ChromaClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8000";
    private static final String CLOUD_BASE_URL = "https://api.trychroma.com";

    private ChromaClient() {}

    public static Builder builder() {
        return new Builder();
    }

    public static CloudBuilder cloud() {
        return new CloudBuilder();
    }

    public static final class Builder {
        private static final String DEFAULT_TENANT_ENV = "CHROMA_TENANT";
        private static final String DEFAULT_DATABASE_ENV = "CHROMA_DATABASE";

        private String baseUrl;
        private AuthProvider authProvider;
        private Tenant tenant;
        private Database database;
        private Duration connectTimeout;
        private Duration readTimeout;
        private Duration writeTimeout;
        private Map<String, String> defaultHeaders;
        private OkHttpClient httpClient;
        private Path sslCertPath;
        private boolean insecure;
        private ChromaLogger logger;

        Builder() {}

        /**
         * Sets the base URL of the Chroma server.
         *
         * @param baseUrl server URL (for example {@code http://localhost:8000})
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        /**
         * Sets the authentication provider directly.
         *
         * <p>If both {@link #auth(AuthProvider)} and {@link #apiKey(String)} are used, the last
         * method invoked determines the effective authentication.</p>
         */
        public Builder auth(AuthProvider authProvider) { this.authProvider = authProvider; return this; }

        /**
         * Convenience for {@code auth(TokenAuth.of(apiKey))}.
         *
         * <p>If both {@link #auth(AuthProvider)} and {@link #apiKey(String)} are used, the last
         * method invoked determines the effective authentication.</p>
         *
         * <p>This configures standard bearer auth and sends
         * {@code Authorization: Bearer &lt;token&gt;}.</p>
         */
        public Builder apiKey(String apiKey) {
            this.authProvider = TokenAuth.of(apiKey);
            return this;
        }

        public Builder tenant(Tenant tenant) { this.tenant = tenant; return this; }

        public Builder tenant(String tenant) { this.tenant = Tenant.of(tenant); return this; }

        public Builder database(Database database) { this.database = database; return this; }

        public Builder database(String database) { this.database = Database.of(database); return this; }

        public Builder timeout(Duration timeout) {
            this.connectTimeout = timeout;
            this.readTimeout = timeout;
            this.writeTimeout = timeout;
            return this;
        }

        public Builder connectTimeout(Duration timeout) { this.connectTimeout = timeout; return this; }

        public Builder readTimeout(Duration timeout) { this.readTimeout = timeout; return this; }

        public Builder writeTimeout(Duration timeout) { this.writeTimeout = timeout; return this; }

        public Builder defaultHeaders(Map<String, String> headers) {
            this.defaultHeaders = headers == null ? null : new LinkedHashMap<String, String>(headers);
            return this;
        }

        /**
         * Overrides HTTP transport with a fully configured OkHttp client.
         *
         * <p>When set, timeout and TLS builder options must be configured on the provided
         * client directly.</p>
         */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
            return this;
        }

        /**
         * Configures a custom CA certificate for TLS verification.
         */
        public Builder sslCert(Path certPath) {
            this.sslCertPath = Objects.requireNonNull(certPath, "certPath");
            return this;
        }

        /**
         * Enables or disables insecure TLS mode (trust-all, hostname verification disabled).
         *
         * <p>For development use only.</p>
         */
        public Builder insecure(boolean insecure) {
            this.insecure = insecure;
            return this;
        }

        /**
         * Sets a structured logger for transport-level request/response events.
         */
        public Builder logger(ChromaLogger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        /**
         * Resolves tenant value from an environment variable.
         */
        public Builder tenantFromEnv(String envVarName) {
            this.tenant = Tenant.of(requireNonBlankEnvVar(envVarName, "tenant"));
            return this;
        }

        /**
         * Resolves database value from an environment variable.
         */
        public Builder databaseFromEnv(String envVarName) {
            this.database = Database.of(requireNonBlankEnvVar(envVarName, "database"));
            return this;
        }

        /**
         * Convenience for reading {@code CHROMA_TENANT} and {@code CHROMA_DATABASE}.
         */
        public Builder tenantAndDatabaseFromEnv() {
            return tenantFromEnv(DEFAULT_TENANT_ENV).databaseFromEnv(DEFAULT_DATABASE_ENV);
        }

        public Client build() {
            String effectiveBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
            Tenant effectiveTenant = tenant != null ? tenant : Tenant.defaultTenant();
            Database effectiveDatabase = database != null ? database : Database.defaultDatabase();
            OkHttpClient resolvedHttpClient = buildHttpClient();
            boolean ownsHttpClient = httpClient == null;
            ChromaApiClient apiClient = new ChromaApiClient(
                    effectiveBaseUrl,
                    authProvider,
                    defaultHeaders,
                    resolvedHttpClient,
                    ownsHttpClient,
                    logger == null ? ChromaLogger.noop() : logger);
            return new ChromaClientImpl(apiClient, effectiveTenant, effectiveDatabase);
        }

        private OkHttpClient buildHttpClient() {
            if (httpClient != null) {
                if (connectTimeout != null || readTimeout != null || writeTimeout != null) {
                    throw new IllegalStateException(
                            "httpClient cannot be combined with connectTimeout/readTimeout/writeTimeout");
                }
                if (sslCertPath != null || insecure) {
                    throw new IllegalStateException(
                            "httpClient cannot be combined with sslCert/insecure TLS options");
                }
                return httpClient;
            }

            if (sslCertPath != null && insecure) {
                throw new IllegalStateException("sslCert and insecure cannot both be enabled");
            }

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (connectTimeout != null) {
                builder.connectTimeout(connectTimeout);
            }
            if (readTimeout != null) {
                builder.readTimeout(readTimeout);
            }
            if (writeTimeout != null) {
                builder.writeTimeout(writeTimeout);
            }

            if (sslCertPath != null) {
                applyCustomCaCertificate(builder, sslCertPath);
            } else if (insecure) {
                applyInsecureTls(builder);
            }

            return builder.build();
        }

        private static String requireNonBlankEnvVar(String envVarName, String target) {
            String variableName = requireNonBlank("envVarName", envVarName);
            String value = System.getenv(variableName);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException(
                        "Environment variable " + variableName + " is required for " + target);
            }
            return value.trim();
        }

        private static final X509TrustManager INSECURE_TRUST_MANAGER = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }
        };

        private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = (hostname, session) -> true;

        private static void applyInsecureTls(OkHttpClient.Builder builder) {
            SSLContext sslContext = newTlsContext(new TrustManager[]{INSECURE_TRUST_MANAGER});
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(socketFactory, INSECURE_TRUST_MANAGER);
            builder.hostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
        }

        private static void applyCustomCaCertificate(OkHttpClient.Builder builder, Path certPath) {
            X509TrustManager trustManager = loadTrustManagerFromCertificate(certPath);
            SSLContext sslContext = newTlsContext(new TrustManager[]{trustManager});
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }

        private static X509TrustManager loadTrustManagerFromCertificate(Path certPath) {
            java.util.Collection<? extends Certificate> certificates;
            try (InputStream inputStream = Files.newInputStream(certPath)) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                certificates = certificateFactory.generateCertificates(inputStream);
            } catch (IOException | GeneralSecurityException e) {
                throw new IllegalArgumentException(
                        "Failed to load SSL certificate from " + certPath + ": " + e.getMessage(),
                        e
                );
            }

            if (certificates == null || certificates.isEmpty()) {
                throw new IllegalArgumentException("No certificates found in " + certPath);
            }

            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);

                int index = 0;
                for (Certificate certificate : certificates) {
                    keyStore.setCertificateEntry("chroma-ca-" + index, certificate);
                    index++;
                }

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Expected exactly one X509TrustManager");
                }
                return (X509TrustManager) trustManagers[0];
            } catch (GeneralSecurityException | IOException e) {
                throw new IllegalArgumentException(
                        "Failed to initialize SSL trust manager from " + certPath + ": " + e.getMessage(),
                        e
                );
            }
        }

        private static SSLContext newTlsContext(TrustManager[] trustManagers) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, new SecureRandom());
                return sslContext;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to initialize TLS context", e);
            }
        }
    }

    public static final class CloudBuilder {
        private String apiKey;
        private String tenant;
        private String database;
        private Duration timeout;

        CloudBuilder() {}

        /**
         * Sets Chroma Cloud API key.
         *
         * <p>This configures Chroma Cloud token auth and sends
         * {@code X-Chroma-Token: &lt;token&gt;} (not bearer auth).</p>
         */
        public CloudBuilder apiKey(String apiKey) {
            this.apiKey = requireNonBlank("apiKey", apiKey);
            return this;
        }

        public CloudBuilder tenant(String tenant) {
            this.tenant = Tenant.of(tenant).getName();
            return this;
        }

        public CloudBuilder database(String database) {
            this.database = Database.of(database).getName();
            return this;
        }

        public CloudBuilder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public Client build() {
            if (apiKey == null) {
                throw new IllegalStateException("apiKey is required for Chroma Cloud");
            }
            if (tenant == null) {
                throw new IllegalStateException("tenant is required for Chroma Cloud");
            }
            if (database == null) {
                throw new IllegalStateException("database is required for Chroma Cloud");
            }
            ChromaApiClient apiClient = new ChromaApiClient(
                    CLOUD_BASE_URL, ChromaTokenAuth.of(apiKey), null,
                    timeout, timeout, timeout);
            return new ChromaClientImpl(apiClient, Tenant.of(tenant), Database.of(database));
        }
    }

    private static String requireNonBlank(String fieldName, String value) {
        String nonNullValue = Objects.requireNonNull(value, fieldName);
        String normalized = nonNullValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    // --- Private implementation ---

    private static final class ChromaClientImpl implements Client {

        private final ChromaApiClient apiClient;
        private final AtomicReference<SessionContext> sessionContext;

        ChromaClientImpl(ChromaApiClient apiClient, Tenant tenant, Database database) {
            this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
            this.sessionContext = new AtomicReference<SessionContext>(new SessionContext(
                    Objects.requireNonNull(tenant, "tenant"),
                    Objects.requireNonNull(database, "database")));
        }

        @Override
        public String heartbeat() {
            Map<String, Long> result = apiClient.get(
                    ChromaApiPaths.heartbeat(),
                    new TypeToken<Map<String, Long>>() {}.getType());
            Long value = result.get("nanosecond heartbeat");
            if (value == null) {
                throw new ChromaDeserializationException(
                        "Server returned heartbeat payload without required 'nanosecond heartbeat' field",
                        200
                );
            }
            return String.valueOf(value);
        }

        @Override
        public String version() {
            return apiClient.get(ChromaApiPaths.version(), String.class);
        }

        @Override
        public PreFlightInfo preFlight() {
            ChromaDtos.PreFlightResponse dto = apiClient.get(
                    ChromaApiPaths.preFlightChecks(),
                    ChromaDtos.PreFlightResponse.class);
            if (dto.maxBatchSize == null) {
                throw new ChromaDeserializationException(
                        "Server returned pre-flight payload without required max_batch_size field",
                        200
                );
            }
            int maxBatchSize = dto.maxBatchSize.intValue();
            if (maxBatchSize <= 0) {
                throw new ChromaDeserializationException(
                        "Server returned pre-flight payload with invalid max_batch_size field: " + dto.maxBatchSize,
                        200
                );
            }
            return new PreFlightInfo(maxBatchSize, dto.supportsBase64Encoding);
        }

        @Override
        public Identity getIdentity() {
            ChromaDtos.IdentityResponse dto = apiClient.get(
                    ChromaApiPaths.authIdentity(),
                    ChromaDtos.IdentityResponse.class);
            String userId = requireNonBlankField("identity.user_id", dto.userId);
            String tenantName = requireNonBlankField("identity.tenant", dto.tenant);
            List<String> databases = requireNonNullListField("identity.databases", dto.databases);
            List<String> normalizedDatabases = new ArrayList<String>(databases.size());
            for (int i = 0; i < databases.size(); i++) {
                normalizedDatabases.add(requireNonBlankField(
                        "identity.databases[" + i + "]", databases.get(i)));
            }
            return new Identity(userId, tenantName, normalizedDatabases);
        }

        @Override
        public void reset() {
            apiClient.post(ChromaApiPaths.reset(), Collections.emptyMap());
        }

        @Override
        public Tenant createTenant(String name) {
            String tenantName = requireNonBlank("name", name);
            apiClient.post(ChromaApiPaths.tenants(),
                    new ChromaDtos.CreateTenantRequest(tenantName),
                    ChromaDtos.TenantResponse.class);
            return Tenant.of(tenantName);
        }

        @Override
        public Tenant getTenant(String name) {
            String tenantName = requireNonBlank("name", name);
            ChromaDtos.TenantResponse dto = apiClient.get(
                    ChromaApiPaths.tenant(tenantName),
                    ChromaDtos.TenantResponse.class);
            return Tenant.of(requireNonBlankField("tenant.name", dto.name));
        }

        @Override
        public void useTenant(Tenant tenant) {
            Tenant validatedTenant = Objects.requireNonNull(tenant, "tenant");
            sessionContext.updateAndGet(current -> new SessionContext(
                    validatedTenant,
                    Database.defaultDatabase()));
        }

        @Override
        public Tenant currentTenant() {
            return sessionContext.get().tenant;
        }

        @Override
        public void useDatabase(Database database) {
            Database validatedDatabase = Objects.requireNonNull(database, "database");
            sessionContext.updateAndGet(current -> new SessionContext(
                    current.tenant,
                    validatedDatabase));
        }

        @Override
        public Database currentDatabase() {
            return sessionContext.get().database;
        }

        @Override
        public Database createDatabase(String name) {
            String databaseName = requireNonBlank("name", name);
            SessionContext context = sessionContext.get();
            apiClient.post(ChromaApiPaths.databases(context.tenant.getName()),
                    new ChromaDtos.CreateDatabaseRequest(databaseName),
                    ChromaDtos.DatabaseResponse.class);
            return Database.of(databaseName);
        }

        @Override
        public Database getDatabase(String name) {
            String databaseName = requireNonBlank("name", name);
            SessionContext context = sessionContext.get();
            ChromaDtos.DatabaseResponse dto = apiClient.get(
                    ChromaApiPaths.database(context.tenant.getName(), databaseName),
                    ChromaDtos.DatabaseResponse.class);
            return Database.of(requireNonBlankField("database.name", dto.name));
        }

        @Override
        public List<Database> listDatabases() {
            SessionContext context = sessionContext.get();
            List<ChromaDtos.DatabaseResponse> dtos = apiClient.get(
                    ChromaApiPaths.databases(context.tenant.getName()),
                    new TypeToken<List<ChromaDtos.DatabaseResponse>>() {}.getType());
            List<Database> result = new ArrayList<Database>(dtos.size());
            for (int i = 0; i < dtos.size(); i++) {
                ChromaDtos.DatabaseResponse dto = dtos.get(i);
                if (dto == null) {
                    throw new ChromaDeserializationException(
                            "Server returned databases list with null entry at index " + i,
                            200
                    );
                }
                result.add(Database.of(requireNonBlankField("database.name", dto.name)));
            }
            return result;
        }

        @Override
        public void deleteDatabase(String name) {
            String databaseName = requireNonBlank("name", name);
            SessionContext context = sessionContext.get();
            apiClient.delete(ChromaApiPaths.database(context.tenant.getName(), databaseName));
        }

        @Override
        public Collection createCollection(String name) {
            return createCollection(name, null);
        }

        @Override
        public Collection createCollection(String name, CreateCollectionOptions options) {
            return postCollection(requireNonBlank("name", name), options, false);
        }

        @Override
        public Collection getCollection(String name) {
            return getCollection(name, null);
        }

        @Override
        public Collection getCollection(String name,
                                        tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction) {
            String collectionName = requireNonBlank("name", name);
            SessionContext context = sessionContext.get();
            ChromaDtos.CollectionResponse dto = apiClient.get(
                    ChromaApiPaths.collectionByName(
                            context.tenant.getName(),
                            context.database.getName(),
                            collectionName),
                    ChromaDtos.CollectionResponse.class);
            return ChromaHttpCollection.from(
                    dto,
                    apiClient,
                    context.tenant,
                    context.database,
                    embeddingFunction
            );
        }

        @Override
        public Collection getOrCreateCollection(String name) {
            return getOrCreateCollection(name, null);
        }

        @Override
        public Collection getOrCreateCollection(String name, CreateCollectionOptions options) {
            return postCollection(requireNonBlank("name", name), options, true);
        }

        @Override
        public List<Collection> listCollections() {
            SessionContext context = sessionContext.get();
            List<ChromaDtos.CollectionResponse> dtos = apiClient.get(
                    ChromaApiPaths.collections(context.tenant.getName(), context.database.getName()),
                    new TypeToken<List<ChromaDtos.CollectionResponse>>() {}.getType());
            return toCollections(dtos, context);
        }

        @Override
        public List<Collection> listCollections(int limit, int offset) {
            if (limit < 0) {
                throw new IllegalArgumentException("limit must be >= 0");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be >= 0");
            }
            Map<String, String> queryParams = new LinkedHashMap<String, String>();
            queryParams.put("limit", String.valueOf(limit));
            queryParams.put("offset", String.valueOf(offset));
            SessionContext context = sessionContext.get();
            List<ChromaDtos.CollectionResponse> dtos = apiClient.get(
                    ChromaApiPaths.collections(context.tenant.getName(), context.database.getName()),
                    queryParams,
                    new TypeToken<List<ChromaDtos.CollectionResponse>>() {}.getType());
            return toCollections(dtos, context);
        }

        @Override
        public void deleteCollection(String name) {
            String collectionName = requireNonBlank("name", name);
            SessionContext context = sessionContext.get();
            apiClient.delete(ChromaApiPaths.collectionByName(
                    context.tenant.getName(),
                    context.database.getName(),
                    collectionName));
        }

        @Override
        public int countCollections() {
            SessionContext context = sessionContext.get();
            return apiClient.get(
                    ChromaApiPaths.collectionsCount(context.tenant.getName(), context.database.getName()),
                    Integer.class);
        }

        @Override
        public void close() {
            apiClient.close();
        }

        private Collection postCollection(String name, CreateCollectionOptions options, boolean getOrCreate) {
            Map<String, Object> metadata = options != null ? options.getMetadata() : null;
            CollectionConfiguration config = options != null ? options.getConfiguration() : null;
            Schema schema = options != null ? options.getSchema() : null;
            tech.amikos.chromadb.embeddings.EmbeddingFunction embeddingFunction =
                    options != null ? options.getEmbeddingFunction() : null;
            SessionContext context = sessionContext.get();
            ChromaDtos.CollectionResponse dto = apiClient.post(
                    ChromaApiPaths.collections(context.tenant.getName(), context.database.getName()),
                    new ChromaDtos.CreateCollectionRequest(
                            name, metadata,
                            ChromaDtos.toConfigurationMap(config),
                            ChromaDtos.toSchemaMap(schema),
                            getOrCreate),
                    ChromaDtos.CollectionResponse.class);
            return ChromaHttpCollection.from(
                    dto,
                    apiClient,
                    context.tenant,
                    context.database,
                    embeddingFunction
            );
        }

        private List<Collection> toCollections(List<ChromaDtos.CollectionResponse> dtos, SessionContext context) {
            if (dtos == null) {
                throw new ChromaDeserializationException(
                        "Server returned collections payload as null",
                        200
                );
            }
            List<Collection> result = new ArrayList<Collection>(dtos.size());
            for (int i = 0; i < dtos.size(); i++) {
                ChromaDtos.CollectionResponse dto = dtos.get(i);
                if (dto == null) {
                    throw new ChromaDeserializationException(
                            "Server returned collections list with null entry at index " + i,
                            200
                    );
                }
                result.add(ChromaHttpCollection.from(dto, apiClient, context.tenant, context.database, null));
            }
            return result;
        }

        private static final class SessionContext {
            private final Tenant tenant;
            private final Database database;

            private SessionContext(Tenant tenant, Database database) {
                this.tenant = Objects.requireNonNull(tenant, "tenant");
                this.database = Objects.requireNonNull(database, "database");
            }

            @Override
            public String toString() {
                return "SessionContext{"
                        + "tenant=" + tenant
                        + ", database=" + database
                        + '}';
            }
        }

        private static String requireNonBlankField(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new ChromaDeserializationException(
                        "Server returned invalid " + fieldName + " field",
                        200
                );
            }
            return value.trim();
        }

        private static <T> List<T> requireNonNullListField(String fieldName, List<T> value) {
            if (value == null) {
                throw new ChromaDeserializationException(
                        "Server returned payload without required " + fieldName + " field",
                        200
                );
            }
            return value;
        }
    }
}
