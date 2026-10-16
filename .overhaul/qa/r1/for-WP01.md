# Visual QA round 1: WP01 (spec v2)

Full table: `DEFECTS.md`. This file has one item. It supports WP02-M2's fixes for QA-09 and
QA-10.

## QA-09 and QA-10 (S2). The spec cannot say which modules are tabs, or which icon each one uses

Today `feature_modules` is a list of names (`generator/genkit/core/spec.py:180`, `:391`), so
every module becomes a tab with the same icon.

Accept an object form next to the plain string, so v1 and v2 specs keep loading:

```json
"feature_modules": ["notes", {"name": "orders", "tab": true, "icon": "Package"}, {"name": "wallet", "tab": false}]
```

- `tab` defaults to `true`, which matches today's behaviour.
- Validate `icon` against WP03's `AppIcons` names: a wrong name is a `SpecError` that names the
  field.
- `validate_feature_modules` warns, rather than fails, when the bar would hold more than 5 tabs.
  WP02-M2 then routes the extras into "More".
- Update the catalogue, the JSON schema the website reads (WP10 consumes it), `spec upgrade`, and
  the fixture tests.
