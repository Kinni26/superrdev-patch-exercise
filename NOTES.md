# Notes

Detailed per-fix write-ups (what/how found/impact/change/why/verification), assumptions and the testing log are in [TECHNICAL_NOTES.md](TECHNICAL_NOTES.md). Handwritten explanations are in `handwritten/`.

## Summary of changes
- **Search SQL precedence:** `A AND B OR C AND D` let archived and wrong-status tasks through. Parenthesised the `OR` in `TaskRepository`, `db/queries/search_tasks.sql` and the Oracle package.
- **Removed artificial `Thread.sleep`** (up to 1s on short/blank searches).
- **API validation:** bad `status`, `page < 1`, `pageSize` outside 1–100 now return 400 `{error}` instead of 500.
- **`useTasks` hook:** stuck "Loading...", stale errors, and out-of-order responses fixed (`finally` + `AbortController`).
- **Search UX:** 300 ms debounce, page resets to 1 on search/filter change, `aria-label`s, debug logging removed.
- Added `TaskControllerTest` (MockMvc).

## Not changed
DB-side pagination, escaping `%`/`_` in LIKE, DTOs, CORS and H2-console config: larger changes I could not verify safely in the timebox.

## Biggest remaining risk
All matches are loaded and paged in memory, and `LIKE '%x%'` cannot use an index, so search will not scale. The H2 console is enabled without auth.

## Tools / AI
Claude (GenAI) for repo exploration, bug finding, drafting fixes and tests. I reviewed every change. The frontend was verified; the backend tests were written but NOT run by the AI (no Maven access); my own local result is in TECHNICAL_NOTES.md.
