import { readFileSync, writeFileSync, existsSync } from 'fs';
import path from 'path';

const resultsPath = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/JavaRA/.understand-anything/tmp/ua-file-extract-results-4.json";
const batchPath = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/JavaRA/.understand-anything/intermediate/batches/batch-4.json";

const results = JSON.parse(readFileSync(resultsPath, 'utf8'));
const batch = JSON.parse(readFileSync(batchPath, 'utf8'));

const { batchImportData } = batch;

const nodes = [];
const edges = [];
const nodeIds = new Set();

function addNode(node) {
  if (nodeIds.has(node.id)) return;
  nodeIds.add(node.id);
  nodes.push(node);
}

// ---- File nodes ----
for (const f of results.results) {
  const fpath = f.path;
  const name = fpath.split('/').pop();
  
  let summary, tags, complexity;
  const nonEmpty = f.nonEmptyLines || f.totalLines;
  
  if (nonEmpty < 50) complexity = 'simple';
  else if (nonEmpty < 200) complexity = 'moderate';
  else complexity = 'complex';

  // Per-file summary based on name/path
  const fileName = name.replace('.java','');
  if (fpath.includes('ai/BaseBuilder')) {
    summary = 'AI building placement logic that manages construction queue, building limits, and selects buildings to construct based on power, economy, and tactical needs.';
    tags = ['ai', 'building', 'strategy'];
  } else if (fpath.includes('IDefense')) {
    summary = 'Marker interface identifying entities that provide defensive capabilities.';
    tags = ['interface', 'defense', 'entity'];
  } else if (fpath.includes('IEffect')) {
    summary = 'Marker interface identifying visual or gameplay effect entities.';
    tags = ['interface', 'effect', 'entity'];
  } else if (fpath.includes('IHaveCost')) {
    summary = 'Interface defining a getBuildingCost method for entities that have a purchase cost.';
    tags = ['interface', 'cost', 'economy'];
  } else if (fpath.includes('IPips')) {
    summary = 'Interface defining pip display methods for entities showing resource or status indicators.';
    tags = ['interface', 'ui', 'pips'];
  } else if (fpath.includes('ISelectable')) {
    summary = 'Interface defining selection behavior (select, cancelSelect, isSelected) for entities.';
    tags = ['interface', 'selection', 'entity'];
  } else if (fpath.includes('IShroudRevealer')) {
    summary = 'Interface defining getRevealingRange for entities that reveal fog of war.';
    tags = ['interface', 'shroud', 'visibility'];
  } else if (fpath.includes('aircraft/Dragon')) {
    summary = 'Dragon aircraft entity with movement, rendering, and building cost defined for the air unit.';
    tags = ['entity', 'aircraft', 'dragon'];
  } else if (fpath.includes('BibType')) {
    summary = 'Enum defining bib types (building foundation graphics) used by structures.';
    tags = ['enum', 'building', 'graphics'];
  } else if (fpath.includes('EntityBuilding.java')) {
    summary = 'Core building entity with HP, repair, damage, footprint, and building progress management.';
    tags = ['entity', 'building', 'core'];
  } else if (fpath.includes('EntityBuildingProgress')) {
    summary = 'Rendered building-in-progress entity that spawns the actual building on completion.';
    tags = ['entity', 'building', 'construction'];
  } else if (fpath.includes('IOreCapacitor')) {
    summary = 'Interface for entities that provide ore storage capacity.';
    tags = ['interface', 'ore', 'economy'];
  } else if (fpath.includes('IPowerConsumer')) {
    summary = 'Interface for entities that consume power, defining getConsumptionLevel.';
    tags = ['interface', 'power', 'economy'];
  } else if (fpath.includes('IPowerProducer')) {
    summary = 'Interface for entities that produce power, defining getPowerProductionLevel.';
    tags = ['interface', 'power', 'economy'];
  } else if (fpath.includes('EntityAdvPowerPlant')) {
    summary = 'Advanced power plant building that produces increased power for the base.';
    tags = ['entity', 'building', 'power'];
  } else if (fpath.includes('EntityConcreteWall')) {
    summary = 'Concrete wall building entity with cost definition.';
    tags = ['entity', 'building', 'defense'];
  } else if (fpath.includes('EntityConstructionYard')) {
    summary = 'Construction yard building that serves as the primary structure for base building deployment.';
    tags = ['entity', 'building', 'construction'];
  } else if (fpath.includes('EntityHelipad')) {
    summary = 'Helipad building that can deploy aircraft entities on build completion.';
    tags = ['entity', 'building', 'aircraft'];
  } else if (fpath.includes('EntityOreSilo')) {
    summary = 'Ore silo building providing ore storage capacity for the base.';
    tags = ['entity', 'building', 'economy'];
  } else if (fpath.includes('EntityPowerPlant')) {
    summary = 'Standard power plant building providing power to the base.';
    tags = ['entity', 'building', 'power'];
  } else if (fpath.includes('EntityProc')) {
    summary = 'Ore processing building that accepts harvested resources and spawns harvesters.';
    tags = ['entity', 'building', 'economy'];
  } else if (fpath.includes('EntityRadarDome')) {
    summary = 'Radar dome building providing shroud revealing range and radar capabilities.';
    tags = ['entity', 'building', 'radar'];
  } else if (fpath.includes('EntityWall')) {
    summary = 'Wall building that connects to adjacent wall segments and provides defensive barriers.';
    tags = ['entity', 'building', 'defense'];
  } else {
    summary = `${fileName} Java source file in the JavaRA RTS game engine.`;
    tags = ['java', 'game'];
  }

  addNode({
    id: `file:${fpath}`,
    type: 'file',
    name,
    filePath: fpath,
    summary,
    tags,
    complexity
  });
  
  // ---- Class nodes (significance: 2+ methods or 20+ lines or exported) ----
  for (const cls of (f.classes || [])) {
    const clsLen = cls.endLine - cls.startLine;
    const isExported = (f.exports || []).some(e => e.name === cls.name && e.line === cls.startLine);
    if (cls.methods.length >= 2 || clsLen >= 20 || isExported) {
      addNode({
        id: `class:${fpath}:${cls.name}`,
        type: 'class',
        name: cls.name,
        filePath: fpath,
        lineRange: [cls.startLine, cls.endLine],
        summary: `${cls.name} class with ${cls.methods.length} methods ${cls.properties.length ? 'and ' + cls.properties.length + ' properties' : ''} in the JavaRA game engine.`,
        tags: ['class', ...(fpath.includes('Entity') ? ['entity'] : ['interface'])],
        complexity: clsLen < 50 ? 'simple' : clsLen < 200 ? 'moderate' : 'complex'
      });
      
      // contains edge
      edges.push({
        source: `file:${fpath}`,
        target: `class:${fpath}:${cls.name}`,
        type: 'contains',
        direction: 'forward',
        weight: 1.0
      });
      
      // exports edge if exported
      if (isExported) {
        edges.push({
          source: `file:${fpath}`,
          target: `class:${fpath}:${cls.name}`,
          type: 'exports',
          direction: 'forward',
          weight: 0.8
        });
      }
    }
  }
  
  // ---- Function nodes (significance: 10+ lines or exported) ----
  for (const fn of (f.functions || [])) {
    const fnLen = fn.endLine - fn.startLine;
    const isExported = (f.exports || []).some(e => e.name === fn.name && e.line === fn.startLine);
    if (fnLen >= 10 || isExported) {
      addNode({
        id: `function:${fpath}:${fn.name}`,
        type: 'function',
        name: fn.name,
        filePath: fpath,
        lineRange: [fn.startLine, fn.endLine],
        summary: `${fn.name} function ${fn.params?.length ? 'with params: ' + fn.params.join(', ') : ''} in ${fileName}.`,
        tags: ['function', ...(fn.name.startsWith('get') ? ['getter'] : []), ...(fn.name.startsWith('set') ? ['setter'] : []), ...(fn.name.startsWith('is') ? ['check'] : [])],
        complexity: fnLen < 10 ? 'simple' : fnLen < 50 ? 'moderate' : 'complex'
      });
      
      // contains edge
      edges.push({
        source: `file:${fpath}`,
        target: `function:${fpath}:${fn.name}`,
        type: 'contains',
        direction: 'forward',
        weight: 1.0
      });
      
      // exports edge if exported
      if (isExported) {
        edges.push({
          source: `file:${fpath}`,
          target: `function:${fpath}:${fn.name}`,
          type: 'exports',
          direction: 'forward',
          weight: 0.8
        });
      }
    }
  }
}

