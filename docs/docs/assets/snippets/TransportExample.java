import okhttp3.OkHttpClient;
import tech.amikos.chromadb.v2.*;

import java.nio.file.Paths;
import java.time.Duration;

public class TransportExample {

    public static void main(String[] args) {
        // --8<-- [start:ssl-cert]
        Client clientWithSsl = ChromaClient.builder()
                .baseUrl("https://your-chroma-host")
                .sslCert(Paths.get("/path/to/ca-cert.pem"))
                .build();
        // --8<-- [end:ssl-cert]

        // --8<-- [start:custom-timeouts]
        Client clientWithTimeouts = ChromaClient.builder()
                .baseUrl("https://your-chroma-host")
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
        // --8<-- [end:custom-timeouts]

        // --8<-- [start:custom-http]
        OkHttpClient custom = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(20))
                .build();

        Client clientWithCustomHttp = ChromaClient.builder()
                .baseUrl("https://your-chroma-host")
                .httpClient(custom)
                .build();
        // --8<-- [end:custom-http]

        // --8<-- [start:insecure]
        Client insecureClient = ChromaClient.builder()
                .baseUrl("https://localhost:8000")
                .insecure(true)
                .build();
        // --8<-- [end:insecure]

        // --8<-- [start:env-tenant]
        Client envClient = ChromaClient.builder()
                .baseUrl("https://your-chroma-host")
                .tenantFromEnv("CHROMA_TENANT")
                .databaseFromEnv("CHROMA_DATABASE")
                .build();
        // --8<-- [end:env-tenant]

        // --8<-- [start:full-example]
        Client fullClient = ChromaClient.builder()
                .baseUrl("https://your-chroma-host")
                .sslCert(Paths.get("/path/to/ca-cert.pem"))
                .tenantFromEnv("CHROMA_TENANT")
                .databaseFromEnv("CHROMA_DATABASE")
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        // --8<-- [end:full-example]
    }
}
