# #721 — Delete-during-migration doesn't keep up with migrator

Prototype on branch `feature-721-migration-pause-prototype` (off
`feature-migaudel`, which owns the threaded-deletion/`fastDelTree` half of
this issue). Status: implemented, compiled, unit-tested; not integration-
tested against a real migration run. For evaluation, not a finished patch.

## The problem, as originally stated

On delta1, same-host migration with deletion, the delete thread falls
behind the migrator: the number of AU dirs in `MIGRATED` slowly increases,
and free space decreases. The issue raised three options (thread pool for
deletion, `rm -rf`/`fastDelTree` instead of the old Java `delTree`, or
both) and one open question: even if those help, the migrator may still
need to watch free space and/or the undeleted-AU backlog and pause when
necessary, since deletion may simply be unable to keep up in all
environments (e.g. network storage).

Thib's response agreed a pausing backstop is needed regardless, but did
not propose a specific mechanism ("not very inspired by what you've
floated so far"). He also asked what happens to deletion state across a
migration interruption or daemon restart; Tom answered that this is a
non-issue by construction — deleted AUs move to `MIGRATED`, and the
deletion process deletes whatever it finds there. Verified against the
actual code: `RepositoryManager.claimNextDir()` discovers work by listing
the `MIGRATED` directory fresh every call rather than tracking a queue in
memory, so a restart just re-lists it. Nothing about restart safety needed
fixing here.

## What existed before this prototype (verified by reading the code, not
## assumed)

**Deletion side** (`RepositoryManager`, added by `feature-migaudel`): a
thread pool of `DeleteAusThread`s, `claimNextDir()`/`findUnclaimedDir()`
picking un-deleted AU dirs from the fullest disk first, `fastDelTree` for
the actual removal. Already public and reusable: `getRepositoryList()`,
`getRepositoryDF(repoSpec)`, `getDiskWarnThreshold()`/
`getDiskFullThreshold()`, `pokeDeleteAusThreads()` (was package-private,
made public by this prototype — see below).

**Migration side** (`V2AuMover`): holds a `repoMgr` field but never called
into it (zero `repoMgr.` call sites before this prototype). Its own
`fetchDiskSpace()`/`diskSpaceXferBytesCurve` mechanism looks like a
destination-side throttle at a glance; tracing where `diskFreeContent` and
`currentDiskSpace` are actually read shows they only adapt how often to
re-poll the V2 target's free space and populate a status-display string —
never gate the copy loop. The one place in the whole daemon that checks a
disk-full threshold against real data (`LockssRepositoryStatus`, for the
admin UI) is likewise display-only. **Nothing in the codebase throttled
anything based on disk space before this prototype.**

## Design questions and decisions

Posed to Daniel and Tom for evaluation; answers below are what this
prototype implements.

**1. Reuse `getDiskFullThreshold()` (100MB / 1% default) for the new gate,
or add a separate threshold?**
→ **Separate threshold.** `getDiskFullThreshold()`'s default (100MB) was
sized for a status-table red/yellow indicator, not for gating multi-GB AU
copies; reusing it risked either tripping too late (an AU write finishes
after the disk was already effectively full) or coupling two independent
tuning knobs by accident. Added
`RepositoryManager.getAuMoverPauseThreshold()`, backed by new
`org.lockss.repository.RepositoryManager.diskSpace.aumoverPause.freeMB`
(default 20000MB) and `...aumoverPause.freePercent` (default 5%) params.
**The 20GB/5% default is a placeholder** — it isn't derived from any real
AU-size data from this daemon's actual AUs, and should be tuned before
this goes beyond prototype.

**2. Precise per-AU space reservation (the issue's original idea — CuMover
knows the AU's size and reserves exactly that much) vs. a simpler static
threshold gate?**
→ **Simple threshold gate for now.** Precise reservation needs a bytes-
based semaphore/pool abstraction and reliable AU size up front (not always
available before a copy starts), a materially bigger lift for a benefit
that's currently speculative. Revisit only if the threshold gate proves
too coarse in practice.

**3. What happens when a repository stays full past all retries — block
forever, or skip silently?**
→ **Neither.** Roll back the ambiguous half-migrated state (in this
design, "rollback" means: give up *before* any content-copy task is
enqueued for the AU, so there is nothing partial to undo), skip the AU,
and write to the migration error log so it's retried on a later run. See
"Implementation" below for exactly how, including a pre-existing gap this
surfaced.

## Implementation

**New/changed in `RepositoryManager`:**
- `PARAM_AUMOVER_PAUSE_FREE_MB` / `PARAM_AUMOVER_PAUSE_FREE_PERCENT` +
  defaults, `paramAuMoverPause` field, wired into the existing
  `DISK_PREFIX`-triggered config-reload block alongside `paramDFWarn`/
  `paramDFFull`.
- `getAuMoverPauseThreshold()` — public getter, same shape as
  `getDiskFullThreshold()`.
- `pokeDeleteAusThreads()` — was package-private; made `public` so
  `V2AuMover` can nudge deletion (harmless if deletion is already busy)
  instead of passively waiting out a retry interval.

**New in `V2AuMover`:**
- `checkDiskSpaceOrAbort(ArchivalUnit au)` — resolves the AU's V1
  repository via `LockssRepositoryImpl.getRepositorySpec(au)` (note: the
  *spec*, e.g. `"local:/path"`, which is what `getRepositoryDF` expects as
  its key — not `getRepositoryRoot(au)`, which returns the already-
  resolved filesystem path and would silently look up the wrong thing).
  Polls `repoMgr.getRepositoryDF(repoSpec)` against
  `getAuMoverPauseThreshold()` up to `diskPauseMaxAttempts` times
  (`PARAM_DISK_PAUSE_MAX_ATTEMPTS`, default 10), `diskPauseRetryInterval`
  apart (`PARAM_DISK_PAUSE_RETRY_INTERVAL`, default 1 minute — so ~10
  minutes worst case before giving up on one AU), calling
  `repoMgr.pokeDeleteAusThreads()` between attempts.
- Called from the top of `enqueueCopyAuContent(AuStatus auStat)` —
  **before** the loop that enqueues any per-CU copy task onto
  `copyExecutor`. This placement matters: `enqueueCopyAuContent` runs on a
  `copyIterExecutor` thread (a small pool whose only job is to kick off
  enqueueing for one AU at a time), not on the single top-level scheduler
  thread (`moveQueuedAus`) that dispatches AUs across *all* plugins.
  Gating here means a full repository stalls only the AUs actually headed
  for it; AUs for other, non-full repositories keep moving. An earlier
  version of this design considered gating inside `moveAu()` or the
  scheduling loop itself — traced through the call graph and rejected,
  since either would stall scheduling for every plugin the moment *any*
  one repository filled up.
- On giving up: `giveUpOnDiskSpace()` calls the existing private
  `addAuError(msg)` directly, then throws `InsufficientDiskSpaceException`
  (a small nested `RuntimeException`). The exception is caught by the
  **existing, unmodified** generic phase-entry handler in `enterPhase()`,
  which calls `auStat.abortAu()` and advances to `Phase.FINISH` — the same
  path already used for e.g. an `OutOfMemoryError` during phase entry. No
  new abort/rollback machinery was added; this reuses what's there.

**A pre-existing gap this surfaced, not fixed here:** `FinishAu`'s
`auStat.isAbort()` branch sets `MigrationState.Aborted` but does **not**
call `addAuError()` — only the successful-finish branch does. So *any*
existing abort path (this one included, and every other exception-
triggered abort already in the codebase) does not, by itself, write to the
aggregate migration error log; only the per-AU `auStat`'s own error list
gets it. That's why `giveUpOnDiskSpace()` calls `addAuError()` explicitly
rather than relying on `FinishAu` to do it — otherwise requirement 3 above
would not actually be met. Whether the general gap (every other abort
reason) should also be fixed is a separate decision, out of scope for this
prototype.

## What "rollback" means here, and its limit

Because the gate runs *before* any CU copy task is enqueued, there is no
partial V2 content to undo in the common case. One thing this prototype
does **not** handle: if `useBulkMode(au)` already called `startBulk()` for
this AU (in `moveAu()`, which runs before `enqueueCopyAuContent()`), that
bulk-mode session in the V2 repository is not explicitly closed or aborted
when this gate fires. Whether that's already handled by the existing
`Phase.ABORT` transition is unclear — `Phase.ABORT` has no entry in
`pdMap`, so entering it runs no explicit action at all (verified by
reading `initPhaseMap()` and `doEnterPhase()`). This is the same situation
as any other abort, not something newly introduced, but worth confirming
before this goes past prototype: does an aborted bulk-mode AU get cleanly
retried on the next migration attempt, or does it need explicit cleanup
first?

## Testing

- `TestRepositoryManager`: 19/19 (unchanged from before this prototype).
- `TestFileUtil`: 29/29 (unchanged).
- `TestV2AuMover`: 4/4 (2 pre-existing + 2 new:
  `testCheckDiskSpaceOrAbortProceedsWhenSpaceOk`,
  `testCheckDiskSpaceOrAbortThrowsAfterMaxAttempts`), using a manual
  `RepositoryManager` subclass overriding `getRepositoryDF`/
  `getAuMoverPauseThreshold`/`pokeDeleteAusThreads` rather than a mocking
  framework (none was already in use in this test class).
- Negative control: temporarily forced `checkDiskSpaceOrAbort` to always
  return immediately; `testCheckDiskSpaceOrAbortThrowsAfterMaxAttempts`
  failed as expected, then reverted.
- **Not tested**: an actual migration run against a full or nearly-full V1
  repository. Everything above is a unit-level check of the gate logic in
  isolation; whether it behaves correctly wired into a live
  `V2AuMover`/`RepositoryManager` pair, under real disk pressure, with real
  AU sizes, is unverified.

## Open items before this is more than a prototype

1. Tune the 20GB/5% default against real AU sizes on delta1 or wherever
   this would first run.
2. Confirm whether an aborted bulk-mode AU needs explicit V2-side cleanup
   (see "What rollback means here, and its limit" above).
3. Decide whether to fix the general `FinishAu`/`addAuError` gap for all
   abort reasons, or leave it local to this one path as implemented.
4. Integration-test against a real or simulated full-disk scenario.
