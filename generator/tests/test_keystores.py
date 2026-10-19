"""Signing keys: one password per PKCS12 store, passwords Gradle can read back, never on argv."""

from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from genkit.core.spec import KeystoreSpec, SpecError
from genkit.targets.android import keystores

KEYTOOL = keystores.find_keytool()


def keystore(**overrides) -> KeystoreSpec:
    fields = dict(name="prod", alias="upload", store_password="hunter22", common_name="Acme", organisation="Acme")
    fields.update(overrides)
    return KeystoreSpec(**fields)


def load_properties(text: str) -> dict[str, str]:
    """`java.util.Properties.load` for the subset the generator writes: escapes and \\uXXXX."""
    values = {}
    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue
        key, _, raw = line.partition("=")
        out, index = [], 0
        while index < len(raw):
            if raw[index] == "\\" and raw[index + 1] == "u":
                out.append(chr(int(raw[index + 2:index + 6], 16)))
                index += 6
            elif raw[index] == "\\":
                out.append(raw[index + 1])
                index += 2
            else:
                out.append(raw[index])
                index += 1
        values[key] = "".join(out).encode("utf-16", "surrogatepass").decode("utf-16")
    return values


class ValidationTest(unittest.TestCase):

    def test_a_generated_key_needs_only_the_store_password(self):
        keystores.validate(keystore())

    def test_an_existing_key_needs_both(self):
        with self.assertRaises(SpecError):
            keystores.validate(keystore(existing_path="/tmp/x.jks", key_password="short"))

    def test_what_keytool_or_the_certificate_would_refuse(self):
        for bad in (
            keystore(name="release"),
            keystore(store_password="short"),
            keystore(alias="-storetype"),
            keystore(common_name="Acme, O=Somebody Else"),
            keystore(country="USA"),
            keystore(validity_days=0),
        ):
            with self.subTest(keystore=bad), self.assertRaises(SpecError):
                keystores.validate(bad)


class PropertiesTest(unittest.TestCase):

    def test_values_survive_java_properties(self):
        self.assertEqual("a\\\\b", keystores.properties_value("a\\b"))
        self.assertEqual("\\ \\ pw", keystores.properties_value("  pw"))
        self.assertEqual("plain=value:ok", keystores.properties_value("plain=value:ok"))

    def test_non_ascii_travels_as_unicode_escapes(self):
        # Properties.load reads ISO-8859-1, so raw UTF-8 bytes would read back as pÃ¤sswÃ¶rd1.
        self.assertEqual("p\\u00e4ssw\\u00f6rd1", keystores.properties_value("pässwörd1"))
        self.assertEqual("\\ud83d\\ude80x", keystores.properties_value("🚀x"))

    def test_a_generated_key_records_the_store_password_for_both(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp)
            keystores.write_properties(project, [keystore(key_password="different", store_password="pässwörd1")])
            raw = (project / "keystore.properties").read_bytes()
            raw.decode("ascii")
            values = load_properties(raw.decode("iso-8859-1"))
        self.assertEqual("pässwörd1", values["prod.storePassword"])
        self.assertEqual("pässwörd1", values["prod.keyPassword"])

    def test_an_existing_key_keeps_its_own_key_password(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp)
            keystores.write_properties(project, [keystore(existing_path="/x.jks", key_password="keypass1")])
            values = load_properties((project / "keystore.properties").read_text(encoding="iso-8859-1"))
        self.assertEqual("keypass1", values["prod.keyPassword"])


class RefusalTest(unittest.TestCase):

    def test_the_warning_quotes_keytool_s_error_not_its_progress(self):
        # What keytool printed on Linux for a folder where the key should go: it found that out only
        # when it wrote the key pair it had generated.
        path = "/tmp/x/Ána/keys/prod.jks"
        refused = subprocess.CompletedProcess(
            [],
            1,
            stdout=f"keytool error: java.io.FileNotFoundException: {path} (Is a directory)\n",
            stderr="Generating 2,048 bit RSA key pair and self-signed certificate (SHA384withRSA) with a validity of "
            "90 days\n\tfor: CN=Acme, O=Acme, C=US\n",
        )
        with (
            tempfile.TemporaryDirectory() as temp,
            mock.patch.object(keystores, "find_keytool", return_value="keytool"),
            mock.patch.object(keystores.subprocess, "run", return_value=refused),
        ):
            _, skipped, [warning] = keystores.generate(Path(temp), (keystore(),))
        self.assertEqual(["prod"], skipped)
        self.assertEqual(f"Keystore 'prod' could not be created: {refused.stdout.strip()}", warning)


@unittest.skipIf(KEYTOOL is None, "keytool is not installed")
class GenerationTest(unittest.TestCase):

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.project = Path(temp.name)

    def keytool_list(self, path: Path, password: str) -> subprocess.CompletedProcess:
        return subprocess.run(
            [KEYTOOL, "-list", "-keystore", str(path), "-storepass:env", "PASSWORD"],
            env={**os.environ, "PASSWORD": password}, capture_output=True, text=True, check=False,
        )

    def test_a_differing_key_password_is_dropped_with_a_warning_and_the_key_opens(self):
        generated, skipped, warnings = keystores.generate(self.project, (keystore(key_password="other-one"),))
        self.assertEqual((["prod"], []), (generated, skipped))
        self.assertTrue(any("PKCS12" in warning for warning in warnings))
        self.assertEqual(0, self.keytool_list(self.project / "keys/prod.jks", "hunter22").returncode)

    def test_a_non_ascii_password_round_trips(self):
        password = "pässwörd1"
        warnings = keystores.write(self.project, (keystore(store_password=password),), [])
        # The PBE key factory of older JDKs, behind their PKCS12 store, takes printable ASCII only.
        if any("Password is not ASCII" in warning for warning in warnings):
            self.skipTest(f"this keytool refuses a PKCS12 password outside ASCII: {warnings[0]}")
        self.assertEqual([], warnings)
        values = load_properties((self.project / "keystore.properties").read_text(encoding="iso-8859-1"))
        self.assertEqual(0, self.keytool_list(self.project / values["prod.storeFile"], values["prod.storePassword"]).returncode)

    def test_passwords_never_appear_on_the_command_line(self):
        seen: list[list[str]] = []
        original = subprocess.run

        def spy(command, *args, **kwargs):
            seen.append(list(command))
            return original(command, *args, **kwargs)

        with mock.patch.object(keystores.subprocess, "run", spy):
            keystores.generate(self.project, (keystore(store_password="secret-on-argv"),))
        self.assertTrue(seen)
        self.assertFalse(any("secret-on-argv" in part for command in seen for part in command))

    def test_keytool_s_refusal_names_a_non_ascii_path_as_it_is(self):
        project = self.project / "Ána"
        # A folder where the key should go: keytool fails to open it and names it.
        (project / "keys" / "prod.jks").mkdir(parents=True)
        generated, skipped, [warning] = keystores.generate(project, (keystore(),))
        self.assertEqual(([], ["prod"]), (generated, skipped))
        self.assertIn(str(project / "keys" / "prod.jks"), warning)

    def test_reusing_the_dev_key_for_staging_copies_it(self):
        dev = keystore(name="dev", alias="shared", store_password="devpass1")
        staging = keystore(name="staging", alias="shared", store_password="devpass1")
        generated, _, _ = keystores.generate(self.project, (dev, staging))
        self.assertEqual(["dev", "staging"], generated)
        self.assertEqual((self.project / "keys/dev.jks").read_bytes(), (self.project / "keys/staging.jks").read_bytes())


if __name__ == "__main__":
    unittest.main()
