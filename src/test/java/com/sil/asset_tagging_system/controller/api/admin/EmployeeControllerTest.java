package com.sil.asset_tagging_system.controller.api.admin;

import com.sil.asset_tagging_system.dto.request.CreateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.ResetEmployeePasswordRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeStatusRequest;
import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.dto.response.PageResponse;
import com.sil.asset_tagging_system.service.EmployeeService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    @DisplayName("POST /api/admin/employees should return 201 Created and Location header")
    void createEmployee_ValidRequest_Returns201Created() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Mostafiz", "Fahim", "mostafiz@test.com", "Password123!", 1L
        );

        EmployeeResponse response = new EmployeeResponse(
                10L, "Mostafiz", "Fahim", "Mostafiz Fahim", "mostafiz@test.com",
                1L, "Engineering", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now()
        );

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/admin/employees/10")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Employee created successfully."))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.email").value("mostafiz@test.com"));
    }

    @Test
    @DisplayName("GET /api/admin/employees should return 200 OK with paginated response")
    void getAllEmployees_ValidParams_Returns200OK() throws Exception {
        EmployeeSummaryResponse summary = new EmployeeSummaryResponse(
                10L, "Mostafiz Fahim", "mostafiz@test.com", 1L, "Engineering", true, LocalDateTime.now()
        );

        PageResponse<EmployeeSummaryResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(summary))
        );

        when(employeeService.getAllEmployees(eq(0), eq(10), any(), any(), any(), eq("createdAt"), eq("desc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/employees")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("mostafiz@test.com"));
    }

    @Test
    @DisplayName("GET /api/admin/employees/{id} should return 200 OK when found")
    void getEmployeeById_ValidId_Returns200OK() throws Exception {
        EmployeeResponse response = new EmployeeResponse(
                10L, "Mostafiz", "Fahim", "Mostafiz Fahim", "mostafiz@test.com",
                1L, "Engineering", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now()
        );

        when(employeeService.getEmployeeById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/employees/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.firstName").value("Mostafiz"));
    }

    @Test
    @DisplayName("PUT /api/admin/employees/{id} should return 200 OK on valid update")
    void updateEmployee_ValidPayload_Returns200OK() throws Exception {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("NewFirst", "NewLast", "new@test.com", 2L);

        EmployeeResponse response = new EmployeeResponse(
                10L, "NewFirst", "NewLast", "NewFirst NewLast", "new@test.com",
                2L, "HR", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now()
        );

        when(employeeService.updateEmployee(eq(10L), any(UpdateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/employees/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("NewFirst"))
                .andExpect(jsonPath("$.data.departmentName").value("HR"));
    }

    @Test
    @DisplayName("PATCH /api/admin/employees/{id}/status should return 200 OK")
    void updateEmployeeStatus_EnableAccount_Returns200OK() throws Exception {
        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(true);

        EmployeeResponse response = new EmployeeResponse(
                10L, "Mostafiz", "Fahim", "Mostafiz Fahim", "mostafiz@test.com",
                1L, "Engineering", true, List.of("ROLE_EMPLOYEE"), LocalDateTime.now()
        );

        when(employeeService.updateEmployeeStatus(eq(10L), any(UpdateEmployeeStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/employees/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Employee account enabled successfully."));
    }

    @Test
    @DisplayName("PATCH /api/admin/employees/{id}/password should return 204 No Content")
    void resetEmployeePassword_ValidPassword_Returns204NoContent() throws Exception {
        ResetEmployeePasswordRequest request = new ResetEmployeePasswordRequest("NewSecret123!");

        doNothing().when(employeeService).resetEmployeePassword(eq(10L), any(ResetEmployeePasswordRequest.class));

        mockMvc.perform(patch("/api/admin/employees/10/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
