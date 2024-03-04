# Chroma Vector Database Java Client

This is a very basic/naive implementation in Java of the Chroma Vector Database API.

This client works with Chroma Versions `0.4.3+`

## Features

### Embeddings Support

- [x] OpenAI API
- [x] Cohere API (including Multi-language support)
- [ ] Sentence Transformers
- [ ] PaLM API
- [ ] Custom Embedding Function

### Feature Parity with ChromaDB API

- [x] Reset
- [x] Heartbeat
- [x] List Collections
- [x] Get Version
- [x] Create Collection
- [x] Delete Collection
- [x] Collection Add
- [x] Collection Get (partial without additional parameters)
- [x] Collection Count
- [x] Collection Query
- [x] Collection Modify
- [x] Collection Update
- [x] Collection Upsert
- [x] Collection Create Index
- [x] Collection Delete - delete documents in collection

## TODO

- [x] Push the package to Maven
  Central - https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven
- ⚒️ Fluent API - make it easier for users to make use of the library 
- [ ] Support for PaLM API
- [x] Support for Sentence Transformers with Hugging Face API
- ⚒️ Authentication ⚒️

## Usage

Add Maven dependency:

```xml
<dependency>
    <groupId>io.github.amikos-tech</groupId>
    <artifactId>chromadb-java-client</artifactId>
    <version>0.1.4</version>
</dependency>
```

Ensure you have a running instance of Chroma running. We recommend one of the two following options:

- Official documentation - https://docs.trychroma.com/usage-guide#running-chroma-in-clientserver-mode
- If you are a fan of Kubernetes, you can use the Helm chart - https://github.com/amikos-tech/chromadb-chart (Note: You
  will need `Docker`, `minikube` and `kubectl` installed)

### Example OpenAI Embedding Function

In this example we rely on `tech.amikos.chromadb.OpenAIEmbeddingFunction` to generate embeddings for our documents.

| **Important**: Ensure you have `OPENAI_API_KEY` environment variable set

```java
package tech.amikos;

import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String apiKey = System.getenv("OPENAI_API_KEY");
            EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey,"text-embedding-3-small");
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
```

The above should output:

```bash
{"documents":[["Hello, my name is Bond. I am a Spy.","Hello, my name is John. I am a Data Scientist."]],"ids":[["2","1"]],"metadatas":[[{"type":"spy"},{"type":"scientist"}]],"distances":[[0.28461432,0.50961685]]}
```

#### Custom OpenAI Endpoint

For endpoints compatible with OpenAI Embeddings API (e.g. [ollama](https://github.com/ollama/ollama)), you can use the following:

> Note: We have added a builder to help with the configuration of the OpenAIEmbeddingFunction

```java
EmbeddingFunction ef = OpenAIEmbeddingFunction.Instance()
        .withOpenAIAPIKey(apiKey)
        .withModelName("llama2")
        .withApiEndpoint("http://localhost:11434/api/embedding") // not really custom, but just to test the method
        .build();
```

Quick Start Guide with Ollama:

```bash
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
docker exec -it ollama ollama run llama2 # press Ctrl+D to exit after model downloads successfully
# test it
curl http://localhost:11434/api/embeddings -d '{\n  "model": "llama2",\n  "prompt": "Here is an article about llamas..."\n}'
```

### Example Cohere Embedding Function

In this example we rely on `tech.amikos.chromadb.CohereEmbeddingFunction` to generate embeddings for our documents.

| **Important**: Ensure you have `COHERE_API_KEY` environment variable set

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
  public static void main(String[] args) {
    try {
      Client client = new Client(System.getenv("CHROMA_URL"));
      client.reset();
      String apiKey = System.getenv("COHERE_API_KEY");
      EmbeddingFunction ef = new CohereEmbeddingFunction(apiKey);
      Collection collection = client.createCollection("test-collection", null, true, ef);
      List<Map<String, String>> metadata = new ArrayList<>();
      metadata.add(new HashMap<String, String>() {{
        put("type", "scientist");
      }});
      metadata.add(new HashMap<String, String>() {{
        put("type", "spy");
      }});
      collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
      Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
      System.out.println(qr);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e);
    }
  }
}
```

The above should output:

```bash
{"documents":[["Hello, my name is Bond. I am a Spy.","Hello, my name is John. I am a Data Scientist."]],"ids":[["2","1"]],"metadatas":[[{"type":"spy"},{"type":"scientist"}]],"distances":[[5112.614,10974.804]]}
```



### Example Hugging Face Sentence Transformers Embedding Function

In this example we rely on `tech.amikos.chromadb.HuggingFaceEmbeddingFunction` to generate embeddings for our documents.

| **Important**: Ensure you have `HF_API_KEY` environment variable set

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
  public static void main(String[] args) {
    try {
      Client client = new Client(System.getenv("CHROMA_URL"));
      client.reset();
      String apiKey = System.getenv("HF_API_KEY");
      EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(apiKey);
      Collection collection = client.createCollection("test-collection", null, true, ef);
      List<Map<String, String>> metadata = new ArrayList<>();
      metadata.add(new HashMap<String, String>() {{
        put("type", "scientist");
      }});
      metadata.add(new HashMap<String, String>() {{
        put("type", "spy");
      }});
      collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
      Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
      System.out.println(qr);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e);
    }
  }
}
```

The above should output:

```bash
{"documents":[["Hello, my name is Bond. I am a Spy.","Hello, my name is John. I am a Data Scientist."]],"ids":[["2","1"]],"metadatas":[[{"type":"spy"},{"type":"scientist"}]],"distances":[[0.9073759,1.6440368]]}
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
