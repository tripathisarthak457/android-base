"""
Signing keys: validated before `keytool` sees anything, generated as PKCS12, and described in
`keystore.properties` the way Gradle reads it back.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
from pathlib import Path

from ...core.spec import KeystoreSpec, SpecError

#: The four keys the build knows. dev and staging may share; prod and playstore must not.
KEYSTORE_NAMES: tuple[str, ...] = ("dev", "staging", "prod", "playstore")

#: keytool's own floor for a PKCS12 store. Shorter is refused several frames later, by a
#: subprocess whose stderr the user never sees.
MIN_KEYSTORE_PASSWORD = 6

#: Roughly a century, which is already past the point of meaning.
MAX_VALIDITY_DAYS = 36_500

ALIAS_PATTERN = r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$"
COUNTRY_PATTERN = r"^[A-Za-z]{2}$"

#: Characters with a meaning inside an X.500 distinguished name. Left in, they do not escape a
#: shell (keytool runs from a list, never a string), but they split one DN field into two and
#: produce a certificate whose subject is not what was asked for.
DNAME_SPECIALS = ',=+<>#;\\"'

_ALIAS = re.compile(ALIAS_PATTERN)
_COUNTRY = re.compile(COUNTRY_PATTERN)


def validate(keystore: KeystoreSpec) -> None:
    where = "android.keystores"
    if keystore.name not in KEYSTORE_NAMES:
        raise SpecError(
            f"Unknown signing key '{keystore.name}'. The build has four: {', '.join(KEYSTORE_NAMES)}.",
            where,
        )
    if not _ALIAS.fullmatch(keystore.alias):
        raise SpecError(
            f"The {keystore.name} key alias must be letters, digits, dots, hyphens or underscores, "
            "and start with a letter or digit.",
            where,
        )

    # A generated store has one password. An existing one is copied, and its passwords are the
    # file's own, which may differ for a JKS store.
    passwords = [("store", keystore.store_password)]
    if keystore.existing_path is not None:
        passwords.append(("key", keystore.key_password))
    for label, password in passwords:
        if len(password) < MIN_KEYSTORE_PASSWORD:
            raise SpecError(
                f"The {keystore.name} {label} password must be at least {MIN_KEYSTORE_PASSWORD} "
                "characters: keytool refuses anything shorter.",
                where,
            )
        if not password.isprintable():
            raise SpecError(f"The {keystore.name} {label} password cannot contain control characters.", where)

    validate_dname_part(f"{keystore.name} common name", keystore.common_name)
    validate_dname_part(f"{keystore.name} organisation", keystore.organisation)
    validate_country(keystore.country)
    if not 1 <= keystore.validity_days <= MAX_VALIDITY_DAYS:
        raise SpecError(
            f"The {keystore.name} validity must be between 1 and {MAX_VALIDITY_DAYS} days.", where
        )


def validate_dname_part(label: str, value: str) -> None:
    """One component of a certificate's subject."""
    if not value.strip():
        raise SpecError(f"The {label} cannot be empty.", "android.keystores")
    if not value.isprintable() or any(character in DNAME_SPECIALS for character in value):
        raise SpecError(
            f"The {label} cannot contain any of {''.join(sorted(DNAME_SPECIALS))}: they separate "
            "the fields of the certificate's subject.",
            "android.keystores",
        )


def validate_country(value: str) -> None:
    if not _COUNTRY.fullmatch(value):
        raise SpecError("The country must be a two-letter code, like US or IN.", "android.keystores")


def find_keytool() -> str | None:
    found = shutil.which("keytool")
    if found is not None:
        return found
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        return None
    candidate = Path(java_home) / "bin" / "keytool"
    for path in (candidate.with_suffix(".exe"), candidate):
        if path.exists():
            return str(path)
    return None


def write(project: Path, keystores: tuple[KeystoreSpec, ...], summary: list[str]) -> list[str]:
    """Creates `keys/*.jks` and `keystore.properties`. Returns warnings."""
    if not keystores:
        return []
    generated, skipped, warnings = generate(project, keystores)
    # Only keys that exist get a stanza: a properties file naming a .jks that is not in the
    # project fails the build, where a missing stanza falls back to the debug key.
    write_properties(project, [keystore for keystore in keystores if keystore.name in generated])
    if generated:
        summary.append(f"Keystores: {', '.join(generated)}")
    if skipped:
        summary.append(f"Keystores skipped: {', '.join(skipped)}")
    return warnings


