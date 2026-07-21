package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Approval;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    Page<Approval> findByRequesterId(
            Long requesterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    Page<Approval> findByRequesterIdAndStatus(
            Long requesterId,
            ApprovalStatus status,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    List<Approval> findByAssetIdOrderByRequestDateDesc(
            Long assetId
    );

    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    Page<Approval> findByStatus(
            ApprovalStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    Page<Approval> findByStatusIn(
            Collection<ApprovalStatus> statuses,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
    Page<Approval> findByRequestType(
            RequestType requestType,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "initiatedBy", "requester"})
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