package com.sampada.metavault.repository;

import com.sampada.metavault.entity.MetadataRecord;
import com.sampada.metavault.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MetadataRecord entities.
 *
 * Notice the pattern: every query includes `isDeleted = false`.
 * This is how we enforce soft-delete — deleted records are invisible
 * to all normal queries. Only an admin recovery operation would need
 * to query with isDeleted = true.
 *
 * PAGINATION: Page<T> + Pageable is Spring Data's pagination system.
 * The caller passes a Pageable object (page number, page size, sort order)
 * and gets back a Page<T> containing:
 *   - The records for that page
 *   - Total count (for the "X of Y pages" UI)
 *   - Metadata about page number, size, etc.
 * The SQL becomes: SELECT ... LIMIT ? OFFSET ?
 */
public interface MetadataRecordRepository extends JpaRepository<MetadataRecord, UUID> {

    /**
     * List all active (non-deleted) records for a specific user.
     * Spring Data generates: SELECT * FROM metadata_records
     *                        WHERE owner_id = ? AND is_deleted = false
     *                        ORDER BY ... LIMIT ... OFFSET ...
     */
    Page<MetadataRecord> findByOwnerAndIsDeletedFalse(User owner, Pageable pageable);

    /**
     * Search records by name (case-insensitive substring match).
     *
     * @Query uses JPQL (Java Persistence Query Language) — like SQL but
     * written against entity class names and field names, not table/column names.
     * Hibernate translates it to real SQL at runtime.
     *
     * LOWER() makes it case-insensitive.
     * %:name% means "contains name anywhere" (like SQL LIKE '%name%').
     *
     * :owner and :name are named parameters — bound to method arguments
     * via @Param.
     */
    @Query("SELECT r FROM MetadataRecord r WHERE r.owner = :owner " +
           "AND r.isDeleted = false " +
           "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<MetadataRecord> searchByOwnerAndName(
            @Param("owner") User owner,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Get a specific record by ID, only if it belongs to this owner and isn't deleted.
     * Returns Optional.empty() if:
     *   - Record doesn't exist
     *   - Record belongs to someone else (security enforcement in the repo layer)
     *   - Record is soft-deleted
     */
    Optional<MetadataRecord> findByIdAndOwnerAndIsDeletedFalse(UUID id, User owner);

    /**
     * Check if a record exists and belongs to a user — used for ownership validation.
     */
    boolean existsByIdAndOwnerAndIsDeletedFalse(UUID id, User owner);
}
