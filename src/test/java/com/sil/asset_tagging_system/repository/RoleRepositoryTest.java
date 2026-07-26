package com.sil.asset_tagging_system.repository;


import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class RoleRepositoryTest {
    @Autowired
    RoleRepository roleRepository;

    @Test
    @DisplayName("Should find role by name")
    void  shouldFindRoleByName(){
        Role role = Role.builder()
                .name(RoleName.ROLE_ADMIN)
                .build();
        roleRepository.save(role);

        Optional<Role> foundRole = roleRepository.findByName(RoleName.ROLE_ADMIN);

        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(RoleName.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should check existence by role name")
    void shouldCheckExistenceByName() {
        Role role = Role.builder()
                .name(RoleName.ROLE_EMPLOYEE)
                .build();
        roleRepository.save(role);
        boolean exists = roleRepository.existsByName(RoleName.ROLE_EMPLOYEE);
        assertThat(exists).isTrue();
    }
}
