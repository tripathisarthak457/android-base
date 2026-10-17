# Generator unit tests on Windows: the baseline (2026-10-17)

`python -I -m unittest discover -s tests -t .` in `generator/`, on a clean copy of the integration head
`overhaul/multiplatform-v2` (`a402d87`, extracted with `core.autocrlf=false`), Python 3.15.0, Windows 11, no Developer
Mode. 249 tests in 113 s: **5 failures, 1 error, 1 skipped**. None comes from a code change; all are host differences.
Package WIN fixes them; until it merges, judge any change by failures beyond these six.

| Test | Result | Cause |
|---|---|---|
| `test_golden.GoldenTest.test_every_case_matches_its_golden_tree` (case `all`) | FAIL | `transformed.txt` written as `...BaseApp\r\n`: `Path.write_text` translates `\n` to CRLF on Windows |
| same, case `none` | FAIL | same |
| same, case `hostile` | FAIL | same |
| `test_golden.GoldenTest.test_the_wrapper_script_is_executable_and_nothing_else_is` | FAIL | Windows has no exec bit: `os.stat` reports `run.bat` as executable and `gradlew` as not |
| `test_cli.CliTest.test_json_means_only_json_on_stdout` | FAIL | the reported path is `C:\Users\crazyhusir\...`, the test expects `C:\Users\CRAZYH~1\...` (8.3 short name in `%TEMP%`) |
| `test_cli` symlink test | ERROR | `os.symlink`: `[WinError 1314] A required privilege is not held by the client` |
