# Phase 6: Documentation Site - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Build a rich documentation site (similar to amikos-tech/chroma-go) covering all library features, API surfaces, and usage examples. The site will be hosted at `java.chromadb.dev` via GitHub Pages + Cloudflare DNS.

</domain>

<decisions>
## Implementation Decisions

### Site Tooling
- **D-01:** Use MkDocs with Material for MkDocs theme — same stack as chroma-go
- **D-02:** Host on GitHub Pages, deployed via GitHub Actions on push to main
- **D-03:** Custom domain `java.chromadb.dev` via Cloudflare DNS (CNAME to GitHub Pages)
- **D-04:** Site lives in `docs/` directory at repo root (matching chroma-go layout: `docs/mkdocs.yml`, `docs/docs/`, `docs/overrides/`)

### Content Structure
- **D-05:** Hybrid organization — use-case guides as primary navigation, with a separate auto-generated Javadoc API reference section
- **D-06:** Mirror chroma-go page structure plus Java-specific extras:
  - Core pages: Client, Auth, Records, Filtering, Search, Embeddings, Cloud Features, Logging
  - Java extras: Schema/CMEK, ID Generators, Transport Options, Migration from v1
- **D-07:** Javadoc generated as part of build, hosted at `/javadoc/` or `/api/` path — guide pages link into it for detailed class/method docs
- **D-08:** Separate examples section (like chroma-go's `go-examples/`) with complete, runnable code walkthroughs organized by topic

### Code Examples
- **D-09:** Use `pymdownx.snippets` to include code from actual `.java` files in a snippets/examples directory — examples are compilable and testable in CI
- **D-10:** v2 API only in main documentation — no v1 examples in guide pages
- **D-11:** A single "Migration from v1" page covers the v1→v2 transition

### Visual Identity
- **D-12:** Match chroma-go aesthetic: black primary palette, Roboto/Roboto Mono fonts, same logo placement and Material theme features
- **D-13:** Include Google Analytics + cookie consent banner, matching chroma-go's pattern
- **D-14:** Material theme features enabled: code copy, code annotate, navigation instant, navigation tracking, navigation indexes

### Claude's Discretion
- CI deployment workflow specifics (GitHub Actions workflow file)
- MkDocs plugin selection beyond search and tags
- Exact nav hierarchy ordering within the hybrid structure
- Snippet file organization (flat vs topic-grouped directories)
- Whether to include EthicalAds integration like chroma-go
- Exact Javadoc integration approach (iframe, separate deploy, or linked external)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Reference Implementation
- `https://github.com/amikos-tech/chroma-go` — The Go client documentation site is the primary reference for structure, theme, and content organization
- chroma-go `docs/mkdocs.yml` — MkDocs configuration with Material theme, plugins, markdown extensions
- chroma-go `docs/docs/` — Guide pages: client.md, auth.md, records.md, filtering.md, search.md, embeddings.md, cloud-features.md, logging.md, rerankers.md
- chroma-go `docs/go-examples/` — Runnable code walkthrough section organized by topic (cloud/features, cloud/search-api, collections, embeddings, querying-collections, run-chroma)
- chroma-go `docs/overrides/main.html` — Custom template overrides

### Existing Content
- `README.md` — Current documentation (845 lines) covering auth, collections, querying, embeddings, schema, ID generators, cloud vs self-hosted. Content source for guide pages.
- `CHANGELOG.md` — Version history starting at 0.2.0

### API Surface (for doc coverage audit)
- `src/main/java/tech/amikos/chromadb/v2/Client.java` — Top-level client interface
- `src/main/java/tech/amikos/chromadb/v2/Collection.java` — Collection interface with fluent builders
- `src/main/java/tech/amikos/chromadb/v2/Search.java` — Search API entry point
- `src/main/java/tech/amikos/chromadb/v2/Where.java` — Typed filter DSL
- `src/main/java/tech/amikos/chromadb/v2/WhereDocument.java` — Document filter DSL
- `src/main/java/tech/amikos/chromadb/embeddings/` — All embedding provider implementations

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `README.md` (845 lines): Comprehensive usage documentation that can be decomposed into individual guide pages — auth, collections, querying, embeddings, schema, ID generators, cloud, transport
- Phase 7 examples directory: Working examples created in Phase 7 (readme-embedding-examples) can seed the examples section
- Javadoc comments: v2 API classes have Javadoc that can be auto-generated into reference docs

### Established Patterns
- chroma-go docs pattern: `docs/mkdocs.yml` + `docs/docs/*.md` + `docs/go-examples/` + `docs/overrides/` + `docs/CNAME`
- Material theme with black primary, Roboto fonts, code copy/annotate features
- `pymdownx.snippets` for testable code inclusion from source files

### Integration Points
- GitHub Actions: New workflow for building and deploying MkDocs site to GitHub Pages
- `pom.xml`: May need maven-javadoc-plugin configuration for Javadoc generation
- `CNAME` file in docs output for `java.chromadb.dev` custom domain
- Cloudflare DNS: CNAME record pointing `java.chromadb.dev` → GitHub Pages

</code_context>

<specifics>
## Specific Ideas

- "See amikos-tech/chroma-go for examples" — user explicitly wants the Java docs to follow the Go client docs site pattern
- Custom domain `java.chromadb.dev` on Cloudflare (user owns `chromadb.dev`)
- Site URL pattern: `https://java.chromadb.dev` (matching `https://go-client.chromadb.dev` for Go)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 06-documentation-site*
*Context gathered: 2026-03-24*
