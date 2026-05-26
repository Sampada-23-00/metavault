package com.sampada.metavault.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MetadataRecord is the "file" in our version control analogy.
 * Think of it like a Git repository — it's the container.
 * The actual content lives in MetadataVersion (the "commits").
 *
 * Key design decisions:
 *
 * 1. SOFT DELETE: We never DELETE rows. We set isDeleted=true and
 *    filter them out in all queries. This preserves history (important
 *    for audit) and allows recovery.
 *
 * 2. currentVersionId: We store the UUID of the latest MetadataVersion
 *    as a plain UUID column, NOT as a JPA @OneToOne relationship.
 *    Why? Because MetadataVersion also has a FK back to MetadataRecord,
 *    creating a circular reference. Storing it as a raw UUID avoids
 *    infinite lazy-loading loops and circular save order problems.
 *    The service layer does the lookup when needed.
 *
 * 3. @ManyToOne owner: Many records can belong to one user.
 *    FetchType.LAZY means the User is NOT loaded from DB unless you
 *    access record.getOwner() — this avoids expensive JOINs when
 *    you only need the record metadata.
 */
@Entity
@Table(name = "metadata_records", indexes = {
    @Index(name = "idx_record_owner", columnList = "owner_id"),
    @Index(name = "idx_record_deleted", columnList = "is_deleted")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * @ManyToOne: many MetadataRecords belong to one User.
     * @JoinColumn: the FK column in this table is called "owner_id".
     * FetchType.LAZY: don't load the User object until it's actually accessed.
     *   (FetchType.EAGER would JOIN the users table in every query — wasteful.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Stores the UUID of the current (latest) version.
     * Plain UUID field — service layer resolves this to a MetadataVersion object.
     * This is updated every time a new version is saved.
     */
    @Column(name = "current_version_id")
    private UUID currentVersionId;

    /**
     * The version number of the current version (e.g., 3 for v3).
     * Stored here as a convenience so we don't always need to JOIN to versions.
     */
    @Column(name = "current_version_number")
    @Builder.Default
    private Integer currentVersionNumber = 0;

    /**
     * Soft delete flag. Default false = record is active.
     * @Builder.Default is needed when using Lombok's @Builder —
     * without it, the builder would set this to null instead of false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * @PreUpdate runs automatically just before any UPDATE SQL is executed.
     * This keeps updatedAt current without any service-layer code.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
