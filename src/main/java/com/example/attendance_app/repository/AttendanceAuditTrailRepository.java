package com.example.attendance_app.repository;

import com.example.attendance_app.entity.AttendanceAuditTrail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceAuditTrailRepository extends JpaRepository<AttendanceAuditTrail, Long> {

    Page<AttendanceAuditTrail> findByAttendanceRecordId(Long attendanceRecordId, Pageable pageable);
}
