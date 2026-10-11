#!/usr/bin/env node
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { readFileSync, writeFileSync } from 'node:fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const pluginRoot = resolve('/Users/xuyi/.config/opencode/skills/understand-anything');

async function loadCore() {
  return await import(pathToFileURL(resolve(pluginRoot, 'packages/core/dist/index.js')).href);
}

function mapFileCategoryToNodeType(cat) {
  if (cat === 'config') return 'config';
  if (cat === 'document' || cat === 'docs' || cat === 'markup') return 'document';
  if (cat === 'service') return 'service';
  if (cat === 'infra') return 'pipeline';
  if (cat === 'data') return 'table';
  return 'file';
}

async function main() {
  const [,, inputPath, outputPath] = process.argv;
  if (!inputPath || !outputPath) {
    process.stderr.write('Usage: node build-batch-graph.mjs <input.json> <output.json>\n');
    process.exit(1);
  }
  const input = JSON.parse(readFileSync(inputPath, 'utf-8'));
  const { projectRoot, batchFiles, batchImportData } = input;
  const { GraphBuilder } = await loadCore();
  const gb = new GraphBuilder('COC', process.env.GIT_HASH || 'unknown');
  const results = [];
  for (const file of batchFiles) {
    const abs = join(projectRoot, file.path);
    let content = '';
    try { content = readFileSync(abs, 'utf-8'); } catch (e) { continue; }
    const lines = content.split('\n');
    const totalLines = content.endsWith('\n') ? Math.max(0, lines.length - 1) : lines.length;
    const nonEmptyLines = lines.filter(l => l.trim().length > 0).length;
    const nodeType = mapFileCategoryToNodeType(file.fileCategory);
    if (nodeType === 'file') {
      gb.addFile(file.path, {
        summary: `${file.language || 'unknown'} file: ${file.path}`,
        tags: [file.language || 'unknown'].filter(Boolean),
        complexity: 'simple'
      });
    } else {
      gb.addNonCodeFile(file.path, {
        nodeType,
        summary: `${nodeType}: ${file.path}`,
        tags: [nodeType],
        complexity: 'simple'
      });
    }
    const imports = batchImportData?.[file.path] || [];
    for (const imp of imports) {
      try { gb.addImportEdge(file.path, imp); } catch (e) {}
    }
    results.push({ path: file.path, totalLines, nonEmptyLines });
  }
  const graph = gb.build();
  const out = {
    scriptCompleted: true,
    filesAnalyzed: batchFiles.length,
    filesSkipped: [],
    nodes: graph.nodes || [],
    edges: graph.edges || [],
    results
  };
  writeFileSync(outputPath, JSON.stringify(out, null, 2));
}
main().catch(e => { process.stderr.write(e.message + '\n'); process.exit(1); });
