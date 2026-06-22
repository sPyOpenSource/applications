const fs = require('fs');

const edges = [];
const nodes = [];

function node(id, type, label, props = {}) {
  return { id, type, label, ...props };
}

function edge(from, to, type, weight = 1, props = {}) {
  return { from, to, type, weight, ...props };
}

// ============ FILE NODES ============
const files = [
  {path: "src/BigIntegers/ECMBigInteger.java", fileNodeType: "file:code"},
  {path: "src/BigIntegers/LargeInteger.java", fileNodeType: "file:code"},
  {path: "src/Factorzations/GUI.java", fileNodeType: "file:code"},
  {path: "src/calculator/EncodedOperation.java", fileNodeType: "file:interface"},
  {path: "src/calculator/Operation.java", fileNodeType: "file:interface"},
  {path: "src/calculator/GUI.java", fileNodeType: "file:code"},
  {path: "src/calculator/calculatorUI.java", fileNodeType: "file:interface"},
  {path: "src/calculator/javamodel/CalculatorImpl.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Push.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Pop.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Add.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Subtract.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Multiply.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Divide.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Power.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/Remainder.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/ChangeSigns.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/ClearStack.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/RotateDown.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/RotateUp.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/TwoToPower.java", fileNodeType: "file:code"},
  {path: "src/calculator/javamodel/TenToPower.java", fileNodeType: "file:code"},
  {path: "src/calculator/largemodel/CalculatorImpl.java", fileNodeType: "file:code"},
  {path: "src/calculator/largemodel/Push.java", fileNodeType: "file:code"},
  {path: "src/calculator/largemodel/Add.java", fileNodeType: "file:code"},
  {path: "src/calculator/largemodel/Multiply.java", fileNodeType: "file:code"},
  {path: "src/calculator/largemodel/Power.java", fileNodeType: "file:code"}
];

const fileNodeIds = {};
files.forEach(f => {
  const id = `file:${f.path}`;
  fileNodeIds[f.path] = id;
  nodes.push(node(id, f.fileNodeType, f.path, { language: "java" }));
});

// ============ IMPORT EDGES (from batchImportData) ============
const importData = {
  "src/BigIntegers/ECMBigInteger.java": [],
  "src/BigIntegers/LargeInteger.java": ["src/utilities/BitReverseCounter.java", "src/utilities/FFT.java", "src/utilities/NTT.java", "src/utilities/Zeef.java"],
  "src/Factorzations/GUI.java": ["src/BigIntegers/AprtCleInteger.java", "src/BigIntegers/LargeInteger.java", "src/calculator/Deque.java", "src/calculator/EmptyDequeException.java", "src/calculator/EncodedOperation.java", "src/calculator/largemodel/Add.java", "src/calculator/largemodel/CalculatorImpl.java", "src/calculator/largemodel/Divide.java", "src/calculator/largemodel/Multiply.java", "src/calculator/largemodel/Power.java", "src/calculator/largemodel/Push.java", "src/calculator/largemodel/Remainder.java", "src/calculator/largemodel/Subtract.java", "src/ecm/PrimeTest/AprtCle.java", "src/Factorzations/Classical.java", "src/Factorzations/ecm/ECM.java", "src/Factorzations/Lehman.java", "src/Factorzations/siqs/Siqs.java", "src/utilities/FFT.java", "src/utilities/NTT.java"],
  "src/calculator/EncodedOperation.java": [],
  "src/calculator/Operation.java": [],
  "src/calculator/GUI.java": ["src/calculator/BaseListener.java", "src/calculator/ButtonAction.java", "src/calculator/CalculatorUI.java", "src/calculator/ConvertBase.java", "src/calculator/DefaultUI.java", "src/calculator/Deque.java", "src/calculator/EmptyDequeException.java", "src/calculator/EncodedOperation.java", "src/calculator/EndAction.java", "src/calculator/HomeAction.java", "src/calculator/NumericDocument.java", "src/calculator/Operation.java", "src/calculator/SelectAllAction.java", "src/calculator/SetSticky.java", "src/calculator/TextButtonAction.java", "src/calculator/javamodel/CalculatorImpl.java", "src/calculator/javamodel/Add.java", "src/calculator/javamodel/ChangeSigns.java", "src/calculator/javamodel/ClearStack.java", "src/calculator/javamodel/Divide.java", "src/calculator/javamodel/Multiply.java", "src/calculator/javamodel/Pop.java", "src/calculator/javamodel/Power.java", "src/calculator/javamodel/Push.java", "src/calculator/javamodel/Remainder.java", "src/calculator/javamodel/RotateDown.java", "src/calculator/javamodel/RotateUp.java", "src/calculator/javamodel/Subtract.java", "src/calculator/javamodel/TenToPower.java", "src/calculator/javamodel/TwoToPower.java", "src/calculator/largemodel/CalculatorImpl.java", "src/calculator/largemodel/Add.java", "src/calculator/largemodel/Divide.java", "src/calculator/largemodel/Multiply.java", "src/calculator/largemodel/Power.java", "src/calculator/largemodel/Push.java", "src/calculator/largemodel/Remainder.java", "src/calculator/largemodel/Subtract.java"],
  "src/calculator/calculatorUI.java": [],
  "src/calculator/javamodel/CalculatorImpl.java": [],
  "src/calculator/javamodel/Push.java": [],
  "src/calculator/javamodel/Pop.java": [],
  "src/calculator/javamodel/Add.java": [],
  "src/calculator/javamodel/Subtract.java": [],
  "src/calculator/javamodel/Multiply.java": [],
  "src/calculator/javamodel/Divide.java": [],
  "src/calculator/javamodel/Power.java": [],
  "src/calculator/javamodel/Remainder.java": [],
  "src/calculator/javamodel/ChangeSigns.java": [],
  "src/calculator/javamodel/ClearStack.java": [],
  "src/calculator/javamodel/RotateDown.java": [],
  "src/calculator/javamodel/RotateUp.java": [],
  "src/calculator/javamodel/TwoToPower.java": [],
  "src/calculator/javamodel/TenToPower.java": [],
  "src/calculator/largemodel/CalculatorImpl.java": ["src/calculator/EncodedOperation.java", "src/calculator/Operation.java"],
  "src/calculator/largemodel/Push.java": [],
  "src/calculator/largemodel/Add.java": [],
  "src/calculator/largemodel/Multiply.java": [],
  "src/calculator/largemodel/Power.java": []
};

