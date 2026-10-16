# WP06 early note: where scaffolded modules add their tables

Written 2026-10-09 at the start of WP06, for WP02-M2's scaffolder (`scaffold.py`). It holds until WP06 merges;
the WP06 handoff repeats it with anything that changed.

## The database (`:core:database`, Room 3, `androidx.room3` 3.0.3)

- One `AppDatabase` lists every table. Entities and DAOs live in `:core:database`, one sub-package per data
  domain: `core/database/src/main/kotlin/<pkg_path>/core/database/<name>/<P>Entity.kt` and `<P>Dao.kt`,
  package `<pkg>.core.database.<name>`. Core may not depend on data, so the table cannot live in `:data:<name>`.
- The database starts at version 1 in every generated project; a scaffolded table joins version 1. There is no
  migration to write until the app has shipped.
- Each data module takes its DAO from `AppDatabase` in its own Hilt module; `:core:database` provides only the
  database. The data module adds `implementation(project(":core:database"))` (inside `<opt:database>` in the
  template; unconditionally in a scaffolded module when `database` is on).

## The three generated points in `AppDatabase.kt`

File: `templates/android/core/database/src/main/kotlin/com/base/app/core/database/AppDatabase.kt`.
All three are points (`// <generated:name>` with no end marker), so the target must supply each one: `[]` when
nothing is scaffolded. The names are in `template.toml` `[regions] required`.

| Point | Where | One line per scaffolded module (exact text, `\n`-terminated) |
|---|---|---|
| `database-imports` | after the last import | `import <pkg>.core.database.<name>.<P>Dao` and `import <pkg>.core.database.<name>.<P>Entity` (two lines) |
| `database-entities` | inside `entities = [ … ]` | `        <P>Entity::class,` (8 spaces) |
| `database-daos` | in the class body, after the template's DAOs | `    abstract fun <camel>(): <P>Dao` (4 spaces; `<camel>` is `<P>` with a lower-case first letter, `orderHistory`) |

`<pkg>` is the dotted package, `<name>` the module name as written in the spec (lower snake case is legal in
a package segment), `<P>` its Pascal form (`pascal_case(name)`). As built, WP06's own `generator/genkit/targets/
android/database.py` already emits these points and writes the entity and DAO for every `feature_modules` entry
when `database` is on (`database.regions(spec)`, `database.write_tables(project, spec)`); WP02-M2's scaffolder
only needs its data module to map them. A project with `database` but neither `sample` nor a module gets a
starter table `note`; `di`, `queue`, `sample` and `note` are reserved module names. The `database` feature owns `core/database`, so a
spec without it never renders these points; `regions()` may still return the keys (they are ignored when the
file is absent) but must not fail.

## The snippet

`generator/genkit/targets/android/snippets/database/Entity.kt` and `Dao.kt`, with the tokens `__PKG__`,
`__NAME__`, `__P__`, `__TABLE__` (the table name, the module name) and `__ABOUT__` (the entity's KDoc line). The entity is `<P>Entity(@PrimaryKey val id: String, val title: String, val body:
String)` with `toDomain()`/`toEntity()` mappers written by the data module in `local/<P>EntityMappers.kt`; the DAO
has `fun observeAll(): Flow<List<<P>Entity>>`, `suspend fun byId(id: String): <P>Entity?`, `@Upsert suspend fun
upsertAll(rows: List<<P>Entity>)` and `@Transaction suspend fun replaceAll(rows)` (delete, then upsert), which is
what an offline-first repository's `refresh()` calls. The template's `sample` domain
(`core/database/.../sample/`) is the reference, and `data/sample` with `database` on is the repository to copy.
