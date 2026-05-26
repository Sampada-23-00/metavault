package com.sampada.metavault.repository;

import com.sampada.metavault.entity.MetadataRecord;
import com.sampada.metavault.entity.MetadataVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MetadataVersion entities.
 *
 * This is the heart of the versioning system. The key operations are:
 * 1. Find what the next version number should be (max + 1)
 * 2. Get version history (newest first, paginated)
 * 3. Get a specific version by number (for diff and restore)
 */
public interface MetadataVersionRepository extends JpaRepository<MetadataVersion, UUID> {

    /**
     * Get full version history for a record, newest first.
     * Returns Page<MetadataVersion> so the API can paginate large histories.
     *
     * "OrderByVersionNumberDesc" → ORDER BY version_number DESC
     */
    Page<MetadataVersion> findByRecordOrderByVersionNumberDesc(MetadataRecord record, Pageable pageable);

    /**
     * Fetch a specific version by its number.
     * Used by:
     *   - GET /records/{id}/versions/{versionNumber}
     *   - GET /records/{id}/diff?from=2&to=5
     *   - POST /records/{id}/restore/{versionNumber}
     */
    Optional<MetadataVersion> findByRecordAndVersionNumber(MetadataRecord record, Integer versionNumber);

    /**
     * Find the highest version number for a record.
     * JPQL: SELECT MAX(v.versionNumber) FROM MetadataVersion v WHERE v.record = :record
     *
     * Returns Optional<Integer> because a brand-new record has 0 versions,
     * so MAX() returns null — Optional handles that gracefully.
     *
     * Usage in service: nextVersionNumber = findMaxVersionNumber(record).orElse(0) + 1
     */
    @Query("SELECT MAX(v.versionNumber) FROM MetadataVersion v WHERE v.record = :record")
    Optional<Integer> findMaxVersionNumberByRecord(@Param("record") MetadataRecord record);

    /**
     * Count total versions for a record.
     * Used in responses to show "this record has N versions".
     */
    long countByRecord(MetadataRecord record);
}
