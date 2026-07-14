package com.sil.asset_tagging_system.repository.view;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

import com.sil.asset_tagging_system.model.view.PendingApprovalView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingApprovalViewRepository extends JpaRepository<PendingApprovalView, Long> {

    Page<PendingApprovalView> findByRequestType(
            RequestType requestType,

            Pageable pageable
    );

    Page<PendingApprovalView> findByStatus(
            ApprovalStatus status,

            Pageable pageable
    );

    Page<PendingApprovalView>
    findByAssetTagContainingIgnoreCaseOrAssetNameContainingIgnoreCaseOrRequesterNameContainingIgnoreCaseOrRequesterEmailContainingIgnoreCase(
            String assetTag,

            String assetName,

            String requesterName,

            String requesterEmail,

            Pageable pageable
    );

}