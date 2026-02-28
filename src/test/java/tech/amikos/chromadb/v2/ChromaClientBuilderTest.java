package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.time.Duration;
import java.util.Collections;

public class ChromaClientBuilderTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testBuilderBuildThrowsNotYetImplemented() {
        ChromaClient.builder().build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCloudBuilderBuildThrowsNotYetImplemented() {
        ChromaClient.cloud().build();
    }

    @Test
    public void testBuilderFluentChaining() {
        // Should not throw — verifies that setters return the builder and store values
        ChromaClient.Builder builder = ChromaClient.builder()
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
                .defaultHeaders(Collections.<String, String>emptyMap());

        // builder is fully configured; build() is not yet implemented
        try {
            builder.build();
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testCloudBuilderFluentChaining() {
        ChromaClient.CloudBuilder builder = ChromaClient.cloud()
                .apiKey("key")
                .tenant("t")
                .database("d")
                .timeout(Duration.ofSeconds(30));

        try {
            builder.build();
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
