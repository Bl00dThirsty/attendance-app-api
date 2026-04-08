package com.example.attendance_app.repository;

import com.example.attendance_app.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPositionId(Long positionId);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<Employee> findByEmailIgnoreCase(String email);
}
