package tech.amikos.chromadb.v2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Static URL path builder for Chroma v2 REST API endpoints.
 */
final class ChromaApiPaths {

    private static final String BASE = "/api/v2";

    private ChromaApiPaths() {
    }

    // Server

    static String heartbeat() {
        return BASE + "/heartbeat";
    }

    static String version() {
        return BASE + "/version";
    }

    static String reset() {
        return BASE + "/reset";
    }

    // Tenants

    static String tenants() {
        return BASE + "/tenants";
    }

    static String tenant(String name) {
        return BASE + "/tenants/" + encode(name);
    }

    // Databases

    static String databases(String tenant) {
        return BASE + "/tenants/" + encode(tenant) + "/databases";
    }

    static String database(String tenant, String db) {
        return BASE + "/tenants/" + encode(tenant) + "/databases/" + encode(db);
    }

    // Collections

    static String collections(String tenant, String db) {
        return database(tenant, db) + "/collections";
    }

    static String collection(String tenant, String db, String name) {
        return collections(tenant, db) + "/" + encode(name);
    }

    static String collectionsCount(String tenant, String db) {
        return database(tenant, db) + "/collections_count";
    }

    // Record operations (the caller provides the collection ID as the "id" argument).

    static String collectionAdd(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/add";
    }

    static String collectionQuery(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/query";
    }

    static String collectionGet(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/get";
    }

    static String collectionUpdate(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/update";
    }

    static String collectionUpsert(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/upsert";
    }

    static String collectionDelete(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/delete";
    }

    static String collectionCount(String tenant, String db, String id) {
        return collections(tenant, db) + "/" + encode(id) + "/count";
    }

    // URLEncoder produces '+' for spaces (form encoding); path segments need '%20' per RFC 3986 §3.3.
    private static String encode(String segment) {
        if (segment == null) {
            throw new IllegalArgumentException("path segment must not be null");
        }
        if (segment.isEmpty()) {
            throw new IllegalArgumentException("path segment must not be empty");
        }
        try {
            return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is required by the JVM specification", e);
        }
    }
}
