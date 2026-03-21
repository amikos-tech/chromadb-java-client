# Phase 1: Result Ergonomics & WhereDocument - Research

**Researched:** 2026-03-20
**Domain:** Java 8 value-object design, Chroma filter DSL serialization, row-oriented result projection
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `ResultRow` is an **interface** (not concrete class) with package-private implementation.
- **D-02:** `QueryResultRow extends ResultRow` adds `Float getDistance()`.
- **D-03:** Fields on `ResultRow`: `getId()`, `getDocument()`, `getMetadata()`, `getEmbedding()`, `getUri()`. `getDistance()` on `QueryResultRow` only.
- **D-04:** Fields not requested via `Include` return **`null`** — no `Optional`, no exceptions.
- **D-05:** URI is included as a field on `ResultRow`.
- **D-06:** `rows()` returns `ResultGroup<R extends ResultRow>`, not a raw `List`.
- **D-07:** `ResultGroup<R>` implements `Iterable<R>` and provides: `get(int index)`, `size()`, `isEmpty()`, `stream()`, `toList()`.
- **D-08:** `get(int index)` throws `IndexOutOfBoundsException` on invalid index (Java `List` convention).
- **D-09:** `ResultGroup` is an interface with package-private implementation.
- **D-10:** `GetResult.rows()` takes no argument.
- **D-11:** `QueryResult.rows(int queryIndex)` always requires an index.
- **D-12:** `QueryResult.groupCount()` returns number of query inputs.
- **D-13:** `QueryResult.stream()` returns `Stream<ResultGroup<QueryResultRow>>`.
- **D-14:** No flat `rows()` without index on `QueryResult`.
- **D-15:** Implement all six operators: `contains`, `notContains`, `regex`, `notRegex`, `and`, `or`.
- **D-16:** Both static factory and instance chaining for `and`/`or`, mirroring `Where` class exactly.
- **D-17:** No client-side regex validation for `regex`/`notRegex` — pass through to Chroma.
- **D-18:** Javadoc on `WhereDocument.contains()` must clarify distinction from `Where.documentContains()`.

### Claude's Discretion

- Immutability implementation details for `ResultRow` and `ResultGroup` (defensive copies, unmodifiable collections)
- Package-private impl class naming (`ResultRowImpl`, `QueryResultRowImpl`, `ResultGroupImpl` or similar)
- Whether `ResultGroup` also implements `RandomAccess` marker interface
- Test structure and assertion style for new types
- `WhereDocument` inner class naming and structure (follow `Where.MapWhere` pattern)

### Deferred Ideas (OUT OF SCOPE)

- `data` field on ResultRow (Python multimodal) — Phase 4
- `SearchResult` / `SearchResultRow` with `getScore()` — Phase 3
- `included()` field tracking which fields were requested — not in requirements
</user_constraints>

---

## Summary

Phase 1 is a pure client-side convenience layer with zero new server interactions. It has two independent tracks: (1) adding row-oriented projection types (`ResultRow`, `QueryResultRow`, `ResultGroup`) on top of the existing column-oriented `QueryResult`/`GetResult`, and (2) replacing `UnsupportedOperationException` stubs in `WhereDocument` with real implementations that mirror the existing `Where` class.

The codebase is exceptionally well-prepared. `Where.java` is a complete, tested blueprint for `WhereDocument`. `QueryResultImpl`/`GetResultImpl` have all the column data needed to construct row objects — the only work is building the projection layer on top. No schema changes, no HTTP layer changes, no new dependencies.

Java 8 compatibility is enforced at compile time by animal-sniffer against the `java18` signature. `java.util.stream.Stream` is already used throughout the codebase and is Java 8-safe. All new public types must avoid any post-Java-8 API.

**Primary recommendation:** Implement `ResultGroup` and `ResultRow` as interfaces with package-private impls in `tech.amikos.chromadb.v2`, extend `QueryResult`/`GetResult` interfaces with new methods, implement in `QueryResultImpl`/`GetResultImpl`, and implement `WhereDocument` operators by copying `Where`'s proven private-helper pattern verbatim.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 4 | 4.13.2 | Unit + integration tests | Already in project, matches all existing test patterns |
| WireMock | (existing) | HTTP mock for unit tests | Used in `Phase01ValidationTest` pattern |
| TestContainers (ChromaDB) | (existing) | Integration tests against real Chroma | Used in `AbstractChromaIntegrationTest` |

