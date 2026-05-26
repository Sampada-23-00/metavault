package com.sampada.metavault.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampada.metavault.dto.DiffResponse;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DiffUtil computes the difference between two JSON objects.
 *
 * HOW IT WORKS:
 * We use Jackson's JsonNode to walk both JSON trees and compare field by field.
 *
 * Given: fromJson = {"timeout": 30, "debug": false, "host": "old.server.com"}
 *          toJson = {"timeout": 60, "debug": false, "retries": 3}
 *
 * Result:
 *   added:   { "retries": 3 }               ← in toJson, not in fromJson
 *   removed: { "host": "old.server.com" }   ← in fromJson, not in toJson
 *   changed: { "timeout": {"from":30,"to":60} }  ← in both but different value
 *
 * NOTE: This is a SHALLOW diff — it compares top-level keys only.
 * Nested objects are compared by identity (if the object changed at all,
 * the whole object appears in "changed"). This is sufficient for configs.
 * A deep recursive diff would be much more complex (see json-patch RFC 6902).
 *
 * This is a utility class (no instances, just static methods).
 */
public final class DiffUtil {

    // ObjectMapper is thread-safe and expensive to create — make it a constant
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Private constructor — prevents instantiation of utility class
    private DiffUtil() {}

    /**
     * Compute the diff between two JSON strings.
     *
     * @param fromJson  the "before" JSON string
     * @param toJson    the "after" JSON string
     * @param fromVersion  version number for "before" (for the response label)
     * @param toVersion    version number for "after"
     * @return DiffResponse with added, removed, changed maps
     */
    public static DiffResponse diff(String fromJson, String toJson,
                                    int fromVersion, int toVersion) {
        try {
            JsonNode fromNode = MAPPER.readTree(fromJson);
            JsonNode toNode   = MAPPER.readTree(toJson);

            Map<String, Object> added   = new LinkedHashMap<>();
            Map<String, Object> removed = new LinkedHashMap<>();
            Map<String, Object> changed = new LinkedHashMap<>();

            // ── Pass 1: Walk toNode fields ─────────────────────────────────────
            // For each key in the "after" JSON:
            //   - If it doesn't exist in "before" → it was ADDED
            //   - If it exists but has a different value → it was CHANGED
            Iterator<Map.Entry<String, JsonNode>> toFields = toNode.fields();
            while (toFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = toFields.next();
                String key    = entry.getKey();
                JsonNode toVal = entry.getValue();
                JsonNode fromVal = fromNode.get(key);

                if (fromVal == null) {
                    // Key exists in "to" but not in "from" → added
                    added.put(key, toVal);
                } else if (!fromVal.equals(toVal)) {
                    // Key exists in both but values differ → changed
                    Map<String, Object> change = new LinkedHashMap<>();
                    change.put("from", fromVal);
                    change.put("to", toVal);
                    changed.put(key, change);
                }
                // If fromVal.equals(toVal) → unchanged, skip it
            }

            // ── Pass 2: Walk fromNode fields ───────────────────────────────────
            // For each key in the "before" JSON:
            //   - If it doesn't exist in "after" → it was REMOVED
            Iterator<Map.Entry<String, JsonNode>> fromFields = fromNode.fields();
            while (fromFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fromFields.next();
                String key = entry.getKey();
                if (toNode.get(key) == null) {
                    // Key exists in "from" but not in "to" → removed
                    removed.put(key, entry.getValue());
                }
            }

            return DiffResponse.builder()
                    .fromVersion(fromVersion)
                    .toVersion(toVersion)
                    .added(added)
                    .removed(removed)
                    .changed(changed)
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compute diff: " + e.getMessage(), e);
        }
    }
}
