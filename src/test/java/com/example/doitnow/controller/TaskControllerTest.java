package com.example.doitnow.controller;

import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Task;
import com.example.doitnow.service.AuthenticationService;
import com.example.doitnow.service.JwtService;
import com.example.doitnow.service.TaskService;
import org.springframework.security.core.userdetails.UserDetailsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Tests d'intégration pour TaskController")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        task1 = new Task();
        task1.setId("task-1");
        task1.setTitle("Première tâche");
        task1.setDescription("Description 1");
        task1.setCompleted(false);

        task2 = new Task();
        task2.setId("task-2");
        task2.setTitle("Deuxième tâche");
        task2.setDescription("Description 2");
        task2.setCompleted(true);
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasksTests {

        @Test
        @DisplayName("Doit retourner 200 OK avec la liste des tâches")
        void shouldReturnAllTasks() throws Exception {
            List<Task> tasks = Arrays.asList(task1, task2);
            when(taskService.getAllTasks()).thenReturn(tasks);

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is("task-1")))
                    .andExpect(jsonPath("$[0].title", is("Première tâche")))
                    .andExpect(jsonPath("$[0].completed", is(false)))
                    .andExpect(jsonPath("$[1].id", is("task-2")))
                    .andExpect(jsonPath("$[1].title", is("Deuxième tâche")))
                    .andExpect(jsonPath("$[1].completed", is(true)));
        }

        @Test
        @DisplayName("Doit retourner 200 OK avec une liste vide")
        void shouldReturnEmptyList() throws Exception {
            when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Doit retourner 200 OK avec la tâche demandée")
        void shouldReturnTaskById() throws Exception {
            when(taskService.getTaskById("task-1")).thenReturn(task1);

            mockMvc.perform(get("/api/tasks/{id}", "task-1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is("task-1")))
                    .andExpect(jsonPath("$.title", is("Première tâche")))
                    .andExpect(jsonPath("$.description", is("Description 1")))
                    .andExpect(jsonPath("$.completed", is(false)));
        }

        @Test
        @DisplayName("Doit retourner 404 Not Found quand la tâche n'existe pas")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            when(taskService.getTaskById("non-existent"))
                    .thenThrow(new ResourceNotFoundException("Tâche non trouvée"));

            mockMvc.perform(get("/api/tasks/{id}", "non-existent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTaskTests {

        @Test
        @DisplayName("Doit retourner 201 Created avec la tâche créée")
        void shouldCreateTaskAndReturn201() throws Exception {
            Task newTask = new Task();
            newTask.setTitle("Nouvelle tâche");
            newTask.setDescription("Description");

            Task createdTask = new Task();
            createdTask.setId("new-id");
            createdTask.setTitle("Nouvelle tâche");
            createdTask.setDescription("Description");
            createdTask.setCompleted(false);

            when(taskService.createTask(any(Task.class))).thenReturn(createdTask);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newTask)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is("new-id")))
                    .andExpect(jsonPath("$.title", is("Nouvelle tâche")))
                    .andExpect(jsonPath("$.completed", is(false)));

            verify(taskService, times(1)).createTask(any(Task.class));
        }

    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTaskTests {

        @Test
        @DisplayName("Doit retourner 204 No Content quand la tâche est supprimée")
        void shouldReturn204WhenTaskDeleted() throws Exception {
            doNothing().when(taskService).deleteTask("task-1");

            mockMvc.perform(delete("/api/tasks/{id}", "task-1"))
                    .andExpect(status().isNoContent());

            verify(taskService, times(1)).deleteTask("task-1");
        }

        @Test
        @DisplayName("Doit retourner 404 Not Found quand la tâche n'existe pas")
        void shouldReturn404WhenDeletingNonExistentTask() throws Exception {
            doThrow(new ResourceNotFoundException("Tâche non trouvée"))
                    .when(taskService).deleteTask("non-existent");

            mockMvc.perform(delete("/api/tasks/{id}", "non-existent"))
                    .andExpect(status().isNotFound());
        }
    }
}
