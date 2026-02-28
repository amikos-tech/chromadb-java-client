package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChromaApiPathsTest {

    // --- Server paths ---

    @Test
    public void testHeartbeat() {
        assertEquals("/api/v2/heartbeat", ChromaApiPaths.heartbeat());
    }

    @Test
    public void testVersion() {
        assertEquals("/api/v2/version", ChromaApiPaths.version());
    }

    @Test
    public void testReset() {
        assertEquals("/api/v2/reset", ChromaApiPaths.reset());
    }

    // --- Tenant paths ---

    @Test
    public void testTenants() {
        assertEquals("/api/v2/tenants", ChromaApiPaths.tenants());
    }

    @Test
    public void testTenant() {
        assertEquals("/api/v2/tenants/my_tenant", ChromaApiPaths.tenant("my_tenant"));
    }

    @Test
    public void testTenantWithSpecialChars() {
        assertEquals("/api/v2/tenants/my%20tenant", ChromaApiPaths.tenant("my tenant"));
    }

    // --- Database paths ---

    @Test
    public void testDatabases() {
        assertEquals("/api/v2/tenants/t1/databases", ChromaApiPaths.databases("t1"));
    }

    @Test
    public void testDatabase() {
        assertEquals("/api/v2/tenants/t1/databases/db1", ChromaApiPaths.database("t1", "db1"));
    }

    @Test
    public void testDatabaseWithSpecialChars() {
        assertEquals("/api/v2/tenants/t%261/databases/db%2F1",
                ChromaApiPaths.database("t&1", "db/1"));
    }

    // --- Collection paths ---

    @Test
    public void testCollections() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections",
                ChromaApiPaths.collections("t1", "db1"));
    }

    @Test
    public void testCollection() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/my_col",
                ChromaApiPaths.collection("t1", "db1", "my_col"));
    }

    @Test
    public void testCollectionsCount() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections_count",
                ChromaApiPaths.collectionsCount("t1", "db1"));
    }

    // --- Record operation paths ---

    @Test
    public void testCollectionAdd() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/add",
                ChromaApiPaths.collectionAdd("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionQuery() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/query",
                ChromaApiPaths.collectionQuery("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionGet() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/get",
                ChromaApiPaths.collectionGet("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionUpdate() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/update",
                ChromaApiPaths.collectionUpdate("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionUpsert() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/upsert",
                ChromaApiPaths.collectionUpsert("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionDelete() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/delete",
                ChromaApiPaths.collectionDelete("t1", "db1", "col-id"));
    }

    @Test
    public void testCollectionCount() {
        assertEquals("/api/v2/tenants/t1/databases/db1/collections/col-id/count",
                ChromaApiPaths.collectionCount("t1", "db1", "col-id"));
    }

    // --- URL encoding ---

    @Test
    public void testSpacesEncodedAsPercent20() {
        assertEquals("/api/v2/tenants/hello%20world", ChromaApiPaths.tenant("hello world"));
    }

    @Test
    public void testSlashEncoded() {
        assertEquals("/api/v2/tenants/a%2Fb", ChromaApiPaths.tenant("a/b"));
    }

    @Test
    public void testPlusSignEncoded() {
        assertEquals("/api/v2/tenants/a%2Bb", ChromaApiPaths.tenant("a+b"));
    }

    @Test
    public void testAllSegmentsEncoded() {
        assertEquals(
                "/api/v2/tenants/t%201/databases/d%201/collections/c%201/add",
                ChromaApiPaths.collectionAdd("t 1", "d 1", "c 1"));
    }
}
