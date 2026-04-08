package com.example.attendance_app.repository;

import com.example.attendance_app.entity.AttendanceIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceIdempotencyKeyRepository extends JpaRepository<AttendanceIdempotencyKey, Long> {

    Optional<AttendanceIdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