// ---- Import edges (1:1 from batchImportData) ----
for (const [filePath, imports] of Object.entries(batchImportData)) {
  for (const imp of imports) {
    edges.push({
      source: `file:${filePath}`,
      target: `file:${imp}`,
      type: 'imports',
      direction: 'forward',
      weight: 0.7
    });
  }
}

// Verify import edge count
let totalImports = 0;
for (const imports of Object.values(batchImportData)) {
  totalImports += imports.length;
}
const importsEdges = edges.filter(e => e.type === 'imports').length;
console.log(`Total expected imports: ${totalImports}, actual: ${importsEdges}`);
console.log(`Total nodes: ${nodes.length}, Total edges: ${edges.length}`);

// ---- Check split requirement ----
const nodeCount = nodes.length;
const edgeCount = edges.length;
console.log(`nodeCount=${nodeCount}, edgeCount=${edgeCount}`);

// ---- Write output ----
const outputDir = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/JavaRA/.understand-anything/intermediate";

if (nodeCount <= 60 && edgeCount <= 120) {
  writeFileSync(`${outputDir}/batch-4.json`, JSON.stringify({ nodes, edges }, null, 2));
  console.log("Written to batch-4.json (single file)");
} else {
  const parts = Math.ceil(Math.max(nodeCount / 60, edgeCount / 120));
  console.log(`Splitting into ${parts} parts`);
  
  // Sort files alphabetically
  const sortedFiles = [...results.results].sort((a, b) => a.path.localeCompare(b.path));
  const filesPerPart = Math.ceil(sortedFiles.length / parts);
  
  for (let k = 0; k < parts; k++) {
    const partFiles = sortedFiles.slice(k * filesPerPart, (k + 1) * filesPerPart);
    const partFilePaths = new Set(partFiles.map(f => f.path));
    
    // Collect nodes for this part
    const partNodeIds = new Set();
    const partNodes = [];
    
    // Include file nodes for this part's files
    for (const f of partFiles) {
      const fpath = f.path;
      for (const n of nodes) {
        if (n.filePath === fpath || (n.id === `file:${fpath}`)) {
          if (!partNodeIds.has(n.id)) {
            partNodeIds.add(n.id);
            partNodes.push(n);
          }
        }
      }
    }
    
    // Include class and function nodes for this part's files
    for (const n of nodes) {
      if (n.filePath && partFilePaths.has(n.filePath) && !partNodeIds.has(n.id)) {
        if (n.type === 'class' || n.type === 'function') {
          partNodeIds.add(n.id);
          partNodes.push(n);
        }
      }
    }
    
    // Include edges whose source is in partNodes
    const partEdges = edges.filter(e => {
      const sourceId = e.source;
      // Check if source matches a node in this part
      for (const pid of partNodeIds) {
        if (sourceId === pid) return true;
      }
      return false;
    });
    
    const partFileName = `${outputDir}/batch-4-part-${k + 1}.json`;
    writeFileSync(partFileName, JSON.stringify({ nodes: partNodes, edges: partEdges }, null, 2));
    console.log(`Part ${k + 1}: ${partNodes.length} nodes, ${partEdges.length} edges -> ${partFileName}`);
  }
}
