package com.example.doitnow.controller;

import com.example.doitnow.dto.CreateTaskDTO;
import com.example.doitnow.dto.TaskDTO;
import com.example.doitnow.dto.TaskStatsDTO;
import com.example.doitnow.model.Priority;
import com.example.doitnow.model.User;
import com.example.doitnow.service.TaskService;
import com.example.doitnow.service.TaskStatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context
        .SecurityContextHolder;


import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskStatService taskStatsService;

    public TaskController(TaskService taskService,
                          TaskStatService taskStatService) {
        this.taskService = taskService;
        this.taskStatsService = taskStatService;
    }

    // GET /api/tasks — Récupérer toutes les tâches (paginé)
    @GetMapping
    public ResponseEntity<Page<TaskDTO>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(taskService.getAllTasks(page, size, sortBy, direction));
    }

    // GET /api/tasks/{id} — Récupérer une tâche par ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    // POST /api/tasks — Créer une nouvelle tâche
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskDTO createTaskDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(createTaskDTO));
    }

    // PUT /api/tasks/{id} — Mettre à jour une tâche
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable String id, @Valid @RequestBody TaskDTO taskDTO) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDTO));
    }

    // DELETE /api/tasks/{id} — Supprimer une tâche
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/tasks/overdue — Tâches en retard
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskDTO>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }

    // GET /api/tasks/search?keyword=xxx — Recherche dans titre et description
    @GetMapping("/search")
    public ResponseEntity<Page<TaskDTO>> searchTasks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(taskService.searchTasks(keyword, page, size, sortBy, direction));
    }

    // GET /api/tasks/tag/{tag} — Filtrer par tag
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<TaskDTO>> getTasksByTag(@PathVariable String tag) {
        return ResponseEntity.ok(taskService.getTasksByTag(tag));
    }

    // GET /api/tasks/priority/{priority} — Filtrer par priorité
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskDTO>> getTasksByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(taskService.getTasksByPriority(priority));
    }

    @GetMapping("/stats")
    public TaskStatsDTO getStats() {
        Authentication auth =
                SecurityContextHolder.getContext()
                        .getAuthentication();
        User user = (User) auth.getPrincipal();
        return taskStatsService
                .getStatsForUser(user.getId());
    }
}
