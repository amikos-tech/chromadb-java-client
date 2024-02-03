package tech.amikos.openai;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.reflect.Type;
import java.util.List;

public class CreateEmbeddingRequest {
    @SerializedName("model")
    private String model = "text-embedding-ada-002";
    @SerializedName("user")
    private String user = "java-client=0.0.1";

    public static class Input {
        private String text;
        private String[] texts;
        private Integer[] integers;

        private List<Integer[]> listOfListOfIntegers;

        public Input(String text) {
            this.text = text;
        }

        public Input(String[] texts) {
            this.texts = texts;
        }

        public Input(Integer[] integers) {
            this.integers = integers;
        }

        public Input(List<Integer[]> listOfListOfIntegers) {
            this.listOfListOfIntegers = listOfListOfIntegers;
        }

        public Object serialize() {
            if (text != null) {
                return text;
            } else if (texts != null) {
                return texts;
            } else if (integers != null) {
                return integers;
            } else if (listOfListOfIntegers != null) {
                return listOfListOfIntegers;
            } else {
                throw new RuntimeException("Invalid input");
            }
        }
    }

    @SerializedName("input")
    private Input input;

    public CreateEmbeddingRequest() {
    }

    public CreateEmbeddingRequest(String model, String user) {
        this.model = model;
        this.user = user;
    }

    public CreateEmbeddingRequest(String model) {
        this.model = model;
    }

    public CreateEmbeddingRequest model(String model) {
        this.model = model;
        return this;
    }

    @Schema(example = "text-embedding-ada-002", required = true, description = "ID of the model to use. You can use the [List models](/docs/api-reference/models/list) API to see all of your available models, or see our [Model overview](/docs/models/overview) for descriptions of them. ")
    public String getModel() {
        return model;
    }

    public CreateEmbeddingRequest input(Input input) {
        this.input = input;
        return this;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Schema(example = "The quick brown fox jumped over the lazy dog", required = true, description = "Input text to embed, encoded as a string or array of tokens. To embed multiple inputs in a single request, pass an array of strings or array of token arrays. Each input must not exceed the max input tokens for the model (8191 tokens for `text-embedding-ada-002`). [Example Python code](https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb) for counting tokens. ")
    public Input getInput() {
        return input;
    }

    @Schema(example = "user-1234", description = "A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse. [Learn more](/docs/guides/safety-best-practices/end-user-ids). ")
    public String getUser() {
        return user;
    }

    public CreateEmbeddingRequest user(String user) {
        this.user = user;
        return this;
    }

    public static class CreateEmbeddingRequestSerializer implements JsonSerializer<CreateEmbeddingRequest> {
        @Override
        public JsonElement serialize(CreateEmbeddingRequest req, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("model", req.getModel());
            jsonObject.addProperty("user", req.getUser());
            JsonElement input = context.serialize(req.getInput().serialize());
            jsonObject.add("input", input);
            return jsonObject;
        }
    }

    public String json() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(CreateEmbeddingRequest.class, new CreateEmbeddingRequestSerializer())
                .create();
        return gson.toJson(this);
    }

    public String toString() {
        return new Gson().toJson(this);
    }

}
