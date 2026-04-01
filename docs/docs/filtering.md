# Filtering

Use `Where` for metadata, ID, and inline-document filters and `WhereDocument` for document
content filters. Both DSLs serialize to Chroma's JSON wire format.

## Metadata Filters

### Equality

Match records where a metadata key has an exact value:

```java
--8<-- "FilteringExample.java:metadata-eq"
```

String, integer, float, and boolean overloads are available for all equality operators.

### Comparison

Filter on numeric metadata using greater-than, less-than, and their inclusive variants:

```java
--8<-- "FilteringExample.java:metadata-comparison"
```

### Not Equal

```java
--8<-- "FilteringExample.java:metadata-ne"
```

### In / Not In

Filter by a set of allowed or excluded values:

```java
--8<-- "FilteringExample.java:metadata-in"
```

### Contains / Not Contains

Filter on array-valued metadata fields — checks whether the array contains a specific element:

```java
--8<-- "FilteringExample.java:metadata-contains"
```

## ID Filters

Filter by record ID directly inside a `Where` clause:

```java
--8<-- "FilteringExample.java:id-filter"
```

`idIn` and `idNotIn` accept one or more non-blank ID strings.

## Inline Document Filters

Filter by document content directly inside a `Where` clause:

```java
--8<-- "FilteringExample.java:document-inline"
```

!!! warning
    Inline `#document` filters in `Where` are Cloud-oriented and may be rejected by local Chroma
    deployments. Use `WhereDocument` (see below) for local-compatible document filtering.

## Logical Combinators

Combine multiple conditions with `and` and `or`:

```java
--8<-- "FilteringExample.java:logical"
```

Both combinators require at least two child conditions. The chained combinator style (`.and(...)`)
is equivalent to the static factory style (`Where.and(...)`).

## WhereDocument Filters

`WhereDocument` provides the local-compatible path for document content filtering:

```java
--8<-- "FilteringExample.java:where-document"
```

- `contains` and `notContains` match document text substrings (non-blank required).
- `regex` and `notRegex` match against regular expression patterns.

### Logical Combinators

```java
--8<-- "FilteringExample.java:where-document-logical"
```

## Combined Filters in Queries

Combine `Where` and `WhereDocument` in a single query for precise filtering:

```java
--8<-- "FilteringExample.java:query-with-filters"
```

Pass `Where` to `.where(...)` and `WhereDocument` to `.whereDocument(...)` on the query builder.
Both are optional and independent.
