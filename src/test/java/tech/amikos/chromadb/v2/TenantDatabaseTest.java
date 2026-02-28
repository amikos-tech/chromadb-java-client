package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.*;

public class TenantDatabaseTest {

    // --- Tenant ---

    @Test
    public void testTenantOf() {
        Tenant tenant = Tenant.of("my-tenant");
        assertEquals("my-tenant", tenant.getName());
    }

    @Test
    public void testTenantDefaultName() {
        Tenant tenant = Tenant.defaultTenant();
        assertEquals("default_tenant", tenant.getName());
    }

    @Test
    public void testTenantEquals() {
        Tenant a = Tenant.of("t1");
        Tenant b = Tenant.of("t1");
        Tenant c = Tenant.of("t2");

        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    public void testTenantHashCode() {
        Tenant a = Tenant.of("t1");
        Tenant b = Tenant.of("t1");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testTenantToString() {
        Tenant tenant = Tenant.of("my-tenant");
        assertEquals("my-tenant", tenant.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testTenantRejectsNull() {
        Tenant.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTenantRejectsEmpty() {
        Tenant.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTenantRejectsBlank() {
        Tenant.of("   ");
    }

    @Test
    public void testTenantNormalizesWhitespace() {
        Tenant trimmed = Tenant.of("tenant-a");
        Tenant withWhitespace = Tenant.of("  tenant-a  ");
        assertEquals("tenant-a", withWhitespace.getName());
        assertEquals(trimmed, withWhitespace);
    }

    @Test
    public void testTenantNotEqualToNull() {
        assertNotEquals(Tenant.of("t1"), null);
    }

    @Test
    public void testTenantNotEqualToDifferentType() {
        assertNotEquals(Tenant.of("name"), "name");
    }

    @Test
    public void testTenantDefaultIsSingleton() {
        assertSame(Tenant.defaultTenant(), Tenant.defaultTenant());
    }

    // --- Database ---

    @Test
    public void testDatabaseOf() {
        Database db = Database.of("my-db");
        assertEquals("my-db", db.getName());
    }

    @Test
    public void testDatabaseDefaultName() {
        Database db = Database.defaultDatabase();
        assertEquals("default_database", db.getName());
    }

    @Test
    public void testDatabaseEquals() {
        Database a = Database.of("d1");
        Database b = Database.of("d1");
        Database c = Database.of("d2");

        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    public void testDatabaseHashCode() {
        Database a = Database.of("d1");
        Database b = Database.of("d1");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testDatabaseToString() {
        Database db = Database.of("my-db");
        assertEquals("my-db", db.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testDatabaseRejectsNull() {
        Database.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDatabaseRejectsEmpty() {
        Database.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDatabaseRejectsBlank() {
        Database.of("   ");
    }

    @Test
    public void testDatabaseNormalizesWhitespace() {
        Database trimmed = Database.of("db-a");
        Database withWhitespace = Database.of("  db-a  ");
        assertEquals("db-a", withWhitespace.getName());
        assertEquals(trimmed, withWhitespace);
    }

    @Test
    public void testDatabaseNotEqualToNull() {
        assertNotEquals(Database.of("d1"), null);
    }

    @Test
    public void testDatabaseNotEqualToDifferentType() {
        assertNotEquals(Database.of("name"), "name");
    }

    @Test
    public void testDatabaseDefaultIsSingleton() {
        assertSame(Database.defaultDatabase(), Database.defaultDatabase());
    }

    // --- Cross-type ---

    @Test
    public void testTenantNotEqualToDatabase() {
        Tenant tenant = Tenant.of("same-name");
        Database database = Database.of("same-name");
        // Different types, should not be equal even with same name
        assertNotEquals(tenant, database);
    }
}
