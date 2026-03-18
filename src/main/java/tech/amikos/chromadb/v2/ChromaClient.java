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
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
        private static final String AUTH_SETTER_AUTH = "auth(...)";
        private static final String AUTH_SETTER_API_KEY = "apiKey(...)";
        private static final String AUTHORIZATION_HEADER = "Authorization";
        private static final String CHROMA_TOKEN_HEADER = "X-Chroma-Token";

        private String baseUrl;
        private AuthProvider authProvider;
        private String authSetter;
        private int authStrategyCount;
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
         */
        public Builder auth(AuthProvider authProvider) {
            return configureAuth(
                    Objects.requireNonNull(authProvider, "authProvider"),
                    AUTH_SETTER_AUTH
            );
        }

        /**
         * Convenience for {@code auth(TokenAuth.of(apiKey))}.
         *
         * <p>This configures standard bearer auth and sends
         * {@code Authorization: Bearer &lt;token&gt;}.</p>
         */
        public Builder apiKey(String apiKey) {
            return configureAuth(TokenAuth.of(apiKey), AUTH_SETTER_API_KEY);
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
            Map<String, String> headerCopy = headers == null ? null : new LinkedHashMap<String, String>(headers);
            validateNoReservedAuthHeaders(headerCopy);
            this.defaultHeaders = headerCopy;
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
         * Configures a custom CA certificate file (PEM or DER) for TLS verification.
         *
         * <p>Certificate loading happens eagerly at {@link #build()} time. The provided certificate(s)
         * are added in addition to the default JVM trust store (not a replacement).</p>
         *
         * <p>Mutually exclusive with {@link #insecure(boolean)}.</p>
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
         * Resolves tenant value from an environment variable name.
         *
         * <p>Resolution happens immediately when this method is called.</p>
         *
         * @throws IllegalStateException if the variable is missing or blank
         */
        public Builder tenantFromEnv(String envVarName) {
            this.tenant = Tenant.of(requireNonBlankEnvVar(envVarName, "tenant"));
            return this;
        }

        /**
         * Resolves database value from an environment variable name.
         *
         * <p>Resolution happens immediately when this method is called.</p>
         *
         * @throws IllegalStateException if the variable is missing or blank
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
            validateAuthConfiguration();
            validateNoReservedAuthHeaders(defaultHeaders);
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
                    logger);
            return new ChromaClientImpl(apiClient, effectiveTenant, effectiveDatabase);
        }

        private Builder configureAuth(AuthProvider provider, String setterName) {
            if (authStrategyCount >= 1) {
                throw new IllegalStateException(buildAuthStrategyConflictMessage(setterName));
            }
            this.authProvider = Objects.requireNonNull(provider, "provider");
            this.authSetter = setterName;
            this.authStrategyCount = 1;
            return this;
        }

        private void validateAuthConfiguration() {
            if (authStrategyCount < 0 || authStrategyCount > 1) {
                throw new IllegalStateException("Exactly one auth strategy can be configured per builder instance");
            }
            if ((authProvider == null) != (authStrategyCount == 0)) {
                throw new IllegalStateException(
                        "Builder auth state is inconsistent; configure credentials exactly once via auth(...)");
            }
        }

        private String buildAuthStrategyConflictMessage(String attemptedSetter) {
            return "Auth strategy already configured via " + authSetter
                    + "; cannot also configure " + attemptedSetter
                    + ". Configure exactly one auth strategy per builder instance via auth(...).";
        }

        private void validateNoReservedAuthHeaders(Map<String, String> headers) {
            if (headers == null) {
                return;
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String name = entry.getKey();
                if (name == null) {
                    continue;
                }
                String trimmedName = name.trim();
                if (AUTHORIZATION_HEADER.equalsIgnoreCase(trimmedName)
                        || CHROMA_TOKEN_HEADER.equalsIgnoreCase(trimmedName)) {
                    throw new IllegalArgumentException(
                            "defaultHeaders must not include " + AUTHORIZATION_HEADER + " or "
                                    + CHROMA_TOKEN_HEADER
                                    + "; configure credentials via auth(...).");
                }
            }
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
            X509TrustManager trustManager = loadAugmentedTrustManager(certPath);
            SSLContext sslContext = newTlsContext(new TrustManager[]{trustManager});
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }

        private static X509TrustManager loadAugmentedTrustManager(Path certPath) {
            java.util.Collection<? extends Certificate> certificates = loadCertificates(certPath);
            X509TrustManager defaultTrustManager = loadDefaultTrustManager();
            X509TrustManager customTrustManager = loadTrustManagerFromCertificates(certPath, certificates);
            return new CompositeX509TrustManager(customTrustManager, defaultTrustManager);
        }

        private static java.util.Collection<? extends Certificate> loadCertificates(Path certPath) {
            try (InputStream inputStream = Files.newInputStream(certPath)) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                java.util.Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(inputStream);
                if (certificates == null || certificates.isEmpty()) {
                    throw new IllegalArgumentException("No certificates found in " + certPath);
                }
                return certificates;
            } catch (IOException | CertificateException e) {
                throw new IllegalArgumentException(
                        "Failed to load SSL certificate from " + certPath + ": " + e.getMessage(),
                        e
                );
            }
        }

        private static X509TrustManager loadDefaultTrustManager() {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                return requireSingleX509TrustManager(
                        trustManagerFactory.getTrustManagers(),
                        "default JVM trust store"
                );
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(
                        "Failed to initialize default JVM trust manager: " + e.getMessage(),
                        e
                );
            }
        }

        private static X509TrustManager loadTrustManagerFromCertificates(
                Path certPath,
                java.util.Collection<? extends Certificate> certificates
        ) {
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
                return requireSingleX509TrustManager(
                        trustManagerFactory.getTrustManagers(),
                        "custom certificate trust store " + certPath
                );
            } catch (GeneralSecurityException | IOException e) {
                throw new IllegalStateException(
                        "Failed to initialize SSL trust manager from " + certPath + ": " + e.getMessage(),
                        e
                );
            }
        }

        private static X509TrustManager requireSingleX509TrustManager(
                TrustManager[] trustManagers,
                String source
        ) {
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Expected exactly one X509TrustManager from " + source);
            }
            return (X509TrustManager) trustManagers[0];
        }

        private static SSLContext newTlsContext(TrustManager[] trustManagers) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustManagers, new SecureRandom());
                return sslContext;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to initialize TLS context", e);
            }
        }

        private static final class CompositeX509TrustManager implements X509TrustManager {
            private final X509TrustManager primary;
            private final X509TrustManager fallback;

            private CompositeX509TrustManager(X509TrustManager primary, X509TrustManager fallback) {
                this.primary = primary;
                this.fallback = fallback;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegateCheck(chain, authType, X509TrustManager::checkClientTrusted);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                delegateCheck(chain, authType, X509TrustManager::checkServerTrusted);
            }

            private void delegateCheck(X509Certificate[] chain, String authType,
                    TrustCheck check) throws CertificateException {
                try {
                    check.verify(primary, chain, authType);
                } catch (CertificateException primaryFailure) {
                    try {
                        check.verify(fallback, chain, authType);
                    } catch (CertificateException fallbackFailure) {
                        primaryFailure.addSuppressed(fallbackFailure);
                        throw primaryFailure;
                    }
                }
            }

            @FunctionalInterface
            private interface TrustCheck {
                void verify(X509TrustManager tm, X509Certificate[] chain, String authType)
                        throws CertificateException;
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] primaryIssuers = primary.getAcceptedIssuers();
                X509Certificate[] fallbackIssuers = fallback.getAcceptedIssuers();
                X509Certificate[] issuers = new X509Certificate[primaryIssuers.length + fallbackIssuers.length];
                System.arraycopy(primaryIssuers, 0, issuers, 0, primaryIssuers.length);
                System.arraycopy(fallbackIssuers, 0, issuers, primaryIssuers.length, fallbackIssuers.length);
                return issuers;
            }
        }
    }

    public static final class CloudBuilder {
        private static final String AUTH_SETTER_API_KEY = "apiKey(...)";

        private AuthProvider authProvider;
        private String authSetter;
        private int authStrategyCount;
        private String tenant;
        private String database;
        private Duration timeout;
        private ChromaLogger logger;

        CloudBuilder() {}

        /**
         * Sets Chroma Cloud API key.
         *
         * <p>This configures Chroma Cloud token auth and sends
         * {@code X-Chroma-Token: &lt;token&gt;} (not bearer auth).</p>
         */
        public CloudBuilder apiKey(String apiKey) {
            return configureAuth(
                    ChromaTokenAuth.of(requireNonBlank("apiKey", apiKey)),
                    AUTH_SETTER_API_KEY
            );
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

        /**
         * Sets a structured logger for transport-level request/response events.
         */
        public CloudBuilder logger(ChromaLogger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public Client build() {
            validateAuthConfiguration();
            if (authProvider == null) {
                throw new IllegalStateException("apiKey is required for Chroma Cloud");
            }
            if (tenant == null) {
                throw new IllegalStateException("tenant is required for Chroma Cloud");
            }
            if (database == null) {
                throw new IllegalStateException("database is required for Chroma Cloud");
            }
            Builder delegate = ChromaClient.builder()
                    .baseUrl(CLOUD_BASE_URL)
                    .auth(authProvider)
                    .tenant(tenant)
                    .database(database);
            if (timeout != null) {
                delegate.timeout(timeout);
            }
            if (logger != null) {
                delegate.logger(logger);
            }
            return delegate.build();
        }

        private CloudBuilder configureAuth(AuthProvider provider, String setterName) {
            if (authStrategyCount >= 1) {
                throw new IllegalStateException(buildAuthStrategyConflictMessage(setterName));
            }
            this.authProvider = Objects.requireNonNull(provider, "provider");
            this.authSetter = setterName;
            this.authStrategyCount = 1;
            return this;
        }

        private void validateAuthConfiguration() {
            if (authStrategyCount < 0 || authStrategyCount > 1) {
                throw new IllegalStateException("Exactly one auth strategy can be configured per builder instance");
            }
            if ((authProvider == null) != (authStrategyCount == 0)) {
                throw new IllegalStateException(
                        "Builder auth state is inconsistent; configure credentials exactly once via auth(...)");
            }
        }

        private String buildAuthStrategyConflictMessage(String attemptedSetter) {
            return "Auth strategy already configured via " + authSetter
                    + "; cannot also configure " + attemptedSetter
                    + ". Configure exactly one auth strategy per builder instance via auth(...).";
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

        private static final String PREFLIGHT_ENDPOINT = ChromaApiPaths.preFlightChecks();
        private static final String IDENTITY_ENDPOINT = ChromaApiPaths.authIdentity();
        private static final String AUTH_HINT =
                "Verify your Chroma credentials and that your API key/token has access to the configured tenant/database.";

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
            ChromaDtos.PreFlightResponse dto = getAuthProtected(
                    PREFLIGHT_ENDPOINT,
                    ChromaDtos.PreFlightResponse.class,
                    "pre-flight checks");
            if (dto.maxBatchSize == null) {
                throw new ChromaDeserializationException(
                        "Server returned invalid payload from " + PREFLIGHT_ENDPOINT
                                + ": missing required field 'max_batch_size'",
                        200
                );
            }
            int maxBatchSize = dto.maxBatchSize.intValue();
            if (maxBatchSize <= 0) {
                throw new ChromaDeserializationException(
                        "Server returned invalid payload from " + PREFLIGHT_ENDPOINT
                                + ": invalid field 'max_batch_size' value: " + dto.maxBatchSize,
                        200
                );
            }
            return new PreFlightInfo(maxBatchSize, dto.supportsBase64Encoding);
        }

        @Override
        public Identity getIdentity() {
            ChromaDtos.IdentityResponse dto = getAuthProtected(
                    IDENTITY_ENDPOINT,
                    ChromaDtos.IdentityResponse.class,
                    "identity");
            String userId = requireNonBlankField(IDENTITY_ENDPOINT, "identity.user_id", dto.userId);
            String tenantName = requireNonBlankField(IDENTITY_ENDPOINT, "identity.tenant", dto.tenant);
            List<String> databases = requireNonNullListField(IDENTITY_ENDPOINT, "identity.databases", dto.databases);
            List<String> normalizedDatabases = new ArrayList<String>(databases.size());
            for (int i = 0; i < databases.size(); i++) {
                normalizedDatabases.add(requireNonBlankField(
                        IDENTITY_ENDPOINT, "identity.databases[" + i + "]", databases.get(i)));
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

        private static String requireNonBlankField(String endpoint, String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new ChromaDeserializationException(
                        "Server returned invalid payload from " + endpoint
                                + ": invalid field '" + fieldName + "'",
                        200
                );
            }
            return value.trim();
        }

        private static <T> List<T> requireNonNullListField(String endpoint, String fieldName, List<T> value) {
            if (value == null) {
                throw new ChromaDeserializationException(
                        "Server returned invalid payload from " + endpoint
                                + ": missing required field '" + fieldName + "'",
                        200
                );
            }
            return value;
        }

        private <T> T getAuthProtected(String endpoint, Type responseType, String operation) {
            try {
                return apiClient.get(endpoint, responseType);
            } catch (ChromaUnauthorizedException e) {
                throw asUnauthorized(e, endpoint, operation);
            } catch (ChromaForbiddenException e) {
                throw asUnauthorized(e, endpoint, operation);
            }
        }

        private ChromaUnauthorizedException asUnauthorized(ChromaException cause, String endpoint, String operation) {
            String message = "Authentication failed for " + operation + " endpoint " + endpoint
                    + " (HTTP " + cause.getStatusCode() + "). " + AUTH_HINT;
            return new ChromaUnauthorizedException(message, cause.getErrorCode(), cause);
        }
    }
}
