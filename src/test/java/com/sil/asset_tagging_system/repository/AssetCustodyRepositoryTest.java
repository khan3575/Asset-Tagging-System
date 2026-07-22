package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.AssetCustody;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.CustodyStatus;
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
public class AssetCustodyRepositoryTest {

    @Autowired
    private AssetCustodyRepository custodyRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AssetCategoryRepository categoryRepository;

    @Test
    @DisplayName("Should find active custody record by asset ID and status")
    void shouldFindByAssetIdAndStatus() {
        Department dept = departmentRepository.save(Department.builder().name("Logistics").build());
        User custodian = userRepository.save(User.builder().firstName("Cust").lastName("User").email("cust@test.com").password("p").department(dept).build());
        AssetCategory cat = categoryRepository.save(AssetCategory.builder().name("Peripherals").build());
        Asset asset = assetRepository.save(Asset.builder().assetTag("AST-CUST-1").name("Mouse").category(cat).purchaseDate(LocalDate.now()).value(BigDecimal.TEN).status(AssetStatus.ASSIGNED).createdBy(custodian).build());

        AssetCustody custody = AssetCustody.builder()
                .asset(asset)
                .custodian(custodian)
                .assignedBy(custodian)
                .status(CustodyStatus.ACTIVE)
                .build();
        custodyRepository.save(custody);

        Optional<AssetCustody> found = custodyRepository.findByAssetIdAndStatus(asset.getId(), CustodyStatus.ACTIVE);
        boolean exists = custodyRepository.existsByAssetIdAndStatus(asset.getId(), CustodyStatus.ACTIVE);

        assertThat(found).isPresent();
        assertThat(found.get().getCustodian().getEmail()).isEqualTo("cust@test.com");
        assertThat(exists).isTrue();
    }
}