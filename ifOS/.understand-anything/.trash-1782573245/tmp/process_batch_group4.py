#!/usr/bin/env python3
import json, os, re

PROJECT_ROOT = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS"
SKIP_EXTS = {".ctxt", ".bluej", ".class"}

def load_results(batch_idx):
    path = f"{PROJECT_ROOT}/.understand-anything/tmp/ua-file-extract-results-{batch_idx}.json"
    with open(path) as f:
        return json.load(f)

def load_batch_input(batch_idx):
    path = f"{PROJECT_ROOT}/.understand-anything/tmp/ua-file-analyzer-input-{batch_idx}.json"
    with open(path) as f:
        return json.load(f)

def make_file_id(path):
    return f"file:{path}"

def make_class_id(path, class_name):
    return f"class:{path}::{class_name}"

def make_func_id(path, func_name):
    return f"function:{path}::{func_name}"

def has_skip_ext(path):
    return any(path.endswith(ext) for ext in SKIP_EXTS)

def process_batch(batch_idx):
    results = load_results(batch_idx)
    batch_input = load_batch_input(batch_idx)
    import_data = batch_input.get("batchImportData", {})

    nodes = []
    edges = []
    seen_nodes = set()
    seen_edges = set()

    def add_node(node):
        key = node["id"]
        if key not in seen_nodes:
            seen_nodes.add(key)
            nodes.append(node)

    def add_edge(edge):
        key = f"{edge['source']}|{edge['target']}|{edge['type']}"
        if key not in seen_edges:
            seen_edges.add(key)
            edges.append(edge)

    for res in results:
        fpath = res["path"]
        lang = res.get("language", "unknown")
        file_id = make_file_id(fpath)

        # Determine node type based on language
        if lang == "java":
            node_type = "file"
        elif lang in ("xml", "properties"):
            node_type = "config"
        elif lang in ("markdown", "md"):
            node_type = "document"
        else:
            # .ctxt, .bluej, etc — still a config-like file
            node_type = "config"

        # Create file node
        file_node = {
            "id": file_id,
            "type": node_type,
            "label": os.path.basename(fpath),
            "properties": {
                "path": fpath,
                "language": lang,
                "totalLines": res.get("totalLines", 0),
                "nonEmptyLines": res.get("nonEmptyLines", 0),
                "fileCategory": res.get("fileCategory", "code"),
                "batchIndex": batch_idx,
            }
        }

        # Add metrics if present
        metrics = res.get("metrics", {})
        if metrics:
            for k, v in metrics.items():
                if v is not None:
                    file_node["properties"][k] = v

        add_node(file_node)

        # Process classes
        classes = res.get("classes", [])
        for cls in classes:
            cls_name = cls["name"]
            cls_id = make_class_id(fpath, cls_name)
            cls_node = {
                "id": cls_id,
                "type": "class",
                "label": cls_name,
                "properties": {
                    "file": fpath,
                    "startLine": cls.get("startLine"),
                    "endLine": cls.get("endLine"),
                    "methods": cls.get("methods", []),
                    "properties": cls.get("properties", []),
                }
            }
            add_node(cls_node)

            # contains edge: file -> class
            add_edge({
                "source": file_id,
                "target": cls_id,
                "type": "contains",
                "weight": 1.0,
            })

            # Export edge: file -> class (this class is exported by the file)
            add_edge({
                "source": file_id,
                "target": cls_id,
                "type": "exports",
                "weight": 0.8,
            })

        # Process functions (top-level functions, rare in Java)
        funcs = res.get("functions", [])
        for fn in funcs:
            fn_name = fn["name"]
            fn_id = make_func_id(fpath, fn_name)
            fn_node = {
                "id": fn_id,
                "type": "function",
                "label": fn_name,
                "properties": {
                    "file": fpath,
                    "startLine": fn.get("startLine"),
                    "endLine": fn.get("endLine"),
                    "params": fn.get("params", []),
                }
            }
            add_node(fn_node)

            add_edge({
                "source": file_id,
                "target": fn_id,
                "type": "contains",
                "weight": 1.0,
            })

        # Imports from batchImportData
        file_imports = import_data.get(fpath, [])
        for imp_path in file_imports:
            imp_id = make_file_id(imp_path)
            add_edge({
                "source": file_id,
                "target": imp_id,
                "type": "imports",
                "weight": 0.7,
            })

        # Call graph edges
        call_graph = res.get("callGraph", [])
        for cg in call_graph:
            caller_name = cg.get("caller", "")
            callee_name = cg.get("callee", "")
            if not caller_name or not callee_name:
                continue
            caller_id = make_func_id(fpath, caller_name)
            callee_id = make_func_id(fpath, callee_name)
            # Ensure function nodes exist
            for fn_id, fn_n in [(caller_id, caller_name), (callee_id, callee_name)]:
                add_node({
                    "id": fn_id,
                    "type": "function",
                    "label": fn_n,
                    "properties": {"file": fpath},
                })
            add_edge({
                "source": caller_id,
                "target": callee_id,
                "type": "calls",
                "weight": 0.8,
            })

        # Process exports from extract results
        exports = res.get("exports", [])
        for exp in exports:
            exp_name = exp["name"]
            # Could be a class or function
            exp_id = make_class_id(fpath, exp_name)
            if exp_id in seen_nodes:
                add_edge({
                    "source": file_id,
                    "target": exp_id,
                    "type": "exports",
                    "weight": 0.8,
                })

    output = {
        "batchIndex": batch_idx,
        "filesAnalyzed": results.get("filesAnalyzed", len(results.get("results", []))),
        "filesSkipped": results.get("filesSkipped", []),
        "nodes": nodes,
        "edges": edges,
        "nodeCount": len(nodes),
        "edgeCount": len(edges),
    }

    out_path = f"{PROJECT_ROOT}/.understand-anything/intermediate/batch-{batch_idx}.json"
    with open(out_path, "w") as f:
        json.dump(output, f, indent=2)

    print(f"Batch {batch_idx}: {len(nodes)} nodes, {len(edges)} edges -> {out_path}")

    return output


if __name__ == "__main__":
    for idx in range(24, 30):
        process_batch(idx)
    print("Done processing batch group 4 (batches 24-29)")
