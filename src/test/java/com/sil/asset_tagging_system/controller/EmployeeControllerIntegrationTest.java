package com.sil.asset_tagging_system.controller;

import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import com.sil.asset_tagging_system.dto.request.CreateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.ResetEmployeePasswordRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeStatusRequest;

import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;

import com.sil.asset_tagging_system.model.enums.RoleName;

import com.sil.asset_tagging_system.repository.DepartmentRepository;
import com.sil.asset_tagging_system.repository.RoleRepository;
import com.sil.asset_tagging_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Integration tests for administrator-managed
 * employee REST APIs.
 * <p>
 * These tests verify:
 * <p>
 * - controller routing
 * - request validation
 * - service business logic
 * - mapper output
 * - repository persistence
 * - JSON responses
 * - HTTP status codes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(
        username = "integration.admin@enterprise.com",
        roles = "ADMIN"
)
class EmployeeControllerIntegrationTest {


    // ========================================================
    // TEST DEPENDENCIES
    // ========================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // ========================================================
    // TEST DATA
    // ========================================================

    private Department engineeringDepartment;

    private Department financeDepartment;

    private Role employeeRole;

    private User existingEmployee;


    // ========================================================
    // TEST SETUP
    // ========================================================

    @BeforeEach
    void setUp() {

        engineeringDepartment =
                departmentRepository
                        .findById(1L)
                        .orElseGet(
                                () -> {

                                    Department department =
                                            new Department();

                                    department.setName(
                                            "Integration Engineering"
                                    );

                                    return departmentRepository.save(
                                            department
                                    );
                                }
                        );

        financeDepartment =
                departmentRepository
                        .findById(3L)
                        .orElseGet(
                                () -> {

                                    Department department =
                                            new Department();

                                    department.setName(
                                            "Integration Finance"
                                    );

                                    return departmentRepository.save(
                                            department
                                    );
                                }
                        );

        employeeRole =
                roleRepository
                        .findByName(
                                RoleName.ROLE_EMPLOYEE
                        )
                        .orElseGet(
                                () -> {

                                    Role role =
                                            new Role();

                                    role.setName(
                                            RoleName.ROLE_EMPLOYEE
                                    );

                                    return roleRepository.save(
                                            role
                                    );
                                }
                        );

        existingEmployee =
                createEmployeeEntity(

                        "Existing",

                        "Employee",

                        "existing.employee.integration@enterprise.com",

                        engineeringDepartment
                );
    }


    // ========================================================
    // CREATE EMPLOYEE
    // ========================================================

    @Test
    void createEmployee_shouldReturnCreatedEmployee()
            throws Exception {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest(

                        "Rahim",

                        "Ahmed",

                        "rahim.integration@enterprise.com",

                        "Password123!",

                        engineeringDepartment.getId()
                );

        mockMvc.perform(

                        post(
                                "/api/admin/employees"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        true
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "Employee created successfully."
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.firstName"
                        )
                                .value(
                                        "Rahim"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.lastName"
                        )
                                .value(
                                        "Ahmed"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.email"
                        )
                                .value(
                                        "rahim.integration@enterprise.com"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.departmentName"
                        )
                                .value(
                                        engineeringDepartment.getName()
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.enabled"
                        )
                                .value(
                                        true
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.roles",
                                hasItem(
                                        "ROLE_EMPLOYEE"
                                )
                        )
                )

                .andExpect(
                        jsonPath(
                                "$.data.password"
                        )
                                .doesNotExist()
                );
    }


    // ========================================================
    // GET EMPLOYEE BY ID
    // ========================================================

