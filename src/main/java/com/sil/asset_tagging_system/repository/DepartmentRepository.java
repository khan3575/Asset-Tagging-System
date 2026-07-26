package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    Optional<Department> findByNameAndEnabled(String name, Boolean enabled);

    boolean existsByNameIgnoreCase(String name);

}