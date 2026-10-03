package com.internal.tasktracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Search non-archived tasks by term (title or description) and optional status filter.
    // The OR is parenthesised so that archived and status apply to BOTH match branches.
    @Query(value = "SELECT * FROM tasks WHERE archived = FALSE "
                 + "AND (LOWER(title) LIKE :term OR LOWER(description) LIKE :term) "
                 + "AND (:status IS NULL OR status = :status) "
                 + "ORDER BY created_at DESC",
           nativeQuery = true)
    List<Task> searchTasks(@Param("term") String term, @Param("status") String status);
}
