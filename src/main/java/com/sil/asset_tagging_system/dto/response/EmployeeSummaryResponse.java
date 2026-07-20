package com.sil.asset_tagging_system.dto.response;

import java.time.LocalDateTime;


/**
 * Lightweight employee information returned
 * by employee list and search APIs.
 */
public record EmployeeSummaryResponse(

        Long id,

        String fullName,

        String email,

        Long departmentId,

        String departmentName,

        Boolean enabled,

        LocalDateTime createdAt

) {
}