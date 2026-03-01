package tech.amikos.chromadb.v2;

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TenantDatabaseIntegrationTest extends AbstractChromaIntegrationTest {

    // --- Tenant: get default ---

    @Test
    public void testGetDefaultTenant() {
        Tenant tenant = client.getTenant("default_tenant");
        assertNotNull(tenant);
        assertEquals("default_tenant", tenant.getName());
    }

    // --- Tenant: create and get ---

    @Test
    public void testCreateAndGetTenant() {
        Tenant created = client.createTenant("test_tenant");
        assertNotNull(created);
        assertEquals("test_tenant", created.getName());

        Tenant fetched = client.getTenant("test_tenant");
        assertEquals("test_tenant", fetched.getName());
    }

    // --- Tenant: duplicate → conflict ---

    @Test(expected = ChromaConflictException.class)
    public void testCreateDuplicateTenantThrowsConflict() {
        client.createTenant("dup_tenant");
        client.createTenant("dup_tenant");
    }

    // --- Tenant: nonexistent → not found ---

    @Test(expected = ChromaNotFoundException.class)
    public void testGetNonexistentTenantThrowsNotFound() {
        client.getTenant("no_such_tenant");
    }

    // --- Database: get configured ---

    @Test
    public void testGetConfiguredDatabase() {
        Database db = client.getDatabase(databaseName);
        assertNotNull(db);
        assertEquals(databaseName, db.getName());
    }

    // --- Database: create and get ---

    @Test
    public void testCreateAndGetDatabase() {
        Database created = client.createDatabase("test_db");
        assertNotNull(created);
        assertEquals("test_db", created.getName());

        Database fetched = client.getDatabase("test_db");
        assertEquals("test_db", fetched.getName());
    }

    // --- Database: duplicate → conflict ---

    @Test(expected = ChromaConflictException.class)
    public void testCreateDuplicateDatabaseThrowsConflict() {
        client.createDatabase("dup_db");
        client.createDatabase("dup_db");
    }

    // --- Database: nonexistent → not found ---

    @Test(expected = ChromaNotFoundException.class)
    public void testGetNonexistentDatabaseThrowsNotFound() {
        client.getDatabase("no_such_db");
    }

    // --- Database: list includes configured and created ---

    @Test
    public void testListDatabasesIncludesConfiguredAndCreated() {
        client.createDatabase("extra_db");

        List<Database> databases = client.listDatabases();
        assertNotNull(databases);
        assertTrue("should contain at least 2 databases", databases.size() >= 2);

        boolean hasConfigured = false;
        boolean hasExtra = false;
        for (Database db : databases) {
            if (databaseName.equals(db.getName())) hasConfigured = true;
            if ("extra_db".equals(db.getName())) hasExtra = true;
        }
        assertTrue("should contain configured database", hasConfigured);
        assertTrue("should contain extra_db", hasExtra);
    }

    // --- Database: delete ---

    @Test
    public void testDeleteDatabase() {
        client.createDatabase("to_delete_db");
        client.getDatabase("to_delete_db"); // verify it exists

        client.deleteDatabase("to_delete_db");

        try {
            client.getDatabase("to_delete_db");
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            // expected
        }
    }

    // --- Database: delete nonexistent → not found ---

    @Test(expected = ChromaNotFoundException.class)
    public void testDeleteNonexistentDatabaseThrowsNotFound() {
        client.deleteDatabase("nonexistent_db");
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }
}
