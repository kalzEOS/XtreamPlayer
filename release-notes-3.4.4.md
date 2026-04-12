# Changes:
- Fixed search normalization so query language prefixes are handled without rewriting cached title text.
- Fixed Continue Watching write throttling to refresh persisted metadata changes while still avoiding noisy rewrites.
- Fixed local playback resume throttling so title/duration updates are saved even when position changes are small.
- Added regression tests for search normalization and resume/write guard behavior.
