package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private AssetCategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private Asset createSampleAsset(String tag, String name, AssetStatus status, boolean enabled) {
        AssetCategory category = categoryRepository.save(AssetCategory.builder().name("Category_" + tag).build());
        Department dept = departmentRepository.save(Department.builder().name("Dept_" + tag).build());
        User creator = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("creator_" + tag + "@test.com")
                .password("pass")
                .department(dept)
                .build());

        Asset asset = Asset.builder()
                .assetTag(tag)
                .name(name)
                .category(category)
                .purchaseDate(LocalDate.now())
                .value(new BigDecimal("1200.00"))
                .status(status)
                .createdBy(creator)
                .enabled(enabled)
                .build();
        return assetRepository.save(asset);
    }

    @Test
    @DisplayName("Should find asset by tag ignoring case with fetched category and creator")
    void shouldFindByAssetTagIgnoreCase() {
        createSampleAsset("AST-2001", "MacBook Pro", AssetStatus.AVAILABLE, true);
        Optional<Asset> found = assetRepository.findByAssetTagIgnoreCase("ast-2001");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("MacBook Pro");
        assertThat(found.get().getCategory()).isNotNull();
        assertThat(found.get().getCreatedBy()).isNotNull();
    }

    @Test
    @DisplayName("Should check existence by asset tag excluding given ID")
    void shouldCheckExistsByAssetTagIgnoreCaseAndIdNot() {
        Asset asset = createSampleAsset("AST-2002", "Dell Laptop", AssetStatus.AVAILABLE, true);
        boolean existsOther = assetRepository.existsByAssetTagIgnoreCaseAndIdNot("AST-2002", asset.getId() + 99);
        boolean existsSame = assetRepository.existsByAssetTagIgnoreCaseAndIdNot("AST-2002", asset.getId());
        assertThat(existsOther).isTrue();
        assertThat(existsSame).isFalse();
    }

    @Test
    @DisplayName("Should find assets by status and enabled state")
    void shouldFindByStatusAndEnabled() {
        createSampleAsset("AST-2003", "Monitor 27in", AssetStatus.ASSIGNED, true);
        Page<Asset> page = assetRepository.findByStatusAndEnabled(AssetStatus.ASSIGNED, true, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAssetTag()).isEqualTo("AST-2003");
    }

    @Test
    @DisplayName("Should search assets by keyword across tag, name, and category")
    void shouldSearchAssetsByKeyword() {
        createSampleAsset("AST-2004", "ThinkPad T14", AssetStatus.AVAILABLE, true);
        Page<Asset> result = assetRepository.searchAssets("ThinkPad", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("ThinkPad T14");
    }
}
