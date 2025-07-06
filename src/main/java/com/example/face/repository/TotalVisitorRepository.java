package com.example.face.repository;

import com.example.face.entity.TotalVisitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TotalVisitorRepository extends JpaRepository<TotalVisitor, Long> {
    Optional<TotalVisitor> findByPageName(String pageName);
}