package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.AssetDocument;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class AssetDocumentRepositoryTest {

    @Autowired
    private AssetDocumentRepository documentRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("Should save and find asset document by asset ID via inherited findById")
    void shouldSaveAndFindDocumentById() {
        AssetCategory category = categoryRepository.save(AssetCategory.builder().name("Hardware").build());
        Department dept = departmentRepository.save(Department.builder().name("IT_Docs").build());
        User user = userRepository.save(User.builder().firstName("Doc").lastName("Admin").email("doc@test.com").password("p").department(dept).build());
        Asset asset = assetRepository.save(Asset.builder().assetTag("AST-DOC-1").name("Server").category(category).purchaseDate(LocalDate.now()).value(BigDecimal.TEN).status(AssetStatus.AVAILABLE).createdBy(user).build());

        AssetDocument document = AssetDocument.builder()
                .asset(asset)
                .imageMimeType("image/png")
                .invoiceMimeType("application/pdf")
                .build();
        documentRepository.save(document);

        Optional<AssetDocument> found = documentRepository.findById(asset.getId());
        boolean exists = documentRepository.existsByAssetId(asset.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getImageMimeType()).isEqualTo("image/png");
        assertThat(exists).isTrue();
    }
}