package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.RoleName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration tests for the application's
 * database entities and repositories.
 *
 * These tests use the local MySQL database
 * and verify the existing development seed data.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RepositoryIntegrationTest {


    // ========================================================
    // REPOSITORIES
    // ========================================================

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssetCategoryRepository assetCategoryRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetDocumentRepository assetDocumentRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private AssetCustodyRepository assetCustodyRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;


    // ========================================================
    // MAIN DATABASE TABLE TEST
    // ========================================================

    @Test
    @DisplayName(
            "All main entity repositories should read the seed data"
    )
    void shouldReadSeedDataFromMainTables() {

        // ----------------------------------------------------
        // DEPARTMENTS
        // ----------------------------------------------------

        assertThat(
                departmentRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // ROLES
        // ----------------------------------------------------

        assertThat(
                roleRepository.count()
        )
                .isEqualTo(2);


        // ----------------------------------------------------
        // USERS
        // ----------------------------------------------------

        assertThat(
                userRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // ASSET CATEGORIES
        // ----------------------------------------------------

        assertThat(
                assetCategoryRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // ASSETS
        // ----------------------------------------------------

        assertThat(
                assetRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // ASSET DOCUMENTS
        // ----------------------------------------------------

        assertThat(
                assetDocumentRepository.count()
        )
                .isEqualTo(2);


        // ----------------------------------------------------
        // APPROVALS
        // ----------------------------------------------------

        assertThat(
                approvalRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // ASSET CUSTODY RECORDS
        // ----------------------------------------------------

        assertThat(
                assetCustodyRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // ASSET HISTORY RECORDS
        // ----------------------------------------------------

        assertThat(
                assetHistoryRepository.count()
        )
                .isEqualTo(5);
    }


    // ========================================================
    // USER AND ROLE RELATIONSHIP TEST
    // ========================================================

    @Test
    @DisplayName(
            "Sakib should exist as an enabled employee"
    )
    void shouldFindEmployeeWithRole() {

        User employee = userRepository
                .findByEmailIgnoreCase(
                        "sakib.khan@enterprise.com"
                )
                .orElseThrow();


        assertThat(
                employee.getFirstName()
        )
                .isEqualTo("Sakib");


        assertThat(
                employee.getLastName()
        )
                .isEqualTo("Khan");


        assertThat(
                employee.getEmail()
        )
                .isEqualTo(
                        "sakib.khan@enterprise.com"
                );


        assertThat(
                employee.getEnabled()
        )
                .isTrue();


        assertThat(
                employee.getDepartment()
        )
                .isNotNull();


        assertThat(
                employee.getDepartment().getName()
        )
                .isEqualTo("Engineering");


        assertThat(
                employee.getRoles()
        )
                .extracting(
                        role -> role.getName()
                )
                .containsExactly(
                        RoleName.ROLE_EMPLOYEE
                );
    }


    // ========================================================
    // ADMIN ROLE TEST
    // ========================================================

    @Test
    @DisplayName(
            "Mehedi should exist as an enabled administrator"
    )
    void shouldFindAdminWithRole() {

        User admin = userRepository
                .findByEmailIgnoreCase(
                        "mehedi.hasan@enterprise.com"
                )
                .orElseThrow();


        assertThat(
                admin.getEnabled()
        )
                .isTrue();


        assertThat(
                admin.getRoles()
        )
                .extracting(
                        role -> role.getName()
                )
                .containsExactly(
                        RoleName.ROLE_ADMIN
                );
    }


    // ========================================================
    // AVAILABLE ASSET TEST
    // ========================================================

    @Test
    @DisplayName(
            "Asset AST-1002 should exist and be available"
    )
    void shouldFindAvailableAsset() {

        Asset asset = assetRepository
                .findByAssetTagIgnoreCase(
                        "AST-1002"
                )
                .orElseThrow();


        assertThat(
                asset.getName()
        )
                .isEqualTo(
                        "Dell UltraSharp 27\""
                );


        assertThat(
                asset.getStatus()
        )
                .isEqualTo(
                        AssetStatus.AVAILABLE
                );


        assertThat(
                asset.getCategory()
        )
                .isNotNull();


        assertThat(
                asset.getCategory().getName()
        )
                .isEqualTo("Monitors");
    }


    // ========================================================
    // ASSIGNED ASSET TEST
    // ========================================================

    @Test
    @DisplayName(
            "Asset AST-1001 should exist and be assigned"
    )
    void shouldFindAssignedAsset() {

        Asset asset = assetRepository
                .findByAssetTagIgnoreCase(
                        "AST-1001"
                )
                .orElseThrow();


        assertThat(
                asset.getStatus()
        )
                .isEqualTo(
                        AssetStatus.ASSIGNED
                );


        assertThat(
                asset.getCreatedBy()
        )
                .isNotNull();


        assertThat(
                asset.getCreatedBy().getEmail()
        )
                .isEqualTo(
                        "mehedi.hasan@enterprise.com"
                );
    }


    // ========================================================
    // REPOSITORY QUERY TEST
    // ========================================================

    @Test
    @DisplayName(
            "User email and asset tag existence checks should work"
    )
    void shouldCheckUniqueDatabaseValues() {

        assertThat(
                userRepository.existsByEmailIgnoreCase(
                        "sakib.khan@enterprise.com"
                )
        )
                .isTrue();


        assertThat(
                userRepository.existsByEmailIgnoreCase(
                        "unknown@enterprise.com"
                )
        )
                .isFalse();


        assertThat(
                assetRepository.existsByAssetTagIgnoreCase(
                        "AST-1002"
                )
        )
                .isTrue();


        assertThat(
                assetRepository.existsByAssetTagIgnoreCase(
                        "AST-9999"
                )
        )
                .isFalse();
    }

}