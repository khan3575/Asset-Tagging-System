package com.sil.asset_tagging_system.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final AuditTrail auditTrail;

    @Transactional
    public void updateUser(Long id, String firstName, String lastName, Long departmentId, Boolean enabled, Set<RoleName> roles,
                            Actor actor)
    {
        Boolean wasEnabled = userDao.findById(id).map(User::getEnabled).orElse(null);
        boolean beingDisabled = Boolean.TRUE.equals(wasEnabled) && Boolean.FALSE.equals(enabled);

        userDao.updateUser(id, firstName, lastName, departmentId, enabled);
        userDao.replaceRoles(id,roles);

        auditTrail.record(beingDisabled ? ActivityAction.USER_DISABLED : ActivityAction.USER_UPDATED, ActivityEntityType.USER)
                .by(actor)
                .subject(id)
                .summary(beingDisabled ? "Disabled user " + firstName + " " + lastName : "Updated user " + firstName + " " + lastName)
                .details(rolesJson(roles))
                .save();
    }

    private String rolesJson(Set<RoleName> roles)
    {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        StringBuilder json = new StringBuilder("{\"roles\":[");
        boolean first = true;
        for (RoleName role : roles) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(role.name()).append("\"");
            first = false;
        }
        return json.append("]}").toString();
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
