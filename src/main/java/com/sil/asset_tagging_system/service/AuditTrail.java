package com.sil.asset_tagging_system.service;

import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.CorrelationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTrail {
    private final ActivityLogDao activityLogDao;

    public Entry record(ActivityAction action, ActivityEntityType entityType) {
        return new Entry(activityLogDao, action, entityType);
    }

    // static, with the dao passed in -- the dependency is visible in the signature
    // rather than reached through an implicit outer instance.
    public static class Entry {
        private final ActivityLogDao activityLogDao;
        private final ActivityLog.ActivityLogBuilder builder = ActivityLog.builder();
        private boolean refusal;
        private boolean bestEffort;

        private Entry(ActivityLogDao activityLogDao, ActivityAction action, ActivityEntityType entityType) {
            this.activityLogDao = activityLogDao;
            builder.correlationId(CorrelationFilter.getCurrentCorrelationId())
                   .sequenceInAction((short) 1)
                   .action(action)
                   .entityType(entityType)
                   .outcome(ActivityOutcome.SUCCEEDED);
        }

        // who
        public Entry by(Actor actor) {
            builder.actorUserId(actor.userId())
                   .actorRoles(parseRole(actor.role()))
                   .ipAddress(actor.ipAddress());
            return this;
        }

        public Entry sequence(int sequence) {
            builder.sequenceInAction((short) sequence);
            return this;
        }
        public Entry asset(Long assetId) {
            builder.assetId(assetId);
            return this;
        }
        public Entry approval(Long approvalId) {
            builder.approvalId(approvalId);
            return this;
        }

        public Entry subject(Long subjectUserId) {
            builder.subjectUserId(subjectUserId);
            return this;
        }
        //what changed
        public Entry condition(AssetCondition previous, AssetCondition next) {
            builder.previousCondition(previous).newCondition(next);
            return this;
        }
        
        public Entry holder(Long previousHolderId, Long newHolderId) {
            builder.previousHolderId(previousHolderId).newHolderId(newHolderId);
            return this;
        }
        //description
        public Entry summary(String summary) {
            builder.summary(summary);
            return this;
        }

        public Entry details(String detailsJson) {
            builder.details(detailsJson);
            return this;
        }

        //
        public Entry refused(String reason) {
            this.refusal = true;
            builder.outcome(ActivityOutcome.DENIED).failureReason(reason);
            return this;
        }
        // an action that is not complete
        public Entry failed(String reason) {
            this.refusal = true;
            builder.outcome(ActivityOutcome.FAILED).failureReason(reason);
            return this;
        }

        public Entry bestEffort() {
            this.bestEffort = true;
            return this;
        }
        // write
        public void save() {
            ActivityLog row = builder.build();
            try {
                if (refusal) {
                    activityLogDao.logRefusal(row);
                } else {
                    activityLogDao.log(row);
                }
            } catch (RuntimeException e) {
                if (!bestEffort) {
                    throw e;
                }
                log.error("Failed to write {} activity log row", row.getAction(), e);
            }
        }
    }

    private static RoleName parseRole(String actorRole) {
        if (actorRole == null) {
            return null;
        }
        try {
            return RoleName.valueOf(actorRole);
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognised role '{}' on an activity-log row; storing null", actorRole);
            return null;
        }
    }
}
