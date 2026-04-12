package com.example.doitnow.service;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.dto.TaskNotification;
import com.example.doitnow.model.Priority;
import com.example.doitnow.model.Task;
import com.example.doitnow.model.User;
import com.example.doitnow.repository.TaskRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        User mockUser = new User();
        mockUser.setId("user-1");
        mockUser.setEmail("test@example.com");
        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        task1 = new Task();
        task1.setId("task-1");
        task1.setTitle("Première tâche");
        task1.setDescription("Description de la première tâche");
        task1.setCompleted(false);
        task1.setUserId("user-1");
        task1.setPriority(Priority.HIGH);
        task1.setTags(List.of("urgent", "travail"));
        task1.setDueDate(LocalDate.now().plusDays(3));

        task2 = new Task();
        task2.setId("task-2");
        task2.setTitle("Deuxième tâche");
        task2.setDescription("Description de la deuxième tâche");
        task2.setCompleted(true);
        task2.setUserId("user-1");
        task2.setPriority(Priority.LOW);
        task2.setTags(List.of("perso"));
        task2.setDueDate(LocalDate.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Tests de createTask()")
    class CreateTaskTests {

        @Test
        @DisplayName("Doit créer une tâche avec toutes les propriétés")
        void shouldCreateTaskWithAllProperties() {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Nouvelle tâche");
            createDTO.setDescription("Description");
            createDTO.setPriority(Priority.URGENT);
            createDTO.setTags(List.of("important"));
            createDTO.setDueDate(LocalDate.now().plusDays(7));

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId("new-id");
                return t;
            });

            TaskDTO result = taskService.createTask(createDTO);

            assertNotNull(result);
            assertEquals("new-id", result.getId());
            assertEquals(Priority.URGENT, result.getPriority());
            assertEquals(List.of("important"), result.getTags());
            assertFalse(result.isCompleted());

            verify(taskRepository).save(taskCaptor.capture());
            assertEquals("user-1", taskCaptor.getValue().getUserId());
        }

        @Test
        @DisplayName("Doit utiliser MEDIUM par défaut si priority est null")
        void shouldDefaultToMediumPriority() {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Tâche sans priorité");

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.createTask(createDTO);

            verify(taskRepository).save(taskCaptor.capture());
            assertEquals(Priority.MEDIUM, taskCaptor.getValue().getPriority());
        }
    }

    @Nested
    @DisplayName("Tests de getAllTasks()")
    class GetAllTasksTests {

        @Test
        @DisplayName("Doit retourner une page de TaskDTO avec toutes les propriétés")
        void shouldReturnPageOfTasks() {
            Page<Task> taskPage = new PageImpl<>(Arrays.asList(task1, task2));
            when(taskRepository.findByUserId(anyString(), any(Pageable.class))).thenReturn(taskPage);

            Page<TaskDTO> results = taskService.getAllTasks(0, 10, "title", "asc");

            assertEquals(2, results.getTotalElements());
            assertEquals(Priority.HIGH, results.getContent().get(0).getPriority());
            assertEquals(List.of("urgent", "travail"), results.getContent().get(0).getTags());
        }

        @Test
        @DisplayName("Doit retourner une page vide")
        void shouldReturnEmptyPage() {
            when(taskRepository.findByUserId(anyString(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<TaskDTO> results = taskService.getAllTasks(0, 10, "title", "asc");

            assertTrue(results.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("Tests de getTaskById()")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Doit retourner un TaskDTO avec toutes les propriétés")
        void shouldReturnTaskDTO() {
            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));

            TaskDTO result = taskService.getTaskById("task-1");

            assertEquals("task-1", result.getId());
            assertEquals(Priority.HIGH, result.getPriority());
            assertEquals(List.of("urgent", "travail"), result.getTags());
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException")
        void shouldThrowWhenNotFound() {
            when(taskRepository.findByIdAndUserId("non-existent", "user-1")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.getTaskById("non-existent"));
        }
    }

    @Nested
    @DisplayName("Tests de updateTask()")
    class UpdateTaskTests {

        @Test
        @DisplayName("Doit mettre à jour toutes les propriétés")
        void shouldUpdateAllProperties() {
            TaskDTO updateDTO = new TaskDTO();
            updateDTO.setTitle("Titre modifié");
            updateDTO.setDescription("Description modifiée");
            updateDTO.setCompleted(true);
            updateDTO.setPriority(Priority.URGENT);
            updateDTO.setTags(List.of("modifié"));
            updateDTO.setDueDate(LocalDate.now().plusDays(10));

            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO result = taskService.updateTask("task-1", updateDTO);

            assertEquals("Titre modifié", result.getTitle());
            assertTrue(result.isCompleted());
            assertEquals(Priority.URGENT, result.getPriority());
            assertEquals(List.of("modifié"), result.getTags());
        }

        @Test
        @DisplayName("Doit conserver la priorité existante si null dans le DTO")
        void shouldKeepExistingPriorityWhenNull() {
            TaskDTO updateDTO = new TaskDTO();
            updateDTO.setTitle("Titre modifié");
            updateDTO.setCompleted(false);

            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO result = taskService.updateTask("task-1", updateDTO);

            assertEquals(Priority.HIGH, result.getPriority());
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la tâche n'existe pas")
        void shouldThrowWhenNotFound() {
            TaskDTO updateDTO = new TaskDTO();
            updateDTO.setTitle("Titre");
            updateDTO.setCompleted(false);

            when(taskRepository.findByIdAndUserId("non-existent", "user-1")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.updateTask("non-existent", updateDTO));
        }
    }

    @Nested
    @DisplayName("Tests de deleteTask()")
    class DeleteTaskTests {

        @Test
        @DisplayName("Doit supprimer la tâche quand elle existe")
        void shouldDeleteWhenExists() {
            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));

            taskService.deleteTask("task-1");

            verify(taskRepository).deleteById("task-1");
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la tâche n'existe pas")
        void shouldThrowWhenNotFound() {
            when(taskRepository.findByIdAndUserId("non-existent", "user-1")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.deleteTask("non-existent"));
            verify(taskRepository, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("Tests de getOverdueTasks()")
    class GetOverdueTasksTests {

        @Test
        @DisplayName("Doit retourner les tâches en retard")
        void shouldReturnOverdueTasks() {
            when(taskRepository.findOverdueTasks(eq("user-1"), any(LocalDate.class)))
                    .thenReturn(List.of(task2));

            List<TaskDTO> results = taskService.getOverdueTasks();

            assertEquals(1, results.size());
            assertEquals("task-2", results.get(0).getId());
        }

        @Test
        @DisplayName("Doit retourner une liste vide si aucune tâche en retard")
        void shouldReturnEmptyWhenNoOverdue() {
            when(taskRepository.findOverdueTasks(eq("user-1"), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<TaskDTO> results = taskService.getOverdueTasks();

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Tests de searchTasks()")
    class SearchTasksTests {

        @Test
        @DisplayName("Doit rechercher dans titre et description")
        void shouldSearchTasks() {
            when(taskRepository.searchTasks("user-1", "tâche")).thenReturn(List.of(task1, task2));

            List<TaskDTO> results = taskService.searchTasks("tâche");

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Doit retourner une page paginée de résultats")
        void shouldReturnPagedResults() {
            Page<Task> page = new PageImpl<>(List.of(task1));
            when(taskRepository.searchTasks(eq("user-1"), eq("première"), any(Pageable.class)))
                    .thenReturn(page);

            Page<TaskDTO> results = taskService.searchTasks("première", 0, 10, "title", "asc");

            assertEquals(1, results.getTotalElements());
            assertEquals("Première tâche", results.getContent().get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("Tests de getTasksByTag()")
    class GetTasksByTagTests {

        @Test
        @DisplayName("Doit retourner les tâches avec le tag donné")
        void shouldReturnTasksByTag() {
            when(taskRepository.findByUserIdAndTagsContaining("user-1", "urgent"))
                    .thenReturn(List.of(task1));

            List<TaskDTO> results = taskService.getTasksByTag("urgent");

            assertEquals(1, results.size());
            assertEquals("task-1", results.get(0).getId());
            assertTrue(results.get(0).getTags().contains("urgent"));
        }

        @Test
        @DisplayName("Doit retourner une liste vide si aucune tâche avec ce tag")
        void shouldReturnEmptyWhenNoTag() {
            when(taskRepository.findByUserIdAndTagsContaining("user-1", "inexistant"))
                    .thenReturn(List.of());

            assertTrue(taskService.getTasksByTag("inexistant").isEmpty());
        }
    }

    @Nested
    @DisplayName("Tests de getTasksByPriority()")
    class GetTasksByPriorityTests {

        @Test
        @DisplayName("Doit retourner les tâches avec la priorité donnée")
        void shouldReturnTasksByPriority() {
            when(taskRepository.findByUserIdAndPriority("user-1", Priority.HIGH))
                    .thenReturn(List.of(task1));

            List<TaskDTO> results = taskService.getTasksByPriority(Priority.HIGH);

            assertEquals(1, results.size());
            assertEquals(Priority.HIGH, results.get(0).getPriority());
        }

        @Test
        @DisplayName("Doit retourner une liste vide si aucune tâche avec cette priorité")
        void shouldReturnEmptyWhenNoPriority() {
            when(taskRepository.findByUserIdAndPriority("user-1", Priority.URGENT))
                    .thenReturn(List.of());

            assertTrue(taskService.getTasksByPriority(Priority.URGENT).isEmpty());
        }
    }

    @Nested
    @DisplayName("Tests des notifications WebSocket")
    class WebSocketNotificationTests {

        @Captor
        private ArgumentCaptor<TaskNotification> notificationCaptor;

        @Test
        @DisplayName("Doit envoyer une notification CREATED avec le bon contenu")
        void shouldSendCreatedNotificationWithCorrectPayload() {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Tâche notifiée");
            createDTO.setPriority(Priority.HIGH);
            createDTO.setTags(List.of("urgent"));

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId("notif-id");
                return t;
            });

            taskService.createTask(createDTO);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/tasks/user-1"),
                    notificationCaptor.capture());

            TaskNotification notification = notificationCaptor.getValue();
            assertEquals("CREATED", notification.type());
            assertEquals("notif-id", notification.taskId());
            assertNotNull(notification.task());
            assertEquals("Tâche notifiée", notification.task().getTitle());
            assertEquals(Priority.HIGH, notification.task().getPriority());
        }

        @Test
        @DisplayName("Doit envoyer une notification avec le bon contenu lors de la mise à jour")
        void shouldSendUpdateNotificationWithCorrectPayload() {
            TaskDTO updateDTO = new TaskDTO();
            updateDTO.setTitle("Titre modifié");
            updateDTO.setCompleted(true);
            updateDTO.setPriority(Priority.URGENT);

            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.updateTask("task-1", updateDTO);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/tasks/user-1"),
                    notificationCaptor.capture());

            TaskNotification notification = notificationCaptor.getValue();
            assertEquals("task-1", notification.taskId());
            assertNotNull(notification.task());
            assertEquals("Titre modifié", notification.task().getTitle());
            assertTrue(notification.task().isCompleted());
            assertEquals(Priority.URGENT, notification.task().getPriority());
        }

        @Test
        @DisplayName("Doit envoyer une notification DELETED sans tâche")
        void shouldSendDeletedNotificationWithNullTask() {
            when(taskRepository.findByIdAndUserId("task-1", "user-1")).thenReturn(Optional.of(task1));

            taskService.deleteTask("task-1");

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/tasks/user-1"),
                    notificationCaptor.capture());

            TaskNotification notification = notificationCaptor.getValue();
            assertEquals("DELETED", notification.type());
            assertEquals("task-1", notification.taskId());
            assertNull(notification.task());
        }

        @Test
        @DisplayName("Ne doit pas envoyer de notification si la tâche n'existe pas")
        void shouldNotNotifyWhenTaskNotFound() {
            when(taskRepository.findByIdAndUserId("non-existent", "user-1")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.deleteTask("non-existent"));

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(TaskNotification.class));
        }

        @Test
        @DisplayName("Doit envoyer la notification au bon topic utilisateur")
        void shouldSendToCorrectUserTopic() {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Tâche test");

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId("id-1");
                return t;
            });

            taskService.createTask(createDTO);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(messagingTemplate).convertAndSend(
                    topicCaptor.capture(),
                    any(TaskNotification.class));

            assertEquals("/topic/tasks/user-1", topicCaptor.getValue());
        }

        @Test
        @DisplayName("Doit envoyer exactement une notification par opération CRUD")
        void shouldSendExactlyOneNotificationPerOperation() {
            CreateTaskDTO createDTO = new CreateTaskDTO();
            createDTO.setTitle("Tâche test");

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId("id-1");
                return t;
            });

            taskService.createTask(createDTO);

            verify(messagingTemplate, times(1))
                    .convertAndSend(anyString(), any(TaskNotification.class));
        }
    }
}
