package com.sampada.metavault.entity;

import com.sampada.metavault.enums.AuditAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog is the tamper-evident history of every operation in the system.
 * Every create, update, restore, and delete writes one row here.
 *
 * Design principles:
 * - ALL columns are updatable=false — audit logs are append-only, never edited
 * - We store recordId and versionId as plain UUIDs (not FK relationships)
 *   because if a record is hard-deleted (hypothetically), the audit log
 *   should still be readable. FK constraints would orphan the log rows.
 * - The `details` field is free-form JSON for any extra context
 *   (e.g., {"previousVersionNumber": 2, "restoredFromVersionNumber": 1})
 *
 * In a production system, audit logs would go to an immutable store
 * (like AWS CloudTrail or a separate append-only database). For this
 * project, a regular table with no UPDATE/DELETE permissions is sufficient.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_record", columnList = "record_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Who performed the action. Stored as FK to users table.
     * We keep this as a real FK (not raw UUID) because users are never deleted
     * in this system — only their records are soft-deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * What happened: CREATE, UPDATE, RESTORE, or DELETE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditAction action;

    /**
     * Which record was affected. Stored as raw UUID (not FK) so audit logs
     * survive even if the record entity is restructured or archived.
     */
    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    /**
     * Which version was created/affected. Nullable because DELETE operations
     * don't create a new version.
     */
    @Column(name = "version_id", updatable = false)
    private UUID versionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Free-form JSON string for extra context.
     * Example: {"changeMessage":"Updated timeout","versionNumber":3}
     */
    @Column(updatable = false, columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