Object.entries(importData).forEach(([srcPath, targets]) => {
  targets.forEach(t => {
    const targetId = `file:${t}`;
    edges.push(edge(`file:${srcPath}`, targetId, "imports", 1));
  });
});

// ============ CLASS NODES & CONTAINS EDGES ============
const classDefs = {
  "src/BigIntegers/ECMBigInteger.java": {name: "ECMBigInteger", startLine: 12, endLine: 412},
  "src/BigIntegers/LargeInteger.java": {name: "LargeInteger", startLine: 27, endLine: 729},
  "src/Factorzations/GUI.java": {name: "GUI", startLine: 40, endLine: 718},
  "src/calculator/EncodedOperation.java": {name: "EncodedOperation", startLine: 15, endLine: 31},
  "src/calculator/Operation.java": {name: "Operation", startLine: 21, endLine: 27},
  "src/calculator/GUI.java": {name: "GUI", startLine: 49, endLine: 138},
  "src/calculator/calculatorUI.java": {name: "CalculatorUI", startLine: 15, endLine: 42},
  "src/calculator/javamodel/CalculatorImpl.java": {name: "CalculatorImpl", startLine: 27, endLine: 198},
  "src/calculator/javamodel/Push.java": {name: "Push", startLine: 19, endLine: 27},
  "src/calculator/javamodel/Pop.java": {name: "Pop", startLine: 18, endLine: 28},
  "src/calculator/javamodel/Add.java": {name: "Add", startLine: 18, endLine: 40},
  "src/calculator/javamodel/Subtract.java": {name: "Subtract", startLine: 18, endLine: 38},
  "src/calculator/javamodel/Multiply.java": {name: "Multiply", startLine: 18, endLine: 38},
  "src/calculator/javamodel/Divide.java": {name: "Divide", startLine: 18, endLine: 40},
  "src/calculator/javamodel/Power.java": {name: "Power", startLine: 18, endLine: 41},
  "src/calculator/javamodel/Remainder.java": {name: "Remainder", startLine: 18, endLine: 40},
  "src/calculator/javamodel/ChangeSigns.java": {name: "ChangeSigns", startLine: 18, endLine: 38},
  "src/calculator/javamodel/ClearStack.java": {name: "ClearStack", startLine: 18, endLine: 28},
  "src/calculator/javamodel/RotateDown.java": {name: "RotateDown", startLine: 19, endLine: 27},
  "src/calculator/javamodel/RotateUp.java": {name: "RotateUp", startLine: 19, endLine: 27},
  "src/calculator/javamodel/TwoToPower.java": {name: "TwoToPower", startLine: 18, endLine: 43},
  "src/calculator/javamodel/TenToPower.java": {name: "TenToPower", startLine: 18, endLine: 40},
  "src/calculator/largemodel/CalculatorImpl.java": {name: "CalculatorImpl", startLine: 28, endLine: 232},
  "src/calculator/largemodel/Push.java": {name: "Push", startLine: 18, endLine: 27},
  "src/calculator/largemodel/Add.java": {name: "Add", startLine: 19, endLine: 58},
  "src/calculator/largemodel/Multiply.java": {name: "Multiply", startLine: 19, endLine: 64},
  "src/calculator/largemodel/Power.java": {name: "Power", startLine: 18, endLine: 42}
};

