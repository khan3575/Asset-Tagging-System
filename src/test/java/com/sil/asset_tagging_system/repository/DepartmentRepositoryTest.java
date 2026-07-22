package com.sil.asset_tagging_system.repository;


import com.sil.asset_tagging_system.model.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
public class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Should save department successfully")
    void shouldSaveDepartment() {
        //Arrange
        Department department = Department.builder()
                .name("IT")
                .build();
        //Act
        Department savedDepartment = departmentRepository.save(department);
        //Assert
        assertThat(savedDepartment).isNotNull();
        assertThat(savedDepartment.getId()).isNotNull();
        assertThat(savedDepartment.getName()).isEqualTo("IT");
    }

    @Test
    @DisplayName("Should find department by name")
    void shouldFindDepartmentByName() {
        //Arrange
        Department department = Department.builder()
                .name("IT")
                .build();
        departmentRepository.save(department);

        //act
        Optional<Department> deptName = departmentRepository.findByName(department.getName());

        //assert
        assertThat(deptName.isPresent()).isTrue();
        assertThat(deptName.get().getId()).isNotNull();
        assertThat(deptName.get().getName()).isEqualTo("IT");
    }

    @Test
    @DisplayName("Should find all departments")
    void shouldFindAllDepartments() {
        Department department = Department.builder()
                .name("IT")
                .build();
        Department savedDepartment = departmentRepository.save(department);
        List<Department> departments = departmentRepository.findAll();
        assertThat(departments.size()).isEqualTo(1);
        assertThat(savedDepartment.getId()).isNotNull();
        assertThat(savedDepartment.getName()).isEqualTo("IT");
    }



    @Test
    @DisplayName("Should find department by name and enabled status")
    void shouldFindDepartmentByNameAndEnabled() {
        // Arrange
        Department department = Department.builder()
                .name("HR")
                .enabled(true)
                .build();
        departmentRepository.save(department);

        // Act
        Optional<Department> foundDepartment = departmentRepository.findByNameAndEnabled("HR", true);

        // Assert
        assertThat(foundDepartment).isPresent();
        assertThat(foundDepartment.get().getName()).isEqualTo("HR");
        assertThat(foundDepartment.get().getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should not find department when enabled status does not match")
    void shouldNotFindDepartmentByNameAndEnabledWhenDisabled() {
        // Arrange
        Department department = Department.builder()
                .name("Finance")
                .enabled(false)
                .build();
        departmentRepository.save(department);

        // Act
        Optional<Department> foundDepartment = departmentRepository.findByNameAndEnabled("Finance", true);

        // Assert
        assertThat(foundDepartment).isEmpty();
    }

    @Test
    @DisplayName("Should return true is exists - case ignored")
    void shouldReturnTrueIsExists() {
        Department department = Department.builder()
                .name("IT")
                .build();
        Department savedDepartment = departmentRepository.save(department);
        boolean exists = departmentRepository.existsByNameIgnoreCase(savedDepartment.getName());
        System.out.println("exists = " + exists);
        assertThat(exists).isTrue();
    }
}
