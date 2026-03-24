package com.example.doitnow.repository;

import com.example.doitnow.model.Task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

// L'interface définit les opérations de persistance possibles
public interface TaskRepository extends MongoRepository<Task, String>{

    // Query method : trouve les tâches selon leur statut (complétées ou non)
    List<Task> findByCompleted(boolean completed);

    // Query method : recherche par mot-clé dans le titre (insensible à la casse)
    List<Task> findByTitleContainingIgnoreCase(String keyword);
}