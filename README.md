# Chroma Vector Database Java Client

This is a very basic/naive implementation in Java of the Chroma Vector Database API.

This client works with Chroma Versions `0.4.3+`

## Features

### Embeddings Support

- ✅ Default Embedding Function (all-mini-lm model)
- ✅ OpenAI Embedding Function
- ✅ Cohere Embedding Function
- ✅ HuggingFace Embedding Function (Inference API)
- ✅ Ollama Embedding Function
- ✅ Hugging Face Text Embedding Inference (HFEI) API
- [ ] Sentence Transformers
- [ ] PaLM API
- [ ] Custom Embedding Function
- [ ] Cloudflare Workers AI

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
    <version>0.1.7</version>
</dependency>
```

Ensure you have a running instance of Chroma running. We recommend one of the two following options:

- Official documentation - https://docs.trychroma.com/usage-guide#running-chroma-in-clientserver-mode
- If you are a fan of Kubernetes, you can use the Helm chart - https://github.com/amikos-tech/chromadb-chart (Note: You
  will need `Docker`, `minikube` and `kubectl` installed)

### Default Embedding Function

Since version `0.1.6` the library also offers a built-in default embedding function which does not rely on any external
API to generate embeddings and works in the same way it works in core Chroma Python package.

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.reset();
            EmbeddingFunction ef = new DefaultEmbeddingFunction();
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
            System.out.println(e);
        }
    }
}
```

### Example OpenAI Embedding Function

In this example we rely on `tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction` to generate embeddings for
our documents.

| **Important**: Ensure you have `OPENAI_API_KEY` environment variable set

```java
package tech.amikos;

import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String apiKey = System.getenv("OPENAI_API_KEY");
            EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey, "text-embedding-3-small");
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

For endpoints compatible with OpenAI Embeddings API (e.g. [ollama](https://github.com/ollama/ollama)), you can use the
following:

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

In this example we rely on `tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction` to generate embeddings for
our documents.

| **Important**: Ensure you have `COHERE_API_KEY` environment variable set

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;

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

#### Hugging Face Inference API

In this example we rely on `tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction` to generate embeddings for
our documents using HuggingFace cloud-based inference API.

| **Important**: Ensure you have `HF_API_KEY` environment variable set

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client("http://localhost:8000");
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
            System.out.println(e);
        }
    }
}
```

The above should output:

```bash
{"documents":[["Hello, my name is Bond. I am a Spy.","Hello, my name is John. I am a Data Scientist."]],"ids":[["2","1"]],"metadatas":[[{"type":"spy"},{"type":"scientist"}]],"distances":[[0.9073759,1.6440368]]}
```

#### Hugging Face Text Embedding Inference (HFEI) API

In this example we'll use a local Docker based server to generate the embeddings with
`Snowflake/snowflake-arctic-embed-s` mode.

First let's start the HFEI server:

```bash
docker run -d -p 8008:80 --platform linux/amd64 --name hfei ghcr.io/huggingface/text-embeddings-inference:cpu-1.5.0 --model-id Snowflake/snowflake-arctic-embed-s --revision main
```

> Note: Check the official documentation for more details - https://github.com/huggingface/text-embeddings-inference

Then we can use the following code to generate embeddings. Note the use of
`new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API));` to define the API type,
this will ensure the client uses the correct endpoint.

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client("http://localhost:8000");
            EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(
                    WithParam.baseAPI("http://localhost:8008"),
                    new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API));
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
            System.out.println(e);
        }
    }
}
```

The above should similar to the following output:

```bash
{"documents":[["Hello, my name is Bond. I am a Spy.","Hello, my name is John. I am a Data Scientist."]],"ids":[["2","1"]],"metadatas":[[{"type":"spy"},{"type":"scientist"}]],"distances":[[0.19665092,0.42433012]]}
```

### Ollama Embedding Function

In this example we rely on `tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction` to generate embeddings for
our documents.

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.reset();
            EmbeddingFunction ef = new OllamaEmbeddingFunction();
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
            System.out.println(e);
        }
    }
}
```

### Example Auth

> Note: This is a workaround until the client overhaul is completed

**Basic Auth**:

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String encodedString = Base64.getEncoder().encodeToString("admin:admin".getBytes());
            client.setDefaultHeaders(new HashMap<>() {{
                put("Authorization", "Basic " + encodedString);
            }});
            // your code here
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

**Static Auth - Authorization**:

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String encodedString = Base64.getEncoder().encodeToString("admin:admin".getBytes());
            client.setDefaultHeaders(new HashMap<>() {{
                put("Authorization", "Bearer test-token");
            }});
            // your code here
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

**Static Auth - X-Chroma-Token**:

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String encodedString = Base64.getEncoder().encodeToString("admin:admin".getBytes());
            client.setDefaultHeaders(new HashMap<>() {{
                put("X-Chroma-Token", "test-token");
            }});
            // your code here
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
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
