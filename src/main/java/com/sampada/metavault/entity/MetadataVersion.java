package com.sampada.metavault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MetadataVersion is the "commit" in our version control analogy.
 * Each row is an immutable snapshot of a record's content at a point in time.
 *
 * IMMUTABILITY RULE: Once a version is created, its content is NEVER updated.
 * - `content` column is NOT updatable (see @Column annotation below)
 * - `versionNumber` is NOT updatable
 * - `createdAt` is NOT updatable
 *
 * This guarantees the version history is a true audit trail — like Git commits,
 * you can always go back and see exactly what the content was at version N.
 *
 * VERSION NUMBERING: versionNumber is scoped per record.
 * - Record A can have v1, v2, v3
 * - Record B independently has v1, v2
 * - The service layer computes: nextVersion = max(existing versions) + 1
 *
 * RESTORE FLOW: Restoring v2 doesn't roll back the DB. Instead, we read v2's
 * content and create a NEW version (e.g., v4) with that content and a message
 * like "Restored from v2". History stays linear and immutable.
 */
@Entity
@Table(name = "metadata_versions", indexes = {
    @Index(name = "idx_version_record", columnList = "record_id"),
    @Index(name = "idx_version_record_number", columnList = "record_id, version_number", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * FK to MetadataRecord. Many versions belong to one record.
     * updatable=false on @JoinColumn: once this version is associated
     * with a record, that association never changes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false, updatable = false)
    private MetadataRecord record;

    /**
     * The sequential version number for THIS record (1, 2, 3...).
     * updatable=false: version numbers are immutable once set.
     */
    @Column(name = "version_number", nullable = false, updatable = false)
    private Integer versionNumber;

    /**
     * The actual JSON content stored as TEXT.
     * Why TEXT and not JSONB (PostgreSQL's native JSON type)?
     * - TEXT works with both PostgreSQL AND H2 (our local dev DB)
     * - JSONB requires PostgreSQL-specific column definitions
     * - We parse it with Jackson in the service layer anyway
     * In a pure production PostgreSQL setup, JSONB would be better
     * (it validates JSON syntax and enables JSON path queries).
     */
    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Like a Git commit message — describes WHY this version was created.
     * Examples: "Updated timeout settings", "Restored from v2", "Initial version"
     */
    @Column(name = "change_message", length = 500, updatable = false)
    private String changeMessage;

    /**
     * Which user created this version. Different from the record owner —
     * in a team setting, a collaborator could update someone else's record.
     * (For now, only the owner can update their records, but the schema is future-ready.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
