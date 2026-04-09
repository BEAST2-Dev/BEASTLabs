# Releasing BEASTLabs to Maven Central

Pushing a `v*` tag triggers the `ci-publish.yml` workflow, which
derives the Maven version from the tag, builds, tests, GPG-signs, and
auto-publishes to Maven Central.

## Pre-tag checklist

1. **`version.xml`** -- update the `version` attribute to match the release.

2. **`pom.xml`** -- ensure `beast.version` points to a published beast3 release.

3. **Commit** the above changes to master.

## Tag and release

```bash
git tag v2.1.0
git push origin v2.1.0
```

Monitor the workflow run at:
https://github.com/BEAST2-Dev/BEASTLabs/actions/workflows/ci-publish.yml

## After release

Verify the artifacts appear on Maven Central:
https://central.sonatype.com/namespace/io.github.beast2-dev

## Secrets required (GitHub Actions)

Stored as BEAST2-Dev **org-level** secrets so all repos can share them:

| Secret | Description |
|--------|-------------|
| `GPG_PRIVATE_KEY` | `gpg --armor --export-secret-keys <KEY_ID>` |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |
| `CENTRAL_USERNAME` | Sonatype Central Portal token username |
| `CENTRAL_TOKEN` | Sonatype Central Portal token password |

Generate Sonatype tokens at https://central.sonatype.com/account
