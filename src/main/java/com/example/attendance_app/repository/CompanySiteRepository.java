package com.example.attendance_app.repository;

import com.example.attendance_app.entity.CompanySite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySiteRepository extends JpaRepository<CompanySite, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
