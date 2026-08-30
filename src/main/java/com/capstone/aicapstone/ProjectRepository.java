package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCompanyEmail(String companyEmail);
    List<Project> findByCompanyEmailIgnoreCase(String companyEmail);
    List<Project> findByStatus(String status);
    List<Project> findByStatusIgnoreCase(String status);
    boolean existsByTitleAndCompanyEmail(String title, String companyEmail);
    boolean existsByTitleIgnoreCaseAndCompanyEmailIgnoreCase(String title, String companyEmail);
    Optional<Project> findByTitleAndCompanyEmail(String title, String companyEmail);
    Optional<Project> findByTitleIgnoreCaseAndCompanyEmailIgnoreCase(String title, String companyEmail);
}