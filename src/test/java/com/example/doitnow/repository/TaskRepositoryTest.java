package com.example.doitnow.repository;

import com.example.doitnow.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Tests d'intégration pour TaskRepository")
class TaskRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private TaskRepository taskRepository;

    private Task task1;
    private Task task2;
    private Task task3;

    @BeforeEach
    void setUp() {
        // Insertion de données de test
        task1 = new Task();
        task1.setTitle("Tâche à faire");
        task1.setDescription("Description tâche 1");
        task1.setCompleted(false);
        task1 = taskRepository.save(task1);

        task2 = new Task();
        task2.setTitle("Tâche terminée");
        task2.setDescription("Description tâche 2");
        task2.setCompleted(true);
        task2 = taskRepository.save(task2);

        task3 = new Task();
        task3.setTitle("Autre tâche à faire");
        task3.setDescription("Description tâche 3");
        task3.setCompleted(false);
        task3 = taskRepository.save(task3);
    }

    @AfterEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    @Test
    @DisplayName("Doit sauvegarder une tâche et lui attribuer un ID")
    void shouldSaveTaskAndGenerateId() {
        // ARRANGE
        Task newTask = new Task();
        newTask.setTitle("Tâche de test");
        newTask.setDescription("Description test");
        newTask.setCompleted(false);

        // ACT
        Task saved = taskRepository.save(newTask);

        // ASSERT
        assertThat(saved.getId()).isNotNull().isNotBlank();
        assertThat(saved.getTitle()).isEqualTo("Tâche de test");
    }

    @Test
    @DisplayName("Doit retrouver une tâche par son ID")
    void shouldFindTaskById() {
        // ACT
        Optional<Task> found = taskRepository.findById(task1.getId());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Tâche à faire");
        assertThat(found.get().getDescription()).isEqualTo("Description tâche 1");
    }

    @Test
    @DisplayName("Doit retourner un Optional vide pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // ACT
        Optional<Task> found = taskRepository.findById("id-qui-nexiste-pas");

        // ASSERT
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Doit retourner toutes les tâches")
    void shouldFindAllTasks() {
        // ACT
        List<Task> allTasks = taskRepository.findAll();

        // ASSERT
        assertThat(allTasks).hasSize(3);
        assertThat(allTasks)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder(
                        "Tâche à faire",
                        "Tâche terminée",
                        "Autre tâche à faire"
                );
    }

    @Test
    @DisplayName("Doit supprimer une tâche par son ID")
    void shouldDeleteTaskById() {
        // ACT
        taskRepository.deleteById(task1.getId());

        // ASSERT
        Optional<Task> deleted = taskRepository.findById(task1.getId());
        assertThat(deleted).isEmpty();

        List<Task> remaining = taskRepository.findAll();
        assertThat(remaining).hasSize(2);
    }

    @Test
    @DisplayName("Doit mettre à jour une tâche existante")
    void shouldUpdateExistingTask() {
        // ARRANGE
        task1.setTitle("Titre modifié");
        task1.setCompleted(true);

        // ACT
        Task updated = taskRepository.save(task1);

        // ASSERT
        assertThat(updated.getId()).isEqualTo(task1.getId());
        assertThat(updated.getTitle()).isEqualTo("Titre modifié");
        assertThat(updated.isCompleted()).isTrue();

        // Vérifier en re-lisant depuis la base
        Optional<Task> fromDb = taskRepository.findById(task1.getId());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getTitle()).isEqualTo("Titre modifié");
    }

    @Test
    @DisplayName("Doit compter le nombre total de tâches")
    void shouldCountTasks() {
        // ACT
        long count = taskRepository.count();

        // ASSERT
        assertThat(count).isEqualTo(3);
    }
}