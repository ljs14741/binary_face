package com.example.face.repository;

import com.example.face.entity.DailyVisitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyVisitorRepository extends JpaRepository<DailyVisitor, Long> {
    Optional<DailyVisitor> findByPageNameAndDate(String pageName, LocalDate date);
}
