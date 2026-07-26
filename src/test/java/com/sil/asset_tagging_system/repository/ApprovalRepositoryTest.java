package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Approval;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ApprovalRepositoryTest {

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AssetCategoryRepository categoryRepository;

    @Test
    @DisplayName("Should find approvals by requester ID and status with EntityGraph pre-fetching")
    void shouldFindByRequesterIdAndStatus() {
        Department dept = departmentRepository.save(Department.builder().name("Ops").build());
        User requester = userRepository.save(User.builder().firstName("Req").lastName("User").email("req@test.com").password("p").department(dept).build());
        AssetCategory cat = categoryRepository.save(AssetCategory.builder().name("Gadgets").build());
        Asset asset = assetRepository.save(Asset.builder().assetTag("AST-APP-1").name("Phone").category(cat).purchaseDate(LocalDate.now()).value(BigDecimal.ONE).status(AssetStatus.AVAILABLE).createdBy(requester).build());

        Approval approval = Approval.builder()
                .asset(asset)
                .initiatedBy(requester)
                .requester(requester)
                .requestType(RequestType.ASSET_REQUEST)
                .status(ApprovalStatus.PENDING)
                .requestReason("Project requirement")
                .build();
        approvalRepository.save(approval);

        Page<Approval> page = approvalRepository.findByRequesterIdAndStatus(requester.getId(), ApprovalStatus.PENDING, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAsset().getAssetTag()).isEqualTo("AST-APP-1");
        assertThat(page.getContent().get(0).getRequester().getEmail()).isEqualTo("req@test.com");
    }

    @Test
    @DisplayName("Should count approvals by status")
    void shouldCountByStatus() {
        Department dept = departmentRepository.save(Department.builder().name("QA").build());
        User u = userRepository.save(User.builder().firstName("Q").lastName("A").email("qa@test.com").password("p").department(dept).build());
        AssetCategory cat = categoryRepository.save(AssetCategory.builder().name("DevTools").build());
        Asset asset = assetRepository.save(Asset.builder().assetTag("AST-APP-2").name("iPad").category(cat).purchaseDate(LocalDate.now()).value(BigDecimal.TEN).status(AssetStatus.AVAILABLE).createdBy(u).build());

        approvalRepository.save(Approval.builder().asset(asset).initiatedBy(u).requester(u).requestType(RequestType.ASSET_REQUEST).status(ApprovalStatus.APPROVED).build());

        long count = approvalRepository.countByStatus(ApprovalStatus.APPROVED);
        assertThat(count).isEqualTo(1);
    }
}