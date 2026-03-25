package com.example.doitnow.repository;


import com.example.doitnow.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    // Query method : retrouve un utilisateur par son email
    // Utilisé par Spring Security pour l'authentification
    Optional<User> findByEmail(String email);

    // Vérifie si un utilisateur avec cet email existe déjà
    boolean existsByEmail(String email);


}
