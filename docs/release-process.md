# XtreamPlayer Release Process

## Channels
- `Pre-release (RC)`: manual tester installs only. Autoupdater must not pick these.
- `Production`: stable rollout to all users.

## Naming Rules
- Tag: `XtreamPlayervx.x.x` (or `XtreamPlayervx.x.x-rcN` for pre-release)
- Title: `Xtream Player vx.x.x` (or `Xtream Player vx.x.x-rcN` for pre-release)
- Release notes first line: `# Changes:`
- APK filename must include the same version string as the tag.

## Pre-release Flow
1. Build and upload APK from the testing branch.
2. Publish GitHub release as `pre-release`.
3. Verify release asset filename includes `-rcN`.
4. Test manually on TV before promoting.

## Production Flow
1. Ship feature branch through PR merge into `main`.
2. Build release APK from `main` only.
3. Publish non-prerelease GitHub release targeting `main`.
4. Verify notes are user-friendly plain English and start with `# Changes:`.
5. Monitor diagnostics logs and crash signals for 24-48 hours.

## Staged Rollout Guard
- Never publish a production release from non-`main` target.
- Never reuse RC tags as production tags.
- If a production issue is found, publish a new patch version rather than editing an existing release.
