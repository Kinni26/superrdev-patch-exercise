package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchNeverReturnsArchivedTasks() throws Exception {
        // Seed data has archived tasks whose description contains "api".
        mockMvc.perform(get("/api/tasks").param("q", "api").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.items[?(@.archived == true)]").isEmpty());
    }

    @Test
    void statusFilterAppliesToEveryMatch() throws Exception {
        // Blank search matches every title; the status filter must still restrict the result.
        mockMvc.perform(get("/api/tasks").param("status", "DONE").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").exists())
                .andExpect(jsonPath("$.items[?(@.status != 'DONE')]").isEmpty());
    }

    @Test
    void statusFilterIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "open").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.status != 'OPEN')]").isEmpty());
    }

    @Test
    void unknownStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void invalidPageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidPageSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks").param("pageSize", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void pageBeyondLastReturnsEmptyItemsNotAnError() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(9999));
    }

    @Test
    void emptySearchTermReturnsFirstPageOfTen() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.pageSize").value(10));
    }
}
