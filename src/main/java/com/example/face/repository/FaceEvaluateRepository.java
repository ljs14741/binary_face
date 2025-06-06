package com.example.face.repository;

import com.example.face.entity.FaceEvaluate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaceEvaluateRepository extends JpaRepository<FaceEvaluate, Long> {

}
