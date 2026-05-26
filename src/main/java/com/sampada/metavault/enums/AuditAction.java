package com.sampada.metavault.enums;

/**
 * Every meaningful action on a record gets logged with one of these labels.
 * This is the "what happened" part of an audit log entry.
 *
 * Think of it like Git's operation types: commit (CREATE), push (UPDATE),
 * revert (RESTORE), rm (DELETE). Having an enum makes audit reports
 * filterable and type-safe.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    RESTORE,
    DELETE
}
