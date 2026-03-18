package com.example.doitnow.service;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Task;
import com.example.doitnow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO findTaskById(String id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tâche non trouvée avec l'ID : " + id
                ));
        return convertToDTO(task);
    }

    public TaskDTO createTask(CreateTaskDTO createTaskDTO) {
        Task task = new Task();
        task.setTitle(createTaskDTO.getTitle());
        task.setDescription(createTaskDTO.getDescription());
        task.setCompleted(false); // Par défaut

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }


    public TaskDTO updateTask(String id, TaskDTO taskDTO) {
        // 1. Vérifier que la tâche existe
        taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID : " + id));

        // 2. Mettre à jour
        Task taskToUpdate = convertToEntity(taskDTO);
        taskToUpdate.setId(id); // On s'assure de garder le bon ID
        Task updatedTask = taskRepository.save(taskToUpdate);
        return convertToDTO(updatedTask);
    }

    public void deleteTask(String id) {
        // 1. Vérifier que la tâche existe avant de supprimer
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'ID : " + id));

        // 2. Supprimer
        taskRepository.deleteById(task.getId());
    }

    // Méthodes de conversion privées
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCompleted(task.isCompleted());
        return dto;
    }

    private Task convertToEntity(TaskDTO dto) {
        Task task = new Task();
        task.setId(dto.getId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCompleted(dto.isCompleted());
        return task;
    }
}