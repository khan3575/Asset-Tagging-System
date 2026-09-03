package com.sil.asset_tagging_system.dto;

import java.time.LocalDateTime;

import com.sil.asset_tagging_system.model.enums.RequestType;

public record ApprovalRow(
    Long id,
    String assetTag,
    String assetName,
    Long requesterId,
    String requesterFirstName,
    String requesterLastName,
    Long previousHolderId,
    String previousHolderFirstName,
    String previousHolderLastName,
    String requestType,
    String status,
    Byte requiredApprovalCount,
    LocalDateTime requestedAt
) {
    public boolean isSubject(Long userId) {
        Long subject = RequestType.RETURN.name().equals(requestType)
                ? previousHolderId
                : requesterId;
        return subject != null && subject.equals(userId);
    }
}
