package com.sil.asset_tagging_system.controller;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.repository.DepartmentRepository;
import com.sil.asset_tagging_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepartmentRepository deptRepository;

    @Test
    public void withoutSessionReturnsUnauthorized() throws Exception
    {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    public void withSessionReturnsOk() throws Exception {
        User user = new User();
        user.setFirstName("sakib");
        user.setLastName("khan");
        user.setEmail("sakib@gmail.com");
        user.setPassword("a@b12/Abs");

        Department dept = new Department();
        dept.setName("SCE");
        deptRepository.save(dept);
        user.setDepartment(dept);
        userRepository.save(user);

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("sakib@gmail.com","a@b12/Abs")))
        ).andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("sakib"))
                .andExpect(jsonPath("$.data.lastName").value("khan"))
                .andExpect(jsonPath("$.data.fullName").value("sakib khan"))
                .andExpect(jsonPath("$.data.email").value("sakib@gmail.com"))
                .andExpect(jsonPath("$.data.roles").isEmpty());
    }
}
