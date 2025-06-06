package com.example.bitcoin.face.repository;

import com.example.bitcoin.face.entity.FaceEvaluate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaceEvaluateRepository extends JpaRepository<FaceEvaluate, Long> {

}
