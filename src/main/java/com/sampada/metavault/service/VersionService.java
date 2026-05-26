package com.sampada.metavault.service;

import com.sampada.metavault.dto.DiffResponse;
import com.sampada.metavault.dto.PagedResponse;
import com.sampada.metavault.dto.RecordResponse;
import com.sampada.metavault.dto.VersionResponse;
import com.sampada.metavault.entity.MetadataRecord;
import com.sampada.metavault.entity.MetadataVersion;
import com.sampada.metavault.entity.User;
import com.sampada.metavault.enums.AuditAction;
import com.sampada.metavault.exception.ResourceNotFoundException;
import com.sampada.metavault.exception.UnauthorizedException;
import com.sampada.metavault.mapper.RecordMapper;
import com.sampada.metavault.mapper.VersionMapper;
import com.sampada.metavault.repository.MetadataRecordRepository;
import com.sampada.metavault.repository.MetadataVersionRepository;
import com.sampada.metavault.util.DiffUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * VersionService handles all version-related operations:
 * - Reading version history
 * - Fetching a specific version
 * - Computing a diff between two versions
 * - Restoring to a previous version
 */
@Service
@Transactional(readOnly = true)
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    private final MetadataRecordRepository recordRepository;
    private final MetadataVersionRepository versionRepository;
    private final VersionMapper versionMapper;
    private final RecordMapper recordMapper;
    private final AuditService auditService;
    private final RecordService recordService;

    public VersionService(MetadataRecordRepository recordRepository,
                          MetadataVersionRepository versionRepository,
                          VersionMapper versionMapper,
                          RecordMapper recordMapper,
                          AuditService auditService,
                          RecordService recordService) {
        this.recordRepository = recordRepository;
        this.versionRepository = versionRepository;
        this.versionMapper = versionMapper;
        this.recordMapper = recordMapper;
        this.auditService = auditService;
        this.recordService = recordService;
    }

    // ── Version History ───────────────────────────────────────────────────────

    /**
     * GET /api/records/{id}/versions
     * Returns paginated version history, newest first.
     */
    public PagedResponse<VersionResponse> getVersionHistory(UUID recordId,
                                                             String username,
                                                             Pageable pageable) {
        MetadataRecord record = findAndAuthorize(recordId, username);
        Page<MetadataVersion> page = versionRepository
                .findByRecordOrderByVersionNumberDesc(record, pageable);

        List<VersionResponse> responses = page.getContent()
                .stream()
                .map(versionMapper::toResponse)
                .toList();

        return PagedResponse.from(page, responses);
    }

    // ── Specific Version ──────────────────────────────────────────────────────

    /**
     * GET /api/records/{id}/versions/{versionNumber}
     */
    public VersionResponse getVersion(UUID recordId, int versionNumber, String username) {
        MetadataRecord record = findAndAuthorize(recordId, username);
        MetadataVersion version = findVersion(record, versionNumber);
        return versionMapper.toResponse(version);
    }

    // ── Diff ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/records/{id}/diff?from=2&to=5
     *
     * Computes what changed between version `from` and version `to`.
     * Both versions must exist. `from` doesn't have to be less than `to`
     * (you can diff "backwards" to see what was removed).
     */
    public DiffResponse getDiff(UUID recordId, int fromVersion, int toVersion, String username) {
        MetadataRecord record = findAndAuthorize(recordId, username);

        // Load both versions — throw 404 if either doesn't exist
        MetadataVersion from = findVersion(record, fromVersion);
        MetadataVersion to   = findVersion(record, toVersion);

        log.debug("Computing diff for record {} between v{} and v{}", recordId, fromVersion, toVersion);

        return DiffUtil.diff(from.getContent(), to.getContent(), fromVersion, toVersion);
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * POST /api/records/{id}/restore/{versionNumber}
     *
     * RESTORE LOGIC (key interview point):
     * Restoring version N does NOT roll back the database.
     * Instead:
     *   1. Read version N's content
     *   2. Create a NEW version (e.g., v6) with that same content
     *   3. Change message: "Restored from v{N}"
     *   4. The history stays linear: v1 → v2 → v3 → v4 → v5 → v6(=v3's content)
     *
     * Why? Because version history must be immutable and auditable.
     * If we deleted v4 and v5, we'd lose the record of what happened.
     * This is exactly how Git's `git revert` works.
     */
    @Transactional
    public RecordResponse restoreVersion(UUID recordId, int versionNumber, String username) {
        User user = recordService.loadUser(username);
        MetadataRecord record = findAndAuthorize(recordId, username);

        // Find the version to restore
        MetadataVersion targetVersion = findVersion(record, versionNumber);

        // Compute the next version number
        int nextVersionNum = versionRepository
                .findMaxVersionNumberByRecord(record)
                .orElse(0) + 1;

        // Create a new version with the old content
        MetadataVersion restoredVersion = MetadataVersion.builder()
                .record(record)
                .versionNumber(nextVersionNum)
                .content(targetVersion.getContent())    // same content as the target version
                .changeMessage("Restored from v" + versionNumber)
                .createdBy(user)
                .build();
        MetadataVersion savedVersion = versionRepository.save(restoredVersion);

        // Update record to point to the new version
        record.setCurrentVersionId(savedVersion.getId());
        record.setCurrentVersionNumber(nextVersionNum);
        MetadataRecord updatedRecord = recordRepository.save(record);

        // Audit
        auditService.logAction(user, AuditAction.RESTORE,
                recordId, savedVersion.getId(),
                "Restored from v" + versionNumber + " → created v" + nextVersionNum);

        log.info("Restored record {} from v{} → new v{}", recordId, versionNumber, nextVersionNum);

        long count = versionRepository.countByRecord(updatedRecord);
        return recordMapper.toResponse(updatedRecord, savedVersion, count);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Find a record by ID and verify the current user owns it.
     */
    private MetadataRecord findAndAuthorize(UUID recordId, String username) {
        User user = recordService.loadUser(username);

        MetadataRecord record = recordRepository.findById(recordId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("MetadataRecord", recordId));

        if (!record.getOwner().getId().equals(user.getId())) {
            throw UnauthorizedException.accessDenied();
        }
        return record;
    }

    /**
     * Find a specific version of a record by version number.
     */
    private MetadataVersion findVersion(MetadataRecord record, int versionNumber) {
        return versionRepository.findByRecordAndVersionNumber(record, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for record " + record.getId()));
    }
}