No new dependencies required. This phase is entirely within existing classes.

**Installation:** No new installs needed.

---

## Architecture Patterns

### Recommended Project Structure

New files to create in `src/main/java/tech/amikos/chromadb/v2/`:
```
ResultRow.java              # public interface — getId, getDocument, getMetadata, getEmbedding, getUri
QueryResultRow.java         # public interface extends ResultRow — adds getDistance()
ResultGroup.java            # public interface<R extends ResultRow> — Iterable<R>, get, size, isEmpty, stream, toList
ResultRowImpl.java          # package-private final class implements ResultRow
QueryResultRowImpl.java     # package-private final class implements QueryResultRow
ResultGroupImpl.java        # package-private final class<R extends ResultRow> implements ResultGroup<R>
```

Modify:
```
QueryResult.java            # add rows(int), groupCount(), stream()
GetResult.java              # add rows()
QueryResultImpl.java        # implement new methods
GetResultImpl.java          # implement new methods
WhereDocument.java          # replace stubs with real implementations + inner class
```

New test files in `src/test/java/tech/amikos/chromadb/v2/`:
```
ResultRowTest.java          # unit tests for ResultRow / QueryResultRow / ResultGroup
WhereDocumentTest.java      # extend (replace stub-throwing tests with real behavior tests)
```

Integration tests extend `RecordOperationsIntegrationTest` or add to it.

### Pattern 1: Interface-first with package-private impl

All v2 public types are interfaces; implementations are package-private. Follow this exactly.

```java
// public interface
public interface ResultRow {
    String getId();
    String getDocument();
    Map<String, Object> getMetadata();
    float[] getEmbedding();
    String getUri();
}

// package-private impl
final class ResultRowImpl implements ResultRow {
    private final String id;
    private final String document;
    private final Map<String, Object> metadata;
    private final float[] embedding;
    private final String uri;

    ResultRowImpl(String id, String document, Map<String, Object> metadata,
                  float[] embedding, String uri) {
        this.id = id;
        this.document = document;
        this.metadata = metadata == null ? null : Collections.unmodifiableMap(
                new LinkedHashMap<String, Object>(metadata));
        this.embedding = embedding == null ? null : Arrays.copyOf(embedding, embedding.length);
        this.uri = uri;
    }

    @Override public String getId() { return id; }
    @Override public String getDocument() { return document; }
    @Override public Map<String, Object> getMetadata() { return metadata; }
    @Override public float[] getEmbedding() { return embedding == null ? null : Arrays.copyOf(embedding, embedding.length); }
    @Override public String getUri() { return uri; }
}
```

**Key immutability note:** `float[]` is mutable. Return a defensive copy from `getEmbedding()` on every call — the same pattern used in `GetResultImpl.immutableEmbeddings()`. Metadata maps use `Collections.unmodifiableMap(new LinkedHashMap(...))` at construction. Null fields (not included via `Include`) are stored and returned as null.

### Pattern 2: ResultGroup — lightweight read-only collection

`ResultGroup<R>` is an interface backed by an unmodifiable list. `stream()` and `toList()` are part of the contract.

```java
public interface ResultGroup<R extends ResultRow> extends Iterable<R> {
    R get(int index);        // throws IndexOutOfBoundsException
    int size();
    boolean isEmpty();
    java.util.stream.Stream<R> stream();
    java.util.List<R> toList();
}
```

Package-private implementation delegates to an `unmodifiableList`:

```java
final class ResultGroupImpl<R extends ResultRow> implements ResultGroup<R> {
    private final List<R> rows;

    ResultGroupImpl(List<R> rows) {
        this.rows = Collections.unmodifiableList(new ArrayList<R>(rows));
    }

    @Override public R get(int index) { return rows.get(index); }   // List.get throws IOOBE
    @Override public int size() { return rows.size(); }
    @Override public boolean isEmpty() { return rows.isEmpty(); }
    @Override public Iterator<R> iterator() { return rows.iterator(); }
    @Override public Stream<R> stream() { return rows.stream(); }
    @Override public List<R> toList() { return rows; }  // already unmodifiable
}
```

