package com.sil.asset_tagging_system.service.impl;

import com.sil.asset_tagging_system.dto.request.CreateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.ResetEmployeePasswordRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeStatusRequest;

import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.dto.response.PageResponse;

import com.sil.asset_tagging_system.exception.DuplicateResourceException;
import com.sil.asset_tagging_system.exception.ResourceNotFoundException;

import com.sil.asset_tagging_system.mapper.EmployeeMapper;

import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;

import com.sil.asset_tagging_system.model.enums.RoleName;

import com.sil.asset_tagging_system.repository.DepartmentRepository;
import com.sil.asset_tagging_system.repository.RoleRepository;
import com.sil.asset_tagging_system.repository.UserRepository;

import com.sil.asset_tagging_system.service.EmployeeService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


/**
 * Implements administrator-managed employee operations.
 * <p>
 * Responsibilities:
 * <p>
 * - employee creation
 * - automatic ROLE_EMPLOYEE assignment
 * - BCrypt password encoding
 * - employee retrieval
 * - search, filtering, pagination, and sorting
 * - employee profile updates
 * - employee account activation and deactivation
 * - employee password reset
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl
        implements EmployeeService {


    // ========================================================
    // DEPENDENCIES
    // ========================================================

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmployeeMapper employeeMapper;


    @Override
    public EmployeeResponse createEmployee(
            CreateEmployeeRequest request
    ) {

        String normalizedEmail = normalizeEmail(
                request.email()
        );

        validateEmailIsAvailable(
                normalizedEmail
        );

        Department department = findDepartmentById(
                request.departmentId()
        );

        Role employeeRole = findEmployeeRole();

        User employee = new User();

        employee.setFirstName(
                normalizeName(
                        request.firstName()
                )
        );

        employee.setLastName(
                normalizeName(
                        request.lastName()
                )
        );

        employee.setEmail(
                normalizedEmail
        );

        employee.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        employee.setDepartment(
                department
        );

        employee.setEnabled(
                true
        );

        employee.setRoles(
                new HashSet<>(
                        Set.of(
                                employeeRole
                        )
                )
        );

        User savedEmployee = userRepository.save(
                employee
        );

        return employeeMapper.toResponse(
                savedEmployee
        );
    }


    @Override
    @Transactional(
            readOnly = true
    )
    public EmployeeResponse getEmployeeById(
            Long employeeId
    ) {

        User employee = findEmployeeById(
                employeeId
        );

        return employeeMapper.toResponse(
                employee
        );
    }

    @Override
    @Transactional(
            readOnly = true
    )
    public PageResponse<EmployeeSummaryResponse> getAllEmployees(

            int page,

            int size,

            String search,

            Long departmentId,

            Boolean enabled,

            String sortBy,

            String direction
    ) {

        String normalizedSearch = normalizeOptionalSearch(
                search
        );

        Sort sort = createSort(
                sortBy,
                direction
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        Page<User> employeePage =
                userRepository.findEmployees(

                        RoleName.ROLE_EMPLOYEE,

                        normalizedSearch,

                        departmentId,

                        enabled,

                        pageable
                );

        Page<EmployeeSummaryResponse> responsePage =
                employeePage.map(
                        employeeMapper::toSummaryResponse
                );

        return PageResponse.from(
                responsePage
        );
    }

    @Override
    public EmployeeResponse updateEmployee(

            Long employeeId,

            UpdateEmployeeRequest request
    ) {

        User employee = findEmployeeById(
                employeeId
        );

        String normalizedEmail = normalizeEmail(
                request.email()
        );

        validateEmailIsAvailableForUpdate(
                normalizedEmail,
                employeeId
        );

        Department department = findDepartmentById(
                request.departmentId()
        );

        employee.setFirstName(
                normalizeName(
                        request.firstName()
                )
        );

        employee.setLastName(
                normalizeName(
                        request.lastName()
                )
        );

        employee.setEmail(
                normalizedEmail
        );

        employee.setDepartment(
                department
        );

        User updatedEmployee = userRepository.save(
                employee
        );

        return employeeMapper.toResponse(
                updatedEmployee
        );
    }

    @Override
    public EmployeeResponse updateEmployeeStatus(

            Long employeeId,

            UpdateEmployeeStatusRequest request
    ) {

        User employee = findEmployeeById(
                employeeId
        );

        employee.setEnabled(
                request.enabled()
        );

        User updatedEmployee = userRepository.save(
                employee
        );

        return employeeMapper.toResponse(
                updatedEmployee
        );
    }

    @Override
    public void resetEmployeePassword(

            Long employeeId,

            ResetEmployeePasswordRequest request
    ) {

        User employee = findEmployeeById(
                employeeId
        );

        String encodedPassword =
                passwordEncoder.encode(
                        request.newPassword()
                );

        employee.setPassword(
                encodedPassword
        );

        userRepository.save(
                employee
        );
    }

    private User findEmployeeById(
            Long employeeId
    ) {

        return userRepository
                .findByIdAndRoleName(

                        employeeId,

                        RoleName.ROLE_EMPLOYEE
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Employee",
                                        "ID",
                                        employeeId
                                )
                );
    }

    private Department findDepartmentById(
            Long departmentId
    ) {

        return departmentRepository
                .findById(
                        departmentId
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Department",
                                        "ID",
                                        departmentId
                                )
                );
    }

    private Role findEmployeeRole() {

        return roleRepository
                .findByName(
                        RoleName.ROLE_EMPLOYEE
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Role",
                                        "name",
                                        RoleName.ROLE_EMPLOYEE
                                )
                );
    }

    private void validateEmailIsAvailable(
            String email
    ) {

        boolean emailExists =
                userRepository
                        .existsByEmailIgnoreCase(
                                email
                        );

        if (emailExists) {

            throw new DuplicateResourceException(
                    "Employee",
                    "email",
                    email
            );
        }
    }

    private void validateEmailIsAvailableForUpdate(

            String email,

            Long employeeId
    ) {

        boolean emailExists =
                userRepository
                        .existsByEmailIgnoreCaseAndIdNot(

                                email,

                                employeeId
                        );

        if (emailExists) {

            throw new DuplicateResourceException(
                    "Employee",
                    "email",
                    email
            );
        }
    }

    private String normalizeEmail(
            String email
    ) {

        return email
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeName(
            String name
    ) {

        return name.trim();
    }

    private String normalizeOptionalSearch(
            String search
    ) {

        if (
                search == null
                        || search.isBlank()
        ) {

            return null;
        }

        return search.trim();
    }

    private static final Set<String>
            ALLOWED_SORT_FIELDS = Set.of(

            "id",
            "firstName",
            "lastName",
            "email",
            "enabled",
            "createdAt"
    );

    private Sort createSort(

            String sortBy,

            String direction
    ) {

        String safeSortField =
                ALLOWED_SORT_FIELDS.contains(
                        sortBy
                )
                        ? sortBy
                        : "createdAt";

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(
                        direction
                )
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return Sort.by(
                sortDirection,
                safeSortField
        );
    }

}