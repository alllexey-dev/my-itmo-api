// Validates spec/openapi/examples/<operationId>.json against the response schema of that operation.
// Examples exist for the research specs (my-itmo-extra, bars-extra, itmo-services); the two main specs
// (my-itmo, bars) are loaded too, so references into them resolve.
// Run from spec/tools: npm run examples
import { readdirSync, readFileSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { Ajv2020 } from "ajv/dist/2020.js";
import addFormats from "ajv-formats";
import { parse } from "yaml";

const specRoot = resolve(import.meta.dirname, "..");
const openapiDir = join(specRoot, "openapi");
const examplesDir = join(openapiDir, "examples");
const mainSpecs = new Set(["my-itmo.yaml", "bars.yaml"]);
// Specs reference each other by file name, so each one gets an $id in the same virtual directory.
const base = "https://my-itmo-api.invalid/spec/openapi/";
const ajv = new Ajv2020({ strict: false, allErrors: true });
addFormats.default(ajv);
ajv.addFormat("int32", true);
ajv.addFormat("int64", true);

const specs = readdirSync(openapiDir).filter((f) => f.endsWith(".yaml")).sort();
const docs = new Map(specs.map((f) => [f, parse(readFileSync(join(openapiDir, f), "utf8"))]));
for (const [file, doc] of docs) ajv.addSchema({ ...doc, $id: base + file });

const escape = (s: string) => s.replaceAll("~", "~0").replaceAll("/", "~1");
const methods = new Set(["get", "post", "put", "patch", "delete"]);
const used = new Set<string>();
let checked = 0;
let skipped = 0;
let failures = 0;
for (const file of specs.filter((f) => !mainSpecs.has(f))) {
  for (const [path, item] of Object.entries<Record<string, { operationId?: string }>>(docs.get(file).paths)) {
    for (const [method, op] of Object.entries(item)) {
      if (!methods.has(method) || !op?.operationId) continue;
      const example = join(examplesDir, `${op.operationId}.json`);
      let body: unknown;
      try {
        body = JSON.parse(readFileSync(example, "utf8"));
      } catch {
        skipped += 1;
        console.log(`skip ${op.operationId} (no example)`);
        continue;
      }
      used.add(`${op.operationId}.json`);
      const pointer = ["paths", path, method, "responses", "200", "content", "application/json", "schema"].map(escape).join("/");
      const validate = ajv.compile({ $ref: `${base}${file}#/${pointer}` });
      const ok = validate(body);
      checked += 1;
      if (!ok) failures += 1;
      console.log(`${ok ? "ok  " : "FAIL"} ${op.operationId} (${relative(specRoot, example)})`);
      for (const e of validate.errors ?? []) console.log(`     ${e.instancePath || "/"} ${e.keyword} ${e.message ?? ""}`);
    }
  }
}
// An example without an operation is a typo in a file name or a removed operation.
const orphans = readdirSync(examplesDir).filter((f) => f.endsWith(".json") && !used.has(f));
for (const f of orphans) console.log(`FAIL ${f}: no operation with this operationId in the research specs`);
console.log(`${checked} checked, ${skipped} without an example, ${failures + orphans.length} failed`);
process.exit(failures + orphans.length ? 1 : 0);
