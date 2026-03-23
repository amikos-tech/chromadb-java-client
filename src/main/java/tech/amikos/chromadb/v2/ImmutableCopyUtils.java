package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Package-private utilities for creating deeply immutable copies of nested collection structures.
 * Shared by {@link QueryResultImpl}, {@link SearchResultImpl}, and {@link GetResultImpl}.
 */
final class ImmutableCopyUtils {

    private ImmutableCopyUtils() {}

    static <T> List<List<T>> nestedList(List<List<T>> source) {
        if (source == null) {
            return null;
        }
        List<List<T>> outer = new ArrayList<List<T>>(source.size());
        for (List<T> inner : source) {
            if (inner == null) {
                outer.add(null);
            } else {
                outer.add(Collections.unmodifiableList(new ArrayList<T>(inner)));
            }
        }
        return Collections.unmodifiableList(outer);
    }

    static List<List<Map<String, Object>>> nestedMetadata(List<List<Map<String, Object>>> source) {
        if (source == null) {
            return null;
        }
        List<List<Map<String, Object>>> outer = new ArrayList<List<Map<String, Object>>>(source.size());
        for (List<Map<String, Object>> inner : source) {
            if (inner == null) {
                outer.add(null);
                continue;
            }
            List<Map<String, Object>> innerCopy = new ArrayList<Map<String, Object>>(inner.size());
            for (Map<String, Object> metadata : inner) {
                innerCopy.add(metadata == null
                        ? null
                        : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata)));
            }
            outer.add(Collections.unmodifiableList(innerCopy));
        }
        return Collections.unmodifiableList(outer);
    }

    static List<List<float[]>> nestedEmbeddings(List<List<float[]>> source) {
        if (source == null) {
            return null;
        }
        List<List<float[]>> outer = new ArrayList<List<float[]>>(source.size());
        for (List<float[]> inner : source) {
            if (inner == null) {
                outer.add(null);
                continue;
            }
            List<float[]> innerCopy = new ArrayList<float[]>(inner.size());
            for (float[] embedding : inner) {
                innerCopy.add(embedding == null ? null : Arrays.copyOf(embedding, embedding.length));
            }
            outer.add(Collections.unmodifiableList(innerCopy));
        }
        return Collections.unmodifiableList(outer);
    }
}
