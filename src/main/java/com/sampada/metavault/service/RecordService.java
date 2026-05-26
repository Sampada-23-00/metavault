package com.sampada.metavault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampada.metavault.dto.CreateRecordRequest;
import com.sampada.metavault.dto.PagedResponse;
import com.sampada.metavault.dto.RecordResponse;
import com.sampada.metavault.dto.UpdateRecordRequest;
import com.sampada.metavault.entity.MetadataRecord;
import com.sampada.metavault.entity.MetadataVersion;
import com.sampada.metavault.entity.User;
import com.sampada.metavault.enums.AuditAction;
import com.sampada.metavault.exception.ResourceNotFoundException;
import com.sampada.metavault.exception.UnauthorizedException;
import com.sampada.metavault.mapper.RecordMapper;
import com.sampada.metavault.repository.MetadataRecordRepository;
import com.sampada.metavault.repository.MetadataVersionRepository;
import com.sampada.metavault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * RecordService — all business logic for MetadataRecord operations.
 *
 * THE VERSIONING CONTRACT (key interview point):
 * - CREATE record → auto-creates Version 1
 * - UPDATE record → NEVER overwrites content. Always creates Version N+1
 * - DELETE record → soft delete only (isDeleted = true), history preserved
 * - All version content is immutable after creation
 */
