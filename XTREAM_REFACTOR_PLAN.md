# XtreamPlayer Refactor Plan

Status legend:
- `[ ]` not started
- `[~]` in progress
- `[x]` done

## What is already done

- `[x]` `MainActivity` no longer injects and forwards repositories into the UI.
- `[x]` `RootScreen` now resolves app singletons through a Hilt entry point.
- `[x]` `SettingsRepository` is no longer manually instantiated inside `RootScreen`.
- `[x]` update-check state has been moved out of local composable state into `UpdateViewModel`.
- `[x]` `:app:compileDebugKotlin` passes after the refactor.

## Goal

Reduce the size and coupling of `MainActivityUi.kt` without changing behavior, then move the remaining app state out of the root composable and into focused view models or feature controllers.

## Phase 1: Split the root UI into smaller composables

- `[x]` Extract startup/update logic into `UpdateSection` or `UpdateHost`.
- `[x]` Extract top-level navigation shell into a dedicated composable file.
- `[x]` Extract dialog orchestration into a separate `DialogsHost`.
- `[x]` Extract playback/player wiring into a dedicated `PlayerHost`.
- `[x]` Extract settings and sync coordination into a `SettingsAndSyncHost`.
- `[x]` Keep each extracted host behavior-identical at first.
- `[x]` Re-run `:app:compileDebugKotlin` after each extraction or small batch.

Acceptance criteria:
- `MainActivityUi.kt` still compiles.
- No user-visible behavior changes.
- Each extracted block has a narrow set of inputs and outputs.

## Phase 2: Move root state into view models

- `[ ]` Identify the state that currently belongs to the screen, not the component tree.
- `[x]` Identify the state that currently belongs to the screen, not the component tree.
- `[x]` Add `UiState` data classes for major sections instead of many `mutableStateOf` fields.
- `[ ]` Migrate state into `StateFlow` or `MutableStateFlow` where appropriate.
- `[x]` Keep Compose state only for ephemeral UI details that are truly local.
- `[x]` Migrate browse, update, and playback orchestration first.

Suggested targets:
- `[x]` `selectedSection`
- `[x]` `navExpanded`
- `[x]` update dialog state
- `[x]` startup update check flags
- `[x]` sync progress and sync pause state
- `[x]` player retry / recovery state

Acceptance criteria:
- Screen state can be reasoned about from a small number of immutable state objects.
- ViewModels become the source of truth for long-lived UI state.

## Phase 3: Break up the repository layer

- `[ ]` Split `XtreamApi` into a transport client and parsing helpers.
- `[ ]` Split `ContentRepository` into feature-specific repositories or services.
- `[ ]` Keep cache/index logic isolated from fetch/parsing logic.
- `[ ]` Move sync coordination into a dedicated manager.

Suggested boundaries:
- `[ ]` live content
- `[ ]` VOD content
- `[ ]` series/episode content
- `[ ]` search/indexing
- `[ ]` sync/cache maintenance

Acceptance criteria:
- No single repository owns networking, parsing, caching, and sync orchestration together.
- The code becomes easier to test in isolation.

## Phase 4: Tighten dependency injection

- `[ ]` Verify all repositories and controllers come from Hilt.
- `[ ]` Remove any remaining manual construction in composables.
- `[ ]` Make sure activities only host the UI and release lifecycle-bound resources.
- `[ ]` Prefer entry points only where Compose or lifecycle boundaries require them.

Acceptance criteria:
- No new direct instantiation of app singletons in UI code.
- `MainActivity` stays thin.

## Phase 5: Verify and stabilize

- `[ ]` Run `:app:compileDebugKotlin`
- `[ ]` Run `:app:testDebugUnitTest` if it is practical in the current branch state
- `[ ]` Smoke-test navigation, playback, update checks, and settings flows
- `[ ]` Check for regressions in startup and activity recreation

## Working rules for the next chat

- `[ ]` Make one behavior-preserving change at a time.
- `[ ]` Prefer extraction over logic rewrites.
- `[ ]` Keep each refactor shippable.
- `[ ]` If a change requires a bigger redesign, stop and split it.

## Suggested order

1. Extract dialog and update hosts.
2. Extract navigation shell and player host.
3. Move remaining long-lived state into view models.
4. Split repository/service responsibilities.
5. Re-run compile and tests after each batch.
