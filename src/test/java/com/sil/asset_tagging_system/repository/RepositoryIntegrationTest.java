package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;

import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.RoleName;

import com.sil.asset_tagging_system.model.view.AssetSummaryView;
import com.sil.asset_tagging_system.model.view.AssignedAssetView;
import com.sil.asset_tagging_system.model.view.AvailableAssetView;
import com.sil.asset_tagging_system.model.view.PendingApprovalView;

import com.sil.asset_tagging_system.repository.view.AssetSummaryViewRepository;
import com.sil.asset_tagging_system.repository.view.AssignedAssetViewRepository;
import com.sil.asset_tagging_system.repository.view.AvailableAssetViewRepository;
import com.sil.asset_tagging_system.repository.view.PendingApprovalViewRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration tests for the Phase 1
 * database and repository configuration.
 *
 * These tests use the existing local MySQL database.
 */
@SpringBootTest

@ActiveProfiles("local")

class RepositoryIntegrationTest {


    // ========================================================
    // MAIN TABLE REPOSITORIES
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
    // DATABASE VIEW REPOSITORIES
    // ========================================================

    @Autowired
    private AssetSummaryViewRepository
            assetSummaryViewRepository;


    @Autowired
    private AssignedAssetViewRepository
            assignedAssetViewRepository;


    @Autowired
    private AvailableAssetViewRepository
            availableAssetViewRepository;


    @Autowired
    private PendingApprovalViewRepository
            pendingApprovalViewRepository;


    // ========================================================
    // DATABASE TABLE TEST
    // ========================================================

    @Test

    @DisplayName(
            "All main database tables should contain seed data"
    )

    void shouldReadSeedDataFromMainTables() {


        // ----------------------------------------------------
        // VERIFY DEPARTMENTS
        // ----------------------------------------------------

        assertThat(
                departmentRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // VERIFY ROLES
        // ----------------------------------------------------

        assertThat(
                roleRepository.count()
        )
                .isEqualTo(2);


        // ----------------------------------------------------
        // VERIFY USERS
        // ----------------------------------------------------

        assertThat(
                userRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // VERIFY ASSET CATEGORIES
        // ----------------------------------------------------

        assertThat(
                assetCategoryRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // VERIFY ASSETS
        // ----------------------------------------------------

        assertThat(
                assetRepository.count()
        )
                .isEqualTo(4);


        // ----------------------------------------------------
        // VERIFY ASSET DOCUMENTS
        // ----------------------------------------------------

        assertThat(
                assetDocumentRepository.count()
        )
                .isEqualTo(2);


        // ----------------------------------------------------
        // VERIFY APPROVALS
        // ----------------------------------------------------

        assertThat(
                approvalRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // VERIFY CUSTODY RECORDS
        // ----------------------------------------------------

        assertThat(
                assetCustodyRepository.count()
        )
                .isEqualTo(3);


        // ----------------------------------------------------
        // VERIFY ASSET HISTORY
        // ----------------------------------------------------

        assertThat(
                assetHistoryRepository.count()
        )
                .isEqualTo(5);

    }


    // ========================================================
    // USER AND ROLE TEST
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
                employee.getEnabled()
        )
                .isTrue();


        assertThat(
                employee.getRoles()
        )
                .extracting(
                        role -> role.getName()
                )
                .contains(
                        RoleName.ROLE_EMPLOYEE
                );

    }


    // ========================================================
    // ASSET TEST
    // ========================================================

    @Test

    @DisplayName(
            "Asset AST-1002 should be available"
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

    }


    // ========================================================
    // ASSET SUMMARY VIEW TEST
    // ========================================================

    @Test

    @DisplayName(
            "Asset summary view should return dashboard data"
    )

    void shouldReadAssetSummaryView() {


        List<AssetSummaryView> summaries =
                assetSummaryViewRepository.findAll();


        assertThat(
                summaries
        )
                .hasSize(1);


        AssetSummaryView summary =
                summaries.getFirst();


        assertThat(
                summary.getTotalAssets()
        )
                .isEqualTo(4L);


        assertThat(
                summary.getAssignedAssets()
        )
                .isNotNull();


        assertThat(
                summary.getAvailableAssets()
        )
                .isNotNull();


        assertThat(
                summary.getDamagedAssets()
        )
                .isNotNull();


        assertThat(
                summary.getTotalPurchaseValue()
        )
                .isNotNull();

    }


    // ========================================================
    // ASSIGNED ASSET VIEW TEST
    // ========================================================

    @Test

    @DisplayName(
            "Assigned asset view should return active custody"
    )

    void shouldReadAssignedAssetView() {


        List<AssignedAssetView> assignedAssets =
                assignedAssetViewRepository.findAll();


        assertThat(
                assignedAssets
        )
                .isNotEmpty();


        assertThat(
                assignedAssets
        )
                .extracting(
                        AssignedAssetView::getAssetTag
                )
                .contains(
                        "AST-1001",
                        "AST-1003"
                );

    }


    // ========================================================
    // AVAILABLE ASSET VIEW TEST
    // ========================================================

    @Test

    @DisplayName(
            "Available asset view should return available assets"
    )

    void shouldReadAvailableAssetView() {


        List<AvailableAssetView> availableAssets =
                availableAssetViewRepository.findAll();


        assertThat(
                availableAssets
        )
                .isNotEmpty();


        assertThat(
                availableAssets
        )
                .extracting(
                        AvailableAssetView::getAssetTag
                )
                .contains(
                        "AST-1002"
                );

    }


    // ========================================================
    // PENDING APPROVAL VIEW TEST
    // ========================================================

    @Test

    @DisplayName(
            "Pending approval view should return open requests"
    )

    void shouldReadPendingApprovalView() {


        List<PendingApprovalView> pendingApprovals =
                pendingApprovalViewRepository.findAll();


        assertThat(
                pendingApprovals
        )
                .isNotEmpty();


        assertThat(
                pendingApprovals
        )
                .extracting(
                        PendingApprovalView::getAssetTag
                )
                .contains(
                        "AST-1002",
                        "AST-1003"
                );

    }

}