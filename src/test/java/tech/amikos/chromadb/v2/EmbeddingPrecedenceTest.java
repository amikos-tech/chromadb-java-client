package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Contract tests for the EF precedence chain in {@link ChromaHttpCollection}.
 *
 * <p>Verifies that runtime/explicit EF always wins, auto-wire from spec is logged at FINE level,
 * and a WARNING is logged when explicit EF overrides a persisted spec.</p>
 */
public class EmbeddingPrecedenceTest {

    private static final String COLLECTIONS_PATH =
            "/api/v2/tenants/default_tenant/databases/default_database/collections";

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private Client client;

    @Before
    public void setUp() {
        client = ChromaClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // --- helper: fixed embedding function ---

    private static EmbeddingFunction fixedEmbeddingFunction(final float[] vector) {
        return new EmbeddingFunction() {
            @Override
            public Embedding embedQuery(String query) throws EFException {
                return new Embedding(vector);
            }

            @Override
            public List<Embedding> embedDocuments(List<String> documents) throws EFException {
                List<Embedding> result = new ArrayList<Embedding>(documents.size());
                for (int i = 0; i < documents.size(); i++) {
                    result.add(new Embedding(vector));
                }
                return result;
            }

            @Override
            public List<Embedding> embedDocuments(String[] documents) throws EFException {
                return embedDocuments(Arrays.asList(documents));
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

    // --- log capture helper ---

    private static final class TestHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<LogRecord>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        public boolean hasMessage(String fragment) {
            for (LogRecord record : records) {
                if (record.getMessage() != null && record.getMessage().contains(fragment)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasMessage(Level level, String fragment) {
            for (LogRecord record : records) {
                if (record.getLevel().equals(level) && record.getMessage() != null
                        && record.getMessage().contains(fragment)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * When a runtime EF is set AND a persisted spec exists, the explicit EF wins.
     * Its embeddings appear in the query; no ChromaException about the unknown spec provider.
     */
    @Test
    public void testExplicitEFWinsOverConfigSpec() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/explicit_wins_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-explicit-wins\",\"name\":\"explicit_wins_col\","
                                + "\"configuration_json\":{\"embedding_function\":{"
                                + "\"type\":\"known\","
                                + "\"name\":\"consistent_hash\","
                                + "\"config\":{}}}}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-explicit-wins/query"))
                .withRequestBody(matchingJsonPath("$.query_embeddings[0][0]", equalTo("0.42")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Collection col = client.getCollection(
                "explicit_wins_col",
                fixedEmbeddingFunction(new float[]{0.42f})
        );

        // Should succeed using the explicit EF (not try to resolve the "consistent_hash" spec)
        QueryResult result = col.query()
                .queryTexts("hello")
                .execute();

        assertNotNull(result);
        assertEquals("id1", result.getIds().get(0).get(0));
        verify(postRequestedFor(urlEqualTo(COLLECTIONS_PATH + "/col-explicit-wins/query")));
    }

    /**
     * When no explicit EF is provided, the spec from collection configuration is resolved and used.
     * When the spec provider is unsupported, ChromaException is thrown (proving the spec path was taken).
     */
    @Test
    public void testConfigSpecUsedWhenNoExplicitEF() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/spec_only_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-spec-only\",\"name\":\"spec_only_col\","
                                + "\"configuration_json\":{\"embedding_function\":{"
                                + "\"type\":\"known\","
                                + "\"name\":\"consistent_hash\","
                                + "\"config\":{}}}}")));

        Collection col = client.getCollection("spec_only_col");

        // No explicit EF => spec resolution attempted => consistent_hash is unsupported => ChromaException
        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException from spec resolution");
        } catch (ChromaException e) {
            // The spec was resolved (auto-wired path), resolution fails with unknown provider message
            assertTrue(
                "Expected message about 'consistent_hash', got: " + e.getMessage(),
                e.getMessage().contains("consistent_hash")
            );
        }
    }

    /**
     * When both an explicit EF and a persisted spec are present, a WARNING is logged.
     */
    @Test
    public void testWarningLoggedWhenExplicitOverridesSpec() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/warning_log_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-warning-log\",\"name\":\"warning_log_col\","
                                + "\"configuration_json\":{\"embedding_function\":{"
                                + "\"type\":\"known\","
                                + "\"name\":\"my-persisted-ef\","
                                + "\"config\":{}}}}")));
        stubFor(post(urlEqualTo(COLLECTIONS_PATH + "/col-warning-log/query"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[[\"id1\"]]}")));

        Logger logger = Logger.getLogger(ChromaHttpCollection.class.getName());
        Level originalLevel = logger.getLevel();
        TestHandler handler = new TestHandler();
        logger.addHandler(handler);
        try {
            Collection col = client.getCollection(
                    "warning_log_col",
                    fixedEmbeddingFunction(new float[]{0.5f})
            );

            col.query().queryTexts("hello").execute();

            assertTrue(
                "Expected WARNING log containing 'overrides persisted collection EF'",
                handler.hasMessage("overrides persisted collection EF")
            );
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
    }

    /**
     * When no explicit EF is set and a spec is auto-wired, a FINE-level log is emitted.
     */
    @Test
    public void testAutoWireLoggedAtFineLevel() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/autowire_log_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-autowire-log\",\"name\":\"autowire_log_col\","
                                + "\"configuration_json\":{\"embedding_function\":{"
                                + "\"type\":\"known\","
                                + "\"name\":\"my-spec-ef\","
                                + "\"config\":{}}}}")));

        Logger logger = Logger.getLogger(ChromaHttpCollection.class.getName());
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.FINE);
        TestHandler handler = new TestHandler();
        logger.addHandler(handler);
        try {
            Collection col = client.getCollection("autowire_log_col");

            // Trigger resolution (will fail with unsupported provider, but log fires first)
            try {
                col.query().queryTexts("hello").execute();
            } catch (ChromaException ignored) {
                // Expected: unsupported provider
            }

            assertTrue(
                "Expected FINE log containing 'Auto-wired embedding function:'",
                handler.hasMessage(Level.FINE, "Auto-wired embedding function:")
            );
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
    }

    /**
     * When neither explicit EF nor spec is available, ChromaException is thrown with
     * message explaining what to do.
     */
    @Test
    public void testNoEFThrowsChromaException() {
        stubFor(get(urlEqualTo(COLLECTIONS_PATH + "/no_ef_col"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"col-no-ef\",\"name\":\"no_ef_col\"}")));

        Collection col = client.getCollection("no_ef_col");

        try {
            col.query().queryTexts("hello").execute();
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(
                "Expected message about 'queryTexts requires an embedding function', got: " + e.getMessage(),
                e.getMessage().contains("queryTexts requires an embedding function")
            );
        }
    }
}
