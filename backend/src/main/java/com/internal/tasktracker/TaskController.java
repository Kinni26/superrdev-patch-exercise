package com.internal.tasktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Normalize query input
        String query = q == null ? "" : q.trim();
        String searchTerm = "%" + query.toLowerCase() + "%";

        // Parse status filter: an unknown value is a client error (400), not a server error (500)
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException e) {
                return badRequest("Invalid status '" + status + "'. Allowed values: "
                        + Arrays.toString(TaskStatus.values()));
            }
        }

        // Validate pagination so bad input cannot produce a negative offset or an unbounded page
        if (page < 1) {
            return badRequest("page must be >= 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return badRequest("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }

        log.debug("searchTasks q='{}' status={} page={} pageSize={}", query, normalizedStatus, page, pageSize);

        List<Task> allResults = taskRepository.searchTasks(searchTerm, normalizedStatus);

        // long arithmetic: (page - 1) * pageSize can overflow int for very large page values
        long start = (long) (page - 1) * pageSize;
        int total = allResults.size();
        List<Task> pageResults = (start < total)
                ? allResults.subList((int) start, (int) Math.min(start + pageSize, total))
                : Collections.emptyList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageResults);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
