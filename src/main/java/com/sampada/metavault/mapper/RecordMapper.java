package com.sampada.metavault.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampada.metavault.dto.RecordResponse;
import com.sampada.metavault.entity.MetadataRecord;
import com.sampada.metavault.entity.MetadataVersion;
import org.springframework.stereotype.Component;

/**
 * RecordMapper converts MetadataRecord entities to RecordResponse DTOs.
 *
 * Why a separate mapper class instead of doing this in the service?
 * → Single Responsibility Principle: services handle business logic,
 *   mappers handle shape transformation. Easier to test and modify.
 *
 * Why not MapStruct (the popular annotation-based mapper)?
 * → MapStruct is great for large projects, but requires annotation processing
 *   setup and adds complexity. Manual mappers are more readable for learning.
 *
 * @Component: Spring manages this as a singleton bean — injected into services.
 */
@Component
public class RecordMapper {

    // ObjectMapper converts stored JSON strings → JsonNode for clean API responses
    private final ObjectMapper objectMapper;

    public RecordMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert a MetadataRecord + its current MetadataVersion → RecordResponse.
     *
     * @param record        the record entity (loaded from DB)
     * @param currentVersion the current version entity (may be null for empty records)
     * @param versionCount  total number of versions
     */
    public RecordResponse toResponse(MetadataRecord record,
                                     MetadataVersion currentVersion,
                                     long versionCount) {
        JsonNode contentNode = null;
        if (currentVersion != null && currentVersion.getContent() != null) {
            contentNode = parseJson(currentVersion.getContent());
        }

        return RecordResponse.builder()
                .id(record.getId())
                .name(record.getName())
                .description(record.getDescription())
                // record.getOwner() is LAZY — accessing it here triggers a DB load.
                // This is fine because we're inside a @Transactional service method.
                .ownerUsername(record.getOwner().getUsername())
                .currentVersionNumber(record.getCurrentVersionNumber())
                .currentVersionId(record.getCurrentVersionId())
                .currentContent(contentNode)
                .versionCount(versionCount)
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    /**
     * Parse a JSON string into a JsonNode.
     * Returns null if parsing fails (shouldn't happen — we validate on write).
     */
    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
