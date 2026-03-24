import tech.amikos.chromadb.v2.*;

// --8<-- [start:basic]
Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(BasicAuth.of("admin", "password"))
        .build();
// --8<-- [end:basic]

// --8<-- [start:token]
Client tokenClient = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(TokenAuth.of(System.getenv("CHROMA_TOKEN")))
        .build();
// --8<-- [end:token]

// --8<-- [start:chroma-token]
Client chromaClient = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(ChromaTokenAuth.of(System.getenv("CHROMA_TOKEN")))
        .build();
// --8<-- [end:chroma-token]

// --8<-- [start:cloud]
Client cloudClient = ChromaClient.cloud()
        .apiKey(System.getenv("CHROMA_API_KEY"))
        .tenant(System.getenv("CHROMA_TENANT"))
        .database(System.getenv("CHROMA_DATABASE"))
        .build();
// --8<-- [end:cloud]