**Java 8 note:** `List.stream()` is Java 8. `Stream` from `java.util.stream` is Java 8. Both are animal-sniffer-safe (already used in embeddings code).

**RandomAccess:** The backing `ArrayList` wrapped in `unmodifiableList` supports `RandomAccess`. Implementing `RandomAccess` on the interface is fine but not mandatory — leave it to Claude's discretion. It has no behavioral impact.

### Pattern 3: QueryResult / GetResult method additions

```java
// In QueryResult interface:
ResultGroup<QueryResultRow> rows(int queryIndex);
int groupCount();
java.util.stream.Stream<ResultGroup<QueryResultRow>> stream();

// In GetResult interface:
ResultGroup<ResultRow> rows();
```

`QueryResultImpl.rows(int queryIndex)` constructs `QueryResultRowImpl` objects from column slices:

```java
@Override
public ResultGroup<QueryResultRow> rows(int queryIndex) {
    List<String> colIds = ids.get(queryIndex);
    List<QueryResultRow> result = new ArrayList<QueryResultRow>(colIds.size());
    for (int i = 0; i < colIds.size(); i++) {
        result.add(new QueryResultRowImpl(
            colIds.get(i),
            documents  == null ? null : documents.get(queryIndex).get(i),
            metadatas  == null ? null : metadatas.get(queryIndex).get(i),
            embeddings == null ? null : embeddings.get(queryIndex).get(i),
            uris       == null ? null : uris.get(queryIndex).get(i),
            distances  == null ? null : distances.get(queryIndex).get(i)
        ));
    }
    return new ResultGroupImpl<QueryResultRow>(result);
}
```

`GetResultImpl.rows()` constructs `ResultRowImpl` objects from flat column lists (same slice logic, no outer index).

### Pattern 4: WhereDocument — follow Where's private-helper pattern verbatim

`Where.java` is the definitive blueprint. The operators for `where_document` are simpler: no metadata key, no type variants — just a string value paired with an operator.

Chroma API operator string constants for `where_document`:
| Java factory | Chroma JSON key | Serialized form |
|---|---|---|
| `contains(text)` | `$contains` | `{"$contains": "text"}` |
| `notContains(text)` | `$not_contains` | `{"$not_contains": "text"}` |
| `regex(pattern)` | `$regex` | `{"$regex": "pattern"}` |
| `notRegex(pattern)` | `$not_regex` | `{"$not_regex": "pattern"}` |
| `and(conditions...)` | `$and` | `{"$and": [{...}, {...}]}` |
| `or(conditions...)` | `$or` | `{"$or": [{...}, {...}]}` |

