package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.enums.RoleName;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);

}