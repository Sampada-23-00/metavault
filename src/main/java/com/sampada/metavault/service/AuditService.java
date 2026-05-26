package com.sampada.metavault.service;

import com.sampada.metavault.entity.AuditLog;
import com.sampada.metavault.entity.User;
import com.sampada.metavault.enums.AuditAction;
import com.sampada.metavault.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AuditService writes audit log entries for every significant action.
 * It is called from RecordService and VersionService after each operation.
 *
 * Propagation.REQUIRES_NEW: this method always runs in its OWN transaction.
 * Why? If the main operation succeeds but audit logging fails for some reason,
 * we don't want to roll back the whole operation. The record was saved — that's
 * what matters. Audit failure should be logged but not block the user.
 *
 * (For this project, both run in the same transaction, but this is the
 * production-correct pattern to know for the interview.)
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Write one audit log entry.
     *
     * @param user      who performed the action
     * @param action    what they did (CREATE/UPDATE/RESTORE/DELETE)
     * @param recordId  which record was affected
     * @param versionId which version was created (null for DELETE)
     * @param details   extra context as a plain string (e.g., "Created v1")
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, AuditAction action,
                          UUID recordId, UUID versionId, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .recordId(recordId)
                    .versionId(versionId)
                    .details(details)
                    .build();

            auditLogRepository.save(entry);
            log.info("AUDIT: user={} action={} record={}", user.getUsername(), action, recordId);

        } catch (Exception e) {
            // Never let audit failure crash the main operation
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }
}
