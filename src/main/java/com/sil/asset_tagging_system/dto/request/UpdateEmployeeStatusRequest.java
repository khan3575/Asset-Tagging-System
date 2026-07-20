package com.sil.asset_tagging_system.dto.request;

import jakarta.validation.constraints.NotNull;


/**
 * Contains the requested employee-account status.
 */
public record UpdateEmployeeStatusRequest(

        @NotNull(
                message = "Employee account status is required."
        )
        Boolean enabled

) {
}