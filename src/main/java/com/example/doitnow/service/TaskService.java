package com.example.doitnow.service;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.dto.TaskNotification;
import com.example.doitnow.exception.ResourceNotFoundException;
import com.example.doitnow.model.Priority;
import com.example.doitnow.model.Task;
import com.example.doitnow.model.User;
import com.example.doitnow.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(
            TaskRepository taskRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.taskRepository = taskRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return user.getId();
    }

    // --- CRUD ---

    public Page<TaskDTO> getAllTasks(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return taskRepository.findByUserId(getCurrentUserId(), pageable)
                .map(this::toDTO);
    }

    public TaskDTO getTaskById(String id) {
        Task task = taskRepository.findByIdAndUserId(id, getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
        return toDTO(task);
    }

    public TaskDTO createTask(CreateTaskDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCompleted(false);
        task.setUserId(getCurrentUserId());
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        task.setTags(dto.getTags() != null ? dto.getTags() : new ArrayList<>());
        task.setDueDate(dto.getDueDate());

        TaskDTO result = toDTO(taskRepository.save(task));

        // Notifier en temps réel
        publishNotification(getCurrentUserId(), "CREATED", result);

        return result;
    }

    public TaskDTO updateTask(String id, TaskDTO dto) {
        Task task = taskRepository.findByIdAndUserId(id, getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCompleted(dto.isCompleted());
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : task.getPriority());
        task.setTags(dto.getTags() != null ? dto.getTags() : task.getTags());
        task.setDueDate(dto.getDueDate());

        TaskDTO result = toDTO(taskRepository.save(task));

        // Notifier en temps réel
        publishNotification(getCurrentUserId(), "CREATED", result);

        return result;
    }

    public void deleteTask(String id) {
        Task task = taskRepository.findByIdAndUserId(id, getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée avec l'id : " + id));
        taskRepository.deleteById(task.getId());

        // Pour une suppression, on envoie seulement l'id
        TaskNotification notification = new TaskNotification(
                "DELETED", id, null);
        messagingTemplate.convertAndSend(
                "/topic/tasks/" + getCurrentUserId(), notification);
    }

    // --- Recherche et filtres ---

    public List<TaskDTO> getOverdueTasks() {
        return taskRepository.findOverdueTasks(getCurrentUserId(), LocalDate.now()).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TaskDTO> searchTasks(String keyword) {
        return taskRepository.searchTasks(getCurrentUserId(), keyword).stream()
                .map(this::toDTO)
                .toList();
    }

    public Page<TaskDTO> searchTasks(String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return taskRepository.searchTasks(getCurrentUserId(), keyword, pageable)
                .map(this::toDTO);
    }

    public List<TaskDTO> getTasksByTag(String tag) {
        return taskRepository.findByUserIdAndTagsContaining(getCurrentUserId(), tag).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TaskDTO> getTasksByPriority(Priority priority) {
        return taskRepository.findByUserIdAndPriority(getCurrentUserId(), priority).stream()
                .map(this::toDTO)
                .toList();
    }

    // --- Conversion ---

    private TaskDTO toDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCompleted(task.isCompleted());
        dto.setUserId(task.getUserId());
        dto.setPriority(task.getPriority());
        dto.setTags(task.getTags());
        dto.setDueDate(task.getDueDate());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }

    private void publishNotification(
            String userId, String type, TaskDTO task) {
        TaskNotification notification =
                new TaskNotification(type, task.getId(), task);
        messagingTemplate.convertAndSend(
                "/topic/tasks/" + userId, notification);
    }
}
