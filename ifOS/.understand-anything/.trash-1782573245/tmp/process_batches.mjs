
import fs from 'fs';
import path from 'path';

const projectRoot = '/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS';
const tmpDir = path.join(projectRoot, '.understand-anything/tmp');
const intermediateDir = path.join(projectRoot, '.understand-anything/intermediate');

if (!fs.existsSync(intermediateDir)) {
  fs.mkdirSync(intermediateDir, { recursive: true });
}

const batches = [24, 25, 26, 27, 28, 29];

batches.forEach(batchIndex => {
  const inputPath = path.join(tmpDir, `ua-file-analyzer-input-${batchIndex}.json`);
  const resultsPath = path.join(tmpDir, `ua-file-extract-results-${batchIndex}.json`);
  const group4Path = path.join(tmpDir, `ua-file-analyzer-input-group4-formatted.json`);

  if (!fs.existsSync(resultsPath)) {
    console.error(`Results file not found for batch ${batchIndex}`);
    return;
  }

  const results = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
  const group4Data = JSON.parse(fs.readFileSync(group4Path, 'utf8'));
  const batchData = group4Data.batches.find(b => b.batchIndex === batchIndex);

  const nodes = [];
  const edges = [];
  const nodeSet = new Set();

  function addNode(id, type, label) {
    if (!nodeSet.has(id)) {
      nodes.push({ id, type, label });
      nodeSet.add(id);
    }
  }

  function addEdge(source, target, type, weight) {
    edges.push({ source, target, type, weight });
  }

  // 1. Process structure results
  results.results.forEach(fileResult => {
    const filePath = fileResult.filePath;
    const fileId = `file:${filePath}`;
    addNode(fileId, 'file', filePath);

    if (fileResult.classes) {
      fileResult.classes.forEach(cls => {
        const classId = `class:${cls.name}:${filePath}`;
        addNode(classId, 'class', cls.name);
        addEdge(fileId, classId, 'contains', 1.0);
      });
    }

    if (fileResult.functions) {
      fileResult.functions.forEach(fn => {
        const funcId = `function:${fn.name}:${filePath}`;
        addNode(funcId, 'function', fn.name);
        addEdge(fileId, funcId, 'contains', 1.0);
      });
    }
  });

  // 2. Process imports from batchImportData
  if (batchData && batchData.batchImportData) {
    Object.entries(batchData.batchImportData).forEach(([filePath, imports]) => {
      const sourceId = `file:${filePath}`;
      imports.forEach(importPath => {
        const targetId = `file:${importPath}`;
        addEdge(sourceId, targetId, 'imports', 0.7);
      });
    });
  }

  const output = {
    nodes,
    edges
  };

  fs.writeFileSync(
    path.join(intermediateDir, `batch-${batchIndex}.json`),
    JSON.stringify(output, null, 2)
  );
  console.log(`Processed batch ${batchIndex}: ${nodes.length} nodes, ${edges.length} edges`);
});