@Service
@Transactional
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final MetadataRecordRepository recordRepository;
    private final MetadataVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final RecordMapper recordMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public RecordService(MetadataRecordRepository recordRepository,
                         MetadataVersionRepository versionRepository,
                         UserRepository userRepository,
                         RecordMapper recordMapper,
                         AuditService auditService,
                         ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.recordMapper = recordMapper;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Create a new record with its first version (v1).
     *
     * Why save the record BEFORE the version?
     * MetadataVersion has a FK to MetadataRecord (record_id).
     * The record must exist in DB before we can save a version that references it.
     * We then update the record with currentVersionId after saving the version.
     */
    public RecordResponse createRecord(CreateRecordRequest request, String username) {
        User user = loadUser(username);
        String contentJson = serializeContent(request.getContent());

        // Step 1: Save the record (without version yet)
        MetadataRecord record = MetadataRecord.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(user)
                .build();
        MetadataRecord savedRecord = recordRepository.save(record);

        // Step 2: Create version 1
        String changeMsg = StringUtils.hasText(request.getChangeMessage())
                ? request.getChangeMessage() : "Initial version";

        MetadataVersion version = MetadataVersion.builder()
                .record(savedRecord)
                .versionNumber(1)
                .content(contentJson)
                .changeMessage(changeMsg)
                .createdBy(user)
                .build();
        MetadataVersion savedVersion = versionRepository.save(version);

        // Step 3: Link record to its first version
        savedRecord.setCurrentVersionId(savedVersion.getId());
        savedRecord.setCurrentVersionNumber(1);
        recordRepository.save(savedRecord);

        // Step 4: Audit
        auditService.logAction(user, AuditAction.CREATE,
                savedRecord.getId(), savedVersion.getId(),
                "Record created with initial version");

        log.info("Created record '{}' (id={}) for user '{}'",
                savedRecord.getName(), savedRecord.getId(), username);

        return recordMapper.toResponse(savedRecord, savedVersion, 1L);
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    /**
     * List all active records for the current user, paginated.
     * Supports optional name search (case-insensitive substring match).
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecordResponse> listRecords(String username,
                                                      Pageable pageable,
                                                      String search) {
        User user = loadUser(username);
        Page<MetadataRecord> page;

        if (StringUtils.hasText(search)) {
            page = recordRepository.searchByOwnerAndName(user, search, pageable);
        } else {
            page = recordRepository.findByOwnerAndIsDeletedFalse(user, pageable);
        }

        List<RecordResponse> responses = page.getContent().stream()
                .map(rec -> {
                    // For list view: load current version by ID
                    MetadataVersion currentVersion = null;
                    if (rec.getCurrentVersionId() != null) {
                        currentVersion = versionRepository.findById(rec.getCurrentVersionId())
                                .orElse(null);
                    }
                    long count = versionRepository.countByRecord(rec);
                    return recordMapper.toResponse(rec, currentVersion, count);
                })
                .toList();

        return PagedResponse.from(page, responses);
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RecordResponse getRecord(UUID id, String username) {
        User user = loadUser(username);
        MetadataRecord record = findAndAuthorize(id, user);

        MetadataVersion currentVersion = null;
        if (record.getCurrentVersionId() != null) {
            currentVersion = versionRepository.findById(record.getCurrentVersionId())
                    .orElse(null);
        }
        long count = versionRepository.countByRecord(record);
        return recordMapper.toResponse(record, currentVersion, count);
    }

    // ── UPDATE (creates a new version) ───────────────────────────────────────

    /**
     * Update a record's content. ALWAYS creates a new version — never overwrites.
     *
     * The version number is computed as: max(existing versions) + 1
     * This is safe even under concurrent updates because the unique constraint
     * on (record_id, version_number) in the DB prevents duplicates.
     */
    public RecordResponse updateRecord(UUID id, UpdateRecordRequest request, String username) {
        User user = loadUser(username);
        MetadataRecord record = findAndAuthorize(id, user);
        String contentJson = serializeContent(request.getContent());

        // Compute next version number
        int nextVersionNum = versionRepository
                .findMaxVersionNumberByRecord(record)
                .orElse(0) + 1;

        // Create the new version
        MetadataVersion newVersion = MetadataVersion.builder()
                .record(record)
                .versionNumber(nextVersionNum)
                .content(contentJson)
                .changeMessage(request.getChangeMessage())
                .createdBy(user)
                .build();
        MetadataVersion savedVersion = versionRepository.save(newVersion);

        // Update the record pointer to the new version
        record.setCurrentVersionId(savedVersion.getId());
        record.setCurrentVersionNumber(nextVersionNum);
        MetadataRecord updatedRecord = recordRepository.save(record);

        auditService.logAction(user, AuditAction.UPDATE,
                id, savedVersion.getId(),
                "Updated to v" + nextVersionNum + ": " + request.getChangeMessage());

        log.info("Updated record '{}' to v{}", id, nextVersionNum);

        long count = versionRepository.countByRecord(updatedRecord);
        return recordMapper.toResponse(updatedRecord, savedVersion, count);
    }

    // ── SOFT DELETE ───────────────────────────────────────────────────────────

    /**
     * Soft delete: sets isDeleted=true. The record and all its versions remain
     * in the database — they're just invisible to normal queries.
     * Audit log is still written. History is fully preserved.
     */
    public void deleteRecord(UUID id, String username) {
        User user = loadUser(username);
        MetadataRecord record = findAndAuthorize(id, user);

        record.setDeleted(true);
        recordRepository.save(record);

        auditService.logAction(user, AuditAction.DELETE, id, null,
                "Record soft-deleted");

        log.info("Soft-deleted record '{}'", id);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Find a record by ID, then verify ownership.
     * Returns 404 if the record doesn't exist (or is deleted).
     * Returns 403 if it exists but belongs to someone else.
     *
     * Two-step approach: better error messages than a single combined query.
     */
    private MetadataRecord findAndAuthorize(UUID id, User user) {
        // Find by ID (ignoring owner) — if not found or deleted, throw 404
        MetadataRecord record = recordRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("MetadataRecord", id));

        // Check ownership — if not owner, throw 403
        if (!record.getOwner().getId().equals(user.getId())) {
            throw UnauthorizedException.accessDenied();
        }
        return record;
    }

    User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String serializeContent(com.fasterxml.jackson.databind.JsonNode content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON content: " + e.getMessage());
        }
    }
}