    @Test
    void getEmployeeById_shouldReturnEmployee()
            throws Exception {

        mockMvc.perform(

                        get(
                                "/api/admin/employees/{employeeId}",

                                existingEmployee.getId()
                        )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        true
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.id"
                        )
                                .value(
                                        existingEmployee.getId()
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.fullName"
                        )
                                .value(
                                        "Existing Employee"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.email"
                        )
                                .value(
                                        existingEmployee.getEmail()
                                )
                );
    }


    // ========================================================
    // GET ALL EMPLOYEES
    // ========================================================

    @Test
    void getAllEmployees_shouldReturnPaginatedEmployees()
            throws Exception {

        mockMvc.perform(

                        get(
                                "/api/admin/employees"
                        )

                                .param(
                                        "page",
                                        "0"
                                )

                                .param(
                                        "size",
                                        "10"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        true
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.content"
                        )
                                .isArray()
                )

                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        )
                                .isNumber()
                );
    }


    // ========================================================
    // SEARCH EMPLOYEES
    // ========================================================

    @Test
    void getAllEmployees_shouldSearchByEmail()
            throws Exception {

        mockMvc.perform(

                        get(
                                "/api/admin/employees"
                        )

                                .param(
                                        "search",
                                        "existing.employee.integration"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.data.content",
                                hasSize(
                                        1
                                )
                        )
                )

                .andExpect(
                        jsonPath(
                                "$.data.content[0].email"
                        )
                                .value(
                                        existingEmployee.getEmail()
                                )
                );
    }


    // ========================================================
    // UPDATE EMPLOYEE
    // ========================================================

    @Test
    void updateEmployee_shouldReturnUpdatedEmployee()
            throws Exception {

        UpdateEmployeeRequest request =
                new UpdateEmployeeRequest(

                        "Updated",

                        "Employee",

                        "updated.employee.integration@enterprise.com",

                        financeDepartment.getId()
                );

        mockMvc.perform(

                        put(
                                "/api/admin/employees/{employeeId}",

                                existingEmployee.getId()
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.data.fullName"
                        )
                                .value(
                                        "Updated Employee"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.email"
                        )
                                .value(
                                        "updated.employee.integration@enterprise.com"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.departmentId"
                        )
                                .value(
                                        financeDepartment.getId()
                                )
                );
    }


    // ========================================================
    // DISABLE EMPLOYEE
    // ========================================================

    @Test
    void updateEmployeeStatus_shouldDisableEmployee()
            throws Exception {

        UpdateEmployeeStatusRequest request =
                new UpdateEmployeeStatusRequest(
                        false
                );

        mockMvc.perform(

                        patch(
                                "/api/admin/employees/{employeeId}/status",

                                existingEmployee.getId()
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "Employee account disabled successfully."
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.enabled"
                        )
                                .value(
                                        false
                                )
                );
    }


    // ========================================================
    // RESET EMPLOYEE PASSWORD
    // ========================================================

    @Test
    void resetEmployeePassword_shouldUpdatePassword()
            throws Exception {

        ResetEmployeePasswordRequest request =
                new ResetEmployeePasswordRequest(
                        "NewPassword456!"
                );

        mockMvc.perform(

                        patch(
                                "/api/admin/employees/{employeeId}/password",

                                existingEmployee.getId()
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        true
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "Employee password reset successfully."
                                )
                );

        User updatedEmployee =
                userRepository
                        .findById(
                                existingEmployee.getId()
                        )
                        .orElseThrow();

        assert passwordEncoder.matches(

                "NewPassword456!",

                updatedEmployee.getPassword()
        );
    }


    // ========================================================
    // DUPLICATE EMAIL
    // ========================================================

    @Test
    void createEmployee_withDuplicateEmail_shouldReturnConflict()
            throws Exception {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest(

                        "Duplicate",

                        "Employee",

                        existingEmployee.getEmail(),

                        "Password123!",

                        engineeringDepartment.getId()
                );

        mockMvc.perform(

                        post(
                                "/api/admin/employees"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isConflict()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        false
                                )
                );
    }


    // ========================================================
    // INVALID REQUEST
    // ========================================================

    @Test
    void createEmployee_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest(

                        "",

                        "",

                        "invalid-email",

                        "123",

                        0L
                );

        mockMvc.perform(

                        post(
                                "/api/admin/employees"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        false
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.message"
                        )
                                .value(
                                        "Request validation failed."
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.data.email"
                        )
                                .exists()
                )

                .andExpect(
                        jsonPath(
                                "$.data.password"
                        )
                                .exists()
                );
    }


    // ========================================================
    // EMPLOYEE NOT FOUND
    // ========================================================

    @Test
    void getEmployeeById_whenNotFound_shouldReturnNotFound()
            throws Exception {

        mockMvc.perform(

                        get(
                                "/api/admin/employees/{employeeId}",

                                999999L
                        )
                )

                .andExpect(
                        status().isNotFound()
                )

                .andExpect(
                        jsonPath(
                                "$.success"
                        )
                                .value(
                                        false
                                )
                );
    }


    // ========================================================
    // TEST-ENTITY CREATION HELPER
    // ========================================================

    private User createEmployeeEntity(

            String firstName,

            String lastName,

            String email,

            Department department
    ) {

        User employee = new User();

        employee.setFirstName(
                firstName
        );

        employee.setLastName(
                lastName
        );

        employee.setEmail(
                email
        );

        employee.setPassword(
                passwordEncoder.encode(
                        "Password123!"
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

        return userRepository.saveAndFlush(
                employee
        );
    }

}