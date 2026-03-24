# Phase 6: Documentation Site - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-24
**Phase:** 06-documentation-site
**Areas discussed:** Site tooling, Content structure, Code examples, Visual identity

---

## Site Tooling

### Static Site Generator

| Option | Description | Selected |
|--------|-------------|----------|
| MkDocs + Material | Python-based, widely used for Java/Go library docs. Material theme has search, tabs, code highlighting, versioning. chroma-go uses this. | ✓ |
| Docusaurus | React-based (Node.js). Rich plugin ecosystem, MDX support, versioning built-in. Heavier build. | |
| Hugo | Go-based, extremely fast builds. Less opinionated — requires more theme/plugin work. | |
| You decide | Claude picks the best fit. | |

**User's choice:** MkDocs + Material (Recommended)
**Notes:** None

### Hosting

| Option | Description | Selected |
|--------|-------------|----------|
| GitHub Pages | Free, integrates with CI. Deploy via GitHub Actions on push to main. chroma-go uses this. | ✓ |
| Netlify | Free tier, preview deploys on PRs, custom domain support. | |
| You decide | Claude picks based on simplicity and CI integration. | |

**User's choice:** GitHub Pages with custom domain
**Notes:** User has Cloudflare domain `chromadb.dev` — wants to host at `java.chromadb.dev`. Referenced amikos-tech/chroma-go for the pattern.

---

## Content Structure

### Navigation Organization

| Option | Description | Selected |
|--------|-------------|----------|
| By use case | Getting Started → Guides → API Reference → Cloud. Progressive disclosure. | |
| By API surface | Client → Collection → Search → Embeddings → Auth → Configuration. Reference-first. | |
| Hybrid | Use-case guides as primary nav, with separate API Reference section. | ✓ |

**User's choice:** Hybrid, deferring to amikos-tech/chroma-go structure
**Notes:** User explicitly referenced chroma-go as the model to follow.

### Page Coverage

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror chroma-go + Java extras | Same pages as chroma-go plus Schema/CMEK, ID Generators, Transport, Migration from v1. | ✓ |
| Minimal launch set | Just Getting Started, Client, Collections, Querying, Search, Embeddings, Auth. | |
| You decide | Claude designs the nav structure. | |

**User's choice:** Mirror chroma-go + Java extras (Recommended)
**Notes:** None

### Javadoc API Reference

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, linked separately | Generate Javadoc, host at /api/ or /javadoc/ path. Guides link into it. | ✓ |
| No, manual reference only | Write all API reference content manually in Markdown. | |
| You decide | Claude picks the approach. | |

**User's choice:** Yes, linked separately
**Notes:** None

### Examples Section

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, separate examples section | Dedicated section with runnable walkthroughs, like chroma-go's go-examples/. | ✓ |
| Inline examples only | Examples embedded within guide pages only. | |
| You decide | Claude picks based on available content. | |

**User's choice:** Yes, separate examples section
**Notes:** None

---

## Code Examples

### Example Management

| Option | Description | Selected |
|--------|-------------|----------|
| Snippets from tested files | Use pymdownx.snippets to include code from actual .java files. Examples compilable/testable in CI. | ✓ |
| Inline markdown only | Write examples directly in .md files. Simpler but can drift. | |
| You decide | Claude designs the example management strategy. | |

**User's choice:** Snippets from tested files (Recommended)
**Notes:** None

### API Version Coverage

| Option | Description | Selected |
|--------|-------------|----------|
| v2 only | Main docs cover v2 exclusively. Single 'Migration from v1' page. | ✓ |
| Both with tabs | v2 and v1 side-by-side using MkDocs tabs. | |
| You decide | Claude picks based on project direction. | |

**User's choice:** v2 only (Recommended)
**Notes:** None

---

## Visual Identity

### Theme and Branding

| Option | Description | Selected |
|--------|-------------|----------|
| Match chroma-go | Black primary palette, Roboto fonts, same logo placement. Consistent cross-client branding. | ✓ |
| Java-flavored variant | Distinct accent color (blue/orange) while keeping Chroma feel. | |
| You decide | Claude matches chroma-go with minor Java touches. | |

**User's choice:** Match chroma-go (Recommended)
**Notes:** None

### Analytics

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, same setup | Google Analytics + cookie consent banner, matching chroma-go. | ✓ |
| No analytics | Clean site, no tracking. | |
| You decide | Claude decides. | |

**User's choice:** Yes, same setup
**Notes:** None

---

## Claude's Discretion

- CI deployment workflow specifics
- MkDocs plugin selection beyond search and tags
- Exact nav hierarchy ordering
- Snippet file organization
- EthicalAds integration decision
- Javadoc integration approach

## Deferred Ideas

None — discussion stayed within phase scope
