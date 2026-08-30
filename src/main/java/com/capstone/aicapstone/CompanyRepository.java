package com.capstone.aicapstone;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByEmail(String email);
    Optional<Company> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}