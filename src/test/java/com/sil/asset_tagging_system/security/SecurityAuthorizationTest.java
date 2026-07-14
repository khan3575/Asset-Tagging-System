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
        mockMvc.perform(post("api/admin/asset")).andExpect(status().isForbidden());
    }
}
