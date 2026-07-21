package com.sil.asset_tagging_system.controller.api.admin;

import com.sil.asset_tagging_system.dto.request.CreateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.ResetEmployeePasswordRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeStatusRequest;
import com.sil.asset_tagging_system.dto.response.ApiResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.dto.response.PageResponse;
import com.sil.asset_tagging_system.service.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {

        EmployeeResponse employee = employeeService.createEmployee(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(employee.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(ApiResponse.success("Employee created successfully.", employee));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> getAllEmployees(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number cannot be negative.") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Page size must be at least 1.")
            @Max(value = 100, message = "Page size cannot exceed 100.") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @Positive(message = "Department ID must be greater than zero.") Long departmentId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        PageResponse<EmployeeSummaryResponse> employees = employeeService.getAllEmployees(
                page, size, search, departmentId, enabled, sortBy, direction);

        return ResponseEntity.ok(
                ApiResponse.success("Employees retrieved successfully.", employees));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(
            @PathVariable
            @Positive(message = "Employee ID must be greater than zero.") Long employeeId) {

        EmployeeResponse employee = employeeService.getEmployeeById(employeeId);

        return ResponseEntity.ok(
                ApiResponse.success("Employee retrieved successfully.", employee));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable
            @Positive(message = "Employee ID must be greater than zero.") Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {

        EmployeeResponse employee = employeeService.updateEmployee(employeeId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Employee updated successfully.", employee));
    }

    @PatchMapping("/{employeeId}/status")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployeeStatus(
            @PathVariable
            @Positive(message = "Employee ID must be greater than zero.") Long employeeId,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {

        EmployeeResponse employee = employeeService.updateEmployeeStatus(employeeId, request);

        String message = request.enabled()
                ? "Employee account enabled successfully."
                : "Employee account disabled successfully.";

        return ResponseEntity.ok(ApiResponse.success(message, employee));
    }

    @PatchMapping("/{employeeId}/password")
    public ResponseEntity<Void> resetEmployeePassword(
            @PathVariable
            @Positive(message = "Employee ID must be greater than zero.") Long employeeId,
            @Valid @RequestBody ResetEmployeePasswordRequest request) {

        employeeService.resetEmployeePassword(employeeId, request);

        return ResponseEntity.noContent().build();
    }
}