const classNodeIds = {};
Object.entries(classDefs).forEach(([filePath, cls]) => {
  const id = `class:${cls.name}`;
  const fileId = `class:${filePath.replace(/\//g, '_').replace(/\./g, '_')}`; // unique ID
  const uniqueId = `class:${cls.name}@${filePath}`;
  classNodeIds[filePath] = uniqueId;
  nodes.push(node(uniqueId, "class", cls.name, { startLine: cls.startLine, endLine: cls.endLine }));
  edges.push(edge(`file:${filePath}`, uniqueId, "contains", 1));
});

// ============ FUNCTION NODES (10+ lines) & CONTAINS ============
const funcDefs = {
  "src/BigIntegers/ECMBigInteger.java": [
    {name: "Sum", startLine: 24, endLine: 33},
    {name: "Totient(N)", startLine: 44, endLine: 72},
    {name: "Moebius()", startLine: 74, endLine: 88},
    {name: "Moebius(N)", startLine: 90, endLine: 120},
    {name: "InsertAurifFactors", startLine: 122, endLine: 192},
    {name: "GetAurifeuilleFactor", startLine: 194, endLine: 207},
    {name: "ComputeFourSquares", startLine: 209, endLine: 382},
    {name: "Calculate", startLine: 384, endLine: 411}
  ],
  "src/BigIntegers/LargeInteger.java": [
    {name: "LargeInteger(s,b)", startLine: 81, endLine: 103},
    {name: "add", startLine: 124, endLine: 143},
    {name: "addInts", startLine: 151, endLine: 160},
    {name: "subtract", startLine: 165, endLine: 183},
    {name: "subtractInts", startLine: 191, endLine: 200},
    {name: "multiply", startLine: 211, endLine: 268},
    {name: "squareFFT", startLine: 273, endLine: 319},
    {name: "squareNTT", startLine: 324, endLine: 366},
    {name: "divide", startLine: 390, endLine: 428},
    {name: "getReciprocal", startLine: 442, endLine: 469},
    {name: "pow(exp)", startLine: 503, endLine: 512},
    {name: "pow(a,exp)", startLine: 532, endLine: 541},
    {name: "multiply(a)", startLine: 562, endLine: 576},
    {name: "divide(a)", startLine: 583, endLine: 599},
    {name: "toString", startLine: 606, endLine: 620},
    {name: "compareTo", startLine: 633, endLine: 647},
    {name: "and", startLine: 649, endLine: 661},
    {name: "shiftLeft", startLine: 663, endLine: 673},
    {name: "shiftRight", startLine: 675, endLine: 689},
    {name: "isPrime", startLine: 692, endLine: 728},
    {name: "pow(a,exp,db)", startLine: 549, endLine: 557}
  ],
  "src/Factorzations/GUI.java": [
    {name: "initComponents", startLine: 57, endLine: 405},
    {name: "FactorActionPerformed", startLine: 411, endLine: 464},
    {name: "Checks", startLine: 466, endLine: 493},
    {name: "EnterActionPerformed", startLine: 592, endLine: 607},
    {name: "main", startLine: 661, endLine: 685}
  ],
  "src/calculator/GUI.java": [
    {name: "setup", startLine: 84, endLine: 105}
  ],
  "src/calculator/javamodel/CalculatorImpl.java": [
    {name: "doOperation", startLine: 97, endLine: 121},
    {name: "doEncodedOperation", startLine: 145, endLine: 164},
    {name: "doStackOperation", startLine: 175, endLine: 189},
    {name: "changeBase", startLine: 127, endLine: 135}
  ],
  "src/calculator/largemodel/CalculatorImpl.java": [
    {name: "doOperation", startLine: 121, endLine: 145},
    {name: "doEncodedOperation", startLine: 171, endLine: 188},
    {name: "doStackOperation", startLine: 199, endLine: 217},
    {name: "changeBase", startLine: 151, endLine: 161}
  ],
  "src/calculator/largemodel/Add.java": [
    {name: "AddBigNbr", startLine: 42, endLine: 48},
    {name: "AddBigNbr32", startLine: 50, endLine: 57}
  ],
  "src/calculator/largemodel/Multiply.java": [
    {name: "MultBigNbr", startLine: 50, endLine: 63}
  ]
};