def generate(project: Path, keystores: tuple[KeystoreSpec, ...]) -> tuple[list[str], list[str], list[str]]:
    """Returns (generated, skipped, warnings)."""
    keys = project / "keys"
    keys.mkdir(parents=True, exist_ok=True)

    keytool = find_keytool()
    if keytool is None:
        return [], [keystore.name for keystore in keystores], [
            "keytool was not found on PATH or under JAVA_HOME, so no keystores were created. The "
            "project still builds: every variant falls back to the debug key and the build says "
            "so. Create them later and fill in keystore.properties."
        ]

    generated: list[str] = []
    skipped: list[str] = []
    warnings: list[str] = []
    made: dict[KeystoreSpec, Path] = {}
    for keystore in keystores:
        target = keys / f"{keystore.name}.jks"

        if keystore.existing_path:
            source = Path(keystore.existing_path).expanduser()
            if source.is_file():
                shutil.copy2(source, target)
                generated.append(keystore.name)
            else:
                skipped.append(keystore.name)
                warnings.append(f"Keystore '{keystore.name}': {source} does not exist; skipped.")
            continue

        if keystore.key_password and keystore.key_password != keystore.store_password:
            warnings.append(
                f"The {keystore.name} key password was not used: a PKCS12 keystore has one password "
                "for the store and its key, so keystore.properties uses the store password for both."
            )

        # "Reuse the dev key for staging" means the same key, not a second one with the same alias.
        twin = next((path for spec, path in made.items() if _same_key(spec, keystore)), None)
        if twin is not None:
            shutil.copy2(twin, target)
            generated.append(keystore.name)
            continue

        command = [
            keytool, "-genkeypair", "-noprompt",
            "-keystore", str(target),
            "-alias", keystore.alias,
            # From the environment, never argv: any local user can read a process's arguments.
            "-storepass:env", "GENKIT_STORE_PASSWORD",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", str(keystore.validity_days),
            "-dname", keystore.dname,
            "-storetype", "PKCS12",
        ]
        environment = {**os.environ, "GENKIT_STORE_PASSWORD": keystore.store_password}
        result = subprocess.run(command, capture_output=True, text=True, check=False, env=environment)
        if result.returncode == 0:
            generated.append(keystore.name)
            made[keystore] = target
        else:
            skipped.append(keystore.name)
            output = (result.stderr or result.stdout).strip().splitlines()
            warnings.append(
                f"Keystore '{keystore.name}' could not be created: {output[-1] if output else 'unknown error'}"
            )
    return generated, skipped, warnings


def _same_key(a: KeystoreSpec, b: KeystoreSpec) -> bool:
    return (a.alias, a.store_password, a.dname, a.validity_days) == (b.alias, b.store_password, b.dname, b.validity_days)


def write_properties(project: Path, keystores: list[KeystoreSpec]) -> None:
    if not keystores:
        return
    lines = [
        "# Generated. Never commit this file: it is in .gitignore for a reason.",
        "# Store paths are relative to the root of the build.",
        "",
    ]
    for keystore in keystores:
        key_password = keystore.key_password if keystore.existing_path else keystore.store_password
        lines += [
            f"{keystore.name}.storeFile=keys/{keystore.name}.jks",
            f"{keystore.name}.storePassword={properties_value(keystore.store_password)}",
            f"{keystore.name}.keyAlias={keystore.alias}",
            f"{keystore.name}.keyPassword={properties_value(key_password or keystore.store_password)}",
            "",
        ]
    # java.util.Properties reads ISO-8859-1, so anything beyond ASCII travels as \uXXXX.
    (project / "keystore.properties").write_bytes("\n".join(lines).encode("iso-8859-1"))


def properties_value(value: str) -> str:
    """`value` escaped for java.util.Properties, which is how Gradle reads the file back."""
    escaped = value.replace("\\", "\\\\")
    stripped = escaped.lstrip(" ")
    escaped = "\\ " * (len(escaped) - len(stripped)) + stripped
    out = []
    for character in escaped:
        if ord(character) < 0x7F:
            out.append(character)
        else:
            encoded = character.encode("utf-16-be")
            out.extend(f"\\u{int.from_bytes(encoded[i:i + 2], 'big'):04x}" for i in range(0, len(encoded), 2))
    return "".join(out)
