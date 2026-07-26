package com.sil.asset_tagging_system.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PageResponseTest {

    @Test
    @DisplayName("PageResponse.from should convert Spring Data Page correctly")
    void from_ShouldConvertSpringDataPageCorrectly() {
        // Arrange
        List<String> items = List.of("Item1", "Item2");
        PageRequest pageable = PageRequest.of(0, 10);
        Page<String> springPage = new PageImpl<>(items, pageable, 25);

        // Act
        PageResponse<String> pageResponse = PageResponse.from(springPage);

        // Assert
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).containsExactly("Item1", "Item2");
        assertThat(pageResponse.pageNumber()).isEqualTo(0);
        assertThat(pageResponse.pageSize()).isEqualTo(10);
        assertThat(pageResponse.totalElements()).isEqualTo(25);
        assertThat(pageResponse.totalPages()).isEqualTo(3);
        assertThat(pageResponse.first()).isTrue();
        assertThat(pageResponse.last()).isFalse();
        assertThat(pageResponse.empty()).isFalse();
    }

    @Test
    @DisplayName("PageResponse.from should handle empty page correctly")
    void from_ShouldHandleEmptyPageCorrectly() {
        // Arrange
        Page<String> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // Act
        PageResponse<String> pageResponse = PageResponse.from(emptyPage);

        // Assert
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).isEmpty();
        assertThat(pageResponse.totalElements()).isEqualTo(0);
        assertThat(pageResponse.totalPages()).isEqualTo(0);
        assertThat(pageResponse.first()).isTrue();
        assertThat(pageResponse.last()).isTrue();
        assertThat(pageResponse.empty()).isTrue();
    }
}
