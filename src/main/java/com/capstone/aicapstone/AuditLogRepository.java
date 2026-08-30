package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByIdDesc();

    List<AuditLog> findTop100ByOrderByIdDesc();

    List<AuditLog> findByActorRoleOrderByIdDesc(String actorRole);

    List<AuditLog> findByActionOrderByIdDesc(String action);
}
