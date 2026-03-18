package com.example.doitnow.repository;

import com.example.doitnow.model.Task;

import java.util.List;
import java.util.Optional;

// L'interface définit les opérations de persistance possibles
public interface TaskRepository {
    Task save(Task task);

    List<Task> findAll();

    Optional<Task> findById(String id);

    void deleteById(String id);
}