package com.sil.asset_tagging_system.dto;

import java.time.LocalDateTime;

public record ActivityLogRow(
        Long id,
        LocalDateTime occurredAt,
        String correlationId,
        Short sequenceInAction,
        Long actorUserId,
        String actorName,
        String actorRoles,
        String ipAddress,
        String action,
        String entityType,
        String outcome,
        String failureReason,
        Long assetId,
        String assetTag,
        Long approvalId,
        Long subjectUserId,
        String subjectName,
        String previousHolderName,
        String newHolderName,
        String previousCondition,
        String newCondition,
        String summary,
        String details
) {

    public boolean refused() {
        return "DENIED".equals(outcome) || "FAILED".equals(outcome);
    }

    public String change() {
        if (previousCondition != null || newCondition != null) {
            return orDash(previousCondition) + "  →  " + orDash(newCondition);
        }
        if (previousHolderName != null || newHolderName != null) {
            return orDash(previousHolderName) + "  →  " + orDash(newHolderName);
        }
        return "";
    }
    
    public String target() {
        if (assetTag != null) {
            return assetTag;
        }
        if (subjectName != null) {
            return subjectName;
        }
        if (approvalId != null) {
            return "Request #" + approvalId;
        }
        return "";
    }

    public String shortCorrelation() {
        return (correlationId == null || correlationId.length() < 8) ? correlationId : correlationId.substring(0, 8);
    }

    public String actorLabel() {
        if (actorName != null) {
            return actorName;
        }
        return actorUserId != null ? ("User " + actorUserId) : "—";
    }

    private static String orDash(String value) {
        return value == null ? "—" : value;
    }
}
