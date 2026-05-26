package com.sampada.metavault.repository;

import com.sampada.metavault.entity.AuditLog;
import com.sampada.metavault.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for AuditLog entries.
 * Audit logs are append-only — only save() and find*() are ever called here.
 * No delete or update operations should ever touch this table.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Get all audit events for a specific record, newest first.
     * Used by: GET /api/audit?recordId=...
     *
     * Spring Data generates:
     *   SELECT * FROM audit_logs
     *   WHERE record_id = ?
     *   ORDER BY timestamp DESC
     *   LIMIT ? OFFSET ?
     */
    Page<AuditLog> findByRecordIdOrderByTimestampDesc(UUID recordId, Pageable pageable);

    /**
     * Get audit trail for a specific user.
     * Useful for admin views: "show me everything user X has done".
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Filter audit logs by action type.
     * Example: find all RESTORE operations across the system.
     */
    Page<AuditLog> findByRecordIdAndActionOrderByTimestampDesc(
            UUID recordId, AuditAction action, Pageable pageable);
}
