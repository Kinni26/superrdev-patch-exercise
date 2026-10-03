# Technical Exercise Notes

## Summary
Task tracker: React 18/Vite UI -> Spring Boot `GET /api/tasks` (search, status filter, pagination) -> H2 `tasks` table, plus reference SQL in `db/`. The main problems were an operator-precedence bug in the search SQL, an artificial delay and missing input validation in the controller, and broken loading/error/race handling in the frontend hook.

## Fixes and Improvements

### Issue: Search SQL operator precedence (P0/P1)
**What was wrong:** `WHERE archived = FALSE AND LOWER(title) LIKE :term OR LOWER(description) LIKE :term AND (:status IS NULL OR status = :status)`. `AND` binds tighter than `OR`, so it parsed as `(archived=FALSE AND title LIKE) OR (description LIKE AND status-filter)`.
**How I found it:** Code inspection, then reproduced against `schema.sql`/`data.sql` in SQLite (a stand-in for H2) comparing old and fixed query.
**Impact:** Archived tasks appeared in results (`q=api` returned 10 rows incl. 2 archived; 8 after fix). The status filter was ignored whenever the title matched (blank search + `DONE` returned 49 rows of all statuses; 5 after fix). The Oracle package had the same bug, and its `COUNT(*)` made pagination totals wrong.
**What I changed:** Parenthesised `(title LIKE OR description LIKE)` in `TaskRepository`, `db/queries/search_tasks.sql`, and both queries in `db/oracle/task_search_package.sql`.
**Why I chose this approach:** Smallest change that fixes the root cause; archived and status must apply to every match.
**Verification:** SQLite reproduction above. `TaskControllerTest` (archived excluded, status filter honoured) is written but NOT run by me.

### Issue: Artificial `Thread.sleep` in the controller (P1)
**What was wrong:** `Thread.sleep((10 - query.length()) * 100)` delayed blank/short searches by up to 1s, labelled as "complexity estimation for logging".
**How I found it:** Code inspection; matches the seed task "search slow when term is short or blank".
**Impact:** Every keystroke on a short query paid up to a second of dead time.
**What I changed:** Removed it, and replaced `System.out.println` with an SLF4J `debug` log.
**Why:** It did no real work; optimising it would be pointless.
**Verification:** Not measured at runtime (backend not run).

### Issue: Missing request validation -> 500s (P1)
**What was wrong:** `TaskStatus.valueOf` on a bad `status` threw (500). `page=0` made the `subList` start negative; `pageSize=0` or huge values were accepted; `(page-1)*pageSize` could overflow `int`.
**How I found it:** Code inspection of the controller.
**Impact:** Client mistakes surfaced as server errors; unbounded page sizes.
**What I changed:** Return 400 with `{"error": "..."}` for unknown status, `page < 1`, `pageSize` outside 1-100; offset computed as `long`. Status parsing is case-insensitive and trimmed (as before).
**Why:** Bad input is a client error. The 100 cap is my assumption (see below).
**Verification:** Covered by `TaskControllerTest`; written, NOT run by me.

### Issue: `useTasks` loading/error/race bugs (P1)
**What was wrong:** `loading` was only cleared on success (stuck on "Loading..." after any error), `error` was never reset, and a slow older response could overwrite a newer one.
**How I found it:** Code inspection; confirmed with throwaway tests.
**Impact:** After one failure the UI never recovered; fast typing could display results for an old query.
**What I changed:** `setError(null)` per request, `finally` to clear loading, `AbortController` cleanup (ignoring `AbortError`), empty results on error. `api.js` accepts a `signal` and surfaces the API's `error` message.
**Why:** Aborting the old request is the idiomatic fix and also cancels network work.
**Verification:** 4 throwaway jsdom/Vitest tests (kept outside the repo): all failed on the original code and pass on the fix.

### Issue: Pagination not reset; request per keystroke (P1/P2)
**What was wrong:** Changing search or status kept the current page (could land on an empty page 3), and every keystroke fired a request.
**How I found it:** Code inspection of `App.jsx`.
**Impact:** Confusing empty results; unnecessary load.
**What I changed:** `setPage(1)` in the search/status handlers, a small `useDebouncedValue` hook (300 ms), a `PAGE_SIZE` constant.
**Why:** Minimal, no new dependency.
**Verification:** Same throwaway tests (one request for "api" typed quickly; status change requests `page=1`). Not checked in a real browser.

## Assumptions
- Archived tasks should never be shown in search.
- Valid statuses are exactly `OPEN`, `IN_PROGRESS`, `DONE`.
- A max `pageSize` of 100 is acceptable (my choice).
- The response shape `{items,total,page,pageSize}` must stay unchanged.

## Testing Summary
| Command / check | Result |
|---|---|
| `cd frontend && npm install && npm run build` | Passed |
| Throwaway Vitest suite (4 tests, outside repo) | Failed 4/4 on original code, passed 4/4 on fix |
| SQLite reproduction of the SQL bug on seed data | Confirmed (numbers above) |
| `npm audit --omit=dev` | 0 vulnerabilities (full `npm audit` lists 7 in dev tooling; not addressed) |
| `cd backend && ./mvnw test` / `spring-boot:run` | **NOT RUN by the AI** (Maven Central blocked in its sandbox). Fill in your own result: `[ ]` |
| Lint / type check | None configured in the project |

## Additional Improvements
- `TaskControllerTest` MockMvc regression tests plus `spring-boot-starter-test` (test scope).
- 300 ms debounce, `aria-label`s on the search box and status select.

## AI Usage
Used Claude (GenAI) for repository exploration, bug identification, implementation assistance, test ideas and code review. I reviewed the resulting changes; the frontend was tested as listed above, and the backend changes still need a local `./mvnw test` run.
