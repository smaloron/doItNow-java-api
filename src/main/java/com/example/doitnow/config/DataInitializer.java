package com.example.doitnow.config;

import com.example.doitnow.model.Priority;
import com.example.doitnow.model.Task;
import com.example.doitnow.model.User;
import com.example.doitnow.repository
        .TaskRepository;
import com.example.doitnow.repository
        .UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password
        .PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer
        implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            System.out.println(
                    "Base déjà initialisée. "
                            + "Seeding ignoré.");
            return;
        }

        System.out.println(
                "Initialisation des données "
                        + "de démonstration...");

        User admin = new User();
        admin.setEmail("admin@doitnow.com");
        admin.setPassword(
                passwordEncoder.encode("admin123"));
        admin = userRepository.save(admin);

        List<Task> tasks = List.of(
                createTask(
                        "Rédiger le rapport Q3",
                        "Inclure les chiffres de vente",
                        Priority.HIGH,
                        List.of("rapport", "finance"),
                        LocalDate.now().plusDays(7),
                        false, admin.getId()),
                createTask(
                        "Préparer la réunion client",
                        "Slides + démo",
                        Priority.URGENT,
                        List.of("client", "présentation"),
                        LocalDate.now().plusDays(2),
                        false, admin.getId()),
                createTask(
                        "Mettre à jour la documentation",
                        "API endpoints",
                        Priority.MEDIUM,
                        List.of("docs", "technique"),
                        LocalDate.now().plusDays(14),
                        false, admin.getId()),
                createTask(
                        "Corriger le bug #42",
                        "NullPointerException en prod",
                        Priority.HIGH,
                        List.of("bug", "production"),
                        LocalDate.now().minusDays(1),
                        false, admin.getId()),
                createTask(
                        "Organiser le team building",
                        "Réserver le restaurant",
                        Priority.LOW,
                        List.of("équipe", "social"),
                        LocalDate.now().plusDays(30),
                        false, admin.getId()),
                createTask(
                        "Déployer la v2.1",
                        "Suivre la checklist",
                        Priority.HIGH,
                        List.of("déploiement"),
                        LocalDate.now().minusDays(3),
                        true, admin.getId()),
                createTask(
                        "Revoir les pull requests",
                        "3 PR en attente",
                        Priority.MEDIUM,
                        List.of("code-review"),
                        LocalDate.now().plusDays(1),
                        true, admin.getId()),
                createTask(
                        "Commander les fournitures",
                        "Post-it, marqueurs",
                        Priority.LOW,
                        List.of("admin", "bureau"),
                        LocalDate.now().plusDays(10),
                        false, admin.getId()));

        taskRepository.saveAll(tasks);
        System.out.println(tasks.size()
                + " tâches créées pour "
                + admin.getEmail());
    }

    private Task createTask(
            String title, String description,
            Priority priority,
            List<String> tags,
            LocalDate dueDate,
            boolean completed,
            String userId) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setTags(tags);
        task.setDueDate(dueDate);
        task.setCompleted(completed);
        task.setUserId(userId);
        return task;
    }
}