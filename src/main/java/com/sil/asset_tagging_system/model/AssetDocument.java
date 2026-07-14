package com.sil.asset_tagging_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDocument {

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


    @Lob

    @Column(
            name = "asset_image",
            columnDefinition = "LONGBLOB"
    )
    private byte[] assetImage;


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


    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

}