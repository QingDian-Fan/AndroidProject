<!-- CODEGRAPH_START -->
## CodeGraph

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), reach for it before grep/find or reading files when you need to understand or locate code:

- MCP tools (when available): `codegraph_explore` answers most code questions in one call—the relevant symbols' verbatim source plus the call paths between them. `codegraph_node` returns one symbol's source and callers, or reads a whole file with line numbers.
- Shell fallback: `codegraph explore "<symbol names or question>"` and `codegraph node <symbol-or-file>`.

If there is no `.codegraph/` directory, skip CodeGraph entirely; indexing is the user's decision.
<!-- CODEGRAPH_END -->
