#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

function readCoverageSummary(baseDir) {
  const candidates = [
    path.join(baseDir, 'coverage-summary.json'),
    path.join(baseDir, 'coverage-final.json'),
    path.join(baseDir, 'coverage', 'coverage-summary.json'),
    path.join(baseDir, 'coverage', 'coverage-final.json'),
    path.join(baseDir, 'lcov-report', 'coverage-summary.json'),
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) {
      try {
        return JSON.parse(fs.readFileSync(p, 'utf8'));
      } catch (e) {
        // ignore and continue
      }
    }
  }
  return null;
}

function getPctFromSummary(summary) {
  if (!summary) return null;
  if (summary.total && summary.total.lines && typeof summary.total.lines.pct === 'number') {
    return summary.total.lines.pct;
  }
  // some tools put totals under 'lines'
  if (summary.lines && typeof summary.lines.pct === 'number') return summary.lines.pct;
  return null;
}

function pctFromCoverageFinal(obj) {
  // obj is the coverage-final.json mapping: file -> metrics
  const files = Object.keys(obj);
  let total = 0;
  let covered = 0;
  for (const f of files) {
    const entry = obj[f];
    if (!entry || !entry.s) continue;
    const statements = Object.keys(entry.s).length;
    const coveredCount = Object.values(entry.s).filter(v => v > 0).length;
    total += statements;
    covered += coveredCount;
  }
  if (total === 0) return null;
  return (covered / total) * 100;
}

const args = process.argv.slice(2);
if (args.length < 1) {
  console.error('Usage: check-coverage.js <coverage-dir> <thresholdPercent>');
  process.exit(2);
}
const coverageDir = args[0];
const threshold = Number(args[1] || 70);

const summary = readCoverageSummary(coverageDir);
if (!summary) {
  console.error('Coverage summary not found in', coverageDir);
  process.exit(3);
}
const pct = getPctFromSummary(summary);
let finalPct = pct;
if (finalPct === null) {
  // try to compute from coverage-final.json structure
  try {
    finalPct = pctFromCoverageFinal(summary);
  } catch (e) {
    finalPct = null;
  }
}
if (finalPct === null) {
  console.error('Could not determine lines coverage percentage from summary.');
  process.exit(4);
}
console.log(`Lines coverage: ${finalPct}%  (threshold ${threshold}%)`);
if (finalPct < threshold) {
  console.error('Coverage threshold not met');
  process.exit(1);
}
process.exit(0);
