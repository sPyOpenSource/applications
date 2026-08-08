# File Analyzer Protocol — testNET knowledge graph

You are analyzing Java source files and build configuration from the **testNET** project — a JNode-derived Java OS networking test suite. It contains:
- An AMD Lance/PCnet10 Ethernet NIC driver (`src/org/jnode/driver/net/lance/*` and `src/jx/net/devices/lance/*`)
- IPv4/DHCP client stack (`src/org/jnode/net/ipv4/dhcp/*`)
- A network manager layer (`src/jx/netmanager/*`) exposing TCP/UDP/IP senders and receivers
- Networking test harnesses (`src/test/net/*`, `src/test/minitcp/*`)
- NFS client tests and RPC stubs (`test/nfs/*`, `test/test/*`)
- Package test programs (`packages/test_*/*`) — filesystem, networking, JDK-level tests
- NetBeans Ant build configuration (`build.xml`, `nbproject/*`, `manifest.mf`)

## Your task
Read each file in your batch (in the project root), analyze it, and produce:
1. **GraphNode** objects for the file itself and for notable classes/functions within it.
2. **GraphEdge** objects capturing structural and semantic relationships.

Write the output as a JSON file to the exact path given in your prompt. The output MUST be a single JSON object:
```json
{
  "nodes": [ ... ],
  "edges": [ ... ]
}
```

## Node types and ID conventions
| Type | ID convention |
|---|---|
| `file` | `file:<relative-path>` (for .java source files) |
| `config` | `config:<relative-path>` (build.xml, nbproject/*, manifest.mf) |
| `document` | `document:<relative-path>` (README.rst) |
| `class` | `class:<relative-path>:<ClassName>` |
| `function` | `function:<relative-path>:<methodName>` |

## Node fields
Each node: `id`, `type`, `name`, `filePath` (relative path), `summary` (2-3 sentences of what this file/class/function does), `tags` (3-6 lowercase keywords), `complexity` (`simple` | `moderate` | `complex`), `languageNotes` (optional, language-specific observations like "no explicit imports in this file" or "inner class").

For file-level nodes, filePath = the relative path itself. For class/function nodes, filePath = the file they live in.

## Edge types and direction
| Edge type | Meaning | Weight |
|---|---|---|
| `imports` | file A imports file B (only for project-internal, use provided importData — do NOT re-resolve) | 0.7 |
| `contains` | file contains class / class contains function | 1.0 |
| `calls` | function/class calls another function/class (cross-file or same-file) | 0.8 |
| `inherits` | class extends another class | 0.9 |
| `implements` | class implements an interface | 0.9 |
| `uses` | file/class references another file's symbol (semantic) | 0.6 |
| `related` | closely related files (same subsystem) | 0.5 |
| `configures` | config file configures/builds a source file or jar | 0.6 |

Edge objects: `source`, `target`, `type`, `weight`, `detail` (optional short note on the relationship).

## Rules
- Use the provided `importData` for `imports` edges verbatim (source → target). Do NOT add imports edges not in the provided data.
- Use the provided `neighborMap` for cross-batch edges (confidence boost): if a file in your batch references a neighbor file listed in neighborMap, emit a `uses` or `calls` edge to it using the neighbor's path.
- Emit `contains` edges: file → its classes; class → its methods.
- Emit `calls` edges when a function in your batch calls a function you can identify (same batch file, or neighborMap file). Use `function:<path>:<name>` IDs for known targets; use `file:` IDs if only the file target is known.
- For Java `extends`/`implements`, most superclasses are in the external JNode OS (not project-internal) — do NOT create dangling edges to files that don't exist in the project. Only create `inherits`/`implements` edges when the target class is another project file in your batch, neighborMap, or clearly within the project.
- Emit file-level `related` edges between files in the same subsystem (e.g., all lance driver files, all netmanager files, all dhcp files).
- Config files (build.xml, nbproject/*, manifest.mf): emit `configures` edges to build outputs only where clear (e.g., build.xml builds the project jar). Prefer minimal, high-confidence edges here.
- Keep summaries factual and specific. Do NOT invent behavior not present in the source.
