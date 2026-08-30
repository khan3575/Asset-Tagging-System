package com.sil.asset_tagging_system.dto;

import java.time.LocalDateTime;

public record ApprovalRow(
    Long id,
    String assetTag,
    String assetName,
    Long requesterId,
    String requesterFirstName,
    String requesterLastName,
    String status,
    Byte requiredApprovalCount,
    LocalDateTime requestedAt
) {
    
}
