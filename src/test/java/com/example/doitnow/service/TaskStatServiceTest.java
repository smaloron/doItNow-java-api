package com.example.doitnow.service;

import com.example.doitnow.dto.TaskStatsDTO;
import com.example.doitnow.model.Task;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour TaskStatService")
class TaskStatServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TaskStatService taskStatService;

    private static final String USER_ID = "user-1";

    private void mockCounts(long total, long completed, long overdue) {
        when(mongoTemplate.count(any(Query.class), eq(Task.class)))
                .thenReturn(total)       // total
                .thenReturn(completed)   // completed
                .thenReturn(overdue);    // overdue
    }

    private void mockAggregation(List<Document> docs) {
        AggregationResults<Document> results = new AggregationResults<>(docs, new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("tasks"), eq(Document.class)))
                .thenReturn(results);
    }

    @Nested
    @DisplayName("Tests de getStatsForUser()")
    class GetStatsForUserTests {

        @Test
        @DisplayName("Doit calculer les statistiques correctement")
        void shouldCalculateStatsCorrectly() {
            mockCounts(10, 7, 1);
            mockAggregation(List.of(
                    new Document("priority", "HIGH").append("count", 4),
                    new Document("priority", "MEDIUM").append("count", 3),
                    new Document("priority", "LOW").append("count", 3)
            ));

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(10, stats.getTotal());
            assertEquals(7, stats.getCompleted());
            assertEquals(3, stats.getPending());
            assertEquals(1, stats.getOverdue());
            assertEquals(70.0, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit retourner 0% quand aucune tâche")
        void shouldReturn0WhenNoTasks() {
            mockCounts(0, 0, 0);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(0, stats.getTotal());
            assertEquals(0, stats.getCompleted());
            assertEquals(0, stats.getPending());
            assertEquals(0, stats.getOverdue());
            assertEquals(0, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit retourner 100% quand toutes les tâches sont complétées")
        void shouldReturn100WhenAllCompleted() {
            mockCounts(5, 5, 0);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(5, stats.getTotal());
            assertEquals(5, stats.getCompleted());
            assertEquals(0, stats.getPending());
            assertEquals(100.0, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit calculer pending = total - completed")
        void shouldCalculatePendingCorrectly() {
            mockCounts(8, 3, 2);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(5, stats.getPending());
        }

        @Test
        @DisplayName("Doit arrondir le taux de complétion à 2 décimales")
        void shouldRoundCompletionRate() {
            mockCounts(3, 1, 0);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(33.33, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit gérer le cas où toutes les tâches sont en retard")
        void shouldHandleAllTasksOverdue() {
            mockCounts(5, 0, 5);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(5, stats.getTotal());
            assertEquals(0, stats.getCompleted());
            assertEquals(5, stats.getPending());
            assertEquals(5, stats.getOverdue());
            assertEquals(0, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit arrondir 66.67% correctement (2/3)")
        void shouldRoundTwoThirds() {
            mockCounts(3, 2, 0);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(66.67, stats.getCompletionRate());
        }

        @Test
        @DisplayName("Doit gérer une seule tâche complétée")
        void shouldHandleSingleCompletedTask() {
            mockCounts(1, 1, 0);
            mockAggregation(List.of(
                    new Document("priority", "HIGH").append("count", 1)
            ));

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertEquals(1, stats.getTotal());
            assertEquals(1, stats.getCompleted());
            assertEquals(0, stats.getPending());
            assertEquals(100.0, stats.getCompletionRate());
        }
    }

    @Nested
    @DisplayName("Tests de l'agrégation par priorité")
    class TasksByPriorityTests {

        @Test
        @DisplayName("Doit retourner la répartition par priorité")
        void shouldReturnTasksByPriority() {
            mockCounts(6, 2, 0);
            mockAggregation(List.of(
                    new Document("priority", "URGENT").append("count", 1),
                    new Document("priority", "HIGH").append("count", 2),
                    new Document("priority", "MEDIUM").append("count", 2),
                    new Document("priority", "LOW").append("count", 1)
            ));

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            Map<String, Long> byPriority = stats.getTasksByPriority();
            assertNotNull(byPriority);
            assertEquals(4, byPriority.size());
            assertEquals(1L, byPriority.get("URGENT"));
            assertEquals(2L, byPriority.get("HIGH"));
            assertEquals(2L, byPriority.get("MEDIUM"));
            assertEquals(1L, byPriority.get("LOW"));
        }

        @Test
        @DisplayName("Doit retourner une map vide si aucune tâche")
        void shouldReturnEmptyMapWhenNoTasks() {
            mockCounts(0, 0, 0);
            mockAggregation(List.of());

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            assertNotNull(stats.getTasksByPriority());
            assertTrue(stats.getTasksByPriority().isEmpty());
        }

        @Test
        @DisplayName("Doit gérer une seule priorité présente")
        void shouldHandleSinglePriority() {
            mockCounts(3, 1, 0);
            mockAggregation(List.of(
                    new Document("priority", "MEDIUM").append("count", 3)
            ));

            TaskStatsDTO stats = taskStatService.getStatsForUser(USER_ID);

            Map<String, Long> byPriority = stats.getTasksByPriority();
            assertEquals(1, byPriority.size());
            assertEquals(3L, byPriority.get("MEDIUM"));
            assertNull(byPriority.get("HIGH"));
        }
    }
}
