package com.sil.asset_tagging_system.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.CorrelationFilter;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;

    @Transactional
    public void updateUser(Long id, String firstName, String lastName, Long departmentId, Boolean enabled, Set<RoleName> roles,
                            Long actorUserId, String actorRole, String ipAddress)
    {
        userDao.updateUser(id, firstName, lastName, departmentId, enabled);
        userDao.replaceRoles(id,roles);

        ActivityLog act = ActivityLog.builder()
                .correlationId(CorrelationFilter.getCurrentCorrelationId())
                .sequenceInAction((short) 1)
                .actorUserId(actorUserId)
                .entityType(ActivityEntityType.USER)
                .action(ActivityAction.USER_UPDATED)
                .outcome(ActivityOutcome.SUCCEEDED)
                .subjectUserId(id)
                .summary("Updated user " + firstName + " " + lastName)
                .ipAddress(ipAddress)
                .actorRoles((actorRole == null) ? null : RoleName.valueOf(actorRole))
                .build();
        activityLogDao.log(act);
    }
    public User getUser(Long id)
    {
        return OptionalUtils.orThrowDbFetch(userDao.findById(id), "User");
    }

    public Optional<User> findUser(Long id)
    {
        return userDao.findById(id);
    }

    public List<User> findUsers(RoleName roleName, String search, Long departmentId, Boolean enabled, int limit, int offset)
    {
        return userDao.findUsers(roleName, search, departmentId, enabled, limit, offset);
    }

    public long countUsers(RoleName roleName, String search, Long departmentId, Boolean enabled)
    {
        return userDao.countUsers(roleName, search, departmentId, enabled);
    }
}
