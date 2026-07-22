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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmployeeMapper employeeMapper;
    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("createEmployee should save employee successfully when request is valid")
    void createEmployee_Success() {
        //Arrange
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Mostafiz", "Fahim", "mostafizfahim@test.com", "Password123!", 1L
        );
        Department dept = Department.builder().id(1L).name("IT").build();
        Role role = Role.builder().id(1L).name(RoleName.ROLE_EMPLOYEE).build();

        User savedUser = User.builder()
                .id(10L)
                .firstName("Mostafiz")
                .lastName("Fahim")
                .email("mostafizfahim@test.com")
                .department(dept)
                .roles(Set.of(role))
                .build();

        EmployeeResponse expectedResponse = new EmployeeResponse(
                10L, "Mostafiz", "Fahim", "Mostafiz Fahim", "mostafizfahim@test.com",
                1L, "IT", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now()
        );

        when(userRepository.existsByEmailIgnoreCase("mostafizfahim@test.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(roleRepository.findByName(RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(employeeMapper.toResponse(savedUser)).thenReturn(expectedResponse);
        // Act
        EmployeeResponse actualResponse = employeeService.createEmployee(request);
        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.id()).isEqualTo(10L);
        assertThat(actualResponse.email()).isEqualTo("mostafizfahim@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createEmployee should throw DuplicateResourceException when email exists")
    void createEmployee_DuplicateEmail_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = new CreateEmployeeRequest("Mostafiz", "Fahim", "mostafizfahim@test.com", "Password123!", 1L);
        when(userRepository.existsByEmailIgnoreCase("mostafizfahim@test.com")).thenReturn(true);
        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee should throw ResourceNotFoundException when department is missing")
    void createEmployee_DepartmentNotFound_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = new CreateEmployeeRequest("Mostafiz", "Fahim", "mostafizfahim@test.com", "Password123!", 99L);
        when(userRepository.existsByEmailIgnoreCase("mostafizfahim@test.com")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getEmployeeById should return EmployeeResponse when found")
    void getEmployeeById_Success() {
        // Arrange
        User user = User.builder().id(10L).firstName("Mostafiz").lastName("Fahim").email("mostafizfahim@test.com").build();
        EmployeeResponse expected = new EmployeeResponse(10L, "Mostafiz", "Fahim", "Mostafiz Fahim", "mostafizfahim@test.com", 1L, "IT", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now());
        when(userRepository.findByIdAndRoleName(10L, RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(user));
        when(employeeMapper.toResponse(user)).thenReturn(expected);
        // Act
        EmployeeResponse response = employeeService.getEmployeeById(10L);
        // Assert
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("getEmployeeById should throw ResourceNotFoundException when employee does not exist")
    void getEmployeeById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByIdAndRoleName(99L, RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAllEmployees should return PageResponse of EmployeeSummaryResponse")
    void getAllEmployees_Success() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .firstName("Mostafiz")
                .lastName("Fahim")
                .email("mostafizfahim@test.com")
                .build();
        EmployeeSummaryResponse summary = new EmployeeSummaryResponse(
                1L, "Mostafiz Fahim",
                "mostafizfahim@test.com",
                1L, "IT",
                true, LocalDateTime.now()
        );
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.findEmployees(eq(RoleName.ROLE_EMPLOYEE), any(), any(), any(), any(Pageable.class))).thenReturn(userPage);
        when(employeeMapper.toSummaryResponse(user)).thenReturn(summary);
        // Act
        PageResponse<EmployeeSummaryResponse> response = employeeService.getAllEmployees(0, 10, null, null, null, "createdAt", "desc");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).email()).isEqualTo("mostafizfahim@test.com");
    }

    @Test
    @DisplayName("updateEmployee should update managed entity and return response")
    void updateEmployee_Success() {
        // Arrange
        User existingUser = User.builder().id(5L).firstName("Old").lastName("Name").email("old@test.com").build();
        Department newDept = Department.builder().id(2L).name("HR").build();
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("NewFirst", "NewLast", "new@test.com", 2L);
        EmployeeResponse expected = new EmployeeResponse(5L, "NewFirst", "NewLast", "NewFirst NewLast", "new@test.com", 2L, "HR", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now());

        when(userRepository.findByIdAndRoleName(5L, RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@test.com", 5L)).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(employeeMapper.toResponse(existingUser)).thenReturn(expected);
        // Act
        EmployeeResponse response = employeeService.updateEmployee(5L, request);
        // Assert
        assertThat(response).isNotNull();
        assertThat(existingUser.getFirstName()).isEqualTo("NewFirst");
        assertThat(existingUser.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("updateEmployeeStatus should change enabled flag")
    void updateEmployeeStatus_Success() {
        // Arrange
        User existingUser = User.builder().id(5L).enabled(true).build();
        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(false);
        EmployeeResponse expected = new EmployeeResponse(5L, "F", "L", "F L", "e@test.com", 1L, "IT", false, List.of("ROLE_EMPLOYEE"), LocalDateTime.now());
        when(userRepository.findByIdAndRoleName(5L, RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(existingUser));
        when(employeeMapper.toResponse(existingUser)).thenReturn(expected);
        // Act
        EmployeeResponse response = employeeService.updateEmployeeStatus(5L, request);
        // Assert
        assertThat(existingUser.getEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("resetEmployeePassword should encode and update user password")
    void resetEmployeePassword_Success() {
        // Arrange
        User existingUser = User.builder().id(5L).password("oldEncodedPass").build();
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("NewSecret123!");
        when(userRepository.findByIdAndRoleName(5L, RoleName.ROLE_EMPLOYEE)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("newEncodedPass");
        // Act
        employeeService.resetEmployeePassword(5L, request);
        // Assert
        assertThat(existingUser.getPassword()).isEqualTo("newEncodedPass");
    }


}

