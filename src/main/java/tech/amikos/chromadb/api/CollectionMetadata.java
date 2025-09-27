package tech.amikos.chromadb.api;

import java.util.HashMap;
import java.util.Map;

public interface CollectionMetadata {
    String getString(String key);
    Integer getInt(String key);
    Float getFloat(String key);
    Boolean getBoolean(String key);
    Map<String,Object> getAsMap(); //TODO immutable map or clone of the original

    class CollectionMetadataImpl implements CollectionMetadata{
        private final Map<String, Object> metadata;

        CollectionMetadataImpl() {
            this.metadata = new HashMap<>();
        }

        void put(String key, String value){
            metadata.put(key, value);
        }

        void put(String key, int value){
            metadata.put(key, value);
        }

        void put(String key, float value){
            metadata.put(key, value);
        }

        void put(String key, boolean value){
            metadata.put(key, value);
        }

        @Override
        public String getString(String key) {
            return (String) metadata.get(key);
        }

        @Override
        public Integer getInt(String key) {
            return (Integer) metadata.get(key);
        }

        @Override
        public Float getFloat(String key) {
            return (Float) metadata.get(key);
        }

        @Override
        public Boolean getBoolean(String key) {
            return (Boolean) metadata.get(key);
        }

        @Override
        public Map<String, Object> getAsMap() {
            return metadata;
        }
    }

    class Builder{
        private final CollectionMetadataImpl metadata;

        private Builder(){
            this.metadata = new CollectionMetadataImpl();
        }
        public static  Builder New()  {
            return new Builder();
        }

        public Builder put(String key, String value){
            this.metadata.put(key, value);
            return this;
        }

        public Builder put(String key, int value){
            this.metadata.put(key, value);
            return this;
        }

        public Builder put(String key, float value){
            this.metadata.put(key, value);
            return this;
        }

        public Builder put(String key, boolean value){
            this.metadata.put(key, value);
            return this;
        }

        public CollectionMetadata build(){
            return metadata;
        }
    }
}
