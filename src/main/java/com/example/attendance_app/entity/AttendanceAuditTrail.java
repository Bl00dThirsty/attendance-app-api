package com.example.attendance_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attendance_audit_trails")
public class AttendanceAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_record_id", nullable = false)
    private AttendanceRecord attendanceRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceAuditAction action;

    @Column(name = "actor_subject", nullable = false, length = 150)
    private String actorSubject;

    @Column(name = "actor_employee_id")
    private Long actorEmployeeId;

    @Column(name = "actor_roles", nullable = false, length = 250)
    private String actorRoles;

    @Column(length = 500)
    private String reason;

    @Column(length = 2000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public AttendanceRecord getAttendanceRecord() {
        return attendanceRecord;
    }

    public void setAttendanceRecord(AttendanceRecord attendanceRecord) {
        this.attendanceRecord = attendanceRecord;
    }

    public AttendanceAuditAction getAction() {
        return action;
    }

    public void setAction(AttendanceAuditAction action) {
        this.action = action;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public void setActorSubject(String actorSubject) {
        this.actorSubject = actorSubject;
    }

    public Long getActorEmployeeId() {
        return actorEmployeeId;
    }

    public void setActorEmployeeId(Long actorEmployeeId) {
        this.actorEmployeeId = actorEmployeeId;
    }

    public String getActorRoles() {
        return actorRoles;
    }

    public void setActorRoles(String actorRoles) {
        this.actorRoles = actorRoles;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
