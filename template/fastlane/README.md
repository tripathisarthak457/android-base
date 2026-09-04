# Release lanes

Run everything through Bundler, so the version that ran in CI is the version that runs here:

```bash
gem install bundler
bundle install
bundle exec fastlane android <lane>
```

| Lane | What it does |
| --- | --- |
| `check` | Detekt and the `devDebug` unit tests. The same thing CI runs on a pull request. |
| `dev` | `installDevDebug` on a connected device. |
| `bump` | Rewrites the version in `AppConfig.kt`. `type:major\|minor\|patch`, or `name:1.4.0`. Always increments `versionCode`. |
| `changelog` | Prepends the commits since the last tag to `CHANGELOG.md`. |
| `release` | Signed per-ABI APKs and a bundle. `flavour:dev\|staging\|prod\|playstore`, default `prod`. |
| `tag` | Tags `v<name>+<code>` and pushes it. |
| `internal` | Uploads the playstore bundle to the internal testing track. |

## A release, end to end

```bash
bundle exec fastlane android bump type:minor
bundle exec fastlane android changelog
git commit -am "Release 1.1.0"
bundle exec fastlane android tag
bundle exec fastlane android release flavour:prod
```

## Before `internal` works

`upload_to_play_store` needs a Google Play service account:

1. Play Console → Setup → API access → create a service account.
2. Grant it **Release manager** on this application only.
3. Download the JSON key, keep it out of the repository, and point at it:

```bash
export SUPPLY_JSON_KEY=/absolute/path/play-service-account.json
```

The path is not defaulted anywhere on purpose. A key pointing at the wrong application uploads a
build to somebody else's listing, and that is not undoable from a terminal.

## Signing

None of these lanes sign anything themselves. Signing is configured in Gradle from
`keystore.properties`, so a release built here and one built with `./gradlew distProdRelease` are
byte-for-byte the same artifact — which is the only thing that makes reproducing a CI build
locally worth doing.
