package com.sil.asset_tagging_system.mapper;

import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EmployeeMapperTest {

    private EmployeeMapper employeeMapper;

    @BeforeEach
    void setUp() {
        employeeMapper = new EmployeeMapper();
    }

    @Test
    @DisplayName("toResponse should map all fields and sort roles correctly")
    void toResponse_ShouldMapAllFieldsCorrectly() {
        // Arrange
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .enabled(true)
                .build();

        Role employeeRole = Role.builder().id(1L).name(RoleName.ROLE_EMPLOYEE).build();
        Role adminRole = Role.builder().id(2L).name(RoleName.ROLE_ADMIN).build();

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .id(10L)
                .firstName("Mostafiz")
                .lastName("Fahim")
                .email("mostafiz.fahim@test.com")
                .department(department)
                .enabled(true)
                .roles(Set.of(employeeRole, adminRole))
                .createdAt(now)
                .build();

        // Act
        EmployeeResponse response = employeeMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.firstName()).isEqualTo("Mostafiz");
        assertThat(response.lastName()).isEqualTo("Fahim");
        assertThat(response.fullName()).isEqualTo("Mostafiz Fahim");
        assertThat(response.email()).isEqualTo("mostafiz.fahim@test.com");
        assertThat(response.departmentId()).isEqualTo(1L);
        assertThat(response.departmentName()).isEqualTo("Engineering");
        assertThat(response.enabled()).isTrue();
        assertThat(response.roles()).containsExactly("ROLE_ADMIN", "ROLE_EMPLOYEE"); // Sorted alphabetically
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toResponse should handle null department safely")
    void toResponse_ShouldHandleNullDepartmentSafely() {
        // Arrange
        User user = User.builder()
                .id(5L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .department(null)
                .enabled(true)
                .roles(Set.of())
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        EmployeeResponse response = employeeMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.departmentId()).isNull();
        assertThat(response.departmentName()).isNull();
    }

    @Test
    @DisplayName("toSummaryResponse should map summary fields correctly")
    void toSummaryResponse_ShouldMapSummaryFieldsCorrectly() {
        // Arrange
        Department department = Department.builder()
                .id(2L)
                .name("HR")
                .build();

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .id(20L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@test.com")
                .department(department)
                .enabled(true)
                .createdAt(now)
                .build();

        // Act
        EmployeeSummaryResponse summary = employeeMapper.toSummaryResponse(user);

        // Assert
        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(20L);
        assertThat(summary.fullName()).isEqualTo("Jane Smith");
        assertThat(summary.email()).isEqualTo("jane.smith@test.com");
        assertThat(summary.departmentId()).isEqualTo(2L);
        assertThat(summary.departmentName()).isEqualTo("HR");
        assertThat(summary.enabled()).isTrue();
        assertThat(summary.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toSummaryResponse should handle null department safely")
    void toSummaryResponse_ShouldHandleNullDepartmentSafely() {
        // Arrange
        User user = User.builder()
                .id(8L)
                .firstName("Bob")
                .lastName("Marley")
                .email("bob@test.com")
                .department(null)
                .enabled(false)
                .build();

        // Act
        EmployeeSummaryResponse summary = employeeMapper.toSummaryResponse(user);

        // Assert
        assertThat(summary).isNotNull();
        assertThat(summary.departmentId()).isNull();
        assertThat(summary.departmentName()).isNull();
        assertThat(summary.enabled()).isFalse();
    }
}
