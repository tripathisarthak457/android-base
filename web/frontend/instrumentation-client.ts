import { flushOnHide, reportErrors } from "./features/telemetry/client.ts";

// Next runs this file on every page before React hydrates, so an error thrown during hydration,
// or anywhere on /admin, which has no configurator, still reaches the admin's error list.
reportErrors();
flushOnHide();
