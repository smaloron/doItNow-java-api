package com.example.doitnow.service;

import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Task;
import com.example.doitnow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    // Injection de dépendance par constructeur
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Récupérer toutes les tâches
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // Récupérer une tâche par son ID
    public Task getTaskById(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
    }

    // Créer une nouvelle tâche
    public Task createTask(Task task) {
        task.setId(null); // On laisse MongoDB générer l'ID
        return taskRepository.save(task);
    }

    // Mettre à jour une tâche existante
    public Task updateTask(String id, Task taskDetails) {
        Task existingTask = getTaskById(id); // Lève une exception si non trouvée

        existingTask.setTitle(taskDetails.getTitle());
        existingTask.setDescription(taskDetails.getDescription());
        existingTask.setCompleted(taskDetails.isCompleted());

        return taskRepository.save(existingTask);
    }

    // Supprimer une tâche
    public void deleteTask(String id) {
        Task existingTask = getTaskById(id); // Vérifie que la tâche existe
        taskRepository.deleteById(existingTask.getId());
    }

    // --- BONUS : Recherche ---

    // Trouver les tâches par statut (complétées ou non)
    public List<Task> getTasksByCompleted(boolean completed) {
        return taskRepository.findByCompleted(completed);
    }

    // Rechercher des tâches par mot-clé dans le titre
    public List<Task> searchTasksByTitle(String keyword) {
        return taskRepository.findByTitleContainingIgnoreCase(keyword);
    }
}