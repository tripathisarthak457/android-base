#!/usr/bin/env bash
# Each mutation of the token store and the backup rules must fail at least one test; the tree is restored after each.
# Run as: $L gradle bash token_store_mutations.win.sh
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06
cd D:/projects/tools/.overhaul/wt/WP06/templates/android
STORE=core/datastore/src/main/kotlin/com/base/app/core/datastore/session/AuthTokenStore.kt
RULES=app/src/main/res/xml/data_extraction_rules.xml
mutate() { # file, old, new
  python - "$1" "$2" "$3" <<'PY'
import sys
p, old, new = sys.argv[1], sys.argv[2].encode().decode("unicode_escape"), sys.argv[3].encode().decode("unicode_escape")
s = open(p, encoding="utf-8", newline="").read()
assert s.count(old) == 1, (p, old)
open(p, "w", encoding="utf-8", newline="").write(s.replace(old, new))
PY
}
run() { # name, file, task
  local name=$1 file=$2 task=$3 log=$O/mut/$1.log
  timeout 15m ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle "$task" --console=plain > "$log" 2>&1
  local rc=$?
  git checkout -q -- "$file"
  if [ $rc -eq 0 ]; then echo "SURVIVED $name"; else echo "KILLED $name: $(grep -E '^[A-Za-z]+Test > .* FAILED' "$log" | tr '\n' ';')"; fi
}
T=:core:datastore:testDebugUnitTest
mutate $STORE '        .flowOn(dispatchers.io)\n' ''; run m1-no-flowon $STORE $T
mutate $STORE '        withContext(dispatchers.io) {\n            val sealed = cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), tokens))\n            dataStore.edit { it[TOKENS] = sealed }\n        }' '        val sealed = cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), tokens))\n        dataStore.edit { it[TOKENS] = sealed }'; run m2-save-on-caller $STORE $T
mutate $STORE 'Boolean =\n        withContext(dispatchers.io) {' 'Boolean =\n        kotlinx.coroutines.coroutineScope {'; run m3-replace-on-caller $STORE $T
mutate $STORE '    override val isAuthenticated: Flow<Boolean> = dataStore.data\n        .map { preferences -> TOKENS in preferences }\n        .distinctUntilChanged()\n\n' ''; run m4-decrypting-isauthenticated $STORE $T
mutate $STORE 'decode(sealed) ?: discard(sealed)' 'decode(sealed)'; run m5-no-discard $STORE $T
mutate $RULES '    <device-transfer>\n        <exclude domain="file" path="datastore/session_tokens.preferences_pb" />' '    <device-transfer>\n        <exclude domain="file" path="datastore/session.preferences_pb" />'; run m6-stale-transfer-rule $RULES :app:testDevDebugUnitTest
git status --short
echo MUTATIONS DONE