(Verified against Chroma docs: https://cookbook.chromadb.dev/core/filters/)

The `$regex` and `$not_regex` operators are supported by Chroma's `where_document`. Pass the pattern string through verbatim — no client-side validation per D-17.

Validation rules for `contains`/`notContains`: text must be non-null, non-blank (same as `Where.documentContains`). For `regex`/`notRegex`: require non-null only (empty pattern string is technically valid regex). The `and`/`or` combinators: require non-null, non-empty varargs, all elements non-null, all `toMap()` outputs non-null — identical to `Where.logicalCondition`.

```java
// Pattern: private static helpers, mirroring Where exactly
private static final String OP_CONTAINS = "$contains";
private static final String OP_NOT_CONTAINS = "$not_contains";
private static final String OP_REGEX = "$regex";
private static final String OP_NOT_REGEX = "$not_regex";
private static final String OP_AND = "$and";
private static final String OP_OR = "$or";

public static WhereDocument contains(String text) {
    return leafCondition(OP_CONTAINS, requireNonBlank(text, "text"));
}

public static WhereDocument notContains(String text) {
    return leafCondition(OP_NOT_CONTAINS, requireNonBlank(text, "text"));
}

public static WhereDocument regex(String pattern) {
    requireNonNull(pattern, "pattern");
    return leafCondition(OP_REGEX, pattern);
}

public static WhereDocument notRegex(String pattern) {
    requireNonNull(pattern, "pattern");
    return leafCondition(OP_NOT_REGEX, pattern);
}

// static logical:
public static WhereDocument and(WhereDocument... conditions) {
    return logicalCondition(OP_AND, conditions);
}
public static WhereDocument or(WhereDocument... conditions) {
    return logicalCondition(OP_OR, conditions);
}

// instance chaining (already present in stubs — just delegate to static):
public WhereDocument and(WhereDocument other) { return WhereDocument.and(this, other); }
public WhereDocument or(WhereDocument other) { return WhereDocument.or(this, other); }
```

The `MapWhereDocument` inner class is already present and working — all new factory methods build `MapWhereDocument` instances. The only inner class needed is the already-present `MapWhereDocument`.

### Anti-Patterns to Avoid

- **Exposing raw mutable arrays:** `getEmbedding()` on `ResultRow` must return a defensive copy every call. Callers mutating `float[]` would corrupt nothing, but the contract must be safe.
- **Lazy construction of ResultGroup in rows():** Construct eagerly in `rows()`/`rows(int)`. The result is already immutable column data — there is nothing to defer.
- **Adding `rows()` overloads to QueryResult:** D-14 is explicit — no flat `rows()` without index on `QueryResult`. A single wrong overload breaks the ergonomics design.
- **Checking null outer lists before inner lists:** `documents`, `metadatas`, etc. in `QueryResultImpl` are already null when not included. The row-construction loop needs null guards at the outer list level, not the inner element level — if the outer list is null the field was not included.
- **Instance chaining `and`/`or` on `WhereDocument` calling static varargs with `this`:** Already wired correctly in the existing stubs. The static overload takes `WhereDocument... conditions`, so `WhereDocument.and(this, other)` works for instance chaining.
- **Using Java 9+ APIs:** `List.of()`, `Map.of()`, `List.copyOf()` are Java 9+. Use `Collections.unmodifiableList(new ArrayList<>(...))` and `new LinkedHashMap<>()` patterns throughout (as all existing v2 code does).

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Immutable map copy | Custom deep-copy utility | `Where.immutableMapCopy` pattern (duplicate the private helpers) | Already tested, handles nested maps and lists |
| Logical combinator validation | Ad-hoc null checks | Port `Where.logicalCondition` private helper verbatim | Handles null varargs, null elements, null toMap() return |
| String validation | Inline checks | Port `Where.requireNonBlank` / `requireNonNull` private helpers | Consistent error messages, already tested indirectly |
| Unmodifiable list wrapping | Custom ImmutableList | `Collections.unmodifiableList(new ArrayList<>(source))` | Java 8-safe, same pattern everywhere in v2 |

**Key insight:** `Where.java` contains every private helper needed by `WhereDocument`. Copy the relevant helpers as package-private statics in `WhereDocument` — they are private in `Where` and cannot be shared, but they are tiny and well-tested via the `Where` tests.

---

## Common Pitfalls

### Pitfall 1: Null outer column list vs null inner element
**What goes wrong:** `documents.get(queryIndex).get(i)` throws NPE when `documents` is null (field not included). The outer list being null means "not requested". The inner element being null means "row has no document" (rare but valid).
**Why it happens:** The row-construction loop accesses two levels of nesting. Null guard must be on the outer list.
**How to avoid:** Pattern: `documents == null ? null : documents.get(queryIndex).get(i)`.
**Warning signs:** NPE in `rows(int)` when `Include` does not contain `DOCUMENTS`.

### Pitfall 2: float[] aliasing across ResultRow instances
**What goes wrong:** Two `ResultRow` objects constructed from the same `float[]` source share the array reference. Caller modifies one, both change.
**Why it happens:** `Arrays.copyOf` was called at `QueryResultImpl` construction time (one copy), but row objects constructed from those already-copied arrays must also copy if they expose the array directly.
**How to avoid:** `ResultRowImpl` stores a defensive copy, `getEmbedding()` returns a fresh copy every call.
**Warning signs:** Test asserting independence of embedding arrays from two different row objects.

### Pitfall 3: WhereDocument and()/or() static overloads shadowing instance methods
**What goes wrong:** The existing instance `and(WhereDocument other)` delegates to `WhereDocument.and(this, other)`. After the static `and(WhereDocument... conditions)` is implemented, the delegation works correctly. But if the static implementation is accidentally named differently from the instance method, the delegation breaks.
**Why it happens:** Varargs static method name must match exactly.
**How to avoid:** Ensure `public static WhereDocument and(WhereDocument... conditions)` is the static form; instance form delegates `WhereDocument.and(this, other)`.
**Warning signs:** Existing `testInstanceLogicalCombinatorsDelegateToStaticFactories` test in `WhereDocumentTest` should start passing (it currently asserts `UnsupportedOperationException` — update tests).

### Pitfall 4: Integration test uses raw `whereDocument(fromMap(...))` — new test needs typed API
**What goes wrong:** Existing `RecordOperationsIntegrationTest` uses anonymous `WhereDocument` subclasses and raw maps. New integration test for ERGO-02 must use `WhereDocument.contains(...)` directly to verify the typed API serializes correctly.
**Why it happens:** The old pattern bypasses the factory methods entirely.
**How to avoid:** Write a separate integration test method that calls `WhereDocument.contains("text")` and verifies the expected documents are returned.
**Warning signs:** Integration test passes even if factory methods produce wrong map keys.

### Pitfall 5: WhereDocumentTest existing tests verify UnsupportedOperationException — they must be replaced
**What goes wrong:** `WhereDocumentTest.testTextFactoriesThrowUnsupportedOperationException()` will fail (expected) once the stubs are implemented. The test must be REPLACED (new behavior: factories return valid objects), not just deleted.
**Why it happens:** Tests currently assert the stub behavior.
**How to avoid:** Replace each stub-asserting test with a serialization-asserting test following the `WhereTest` pattern.
**Warning signs:** `WhereDocumentTest` fully green before implementation, then some tests go red after implementation — that is expected and correct.

---

## Code Examples

### ResultGroup construction from column slices (GetResult case)

```java
// Source: derived from GetResultImpl.java existing pattern + ResultGroup design
@Override
public ResultGroup<ResultRow> rows() {
    List<ResultRow> result = new ArrayList<ResultRow>(ids.size());
    for (int i = 0; i < ids.size(); i++) {
        result.add(new ResultRowImpl(
            ids.get(i),
            documents  == null ? null : documents.get(i),
            metadatas  == null ? null : metadatas.get(i),
            embeddings == null ? null : embeddings.get(i),
            uris       == null ? null : uris.get(i)
        ));
    }
    return new ResultGroupImpl<ResultRow>(result);
}
```

### WhereDocument leaf condition factory

```java
// Source: mirrors Where.operatorCondition pattern
private static WhereDocument leafCondition(String operator, String value) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put(operator, value);
    return new MapWhereDocument(Collections.<String, Object>unmodifiableMap(map));
}
```

### WhereDocument logical combinator (and/or)

```java
// Source: mirrors Where.logicalCondition pattern
private static WhereDocument logicalCondition(String operator, WhereDocument... conditions) {
    if (conditions == null) throw new IllegalArgumentException("conditions must not be null");
    if (conditions.length == 0) throw new IllegalArgumentException("conditions must contain at least 1 clause");
    List<Map<String, Object>> clauses = new ArrayList<Map<String, Object>>(conditions.length);
    for (int i = 0; i < conditions.length; i++) {
        WhereDocument c = conditions[i];
        if (c == null) throw new IllegalArgumentException("conditions[" + i + "] must not be null");
        Map<String, Object> m = c.toMap();
        if (m == null) throw new IllegalArgumentException("conditions[" + i + "].toMap() must not return null");
        clauses.add(m);
    }
    Map<String, Object> conditionMap = new LinkedHashMap<String, Object>();
    conditionMap.put(operator, Collections.<Map<String, Object>>unmodifiableList(clauses));
    return new MapWhereDocument(conditionMap);
}
```

Note: `MapWhereDocument`'s constructor already deep-copies the map via `immutableMapCopy`, so no additional defensive copy is needed before passing to it.

### ResultGroup iteration (user-facing patterns, from CONTEXT.md)

```java
// GetResult — single group
for (ResultRow row : result.rows()) {
    System.out.println(row.getId() + ": " + row.getDocument());
}

// QueryResult — per-query group
ResultGroup<QueryResultRow> group = result.rows(0);
for (QueryResultRow row : group) {
    System.out.println(row.getId() + " distance=" + row.getDistance());
}

// QueryResult — flatMap across all queries
result.stream()
      .flatMap(ResultGroup::stream)
      .forEach(row -> System.out.println(row.getId()));
```

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) |
| Config file | `pom.xml` (surefire plugin) |
| Quick run command | `mvn test -Dtest=ResultRowTest,WhereDocumentTest` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ERGO-01 | `ResultGroup` from `GetResult.rows()` iterates all records | unit | `mvn test -Dtest=ResultRowTest#testGetResultRowsIteratesAllRecords` | Wave 0 |
| ERGO-01 | `ResultGroup.get(index)` returns correct row by index | unit | `mvn test -Dtest=ResultRowTest#testResultGroupGetByIndex` | Wave 0 |
| ERGO-01 | `ResultGroup.get(invalidIndex)` throws `IndexOutOfBoundsException` | unit | `mvn test -Dtest=ResultRowTest#testResultGroupGetThrowsOnBadIndex` | Wave 0 |
| ERGO-01 | Row fields are null when not requested via Include | unit | `mvn test -Dtest=ResultRowTest#testRowFieldsNullWhenNotIncluded` | Wave 0 |
| ERGO-01 | `QueryResult.rows(int)` returns correct group per query index | unit | `mvn test -Dtest=ResultRowTest#testQueryResultRowsByIndex` | Wave 0 |
| ERGO-01 | `QueryResult.groupCount()` matches number of query inputs | unit | `mvn test -Dtest=ResultRowTest#testQueryResultGroupCount` | Wave 0 |
| ERGO-01 | `QueryResult.stream()` enables flatMap across all groups | unit | `mvn test -Dtest=ResultRowTest#testQueryResultStreamFlatMap` | Wave 0 |
| ERGO-01 | `QueryResultRow.getDistance()` returns distance when included | unit | `mvn test -Dtest=ResultRowTest#testQueryResultRowDistance` | Wave 0 |
| ERGO-01 | Integration: row access on real query result end-to-end | integration | `mvn test -Dtest=RecordOperationsIntegrationTest#testRowAccessOnQueryResult` | Wave 0 |
| ERGO-02 | `WhereDocument.contains(text)` serializes to `{"$contains":"text"}` | unit | `mvn test -Dtest=WhereDocumentTest#testContainsSerializesToMap` | Exists (replace stub test) |
| ERGO-02 | `WhereDocument.notContains(text)` serializes to `{"$not_contains":"text"}` | unit | `mvn test -Dtest=WhereDocumentTest#testNotContainsSerializesToMap` | Exists (replace stub test) |
| ERGO-02 | `WhereDocument.regex(pattern)` serializes to `{"$regex":"pattern"}` | unit | `mvn test -Dtest=WhereDocumentTest#testRegexSerializesToMap` | Exists (replace stub test) |
| ERGO-02 | `WhereDocument.notRegex(pattern)` serializes to `{"$not_regex":"pattern"}` | unit | `mvn test -Dtest=WhereDocumentTest#testNotRegexSerializesToMap` | Exists (replace stub test) |
| ERGO-02 | `WhereDocument.and()/or()` serialize nested conditions | unit | `mvn test -Dtest=WhereDocumentTest#testLogicalCombinatorsSerialize` | Exists (replace stub test) |
| ERGO-02 | `WhereDocument.contains()` rejects null/blank text | unit | `mvn test -Dtest=WhereDocumentTest#testContainsRejectsInvalidArgs` | Wave 0 |
| ERGO-02 | Integration: `WhereDocument.contains()` filters get results end-to-end | integration | `mvn test -Dtest=RecordOperationsIntegrationTest#testWhereDocumentContainsFilterOnGet` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=ResultRowTest,WhereDocumentTest`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/tech/amikos/chromadb/v2/ResultRowTest.java` — covers ERGO-01 unit behaviors
- [ ] Add `testRowAccessOnQueryResult` to `RecordOperationsIntegrationTest.java` — covers ERGO-01 integration
- [ ] Add `testWhereDocumentContainsFilterOnGet` to `RecordOperationsIntegrationTest.java` — covers ERGO-02 integration
- [ ] Update `WhereDocumentTest.java` — replace stub-asserting tests with serialization tests (covers ERGO-02 unit)

