package com.example.doitnow.controller;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.dto.TaskStatsDTO;
import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Priority;
import com.example.doitnow.model.User;
import com.example.doitnow.service.AuthenticationService;
import com.example.doitnow.service.JwtService;
import com.example.doitnow.service.TaskService;
import com.example.doitnow.service.TaskStatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private TaskStatService taskStatService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private TaskDTO taskDTO1;
    private TaskDTO taskDTO2;

    @BeforeEach
    void setUp() {
        taskDTO1 = new TaskDTO();
        taskDTO1.setId("task-1");
        taskDTO1.setTitle("Première tâche");
        taskDTO1.setDescription("Description 1");
        taskDTO1.setCompleted(false);
        taskDTO1.setPriority(Priority.HIGH);
        taskDTO1.setTags(List.of("urgent", "travail"));
        taskDTO1.setDueDate(LocalDate.now().plusDays(3));

        taskDTO2 = new TaskDTO();
        taskDTO2.setId("task-2");
        taskDTO2.setTitle("Deuxième tâche");
        taskDTO2.setDescription("Description 2");
        taskDTO2.setCompleted(true);
        taskDTO2.setPriority(Priority.LOW);
        taskDTO2.setTags(List.of("perso"));
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasksTests {

        @Test
        @DisplayName("Doit retourner 200 OK avec une page de tâches")
        void shouldReturnPageOfTasks() throws Exception {
            Page<TaskDTO> page = new PageImpl<>(Arrays.asList(taskDTO1, taskDTO2));
            when(taskService.getAllTasks(anyInt(), anyInt(), anyString(), anyString())).thenReturn(page);

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].priority", is("HIGH")))
                    .andExpect(jsonPath("$.content[0].tags", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Doit retourner 200 OK avec une page vide")
        void shouldReturnEmptyPage() throws Exception {
            when(taskService.getAllTasks(anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Doit retourner 200 OK avec toutes les propriétés")
        void shouldReturnTaskById() throws Exception {
            when(taskService.getTaskById("task-1")).thenReturn(taskDTO1);

            mockMvc.perform(get("/api/tasks/{id}", "task-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is("task-1")))
                    .andExpect(jsonPath("$.priority", is("HIGH")))
                    .andExpect(jsonPath("$.tags[0]", is("urgent")));
        }

        @Test
        @DisplayName("Doit retourner 404 quand la tâche n'existe pas")
        void shouldReturn404() throws Exception {
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
        @DisplayName("Doit retourner 201 avec les nouvelles propriétés")
        void shouldCreateWithAllProperties() throws Exception {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Nouvelle tâche");
            createDTO.setDescription("Description");
            createDTO.setPriority(Priority.URGENT);
            createDTO.setTags(List.of("important"));
            createDTO.setDueDate(LocalDate.now().plusDays(7));

            TaskDTO createdDTO = new TaskDTO();
            createdDTO.setId("new-id");
            createdDTO.setTitle("Nouvelle tâche");
            createdDTO.setPriority(Priority.URGENT);
            createdDTO.setTags(List.of("important"));
            createdDTO.setCompleted(false);

            when(taskService.createTask(any(CreateTaskDTO.class))).thenReturn(createdDTO);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is("new-id")))
                    .andExpect(jsonPath("$.priority", is("URGENT")))
                    .andExpect(jsonPath("$.tags[0]", is("important")));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le titre est vide")
        void shouldReturn400WhenTitleEmpty() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\", \"description\":\"desc\"}"))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }

        @Test
        @DisplayName("Doit retourner 400 quand le titre est trop court")
        void shouldReturn400WhenTitleTooShort() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"ab\"}"))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTaskTests {

        @Test
        @DisplayName("Doit retourner 200 avec les propriétés mises à jour")
        void shouldUpdateAndReturn200() throws Exception {
            TaskDTO updateDTO = new TaskDTO();
            updateDTO.setTitle("Titre modifié");
            updateDTO.setDescription("Description modifiée");
            updateDTO.setCompleted(true);
            updateDTO.setPriority(Priority.URGENT);
            updateDTO.setTags(List.of("modifié"));

            TaskDTO updatedDTO = new TaskDTO();
            updatedDTO.setId("task-1");
            updatedDTO.setTitle("Titre modifié");
            updatedDTO.setCompleted(true);
            updatedDTO.setPriority(Priority.URGENT);
            updatedDTO.setTags(List.of("modifié"));

            when(taskService.updateTask(eq("task-1"), any(TaskDTO.class))).thenReturn(updatedDTO);

            mockMvc.perform(put("/api/tasks/{id}", "task-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Titre modifié")))
                    .andExpect(jsonPath("$.priority", is("URGENT")))
                    .andExpect(jsonPath("$.tags[0]", is("modifié")));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le titre est vide")
        void shouldReturn400WhenTitleEmpty() throws Exception {
            mockMvc.perform(put("/api/tasks/{id}", "task-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\", \"completed\":false}"))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).updateTask(anyString(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTaskTests {

        @Test
        @DisplayName("Doit retourner 204 No Content")
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", "task-1"))
                    .andExpect(status().isNoContent());
            verify(taskService).deleteTask("task-1");
        }

        @Test
        @DisplayName("Doit retourner 404 quand la tâche n'existe pas")
        void shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("Tâche non trouvée"))
                    .when(taskService).deleteTask("non-existent");

            mockMvc.perform(delete("/api/tasks/{id}", "non-existent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/overdue")
    class OverdueTasksTests {

        @Test
        @DisplayName("Doit retourner les tâches en retard")
        void shouldReturnOverdueTasks() throws Exception {
            when(taskService.getOverdueTasks()).thenReturn(List.of(taskDTO2));

            mockMvc.perform(get("/api/tasks/overdue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is("task-2")));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/search")
    class SearchTasksTests {

        @Test
        @DisplayName("Doit rechercher par mot-clé avec pagination")
        void shouldSearchWithPagination() throws Exception {
            Page<TaskDTO> page = new PageImpl<>(List.of(taskDTO1));
            when(taskService.searchTasks(eq("première"), anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/tasks/search")
                            .param("keyword", "première"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title", is("Première tâche")));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/tag/{tag}")
    class TasksByTagTests {

        @Test
        @DisplayName("Doit retourner les tâches par tag")
        void shouldReturnTasksByTag() throws Exception {
            when(taskService.getTasksByTag("urgent")).thenReturn(List.of(taskDTO1));

            mockMvc.perform(get("/api/tasks/tag/{tag}", "urgent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].tags[0]", is("urgent")));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/priority/{priority}")
    class TasksByPriorityTests {

        @Test
        @DisplayName("Doit retourner les tâches par priorité")
        void shouldReturnTasksByPriority() throws Exception {
            when(taskService.getTasksByPriority(Priority.HIGH)).thenReturn(List.of(taskDTO1));

            mockMvc.perform(get("/api/tasks/priority/{priority}", "HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].priority", is("HIGH")));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/stats")
    class GetStatsTests {

        private void setUpSecurityContext() {
            User mockUser = new User();
            mockUser.setId("user-1");
            mockUser.setEmail("test@example.com");
            var auth = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Doit retourner 200 avec les statistiques complètes")
        void shouldReturnStats() throws Exception {
            setUpSecurityContext();

            TaskStatsDTO stats = new TaskStatsDTO();
            stats.setTotal(10);
            stats.setCompleted(7);
            stats.setPending(3);
            stats.setOverdue(1);
            stats.setCompletionRate(70.0);
            stats.setTasksByPriority(Map.of(
                    "HIGH", 4L, "MEDIUM", 3L, "LOW", 3L
            ));

            when(taskStatService.getStatsForUser("user-1")).thenReturn(stats);

            mockMvc.perform(get("/api/tasks/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total", is(10)))
                    .andExpect(jsonPath("$.completed", is(7)))
                    .andExpect(jsonPath("$.pending", is(3)))
                    .andExpect(jsonPath("$.overdue", is(1)))
                    .andExpect(jsonPath("$.completionRate", is(70.0)))
                    .andExpect(jsonPath("$.tasksByPriority.HIGH", is(4)))
                    .andExpect(jsonPath("$.tasksByPriority.MEDIUM", is(3)))
                    .andExpect(jsonPath("$.tasksByPriority.LOW", is(3)));
        }

        @Test
        @DisplayName("Doit retourner des statistiques vides quand aucune tâche")
        void shouldReturnEmptyStats() throws Exception {
            setUpSecurityContext();

            TaskStatsDTO stats = new TaskStatsDTO();
            stats.setTotal(0);
            stats.setCompleted(0);
            stats.setPending(0);
            stats.setOverdue(0);
            stats.setCompletionRate(0);
            stats.setTasksByPriority(Map.of());

            when(taskStatService.getStatsForUser("user-1")).thenReturn(stats);

            mockMvc.perform(get("/api/tasks/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total", is(0)))
                    .andExpect(jsonPath("$.completionRate", is(0.0)))
                    .andExpect(jsonPath("$.tasksByPriority").isEmpty());
        }

        @Test
        @DisplayName("Doit retourner les stats avec toutes les priorités")
        void shouldReturnStatsWithAllPriorities() throws Exception {
            setUpSecurityContext();

            TaskStatsDTO stats = new TaskStatsDTO();
            stats.setTotal(10);
            stats.setCompleted(5);
            stats.setPending(5);
            stats.setOverdue(2);
            stats.setCompletionRate(50.0);
            stats.setTasksByPriority(Map.of(
                    "URGENT", 1L, "HIGH", 3L, "MEDIUM", 4L, "LOW", 2L
            ));

            when(taskStatService.getStatsForUser("user-1")).thenReturn(stats);

            mockMvc.perform(get("/api/tasks/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasksByPriority.URGENT", is(1)))
                    .andExpect(jsonPath("$.tasksByPriority.HIGH", is(3)))
                    .andExpect(jsonPath("$.tasksByPriority.MEDIUM", is(4)))
                    .andExpect(jsonPath("$.tasksByPriority.LOW", is(2)));
        }
    }
}
