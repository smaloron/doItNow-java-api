package com.example.doitnow.service;

import com.example.doitnow.dto.TaskStatsDTO;
import com.example.doitnow.model.Task;
import org.springframework.data.mongodb.core
        .MongoTemplate;
import org.springframework.data.mongodb.core
        .aggregation.Aggregation;
import org.springframework.data.mongodb.core
        .aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query
        .Criteria;
import org.springframework.data.mongodb.core.query
        .Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaskStatService {

    private final MongoTemplate mongoTemplate;

    public TaskStatService(
            MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public TaskStatsDTO getStatsForUser(
            String userId) {
        TaskStatsDTO stats = new TaskStatsDTO();

        // Total
        Query baseQuery = new Query(
                Criteria.where("userId").is(userId));
        long total = mongoTemplate
                .count(baseQuery, Task.class);
        stats.setTotal(total);

        // Complétées
        Query completedQuery = new Query(
                Criteria.where("userId").is(userId)
                        .and("completed").is(true));
        long completed = mongoTemplate
                .count(completedQuery, Task.class);
        stats.setCompleted(completed);
        stats.setPending(total - completed);

        // En retard
        Query overdueQuery = new Query(
                Criteria.where("userId").is(userId)
                        .and("completed").is(false)
                        .and("dueDate")
                        .lt(LocalDate.now()));
        stats.setOverdue(mongoTemplate
                .count(overdueQuery, Task.class));

        // Taux de complétion
        stats.setCompletionRate(total > 0
                ? Math.round(
                (double) completed / total
                        * 10000.0) / 100.0
                : 0);

        // Agrégation par priorité
        Aggregation aggByPriority =
                Aggregation.newAggregation(
                        Aggregation.match(
                                Criteria.where("userId")
                                        .is(userId)),
                        Aggregation.group("priority")
                                .count().as("count"),
                        Aggregation.project("count")
                                .and("_id").as("priority"));

        AggregationResults<org.bson.Document>
                results = mongoTemplate.aggregate(
                aggByPriority, "tasks",
                org.bson.Document.class);

        Map<String, Long> tasksByPriority =
                new HashMap<>();
        for (org.bson.Document doc
                : results.getMappedResults()) {
            tasksByPriority.put(
                    doc.getString("priority"),
                    doc.get("count", Number.class)
                            .longValue());
        }
        stats.setTasksByPriority(tasksByPriority);

        return stats;
    }
}