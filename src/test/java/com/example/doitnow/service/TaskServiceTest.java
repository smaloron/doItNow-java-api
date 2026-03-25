package com.example.doitnow.service;

import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Task;
import com.example.doitnow.repository.TaskRepository;
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

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        task1 = new Task();
        task1.setId("task-1");
        task1.setTitle("Première tâche");
        task1.setDescription("Description de la première tâche");
        task1.setCompleted(false);

        task2 = new Task();
        task2.setId("task-2");
        task2.setTitle("Deuxième tâche");
        task2.setDescription("Description de la deuxième tâche");
        task2.setCompleted(true);
    }

    @Nested
    @DisplayName("Tests de createTask()")
    class CreateTaskTests {

        @Test
        @DisplayName("Doit créer une tâche avec succès")
        void shouldCreateTaskSuccessfully() {
            Task newTask = new Task();
            newTask.setTitle("Nouvelle tâche");
            newTask.setDescription("Description de la nouvelle tâche");

            Task savedTask = new Task();
            savedTask.setId("new-id-123");
            savedTask.setTitle("Nouvelle tâche");
            savedTask.setDescription("Description de la nouvelle tâche");
            savedTask.setCompleted(false);

            when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

            Task result = taskService.createTask(newTask);

            assertNotNull(result);
            assertEquals("new-id-123", result.getId());
            assertEquals("Nouvelle tâche", result.getTitle());
            assertFalse(result.isCompleted());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("Doit mettre l'ID à null avant de sauvegarder")
        void shouldSetIdToNullBeforeSaving() {
            Task newTask = new Task();
            newTask.setId("should-be-null");
            newTask.setTitle("Nouvelle tâche");

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            taskService.createTask(newTask);

            verify(taskRepository).save(taskCaptor.capture());
            assertNull(taskCaptor.getValue().getId(),
                    "L'ID doit être null pour laisser MongoDB le générer");
        }
    }

    @Nested
    @DisplayName("Tests de getTaskById()")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Doit retourner la tâche quand elle existe")
        void shouldReturnTaskWhenFound() {
            when(taskRepository.findById("task-1")).thenReturn(Optional.of(task1));

            Task result = taskService.getTaskById("task-1");

            assertNotNull(result);
            assertEquals("task-1", result.getId());
            assertEquals("Première tâche", result.getTitle());
            verify(taskRepository, times(1)).findById("task-1");
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException quand la tâche n'existe pas")
        void shouldThrowExceptionWhenTaskNotFound() {
            when(taskRepository.findById("non-existent-id")).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.getTaskById("non-existent-id")
            );

            assertTrue(exception.getMessage().contains("non-existent-id"));
            verify(taskRepository, times(1)).findById("non-existent-id");
        }
    }

    @Nested
    @DisplayName("Tests de getAllTasks()")
    class GetAllTasksTests {

        @Test
        @DisplayName("Doit retourner la liste de toutes les tâches")
        void shouldReturnAllTasks() {
            when(taskRepository.findAll()).thenReturn(Arrays.asList(task1, task2));

            List<Task> results = taskService.getAllTasks();

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("Première tâche", results.get(0).getTitle());
            assertEquals("Deuxième tâche", results.get(1).getTitle());
            verify(taskRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Doit retourner une liste vide quand il n'y a aucune tâche")
        void shouldReturnEmptyListWhenNoTasks() {
            when(taskRepository.findAll()).thenReturn(List.of());

            List<Task> results = taskService.getAllTasks();

            assertNotNull(results);
            assertTrue(results.isEmpty());
            verify(taskRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Tests de deleteTask()")
    class DeleteTaskTests {

        @Test
        @DisplayName("Doit supprimer la tâche quand elle existe")
        void shouldDeleteTaskWhenExists() {
            when(taskRepository.findById("task-1")).thenReturn(Optional.of(task1));
            doNothing().when(taskRepository).deleteById("task-1");

            taskService.deleteTask("task-1");

            verify(taskRepository, times(1)).findById("task-1");
            verify(taskRepository, times(1)).deleteById("task-1");
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la tâche n'existe pas")
        void shouldThrowExceptionWhenDeletingNonExistentTask() {
            when(taskRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.deleteTask("non-existent")
            );

            verify(taskRepository, never()).deleteById(anyString());
        }
    }
}
