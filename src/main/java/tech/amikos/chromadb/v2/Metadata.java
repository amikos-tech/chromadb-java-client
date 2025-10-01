package tech.amikos.chromadb.v2;

import java.util.*;

/**
 * Strongly-typed metadata container with builder pattern and custom serialization.
 * Provides type-safe access methods while maintaining flexibility.
 */
public class Metadata {
    private final Map<String, Object> data;

    private Metadata(Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Metadata of(Map<String, Object> data) {
        return new Metadata(data != null ? data : Collections.emptyMap());
    }

    public static Metadata empty() {
        return new Metadata(Collections.emptyMap());
    }

    // Type-safe getters
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    public Integer getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getInt(String key, Integer defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    public Long getLong(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Long getLong(String key, Long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    public Double getDouble(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Double getDouble(String key, Double defaultValue) {
        Double value = getDouble(key);
        return value != null ? value : defaultValue;
    }

    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Set<String> keySet() {
        return data.keySet();
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    // For JSON serialization (Jackson will use this getter)
    public Map<String, Object> toMap() {
        return data;
    }

    // Builder methods that return new instances
    public Metadata with(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put(key, value);
        return new Metadata(newData);
    }

    public Metadata without(String key) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.remove(key);
        return new Metadata(newData);
    }

    public Metadata merge(Metadata other) {
        Map<String, Object> newData = new HashMap<>(data);
        if (other != null) {
            newData.putAll(other.data);
        }
        return new Metadata(newData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(data, metadata.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "Metadata" + data;
    }

    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();

        public Builder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder putString(String key, String value) {
            data.put(key, value);
            return this;
        }

        public Builder putInt(String key, int value) {
            data.put(key, value);
            return this;
        }

        public Builder putLong(String key, long value) {
            data.put(key, value);
            return this;
        }

        public Builder putDouble(String key, double value) {
            data.put(key, value);
            return this;
        }

        public Builder putBoolean(String key, boolean value) {
            data.put(key, value);
            return this;
        }

        public Builder putList(String key, List<?> value) {
            data.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> map) {
            if (map != null) {
                data.putAll(map);
            }
            return this;
        }

        public Builder from(Metadata metadata) {
            if (metadata != null) {
                data.putAll(metadata.data);
            }
            return this;
        }

        public Metadata build() {
            return new Metadata(data);
        }
    }
}