const funcNodeIds = {};
Object.entries(funcDefs).forEach(([filePath, funcs]) => {
  funcs.forEach(fn => {
    const id = `function:${fn.name}@${filePath}`;
    funcNodeIds[`${filePath}:${fn.name}`] = id;
    nodes.push(node(id, "function", fn.name, { startLine: fn.startLine, endLine: fn.endLine }));
    edges.push(edge(classNodeIds[filePath], id, "contains", 1));
  });
});

// ============ EXPORTS EDGES ============
// Main exports from ECMBigInteger.java
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:Sum@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:Totient(N)@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:Moebius()@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:Moebius(N)@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:InsertAurifFactors@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:GetAurifeuilleFactor@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:ComputeFourSquares@src/BigIntegers/ECMBigInteger.java`, "exports", 1));
edges.push(edge(classNodeIds["src/BigIntegers/ECMBigInteger.java"], `function:Calculate@src/BigIntegers/ECMBigInteger.java`, "exports", 1));

// Main exports from LargeInteger.java
const liFuncs = funcDefs["src/BigIntegers/LargeInteger.java"];
liFuncs.forEach(fn => {
  edges.push(edge(classNodeIds["src/BigIntegers/LargeInteger.java"], `function:${fn.name}@src/BigIntegers/LargeInteger.java`, "exports", 1));
});

// Main exports from Factorzations/GUI.java  
edges.push(edge(classNodeIds["src/Factorzations/GUI.java"], `function:main@src/Factorzations/GUI.java`, "exports", 1));

// calculator/GUI.java exports
edges.push(edge(classNodeIds["src/calculator/GUI.java"], `function:setup@src/calculator/GUI.java`, "exports", 1));

// calculator/javamodel/CalculatorImpl.java exports
const jmCalcFuncs = ["doOperation", "doEncodedOperation", "doStackOperation", "changeBase"];
jmCalcFuncs.forEach(fn => {
  const fid = `function:${fn}@src/calculator/javamodel/CalculatorImpl.java`;
  if (nodes.find(n => n.id === fid)) {
    edges.push(edge(classNodeIds["src/calculator/javamodel/CalculatorImpl.java"], fid, "exports", 1));
  }
});

// calculator/largemodel/CalculatorImpl.java exports
const lmCalcFuncs = ["doOperation", "doEncodedOperation", "doStackOperation", "changeBase"];
lmCalcFuncs.forEach(fn => {
  const fid = `function:${fn}@src/calculator/largemodel/CalculatorImpl.java`;
  if (nodes.find(n => n.id === fid)) {
    edges.push(edge(classNodeIds["src/calculator/largemodel/CalculatorImpl.java"], fid, "exports", 1));
  }
});

// ============ IMPLEMENTS / INHERITS EDGES ============
// javamodel operations that implement EncodedOperation
const encodedOpImpls = ["Add", "Subtract", "Multiply", "Divide", "Power", "Remainder", "ChangeSigns", "TwoToPower", "TenToPower"];
encodedOpImpls.forEach(clsName => {
  edges.push(edge(`class:${clsName}@src/calculator/javamodel/${clsName}.java`, "class:EncodedOperation@src/calculator/EncodedOperation.java", "implements", 1));
});

// javamodel operations that implement Operation (non-Encoded)
const operationImpls = ["Push", "Pop", "ClearStack", "RotateDown", "RotateUp"];
operationImpls.forEach(clsName => {
  edges.push(edge(`class:${clsName}@src/calculator/javamodel/${clsName}.java`, "class:Operation@src/calculator/Operation.java", "implements", 1));
});

// largemodel operations that implement EncodedOperation
["Add", "Multiply", "Power"].forEach(clsName => {
  edges.push(edge(`class:${clsName}@src/calculator/largemodel/${clsName}.java`, "class:EncodedOperation@src/calculator/EncodedOperation.java", "implements", 1));
});

// largemodel Push implements Operation
edges.push(edge("class:Push@src/calculator/largemodel/Push.java", "class:Operation@src/calculator/Operation.java", "implements", 1));

// EncodedOperation extends Operation
edges.push(edge("class:EncodedOperation@src/calculator/EncodedOperation.java", "class:Operation@src/calculator/Operation.java", "inherits", 1));

// javamodel CalculatorImpl implements Calculator (interface defined elsewhere)
// largemodel CalculatorImpl implements Calculator

// ============ CALLS EDGES (key ones from call graphs) ============
// ECMBigInteger internal calls
edges.push(edge(`function:InsertAurifFactors@src/BigIntegers/ECMBigInteger.java`, `function:Totient(N)@src/BigIntegers/ECMBigInteger.java`, "calls", 1));
edges.push(edge(`function:InsertAurifFactors@src/BigIntegers/ECMBigInteger.java`, `function:Moebius(N)@src/BigIntegers/ECMBigInteger.java`, "calls", 1));
edges.push(edge(`function:InsertAurifFactors@src/BigIntegers/ECMBigInteger.java`, `function:GetAurifeuilleFactor@src/BigIntegers/ECMBigInteger.java`, "calls", 1));
edges.push(edge(`function:ComputeFourSquares@src/BigIntegers/ECMBigInteger.java`, `function:Calculate@src/BigIntegers/ECMBigInteger.java`, "calls", 1));

// LargeInteger internal calls
edges.push(edge(`function:add@src/BigIntegers/LargeInteger.java`, `function:addInts@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:subtract@src/BigIntegers/LargeInteger.java`, `function:subtractInts@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:divide@src/BigIntegers/LargeInteger.java`, `function:getReciprocal@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:divide@src/BigIntegers/LargeInteger.java`, `function:multiply@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:divide@src/BigIntegers/LargeInteger.java`, `function:subtract@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:divide@src/BigIntegers/LargeInteger.java`, `function:add@src/BigIntegers/LargeInteger.java`, "calls", 1));
edges.push(edge(`function:getReciprocal@src/BigIntegers/LargeInteger.java`, `function:trunc@src/BigIntegers/LargeInteger.java`, "calls", 1));
// Note: trunc and pad might not be 10+ lines, but we can still reference them

// Factorzations/GUI.java internal calls
edges.push(edge(`function:FactorActionPerformed@src/Factorzations/GUI.java`, `function:Checks@src/Factorzations/GUI.java`, "calls", 1));

// calculator/javamodel/CalculatorImpl internal calls
edges.push(edge(`function:doOperation@src/calculator/javamodel/CalculatorImpl.java`, `function:changeBase@src/calculator/javamodel/CalculatorImpl.java`, "calls", 1));
edges.push(edge(`function:doOperation@src/calculator/javamodel/CalculatorImpl.java`, `function:doEncodedOperation@src/calculator/javamodel/CalculatorImpl.java`, "calls", 1));
edges.push(edge(`function:doOperation@src/calculator/javamodel/CalculatorImpl.java`, `function:doStackOperation@src/calculator/javamodel/CalculatorImpl.java`, "calls", 1));

// calculator/largemodel/CalculatorImpl internal calls
edges.push(edge(`function:doOperation@src/calculator/largemodel/CalculatorImpl.java`, `function:changeBase@src/calculator/largemodel/CalculatorImpl.java`, "calls", 1));
edges.push(edge(`function:doOperation@src/calculator/largemodel/CalculatorImpl.java`, `function:doEncodedOperation@src/calculator/largemodel/CalculatorImpl.java`, "calls", 1));
edges.push(edge(`function:doOperation@src/calculator/largemodel/CalculatorImpl.java`, `function:doStackOperation@src/calculator/largemodel/CalculatorImpl.java`, "calls", 1));

// Cross-file calls from Factorzations/GUI to ECMBigInteger
edges.push(edge(`function:FactorActionPerformed@src/Factorzations/GUI.java`, classNodeIds["src/BigIntegers/ECMBigInteger.java"], "calls", 1));
edges.push(edge(`function:FactorActionPerformed@src/Factorzations/GUI.java`, classNodeIds["src/BigIntegers/LargeInteger.java"], "calls", 1));

// calculator/javamodel/CalculatorImpl -> javamodel operations (through doEncodedOperation)
// These are indirect calls, skip for now

// calculator/largemodel/CalculatorImpl -> largemodel operations
// These are also indirect calls

// ============ EXPORTS from interfaces ============
edges.push(edge(classNodeIds["src/calculator/EncodedOperation.java"], classNodeIds["src/calculator/Operation.java"], "exports", 1));
edges.push(edge("file:src/calculator/calculatorUI.java", classNodeIds["src/calculator/calculatorUI.java"], "contains", 1));

// ============ ADD ADDITIONAL FILE-LEVEL EXPORTS ============
// Files export their classes
Object.entries(classDefs).forEach(([filePath, cls]) => {
  edges.push(edge(`file:${filePath}`, classNodeIds[filePath], "exports", 1));
});

// ============ WRITE OUTPUT ============
const result = { nodes, edges };

// Check counts
console.log(`Total nodes: ${nodes.length}, Total edges: ${edges.length}`);

if (nodes.length > 60 || edges.length > 120) {
  // Split into parts
  const midpoint = Math.ceil(nodes.length / 2);
  const part1Nodes = nodes.slice(0, midpoint);
  const part2Nodes = nodes.slice(midpoint);
  
  // For edges, assign to appropriate part based on node IDs in each part
  const part1NodeIds = new Set(part1Nodes.map(n => n.id));
  const part2NodeIds = new Set(part2Nodes.map(n => n.id));
  
  const part1Edges = edges.filter(e => part1NodeIds.has(e.from) || part1NodeIds.has(e.to));
  const part2Edges = edges.filter(e => !part1NodeIds.has(e.from) && !part1NodeIds.has(e.to));
  
  // Some edges may connect part1 to part2 - put them in part1
  const crossEdges = edges.filter(e => 
    (part1NodeIds.has(e.from) && part2NodeIds.has(e.to)) ||
    (part2NodeIds.has(e.from) && part1NodeIds.has(e.to))
  );
  
  // Actually, a simpler approach: split by file groupings
  // Part 1: BigIntegers + calculator top-level interfaces
  // Part 2: calculator/* + calculator/javamodel/* + calculator/largemodel/*
  
  const part1FilePaths = [
    "src/BigIntegers/ECMBigInteger.java",
    "src/BigIntegers/LargeInteger.java",
    "src/Factorzations/GUI.java",
    "src/calculator/EncodedOperation.java",
    "src/calculator/Operation.java",
    "src/calculator/calculatorUI.java"
  ];
  
  const part1FileIds = new Set(part1FilePaths.map(p => `file:${p}`));
  const part1ClassIds = new Set(part1FilePaths.map(p => classNodeIds[p]));
  
  // Get part1 function IDs
  const part1FuncIds = new Set();
  Object.entries(funcDefs).forEach(([fp, funcs]) => {
    if (part1FilePaths.includes(fp)) {
      funcs.forEach(fn => part1FuncIds.add(`function:${fn.name}@${fp}`));
    }
  });
  
  const part1AllIds = new Set([...part1FileIds, ...part1ClassIds, ...part1FuncIds]);
  
  const part1Nodes_list = nodes.filter(n => part1AllIds.has(n.id));
  const part2Nodes_list = nodes.filter(n => !part1AllIds.has(n.id));
  
  const part1Edges_list = edges.filter(e => part1AllIds.has(e.from) && part1AllIds.has(e.to));
  const part2Edges_list = edges.filter(e => !part1AllIds.has(e.from) && !part1AllIds.has(e.to));
  const crossEdges_list = edges.filter(e => 
    (part1AllIds.has(e.from) !== part1AllIds.has(e.to))
  );
  
  // Put cross edges in part1
  part1Edges_list.push(...crossEdges_list);
  
  console.log(`Part 1: ${part1Nodes_list.length} nodes, ${part1Edges_list.length} edges`);
  console.log(`Part 2: ${part2Nodes_list.length} nodes, ${part2Edges_list.length} edges`);
  
  const outDir = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/PrimeFactors/.understand-anything/intermediate";
  
  fs.writeFileSync(`${outDir}/batch-1-part-1.json`, JSON.stringify({nodes: part1Nodes_list, edges: part1Edges_list}, null, 2));
  fs.writeFileSync(`${outDir}/batch-1-part-2.json`, JSON.stringify({nodes: part2Nodes_list, edges: part2Edges_list}, null, 2));
  
  console.log("Written batch-1-part-1.json and batch-1-part-2.json");
} else {
  const outDir = "/Users/xuyi/Source/OS/armOS/lib/jcore/test/PrimeFactors/.understand-anything/intermediate";
  fs.writeFileSync(`${outDir}/batch-1.json`, JSON.stringify(result, null, 2));
  console.log(`Written batch-1.json to ${outDir}`);
}
