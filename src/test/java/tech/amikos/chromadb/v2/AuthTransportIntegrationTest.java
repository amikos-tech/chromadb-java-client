package tech.amikos.chromadb.v2;

import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration coverage for auth/header behavior on the live Testcontainers path.
 */
public class AuthTransportIntegrationTest extends AbstractChromaIntegrationTest {

    @Test
    public void testTokenAuthAndDefaultHeadersAppliedOnLiveRequest() {
        final AtomicReference<String> authorization = new AtomicReference<String>();
        final AtomicReference<String> chromaToken = new AtomicReference<String>();
        final AtomicReference<String> trace = new AtomicReference<String>();

        OkHttpClient instrumented = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    authorization.set(chain.request().header("Authorization"));
                    chromaToken.set(chain.request().header("X-Chroma-Token"));
                    trace.set(chain.request().header("X-Test-Trace"));
                    return chain.proceed(chain.request());
                })
                .build();

        Client tokenClient = ChromaClient.builder()
                .baseUrl(endpoint())
                .tenant(tenantName)
                .database(databaseName)
                .httpClient(instrumented)
                .auth(TokenAuth.of("integration-token"))
                .defaultHeaders(Collections.singletonMap("X-Test-Trace", "trace-1"))
                .build();

        try {
            tokenClient.heartbeat();
            assertEquals("Bearer integration-token", authorization.get());
            assertNull(chromaToken.get());
            assertEquals("trace-1", trace.get());
        } finally {
            tokenClient.close();
        }
    }

    @Test
    public void testChromaTokenAuthAppliedOnLiveRequest() {
        final AtomicReference<String> authorization = new AtomicReference<String>();
        final AtomicReference<String> chromaToken = new AtomicReference<String>();

        OkHttpClient instrumented = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    authorization.set(chain.request().header("Authorization"));
                    chromaToken.set(chain.request().header("X-Chroma-Token"));
                    return chain.proceed(chain.request());
                })
                .build();

        Client cloudTokenClient = ChromaClient.builder()
                .baseUrl(endpoint())
                .tenant(tenantName)
                .database(databaseName)
                .httpClient(instrumented)
                .auth(ChromaTokenAuth.of("integration-chroma-token"))
                .build();

        try {
            cloudTokenClient.version();
            assertNull(authorization.get());
            assertEquals("integration-chroma-token", chromaToken.get());
        } finally {
            cloudTokenClient.close();
        }
    }

    @Test
    public void testBuilderRejectsReservedAuthHeaderBeforeNetworkCall() {
        try {
            ChromaClient.builder()
                    .baseUrl(endpoint())
                    .defaultHeaders(Collections.singletonMap("Authorization", "Basic stale"))
                    .build();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("auth(...)"));
        }
    }
}
