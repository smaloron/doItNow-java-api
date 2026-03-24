package com.example.doitnow.repository;

import com.example.doitnow.model.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository {

    // On utilise une ConcurrentHashMap pour la thread-safety
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        users.put(user.getId(), user);
        return user;
    }

    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }
}
