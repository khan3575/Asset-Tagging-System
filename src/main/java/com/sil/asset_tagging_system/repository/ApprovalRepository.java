package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Approval;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    Page<Approval> findByRequesterId(
            Long requesterId,
            Pageable pageable
    );

    Page<Approval> findByRequesterIdAndStatus(
            Long requesterId,
            ApprovalStatus status,
            Pageable pageable
    );
    List<Approval> findByAssetIdOrderByRequestDateDesc(
            Long assetId
    );

    Page<Approval> findByStatus(
            ApprovalStatus status,
            Pageable pageable
    );

    Page<Approval> findByStatusIn(
            Collection<ApprovalStatus> statuses,
            Pageable pageable
    );
    Page<Approval> findByRequestType(
            RequestType requestType,
            Pageable pageable
    );

    Page<Approval> findByRequestTypeAndStatus(
            RequestType requestType,
            ApprovalStatus status,
            Pageable pageable
    );
    long countByStatus(ApprovalStatus status);
    boolean existsByAssetIdAndRequestTypeAndStatusIn(
            Long assetId,
            RequestType requestType,
            Collection<ApprovalStatus> statuses
    );

}