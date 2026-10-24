import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { test } from "node:test";
import { PRIVACY, REPO } from "../../lib/links.ts";
import { readText, repositoryPath } from "../catalogue/testing.ts";

// The hero promises "here is exactly what", so the privacy page is checked against the code that
// keeps the data: the site's own browser storage, the origins it may load from, the API's tables
// and its retention defaults. A new column or storage key fails here until the page names it.

const PAGE_PATH = PRIVACY.slice(`${REPO}/blob/main/`.length);

function page(): string {
  return readText(...PAGE_PATH.split("/"));
}

/** Source files of the site that ship, tests excluded. */
function siteSources(): string[] {
  const roots = ["app", "components", "features", "lib"].map((root) => repositoryPath("web", "frontend", root));
  const files: string[] = [];
  const walk = (directory: string) => {
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = join(directory, entry.name);
      if (entry.isDirectory()) walk(path);
      else if (/\.tsx?$/.test(entry.name) && !entry.name.endsWith(".test.ts")) files.push(path);
    }
  };
  for (const root of roots) walk(root);
  files.push(repositoryPath("web", "frontend", "instrumentation-client.ts"));
  return files;
}

type StorageKey = { storage: "localStorage" | "sessionStorage"; key: string; file: string };

/** Every key the site reads or writes in web storage, a constant resolved in its own file first. */
function storageKeys(): StorageKey[] {
  const sources = new Map(siteSources().map((file) => [file, readFileSync(file, "utf8")]));
  const constant = (name: string, source: string): string | undefined => {
    const declared = new RegExp(`\\bconst ${name} = "([^"]+)"`);
    const values = new Set([source, ...sources.values()].flatMap((text) => declared.exec(text)?.[1] ?? []));
    return values.size === 1 ? [...values][0] : declared.exec(source)?.[1];
  };
  const keys: StorageKey[] = [];
  for (const [file, source] of sources) {
    const calls = source.matchAll(/\b(localStorage|sessionStorage)\.(?:getItem|setItem|removeItem)\(\s*(?:"([^"]+)"|([A-Za-z_]\w*))/g);
    for (const [, storage, literal, name = ""] of calls) {
      const key = literal ?? constant(name, source);
      assert.ok(key, `${file}: cannot resolve the storage key ${name}`);
      keys.push({ storage: storage as StorageKey["storage"], key, file });
    }
  }
  return keys;
}

/** Each table's columns, from the API's migrations in order. */
function storedColumns(): Map<string, Set<string>> {
  const directory = repositoryPath("web", "api", "internal", "store", "migrations");
  return columnsOf(
    readdirSync(directory)
      .filter((name) => name.endsWith(".sql"))
      .sort()
      .map((name) => readFileSync(join(directory, name), "utf8"))
      .join("\n"),
  );
}

/** Each table's columns once `migrations` (SQL) has run. */
function columnsOf(migrations: string): Map<string, Set<string>> {
  const tables = new Map<string, Set<string>>();
  const sql = migrations.replace(/--.*$/gm, "");
  for (const statement of sql.split(";")) {
    const created = /CREATE TABLE(?: IF NOT EXISTS)?\s+(\w+)\s*\(([\s\S]*)\)\s*$/i.exec(statement.trim());
    if (created) {
      const [, table = "", body = ""] = created;
      const columns = new Set<string>();
      let depth = 0;
      let part = "";
      for (const char of `${body},`) {
        if (char === "(") depth++;
        if (char === ")") depth--;
        if (char === "," && depth === 0) {
          const name = /^\s*(\w+)/.exec(part)?.[1];
          if (name && !/^(PRIMARY|UNIQUE|CONSTRAINT|FOREIGN|CHECK)$/i.test(name)) columns.add(name);
          part = "";
        } else {
          part += char;
        }
      }
      tables.set(table, columns);
      continue;
    }
    const altered = /ALTER TABLE\s+(?:IF EXISTS\s+)?(?:ONLY\s+)?(\w+)/i.exec(statement);
    if (altered?.[1]) {
      const columns = tables.get(altered[1]);
      assert.ok(columns, `ALTER TABLE ${altered[1]} before it is created`);
      for (const [, name = ""] of statement.matchAll(/ADD COLUMN\s+(?:IF NOT EXISTS\s+)?(\w+)/gi)) columns.add(name);
      for (const [, name = ""] of statement.matchAll(/DROP COLUMN\s+(?:IF EXISTS\s+)?(\w+)/gi)) columns.delete(name);
      for (const [, from = "", to = ""] of statement.matchAll(/RENAME COLUMN\s+(\w+)\s+TO\s+(\w+)/gi)) {
        columns.delete(from);
        columns.add(to);
      }
      const renamed = /RENAME TO\s+(\w+)/i.exec(statement)?.[1];
      if (renamed) {
        tables.delete(altered[1]);
        tables.set(renamed, columns);
      }
    }
  }
  return tables;
}

test("the privacy page the site links to is in the repository", () => {
  assert.ok(PRIVACY.startsWith(`${REPO}/blob/main/`), PRIVACY);
  assert.ok(existsSync(repositoryPath(...PAGE_PATH.split("/"))), `${PAGE_PATH} is missing`);
});

test("the privacy page names every key the site keeps in the browser, and where", () => {
  const text = page();
  const keys = storageKeys();
  assert.ok(keys.length >= 4, "the scan found the session, draft, theme and admin token keys");
  for (const { storage, key, file } of keys) {
    const row = text.split("\n").find((line) => line.includes(`\`${key}\``));
    assert.ok(row, `${key} (${file}) is not on the privacy page`);
    assert.ok(row.includes(storage), `the privacy page's line for ${key} does not say it is in ${storage}`);
  }
});

test("the privacy page names every other origin the site may load from", () => {
  const config = readText("web", "frontend", "next.config.mjs");
  const origins = [...config.matchAll(/https:\/\/([a-z0-9.-]+)/g)].map(([, host]) => host ?? "");
  assert.ok(origins.length > 0);
  for (const host of new Set(origins)) assert.ok(page().includes(host), `${host} is not on the privacy page`);
});

test("the privacy page names every column the API stores", () => {
  const text = page();
  const tables = storedColumns();
  assert.ok(tables.size >= 7, [...tables.keys()].join(", "));
  for (const [table, columns] of tables) {
    assert.ok(text.includes(`\`${table}\``), `the table ${table} is not on the privacy page`);
    for (const column of columns) {
      if (column === "id") continue;
      assert.ok(text.includes(`\`${column}\``), `${table}.${column} is not on the privacy page`);
    }
  }
});

test("the retention periods on the privacy page are the API's defaults", () => {
  const config = readText("web", "api", "internal", "config", "config.go");
  const defaults = [...config.matchAll(/days\("(RETENTION_\w+_DAYS)", (\d+)\)/g)];
  assert.ok(defaults.length >= 3);
  for (const [, variable = "", days = ""] of defaults) {
    const row = page()
      .split("\n")
      .find((line) => line.includes(`\`${variable}\``));
    assert.ok(row, `${variable} is not on the privacy page`);
    assert.match(row, new RegExp(`\\b${days}\\b`), `the privacy page's line for ${variable} does not give its default, ${days}`);
  }
});

test("the migration reader follows columns and tables as they are added, dropped and renamed", () => {
  const tables = columnsOf(`
    CREATE TABLE visits (id bigserial PRIMARY KEY, ip_hash text, at timestamptz NOT NULL, UNIQUE (ip_hash, at));
    CREATE TABLE IF NOT EXISTS hits (n integer);
    ALTER TABLE visits ADD COLUMN IF NOT EXISTS city text, ADD COLUMN country text; -- a comment; with a semicolon
    ALTER TABLE visits DROP COLUMN IF EXISTS country;
    ALTER TABLE ONLY visits RENAME COLUMN ip_hash TO visitor_hash;
    ALTER TABLE hits RENAME TO counts;
  `);
  assert.deepEqual([...tables.keys()].sort(), ["counts", "visits"]);
  assert.deepEqual([...(tables.get("visits") ?? [])].sort(), ["at", "city", "id", "visitor_hash"]);
  assert.deepEqual([...(tables.get("counts") ?? [])], ["n"]);
});
