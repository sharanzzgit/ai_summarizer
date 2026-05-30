package com.sharan.ai_summary.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sharan.ai_summary.entity.User;

public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
