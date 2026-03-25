package com.example.doitnow.repository;

import com.example.doitnow.model.Priority;
import com.example.doitnow.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository
        .MongoRepository;
import org.springframework.data.mongodb.repository
        .Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// L'interface définit les opérations de persistance possibles
public interface TaskRepository extends MongoRepository<Task, String>{

    // Query method : trouve les tâches selon leur statut (complétées ou non)
    List<Task> findByCompleted(boolean completed);

    // Query method : recherche par mot-clé dans le titre (insensible à la casse)
    List<Task> findByTitleContainingIgnoreCase(String keyword);

    // Versions non paginées
    List<Task> findByUserId(String userId);

    Optional<Task> findByIdAndUserId(
            String id, String userId
    );

    List<Task> findByUserIdAndPriority(
            String userId, Priority priority
    );

    List<Task> findByUserIdAndTagsContaining(
            String userId, String tag
    );

    List<Task> findByUserIdAndDueDateBeforeAndCompletedFalse(
            String userId, LocalDate date
    );

    // Versions paginées
    Page<Task> findByUserId(
            String userId, Pageable pageable
    );

    // Requêtes @Query
    @Query("{ 'userId': ?0, 'completed': false, "
            + "'dueDate': { $lte: ?1 } }")
    List<Task> findOverdueTasks(
            String userId, LocalDate now
    );

    @Query("{ 'userId': ?0, $or: [ "
            + "{ 'title': "
            + "{ $regex: ?1, $options: 'i' } }, "
            + "{ 'description': "
            + "{ $regex: ?1, $options: 'i' } } "
            + "] }")
    List<Task> searchTasks(
            String userId, String keyword
    );

    @Query("{ 'userId': ?0, $or: [ "
            + "{ 'title': "
            + "{ $regex: ?1, $options: 'i' } }, "
            + "{ 'description': "
            + "{ $regex: ?1, $options: 'i' } } "
            + "] }")
    Page<Task> searchTasks(
            String userId, String keyword,
            Pageable pageable
    );
}