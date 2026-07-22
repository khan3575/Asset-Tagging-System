package com.sil.asset_tagging_system.repository;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private RoleRepository roleRepository;

    private User createSampleUser(String email,String firstName,String lastName , RoleName roleName) {
        Department dept = Department.builder()
                .name("Engineering_" + email).build();
        Department savedDepartment = departmentRepository.save(dept);
        Role role = roleRepository.save(Role.builder().name(roleName).build());

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded_pass")
                .department(dept)
                .enabled(true)
                .build();
        user.addRole(role);

        return userRepository.save(user);
    }

    @Test
    @DisplayName("Should find user by email ignoring case with fetched department and roles")
    void shouldFindByEmailIgnoreCase() {
        createSampleUser("mostafiz.fahim@test.com" , "Mostafiz" , "Fahim" , RoleName.ROLE_EMPLOYEE);
        Optional<User> found = userRepository.findByEmailIgnoreCase("MOSTAfiz.faHIm@Test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Mostafiz");
        assertThat(found.get().getLastName()).isEqualTo("Fahim");
        assertThat(found.get().getDepartment()).isNotNull();
        assertThat(found.get().getRoles()).hasSize(1);

    }
    @Test
    @DisplayName("Should check existence by email ignoring case")
    void shouldCheckExistsByEmailIgnoreCase() {
        createSampleUser("sakib.khan@test.com", "Sakib", "Khan", RoleName.ROLE_ADMIN);
        boolean exists = userRepository.existsByEmailIgnoreCase("sakib.khan@test.com");
        boolean notExists = userRepository.existsByEmailIgnoreCase("unknown@test.com");
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    @Test
    @DisplayName("Should check existence by email ignoring case for other users (IdNot)")
    void shouldCheckExistsByEmailIgnoreCaseAndIdNot() {
        User user = createSampleUser("mehedi.hasan@test.com", "Mehedi", "Hasan", RoleName.ROLE_EMPLOYEE);
        boolean existsForOtherId = userRepository.existsByEmailIgnoreCaseAndIdNot("mehedi.haSan@test.com", user.getId() + 99);
        boolean existsForSameId = userRepository.existsByEmailIgnoreCaseAndIdNot("bob@test.com", user.getId());
        assertThat(existsForOtherId).isTrue();
        assertThat(existsForSameId).isFalse();
    }

    @Test
    @DisplayName("Should find user by ID and RoleName")
    void shouldFindByIdAndRoleName() {
        User user = createSampleUser("mostafiz.fahim@test.com", "Mostafiz", "Fahim", RoleName.ROLE_EMPLOYEE);
        Optional<User> found = userRepository.findByIdAndRoleName(user.getId(), RoleName.ROLE_EMPLOYEE);
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("mostafiz.fahim@test.com");
    }
    @Test
    @DisplayName("Should find employees with filters using pagination")
    void shouldFindEmployeesWithFilters() {
        User user = createSampleUser("mostafiz.fahim@test.com", "Mostafiz", "Fahim", RoleName.ROLE_EMPLOYEE);
        Page<User> result = userRepository.findEmployees(
                RoleName.ROLE_EMPLOYEE,
                "mostafiz",
                user.getDepartment().getId(),
                true,
                PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("mostafiz.fahim@test.com");
    }



}
