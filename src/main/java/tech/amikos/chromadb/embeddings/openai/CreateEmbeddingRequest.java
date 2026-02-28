package tech.amikos.chromadb.embeddings.openai;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
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

    public Input getInput() {
        return input;
    }

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
