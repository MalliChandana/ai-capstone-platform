package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long> {
    Optional<ProjectReport> findByRegistrationId(Long registrationId);
    List<ProjectReport> findByStudentId(Long studentId);
    List<ProjectReport> findByProjectId(Long projectId);
}
