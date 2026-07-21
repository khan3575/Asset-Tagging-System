package com.sil.asset_tagging_system.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AssetDocument {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "asset_id")
    private Long assetId;


    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @MapsId

    @JoinColumn(
            name = "asset_id"
    )
    private Asset asset;


    // Requires Hibernate bytecode enhancement to take effect
    @Basic(fetch = FetchType.LAZY)
    @Lob

    @Column(
            name = "asset_image",
            columnDefinition = "LONGBLOB"
    )
    private byte[] assetImage;


    @Basic(fetch = FetchType.LAZY)
    @Lob

    @Column(
            name = "invoice_pdf",
            columnDefinition = "LONGBLOB"
    )
    private byte[] invoicePdf;


    @Column(
            name = "image_mime_type",
            length = 50
    )
    private String imageMimeType;


    @Column(
            name = "invoice_mime_type",
            length = 50
    )
    private String invoiceMimeType;


    @UpdateTimestamp
    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;

}