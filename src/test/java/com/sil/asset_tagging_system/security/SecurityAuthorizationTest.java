package com.sil.asset_tagging_system.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuthorizationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(roles="EMPLOYEE")
    void employeeCannotAccessAdminEndpoints() throws Exception{
        mockMvc.perform(get("/api/admin/asset")).andExpect(status().isForbidden());
        //this mock return 403 forbidden
    }

    @Test
    @WithMockUser(roles="ADMIN")
    public void adminCanAccessAdminEndPoints() throws Exception{
        mockMvc.perform(post("/api/admin/asset")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles="ADMIN")
    public void adminCanAccessEmployeeEndPoints() throws Exception{
        mockMvc.perform(post("/api/employee/profile")).andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(roles="EMPLOYEE")
    public void employeeCanAccessEmployeeEndPoints() throws Exception{
        mockMvc.perform(post("/api/employee/profile")).andExpect(status().isNotFound());
    }

    @Test
    public void unauthenticatedRequestIsNotRedirectedToLoginPage() throws Exception{
        mockMvc.perform(post("/api/admin/profile")).andExpect(status().isUnauthorized());
    }
}

