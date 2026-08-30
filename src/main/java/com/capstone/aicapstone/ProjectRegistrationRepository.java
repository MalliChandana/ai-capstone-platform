package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRegistrationRepository
        extends JpaRepository<ProjectRegistration, Long> {

    List<ProjectRegistration> findByStudentId(Long studentId);

    List<ProjectRegistration> findByStudentEmail(String studentEmail);

    List<ProjectRegistration> findByProjectId(Long projectId);

    List<ProjectRegistration> findByCompanyEmail(String companyEmail);

    boolean existsByStudentIdAndProjectId(Long studentId, Long projectId);
}