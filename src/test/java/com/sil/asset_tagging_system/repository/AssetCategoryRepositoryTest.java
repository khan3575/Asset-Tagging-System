package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class AssetCategoryRepositoryTest {
    @Autowired
    private AssetCategoryRepository assetCategoryRepository;

    @Test
    @DisplayName("Should find category by name ignoring case")
    void  shouldFindCategoryByNameIgnoreCase(){
        AssetCategory assetCategory = AssetCategory.builder()
                .name("Laptops")
                .depreciationRatePercentage(new BigDecimal("15.00"))
                .build();
        assetCategoryRepository.save(assetCategory);

        Optional<AssetCategory> found =  assetCategoryRepository.findByNameIgnoreCase("Laptops");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptops");
    }
    @Test
    @DisplayName("Should check existence by category name ignoring case")
    void shouldCheckExistenceByNameIgnoreCase() {
        AssetCategory category = AssetCategory.builder()
                .name("Monitors")
                .build();
        assetCategoryRepository.save(category);
        boolean exists = assetCategoryRepository.existsByNameIgnoreCase("MONITORS");
        assertThat(exists).isTrue();
    }
}
