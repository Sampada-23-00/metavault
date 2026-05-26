package com.sampada.metavault.exception;

/**
 * Thrown when a requested resource (record, version, user) doesn't exist.
 * The GlobalExceptionHandler catches this and returns HTTP 404.
 *
 * extends RuntimeException: we use unchecked exceptions in Spring apps.
 * Unlike checked exceptions, we don't have to declare "throws" on every method —
 * Spring's exception handler catches them at the top level automatically.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience method: ResourceNotFoundException.of("MetadataRecord", id)
    // produces: "MetadataRecord not found with id: <uuid>"
    public static ResourceNotFoundException of(String resourceName, Object id) {
        return new ResourceNotFoundException(resourceName + " not found with id: " + id);
    }
}
