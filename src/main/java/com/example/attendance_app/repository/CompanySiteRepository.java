package com.example.attendance_app.repository;

import com.example.attendance_app.entity.CompanySite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanySiteRepository extends JpaRepository<CompanySite, Long>, JpaSpecificationExecutor<CompanySite> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
