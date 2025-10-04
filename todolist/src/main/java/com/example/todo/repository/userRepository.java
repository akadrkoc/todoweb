package com.example.todo.repository;

import com.example.todo.model.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface userRepository extends JpaRepository<user, Long> {

    // Find user by username
    Optional<user> findByUsername(String username);

    // Check if a username exists
    boolean existsByUsername(String username);
}