*(Existing `WhereDocumentTest.java` exists but must have tests replaced — stub tests will fail once implemented.)*

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Column-oriented `getIds().get(i)`, `getDocuments().get(i)` | Row-oriented `rows().get(i).getId()` | Phase 1 (this phase) | Ergonomic parity with Go client |
| `WhereDocument.fromMap(rawMap)` escape hatch | `WhereDocument.contains(text)`, etc. | Phase 1 (this phase) | No raw map construction required |
| Manual `where_document` `$contains`/`$not_contains` via raw map | Typed `WhereDocument` operators including `$regex`/`$not_regex` | Phase 1 (this phase) | Correct serialization, no typos |

**Deprecated/outdated:**
- `WhereDocument` stub methods throwing `UnsupportedOperationException` — replaced by real implementations in this phase. The `fromMap` escape hatch remains but is now supplementary.

---

## Open Questions

1. **`ResultGroup.toList()` return type modifiability**
   - What we know: The backing list inside `ResultGroupImpl` is already `unmodifiableList`.
   - What's unclear: Should `toList()` return the same reference (safe because it's unmodifiable) or a fresh `ArrayList` copy?
   - Recommendation: Return the backing unmodifiable list directly. It's already unmodifiable; a fresh copy would be misleading (callers can't modify either way). Document in Javadoc.

