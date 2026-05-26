package com.sampada.metavault.controller;

import com.sampada.metavault.dto.AuditLogResponse;
import com.sampada.metavault.dto.PagedResponse;
import com.sampada.metavault.entity.AuditLog;
import com.sampada.metavault.exception.ResourceNotFoundException;
import com.sampada.metavault.exception.UnauthorizedException;
import com.sampada.metavault.mapper.VersionMapper;
import com.sampada.metavault.repository.AuditLogRepository;
import com.sampada.metavault.repository.MetadataRecordRepository;
import com.sampada.metavault.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "View audit trail for records")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final MetadataRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final VersionMapper versionMapper;

    public AuditController(AuditLogRepository auditLogRepository,
                           MetadataRecordRepository recordRepository,
                           UserRepository userRepository,
                           VersionMapper versionMapper) {
        this.auditLogRepository = auditLogRepository;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.versionMapper = versionMapper;
    }

    /**
     * GET /api/audit?recordId=...&page=0&size=20
     * Returns the audit trail for a record — owner only.
     */
    @GetMapping
    @Operation(summary = "Get audit log",
               description = "Returns the audit trail for a record. Only the record owner can view it.")
    public ResponseEntity<PagedResponse<AuditLogResponse>> getAuditLog(
            @RequestParam UUID recordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify record exists
        var record = recordRepository.findById(recordId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("MetadataRecord", recordId));

        // Verify ownership
        if (!record.getOwner().getUsername().equals(userDetails.getUsername())) {
            throw UnauthorizedException.accessDenied();
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("timestamp").descending());

        Page<AuditLog> logPage = auditLogRepository
                .findByRecordIdOrderByTimestampDesc(recordId, pageable);

        List<AuditLogResponse> responses = logPage.getContent()
                .stream()
                .map(versionMapper::toAuditResponse)
                .toList();

        return ResponseEntity.ok(PagedResponse.from(logPage, responses));
    }
}
