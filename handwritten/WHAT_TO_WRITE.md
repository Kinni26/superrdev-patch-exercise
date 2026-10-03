# What to handwrite (cue sheet — NOT the final notes)

Write these in YOUR OWN WORDS on paper (2–4 pages), photograph/scan them, and put the
images in this folder (`handwritten/01.jpg`, `02.jpg`, ...). The README asks for notes
that show you can explain the fixes without AI text, so use these only as prompts.
Consider deleting this file before submitting so only your own notes remain.

For each bug, cover the README's four points: WHERE it is, HOW you found it,
ROOT CAUSE, FIX and WHY.

## Page 1 — Overview + Bug 1 (most important)
- App: React/Vite UI -> Spring Boot `GET /api/tasks` -> H2 table `tasks`.
- Bug 1: search SQL, `TaskRepository.searchTasks` (also `db/queries/search_tasks.sql`, Oracle pkg).
- Found: read the query; AND binds tighter than OR; checked on seed data (`q=api` showed 2 archived rows; blank search + status=DONE returned every status).
- Root cause: `a AND b OR c AND d` = `(a AND b) OR (c AND d)`.
- Fix: `a AND (b OR c) AND d`. Why: archived + status must apply to both match columns.
- Same fix in the Oracle package, including the COUNT query.

## Page 2 — Bugs 2 and 3 (backend)
- Bug 2: `TaskController` `Thread.sleep(10 - queryLength) * 100ms`. Found by reading; matches the seed task "search slow when term is short or blank". Fix: delete it (it did no real work).
- Bug 3: `?status=foo` -> `valueOf` throws -> 500; `page=0` -> negative `subList` index; `pageSize=0`/huge. Fix: validate, return 400 with `{error}`; limit pageSize to 100; `long` offset. Why: client mistakes should be 4xx, not 5xx.

## Page 3 — Bugs 4 and 5 (frontend)
- Bug 4: `useTasks.js`. `setLoading(false)` only on success -> "Loading..." forever after an error; `error` never reset; old slow response could overwrite newer one (race). Found by reading + stopping the backend. Fix: `finally`, clear error on new request, `AbortController` cleanup.
- Bug 5: `App.jsx`. Page not reset when search/status changes -> empty page 3; a request on every keystroke. Fix: `setPage(1)` in handlers; `useDebouncedValue(300ms)`.
- Small: removed `console.log`, `System.out`; aria-labels.

## Page 4 — Testing, assumptions, not fixed
- Tests: frontend build; throwaway jsdom tests failed on the original code and passed on the fix; backend `TaskControllerTest` (say honestly what YOU ran: `./mvnw test`, and its result).
- Assumptions: archived tasks should never show; status values are OPEN/IN_PROGRESS/DONE; max page size 100 is my choice.
- Not fixed: in-memory pagination, `%`/`_` not escaped in LIKE, H2 console/CORS config.

## Be ready to explain in the interview
- Why `Thread.sleep` was removed rather than "optimised".
- Why 400 instead of 500, and why AbortController instead of an `ignore` flag.
- Why you did not rewrite pagination to be DB-side (scope/time, can't verify) and what you'd do next.