2. **`QueryResultRow.getDistance()` return type: `Float` (boxed) vs `float` (primitive)**
   - What we know: D-02 says `Float getDistance()`. `QueryResult.getDistances()` returns `List<List<Float>>` (boxed). Returning boxed `Float` allows null when distances not included.
   - What's unclear: Nothing — `Float` (boxed) is correct. D-04 says not-included fields return null, which requires boxed.
   - Recommendation: Use `Float getDistance()` exactly as specified.

3. **`ResultGroup` generic bound and `QueryResult.stream()` return**
   - What we know: `QueryResult.stream()` returns `Stream<ResultGroup<QueryResultRow>>`. Java 8 generics require the bound `R extends ResultRow` on `ResultGroup`.
   - What's unclear: Nothing — this is straightforward Java 8 generics.
   - Recommendation: `ResultGroup<R extends ResultRow>` with `QueryResult.stream()` returning `Stream<ResultGroup<QueryResultRow>>`.

---

## Sources

### Primary (HIGH confidence)
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — Complete, tested implementation; direct blueprint for WhereDocument
- `src/main/java/tech/amikos/chromadb/v2/QueryResultImpl.java` — Column storage patterns, immutability helpers
- `src/main/java/tech/amikos/chromadb/v2/GetResultImpl.java` — Column storage patterns, immutability helpers
- `src/main/java/tech/amikos/chromadb/v2/WhereDocument.java` — Current stubs, `MapWhereDocument` already present
- `src/test/java/tech/amikos/chromadb/v2/WhereTest.java` — Test pattern reference for WhereDocument tests
- `.planning/phases/01-result-ergonomics-wheredocument/01-CONTEXT.md` — All locked implementation decisions

### Secondary (MEDIUM confidence)
- [Chroma Cookbook: Filters](https://cookbook.chromadb.dev/core/filters/) — Verified `$contains`, `$not_contains`, `$regex`, `$not_regex` operator names for `where_document`
- `pom.xml` lines 54-56, 293-311 — Confirmed Java 1.8 target and animal-sniffer enforcement

### Tertiary (LOW confidence)
None — all findings verified from project source or official docs.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new deps, all from existing project
- Architecture: HIGH — blueprints exist in `Where.java`, `QueryResultImpl.java`, `GetResultImpl.java`
- Pitfalls: HIGH — derived from direct code inspection of the exact files being modified
- WhereDocument operator names: HIGH — verified against Chroma Cookbook official docs

**Research date:** 2026-03-20
**Valid until:** 2026-06-20 (stable — operator names are server API, unlikely to change)
