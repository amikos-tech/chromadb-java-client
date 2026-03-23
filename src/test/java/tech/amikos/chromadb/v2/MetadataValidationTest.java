package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link ChromaHttpCollection#validateMetadataArrayTypes} covering:
 * - Homogeneous arrays (all types) — pass
 * - Mixed-type arrays — rejected with {@link ChromaBadRequestException}
 * - Null elements in arrays — rejected
 * - Scalar metadata values — ignored
 * - Edge cases: null list, null entry, empty array
 *
 * Also includes behavioral wiring tests that verify the validation is invoked
 * via the {@code add()}, {@code upsert()}, and {@code update()} execute() methods.
 */
public class MetadataValidationTest {

    // =============================================================================
    // Static validation unit tests
    // =============================================================================

    @Test
    public void testHomogeneousStringArrayPasses() {
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("tags", Arrays.<Object>asList("a", "b", "c"))
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
        // no exception = pass
    }

    @Test
    public void testHomogeneousIntArrayPasses() {
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("counts", Arrays.<Object>asList(1, 2, 3))
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test
    public void testHomogeneousFloatArrayPasses() {
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("scores", Arrays.<Object>asList(1.5f, 2.5f, 3.5f))
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test
    public void testHomogeneousBoolArrayPasses() {
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("flags", Arrays.<Object>asList(true, false, true))
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test
    public void testEmptyArrayPasses() {
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("tags", Collections.emptyList())
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test
    public void testNullMetadatasListPasses() {
        ChromaHttpCollection.validateMetadataArrayTypes(null);
    }

    @Test
    public void testNullMetadataEntryPasses() {
        List<Map<String, Object>> metadatas = new ArrayList<Map<String, Object>>();
        metadatas.add(null);
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test(expected = ChromaBadRequestException.class)
    public void testMixedStringAndIntArrayRejected() {
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("mixed", mixed)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test(expected = ChromaBadRequestException.class)
    public void testMixedStringAndBoolArrayRejected() {
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Boolean.TRUE);
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("mixed", mixed)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test(expected = ChromaBadRequestException.class)
    public void testMixedIntAndBoolArrayRejected() {
        List<Object> mixed = new ArrayList<Object>();
        mixed.add(Integer.valueOf(42));
        mixed.add(Boolean.TRUE);
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("mixed", mixed)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test(expected = ChromaBadRequestException.class)
    public void testNullElementInArrayRejected() {
        List<Object> withNull = new ArrayList<Object>();
        withNull.add("valid");
        withNull.add(null);
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("tags", withNull)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
    }

    @Test
    public void testMixedIntegerAndLongPassesAsCompatible() {
        List<Object> intAndLong = new ArrayList<Object>();
        intAndLong.add(Integer.valueOf(1));
        intAndLong.add(Long.valueOf(2L));
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("ids", intAndLong)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
        // Integer and Long are both "integer group" - should pass
    }

    @Test
    public void testMixedFloatAndDoublePassesAsCompatible() {
        List<Object> floatAndDouble = new ArrayList<Object>();
        floatAndDouble.add(Float.valueOf(1.0f));
        floatAndDouble.add(Double.valueOf(2.0));
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("scores", floatAndDouble)
        );
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
        // Float and Double are both "float group" - should pass
    }

    @Test
    public void testScalarMetadataValuesIgnored() {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("name", "test");
        meta.put("count", Integer.valueOf(5));
        meta.put("active", Boolean.TRUE);
        List<Map<String, Object>> metadatas = Collections.singletonList(meta);
        ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
        // scalar values should not trigger validation
    }

    @Test
    public void testMixedTypeErrorMessageContainsDetails() {
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        mixed.add(Boolean.TRUE);
        List<Map<String, Object>> metadatas = Collections.singletonList(
                singleMetadata("bad_field", mixed)
        );
        try {
            ChromaHttpCollection.validateMetadataArrayTypes(metadatas);
            fail("Expected ChromaBadRequestException");
        } catch (ChromaBadRequestException e) {
            assertTrue("Message should mention field name", e.getMessage().contains("bad_field"));
            assertTrue("Message should mention 'mixed types'", e.getMessage().contains("mixed types"));
        }
    }

    // =============================================================================
    // Behavioral wiring tests
    // Verify that col.add/upsert/update().execute() calls validateMetadataArrayTypes
    // BEFORE any HTTP call. These tests use a stub Collection created via
    // ChromaHttpCollection.from() pointing to a dead endpoint (localhost:1).
    // If validation fires, ChromaBadRequestException is thrown before any network call.
    // If ChromaConnectionException is thrown instead, the wiring is broken.
    // =============================================================================

    @Test
    public void testAddExecuteRejectsMixedTypeArrayBeforeHttpCall() {
        Collection col = createStubCollection();
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("mixed_field", mixed);

        try {
            col.add()
                    .ids("test-1")
                    .documents("test document")
                    .metadatas(Collections.<Map<String, Object>>singletonList(meta))
                    .execute();
            fail("Expected ChromaBadRequestException for mixed-type array in add()");
        } catch (ChromaBadRequestException e) {
            // Correct — validation fired before HTTP call
            assertTrue("Exception message should mention mixed types", e.getMessage().contains("mixed types"));
        } catch (ChromaException e) {
            // ChromaConnectionException or other — wiring is broken (validation did not fire first)
            fail("Expected ChromaBadRequestException but got " + e.getClass().getSimpleName()
                    + ": " + e.getMessage()
                    + " — this means validateMetadataArrayTypes was NOT called before the HTTP call in add().execute()");
        }
    }

    @Test
    public void testUpsertExecuteRejectsMixedTypeArray() {
        Collection col = createStubCollection();
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("mixed_field", mixed);

        try {
            col.upsert()
                    .ids("test-1")
                    .documents("test document")
                    .metadatas(Collections.<Map<String, Object>>singletonList(meta))
                    .execute();
            fail("Expected ChromaBadRequestException for mixed-type array in upsert()");
        } catch (ChromaBadRequestException e) {
            // Correct — validation fired before HTTP call
            assertTrue("Exception message should mention mixed types", e.getMessage().contains("mixed types"));
        } catch (ChromaException e) {
            fail("Expected ChromaBadRequestException but got " + e.getClass().getSimpleName()
                    + ": " + e.getMessage()
                    + " — this means validateMetadataArrayTypes was NOT called before the HTTP call in upsert().execute()");
        }
    }

    @Test
    public void testUpdateExecuteRejectsMixedTypeArray() {
        Collection col = createStubCollection();
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("foo");
        mixed.add(Integer.valueOf(42));
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("mixed_field", mixed);

        try {
            col.update()
                    .ids("test-1")
                    .metadatas(Collections.<Map<String, Object>>singletonList(meta))
                    .execute();
            fail("Expected ChromaBadRequestException for mixed-type array in update()");
        } catch (ChromaBadRequestException e) {
            // Correct — validation fired before HTTP call
            assertTrue("Exception message should mention mixed types", e.getMessage().contains("mixed types"));
        } catch (ChromaException e) {
            fail("Expected ChromaBadRequestException but got " + e.getClass().getSimpleName()
                    + ": " + e.getMessage()
                    + " — this means validateMetadataArrayTypes was NOT called before the HTTP call in update().execute()");
        }
    }

    // =============================================================================
    // Helpers
    // =============================================================================

    private static Map<String, Object> singleMetadata(String key, Object value) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put(key, value);
        return meta;
    }

    /**
     * Creates a stub {@link Collection} backed by a {@link ChromaApiClient} pointing at
     * {@code http://localhost:1} (a dead endpoint). Since mixed-type validation fires
     * BEFORE any HTTP call is attempted, the stub never actually makes a network request.
     *
     * <p>Uses package-private {@code ChromaHttpCollection.from()} and {@code ChromaDtos}
     * since the test is in the same package.</p>
     */
    private static Collection createStubCollection() {
        ChromaApiClient stubApiClient = new ChromaApiClient(
                "http://localhost:1",
                null,
                null,
                Duration.ofMillis(100),
                Duration.ofMillis(100),
                Duration.ofMillis(100)
        );
        ChromaDtos.CollectionResponse dto = new ChromaDtos.CollectionResponse();
        dto.id = "stub-id-00000000-0000-0000-0000-000000000000";
        dto.name = "stub-collection";
        Tenant tenant = Tenant.of("default_tenant");
        Database database = Database.of("default_database");
        return ChromaHttpCollection.from(dto, stubApiClient, tenant, database, null);
    }
}
