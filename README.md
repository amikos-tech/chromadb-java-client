# Chroma Vector Database Java Client

This is a very basic/naive implementation in Java of the Chroma Vector Database API.

This client works with Chroma Versions `0.4.3+`

## Features

### New and Noteworthy

- Multi-tenancy support - from v0.4.15 ChromaDB introduced multi-tenancy feature. It is now fully supported.
- Latest API support
- Collection Builder (see below for example)
- Where and WhereDocument filter builder (see below for example)
- Metadata builder (see below for example)
- Improved API ergonomics

### Embeddings Support

- [x] OpenAI API
- [x] Cohere API (including Multi-language support)
- [x] Hugging Inference API
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
- [ ] Fluent API - make it easier for users to make use of the library
- [ ] Support for PaLM API
- [x] Support for Sentence Transformers with Hugging Face API
- [ ] Authentication ⚒️

## Usage

Add Maven dependency:

```xml
<dependency>
    <groupId>io.github.amikos-tech</groupId>
    <artifactId>chromadb-java-client</artifactId>
    <version>0.2.0</version>
</dependency>
```

Ensure you have a running instance of Chroma running. We recommend one of the two following options:

- Official documentation - https://docs.trychroma.com/usage-guide#running-chroma-in-clientserver-mode
- If you are a fan of Kubernetes, you can use the Helm chart - https://github.com/amikos-tech/chromadb-chart (Note: You
  will need `Docker`, `minikube` and `kubectl` installed)

### Collection Builder

In `0.2.0` we introduced a `CollectionBuilder` to make it easier to create collections.

```java
Collection collection = client.createCollectionWithBuilder("test-collection")
                .withCreateOrGet(true).withMetadata("test", "test")
                .withEmbeddingFunction(ef)
                .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
                .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
                .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
                .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
                .withIdGenerator(new UUIDv4IdGenerator())
                .withDocument("This is UUIDv4 id generated document.")
                .create();
```

### Where and WhereDocument Filters Builders

Since `0.2.0` we also support `Where` and `WhereDocument` builders to make it easier to build filters.

The `Where` builder supports the following methods:

- `eq` - equals (equivalent of Chroma's `$eq`)
- `ne` - not equals (equivalent of Chroma's `$ne`)
- `gt` - greater than (equivalent of Chroma's `$gt`)
- `gte` - greater than or equals (equivalent of Chroma's `$gte`)
- `lt` - less than (equivalent of Chroma's `$lt`)
- `lte` - less than or equals (equivalent of Chroma's `$lte`)
- `in` - in (equivalent of Chroma's `$in`)
- `nin` - not in (equivalent of Chroma's `$nin`)
- `and` - logical and (equivalent of Chroma's `$and`). This method takes a list of `Where` filters
- `or` - or (equivalent of Chroma's `$or`). This method takes a list of `Where` filters

The `WhereDocument` builder supports the following methods:

- `contains` - contains (equivalent of Chroma's `$contains`)
- `notContains` - not contains (equivalent of Chroma's `$not_contains`)

```java
Collection.GetResult resp = client.getCollection("test-collection", ef).
                get(new GetEmbedding().
                        whereDocument(WhereDocumentBuilder.create().contains("John").build())
                        .where(WhereBuilder.create().eq("key", "value").build())
                );
```

### Metadata Builder

```java
collection.add(new AddEmbedding()
        .ids(Arrays.asList("1", "2"))
        .metadatas(Arrays.asList(
                MetadataBuilder.create().forValue("key", "value").forValue("int_key",1).build(),
                MetadataBuilder.create().forValue("key", "value2").build()))
        .documents(Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Another document")));
```


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
            Collection collection = client.createCollectionWithBuilder("test-collection")
                  .withCreateOrGet(true).withMetadata("test", "test")
                  .withEmbeddingFunction(ef)
                  .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
                  .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
                  .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
                  .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
                  .withIdGenerator(new UUIDv4IdGenerator())
                  .withDocument("This is UUIDv4 id generated document.")
                  .create();
            Collection.QueryResponse qr = collection.query(new QueryEmbedding().queryTexts(Arrays.asList("who is named John?")).nResults(10));
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
      Collection collection = client.createCollectionWithBuilder("test-collection")
              .withCreateOrGet(true).withMetadata("test", "test")
              .withEmbeddingFunction(ef)
              .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
              .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
              .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
              .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
              .withIdGenerator(new UUIDv4IdGenerator())
              .withDocument("This is UUIDv4 id generated document.")
              .create();
      Collection.QueryResponse qr = collection.query(new QueryEmbedding().queryTexts(Arrays.asList("Who is the spy")).nResults(10));
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
      Collection collection = client.createCollectionWithBuilder("test-collection")
              .withCreateOrGet(true).withMetadata("test", "test")
              .withEmbeddingFunction(ef)
              .withHNSWDistanceFunction(HnswDistanceFunction.COSINE)
              .withDocument("Hello, my name is John. I am a Data Scientist.", "1")
              .withEmbedding(ef.createEmbedding(Collections.singletonList("This is just an embedding.")).get(0), "2")
              .withDocument("Hello, my name is Bond. I am a Spy.", ef.createEmbedding(Collections.singletonList("Hello, my name is Bond. I am a Spy")).get(0), "3")
              .withIdGenerator(new UUIDv4IdGenerator())
              .withDocument("This is UUIDv4 id generated document.")
              .create();
      Collection.QueryResponse qr = collection.query(new QueryEmbedding().queryTexts(Arrays.asList("Who is the spy")).nResults(10));
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
