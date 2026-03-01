package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class IdentityPreFlightInfoTest {

    @Test
    public void testPreFlightInfo() {
        PreFlightInfo info = new PreFlightInfo(100, Boolean.TRUE);
        assertEquals(100, info.getMaxBatchSize());
        assertEquals(Boolean.TRUE, info.getSupportsBase64Encoding());
        assertTrue(info.supportsBase64Encoding());
        assertEquals(info, new PreFlightInfo(100, Boolean.TRUE));
        assertEquals(info.hashCode(), new PreFlightInfo(100, Boolean.TRUE).hashCode());
    }

    @Test
    public void testPreFlightInfoSupportsBase64EncodingFalseWhenNull() {
        PreFlightInfo info = new PreFlightInfo(100, null);
        assertNull(info.getSupportsBase64Encoding());
        assertFalse(info.supportsBase64Encoding());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreFlightInfoRejectsNegativeMaxBatchSize() {
        new PreFlightInfo(-1, Boolean.FALSE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPreFlightInfoRejectsZeroMaxBatchSize() {
        new PreFlightInfo(0, Boolean.FALSE);
    }

    @Test
    public void testIdentity() {
        Identity identity = new Identity(" user-1 ", " tenant-a ", Arrays.asList(" db1 ", "db2"));
        assertEquals("user-1", identity.getUserId());
        assertEquals("tenant-a", identity.getTenant());
        assertEquals(Arrays.asList("db1", "db2"), identity.getDatabases());
    }

    @Test
    public void testIdentityEqualsHashCode() {
        Identity a = new Identity("user-1", "tenant-a", Arrays.asList("db1", "db2"));
        Identity b = new Identity("user-1", "tenant-a", Arrays.asList("db1", "db2"));
        Identity c = new Identity("user-2", "tenant-a", Arrays.asList("db1", "db2"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test(expected = NullPointerException.class)
    public void testIdentityRejectsNullUserId() {
        new Identity(null, "tenant-a", Arrays.asList("db1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIdentityRejectsBlankUserId() {
        new Identity("   ", "tenant-a", Arrays.asList("db1"));
    }

    @Test(expected = NullPointerException.class)
    public void testIdentityRejectsNullTenant() {
        new Identity("user-1", null, Arrays.asList("db1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIdentityRejectsBlankTenant() {
        new Identity("user-1", "   ", Arrays.asList("db1"));
    }

    @Test(expected = NullPointerException.class)
    public void testIdentityRejectsNullDatabases() {
        new Identity("user-1", "tenant-a", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIdentityRejectsBlankDatabaseName() {
        new Identity("user-1", "tenant-a", Arrays.asList("db1", " "));
    }

    @Test
    public void testIdentityDefensiveCopyAndUnmodifiableDatabases() {
        List<String> dbs = new ArrayList<String>();
        dbs.add("db1");
        Identity identity = new Identity("user-1", "tenant-a", dbs);
        dbs.add("db2");

        assertEquals(Arrays.asList("db1"), identity.getDatabases());
        try {
            identity.getDatabases().add("db3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testIdentityAllowsEmptyDatabasesList() {
        Identity identity = new Identity("user-1", "tenant-a", Arrays.<String>asList());
        assertNotNull(identity.getDatabases());
        assertTrue(identity.getDatabases().isEmpty());
    }
}
