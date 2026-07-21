package com.sil.asset_tagging_system.config;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.repository.DepartmentRepository;
import com.sil.asset_tagging_system.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")

public class SecurityConfigTest {
    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepartmentRepository deptRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    public void validRequest_returnsOk() throws Exception
    {
        User user = new User();
        user.setFirstName("sakib");
        user.setLastName("khan");
        user.setEmail("sakib@gmail.com");
        user.setPassword(encoder.encode("A1b2@aaa"));
        Department dept = new Department();
        dept.setName("cse");
        deptRepository.save(dept);
        user.setDepartment(dept);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "A1b2@aaa");

        String jsonResponse = mapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonResponse)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("sakib"))
                .andExpect(jsonPath("$.data.lastName").value("khan"))
                .andExpect(jsonPath("$.data.email").value("sakib@gmail.com"))
                .andExpect(jsonPath("$.data.roles").isEmpty());

    }

    @Test
    public void invalidCredentialReturnsBadRequest() throws Exception{
        User user = new User();
        user.setFirstName("sakib");
        user.setLastName("khan");
        user.setEmail("sakib@gmail.com");
        user.setPassword(encoder.encode("A1b2@aaa"));
        Department dept = new Department();
        dept.setName("cse");
        deptRepository.save(dept);
        user.setDepartment(dept);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "A1b2@aab");
        String jsonRequest = mapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                ).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

    }
}