package com.sil.asset_tagging_system.dto.response;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Detailed employee information returned by
 * employee create, retrieve, and update APIs.
 *
 * Password information is intentionally excluded.
 */
public record EmployeeResponse(

        Long id,

        String firstName,

        String lastName,

        String fullName,

        String email,

        Long departmentId,

        String departmentName,

        Boolean enabled,

        List<String> roles,

        LocalDateTime createdAt

) {
}