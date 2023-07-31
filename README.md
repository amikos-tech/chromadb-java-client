# Chroma Vector Database Java Client

This is a very basic/naive implementation in Java of the Chroma Vector Database API.

| Important: For now it only supports OpenAI embeddings.

## TODO

- [ ] Add support for other embedding functions
- [ ] Push the package to Maven Central - https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven

## Usage

Clone the repository and install the package locally:

```bash
git clone git@github.com:amikos-tech/chromadb-java-client.git
```

Install dependencies:

```bash
mvn clean compile
```

Ensure you have a running instance of Chroma running. We recommend one of the two following options:

- Official documentation - https://docs.trychroma.com/usage-guide#running-chroma-in-clientserver-mode
- If you are a fan of Kubernetes, you can use the Helm chart - https://github.com/amikos-tech/chromadb-chart (Note: You
  will need `Docker`, `minikube` and `kubectl` installed)

Run tests:

| **Important**: Since we are using the OpenAI API, you need to set the `OPENAI_API_KEY` environment variable. Simply
create `.env` file in the root of the repository.

```bash
mvn test
```

## Example

```java
import com.google.gson.internal.LinkedTreeMap;
import io.github.cdimascio.dotenv.Dotenv;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;

class TestApi {
    public void testQueryExample() throws ApiException {
        Client client = new Client("http://localhost:8000");
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("OPENAI_API_KEY");
        EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey);
        Collection collection = client.createCollection("test-collection", null, true, ef);
        List<Map<String, String>> metadata = new ArrayList<>();
        metadata.add(new HashMap<String, String>() {{
            put("type", "scientist");
        }});
        metadata.add(new HashMap<String, String>() {{
            put("type", "spy");
        }});
        collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
        LinkedTreeMap<String, Object> qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
        System.out.println(qr);
    }
}
```

The above should output:

```bash
{ids=[[2, 1]], distances=[[0.28461432651150426, 0.5096168232841949]], metadatas=[[{key=value}, {key=value}]], embeddings=null, documents=[[Hello, my name is Bond. I am a Spy., Hello, my name is John. I am a Data Scientist.]]}
```

## Development Notes

We have made some minor changes on top of the ChromaDB API (`src/main/resources/openapi/api.yaml`) so that the API can
work with Java and Swagger Codegen. The reason is that statically type languages like Java don't like the `anyOf`
and `oneOf` keywords (This also is the reason why we don't use the generated java client for OpenAI API).

## Contributing

Pull requests are welcome.

## References

- https://docs.trychroma.com/ - Official Chroma documentation
- https://github.com/amikos-tech/chromadb-chart - Chroma Helm chart for cloud-native deployments
- https://github.com/openai/openai-openapi - OpenAI OpenAPI specification (While we don't use it to generate a client
  for Java, it helps us understand the API better)
