package com.example.attendance_app.repository;

import com.example.attendance_app.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    boolean existsByEmployeeId(Long employeeId);

    boolean existsBySiteId(Long siteId);

    Optional<AttendanceRecord> findTopByEmployeeIdAndArrivalTimeBetweenOrderByArrivalTimeDesc(
        Long employeeId,
        Instant from,
        Instant to
    );

    Optional<AttendanceRecord> findTopByEmployeeIdAndIdNotAndArrivalTimeBetweenOrderByArrivalTimeDesc(
        Long employeeId,
        Long excludedRecordId,
        Instant from,
        Instant to
    );
}
