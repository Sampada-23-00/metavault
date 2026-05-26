package com.sampada.metavault.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response for GET /api/records/{id}/diff?from=2&to=5
 *
 * Example response:
 * {
 *   "fromVersion": 2,
 *   "toVersion":   5,
 *   "added":   { "newFeature": true },
 *   "removed": { "oldSetting": "deprecated" },
 *   "changed": { "timeout": { "from": 30, "to": 60 } }
 * }
 */
@Data
@Builder
public class DiffResponse {
    private int fromVersion;
    private int toVersion;
    private Map<String, Object> added;
    private Map<String, Object> removed;
    private Map<String, Object> changed;
}
