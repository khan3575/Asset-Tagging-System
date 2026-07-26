package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.AssetHistory;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.HistoryAction;
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
public class AssetHistoryRepositoryTest {

    @Autowired
    private AssetHistoryRepository historyRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AssetCategoryRepository categoryRepository;

    @Test
    @DisplayName("Should find history records by asset ID ordered by action date descending")
    void shouldFindByAssetIdOrderByActionDateDesc() {
        Department dept = departmentRepository.save(Department.builder().name("Audit").build());
        User actor = userRepository.save(User.builder().firstName("Auditor").lastName("One").email("audit@test.com").password("p").department(dept).build());
        AssetCategory cat = categoryRepository.save(AssetCategory.builder().name("Storage").build());
        Asset asset = assetRepository.save(Asset.builder().assetTag("AST-HIST-1").name("SSD Drive").category(cat).purchaseDate(LocalDate.now()).value(BigDecimal.TEN).status(AssetStatus.AVAILABLE).createdBy(actor).build());

        AssetHistory history = AssetHistory.builder()
                .asset(asset)
                .action(HistoryAction.ASSET_INITIAL_REGISTRATION)
                .newStatus(AssetStatus.AVAILABLE)
                .performedBy(actor)
                .notes("Initial registration entry")
                .build();
        historyRepository.save(history);

        List<AssetHistory> list = historyRepository.findByAssetIdOrderByActionDateDesc(asset.getId());
        Page<AssetHistory> page = historyRepository.findByAction(HistoryAction.ASSET_INITIAL_REGISTRATION, PageRequest.of(0, 10));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPerformedBy().getEmail()).isEqualTo("audit@test.com");
        assertThat(page.getContent()).hasSize(1);
    }
}