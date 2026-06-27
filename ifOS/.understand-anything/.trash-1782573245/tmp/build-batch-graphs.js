const fs = require('fs');
const path = require('path');

const projectRoot = '/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS';
const intermediateDir = path.join(projectRoot, '.understand-anything', 'intermediate');

// Load batch data
const batchGroup = JSON.parse(fs.readFileSync(path.join(projectRoot, '.understand-anything', 'tmp', 'batch-group-0.json'), 'utf8'));

// Load extraction results for all 6 batches
const results = {};
for (let i = 0; i <= 5; i++) {
  results[i] = JSON.parse(fs.readFileSync(path.join(projectRoot, '.understand-anything', 'tmp', `ua-file-extract-results-${i}.json`), 'utf8'));
}

const importData = {};
batchGroup.forEach(b => {
  importData[b.batchIndex] = b.batchImportData || {};
});

function isSignificantFunction(fn) {
  const lineCount = fn.endLine - fn.startLine + 1;
  return lineCount >= 10;
}

function isSignificantClass(cls, fileResult) {
  const methodCount = cls.methods ? cls.methods.length : 0;
  const lineCount = cls.endLine - cls.startLine + 1;
  const exported = (fileResult.exports || []).some(e => e.name === cls.name);
  return methodCount >= 2 || lineCount >= 20 || exported;
}

function determineComplexity(totalLines) {
  if (totalLines > 500) return 'high';
  if (totalLines > 100) return 'medium';
  return 'low';
}

function getTags(language, fileCategory, path) {
  const tags = [];
  if (fileCategory === 'code') tags.push(language);
  if (path.includes('/io/')) tags.push('io');
  if (path.includes('/security/') || path.includes('/cert/') || path.includes('/interfaces/') || path.includes('/acl/')) tags.push('security');
  if (path.includes('/sql/')) tags.push('sql');
  if (path.includes('/util/')) tags.push('util');
  if (path.includes('/lang/')) tags.push('lang');
  if (path.includes('/nio/')) tags.push('nio');
  if (path.includes('/text/')) tags.push('text');
  if (path.includes('/applet/')) tags.push('applet');
  if (path.includes('/invoke/')) tags.push('invoke');
  if (path.includes('/decode/')) tags.push('decode');
  if (path.includes('/stream/')) tags.push('stream');
  if (path.includes('/function/')) tags.push('function');
  if (tags.length === 0) tags.push('java');
  return tags.slice(0, 5);
}

for (const batch of batchGroup) {
  const bi = batch.batchIndex;
  const files = batch.files;
  const batchImportMap = importData[bi] || {};
  const extractResults = results[bi];
  const resultMap = {};
  extractResults.results.forEach(r => { resultMap[r.path] = r; });

  const nodes = [];
  const edges = [];
  const nodeSet = new Set();

  function addNode(id, type, name, filePath, summary, tags, complexity, extra = {}) {
    if (nodeSet.has(id)) return;
    nodeSet.add(id);
    const node = { id, type, name, filePath, summary, tags, complexity, ...extra };
    nodes.push(node);
  }

  for (const f of files) {
    const fp = f.path;
    const ext = path.extname(fp);
    let nodeType = 'file';
    if (ext === '.xml' || ext === '.properties' || ext === '.json' || ext === '.yaml' || ext === '.yml') nodeType = 'config';
    if (ext === '.md' || ext === '.txt' || ext === '.html') nodeType = 'document';

    const fr = resultMap[fp];
    const totalLines = fr ? fr.totalLines : f.sizeLines;
    const summary = fp.split('/').pop() + ' - ' + f.language + ' ' + f.fileCategory + ' file';
    const tags = getTags(f.language, f.fileCategory, fp);
    const complexity = determineComplexity(totalLines);

    const nodeId = `${nodeType}:${fp}`;
    addNode(nodeId, nodeType, path.basename(fp), fp, summary, tags, complexity, { sizeLines: totalLines, language: f.language });

    // Process classes and functions from extraction results
    if (fr && fr.classes) {
      for (const cls of fr.classes) {
        const clsId = `class:${fp}:${cls.name}`;
        if (isSignificantClass(cls, fr)) {
          addNode(clsId, 'class', cls.name, fp, `Class ${cls.name} with ${cls.methods ? cls.methods.length : 0} methods`, tags, determineComplexity(cls.endLine - cls.startLine + 1), { methods: cls.methods ? cls.methods.length : 0 });
          edges.push({ source: nodeId, target: clsId, type: 'contains', direction: 'forward', weight: 1.0 });
          if ((fr.exports || []).some(e => e.name === cls.name)) {
            edges.push({ source: nodeId, target: clsId, type: 'exports', direction: 'forward', weight: 0.8 });
          }
        }
      }
    }

    if (fr && fr.functions) {
      for (const fn of fr.functions) {
        if (isSignificantFunction(fn)) {
          const fnId = `function:${fp}:${fn.name}`;
          addNode(fnId, 'function', fn.name, fp, `Function ${fn.name}`, tags, 'low');
          edges.push({ source: nodeId, target: fnId, type: 'contains', direction: 'forward', weight: 1.0 });
          if ((fr.exports || []).some(e => e.name === fn.name)) {
            edges.push({ source: nodeId, target: fnId, type: 'exports', direction: 'forward', weight: 0.8 });
          }
        }
      }
    }

    // Add call graph edges between functions within same file
    if (fr && fr.callGraph && fr.functions) {
      for (const call of fr.callGraph) {
        const callerFn = fr.functions.find(f => f.name === call.caller);
        const calleeFn = fr.functions.find(f => f.name === call.callee);
        // Only create edges for internal calls (both in same file's functions)
        if (callerFn && calleeFn && callerFn !== calleeFn) {
          if (isSignificantFunction(callerFn) && isSignificantFunction(calleeFn)) {
            const callerId = `function:${fp}:${callerFn.name}`;
            const calleeId = `function:${fp}:${calleeFn.name}`;
            edges.push({ source: callerId, target: calleeId, type: 'calls', direction: 'forward', weight: 0.8 });
          }
        }
      }
    }
  }

  // Add imports edges using batchImportData
  for (const [sourcePath, targets] of Object.entries(batchImportMap)) {
    const sourceNodeId = `file:${sourcePath}`;
    for (const targetPath of targets) {
      const targetNodeId = `file:${targetPath}`;
      edges.push({ source: sourceNodeId, target: targetNodeId, type: 'imports', direction: 'forward', weight: 0.7 });
    }
  }

  // Ensure intermediate dir exists
  if (!fs.existsSync(intermediateDir)) {
    fs.mkdirSync(intermediateDir, { recursive: true });
  }

  const output = {
    batchIndex: bi,
    projectName: 'ifOS',
    nodes,
    edges
  };

  fs.writeFileSync(path.join(intermediateDir, `batch-${bi}.json`), JSON.stringify(output, null, 2));
  console.log(`Batch ${bi}: ${nodes.length} nodes, ${edges.length} edges`);
}

console.log('All batches processed.');
