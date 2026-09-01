package com.sil.asset_tagging_system.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDao userDao;
    private final AuditTrail auditTrail;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateUser(Long id, String firstName, String lastName, Long departmentId, Boolean enabled, Set<RoleName> roles,
                            Actor actor)
    {
        String violation = null;
        if (firstName == null || firstName.isBlank()) {
            violation = "First name is required";
        } else if (lastName == null || lastName.isBlank()) {
            violation = "Last name is required";
        } else if (departmentId == null) {
            violation = "Department is required";
        } else if (enabled == null) {
            violation = "Account status is required";
        } else if (roles == null || roles.isEmpty()) {
            violation = "A user must have at least one role";
        }
        if (violation != null) {
            auditTrail.record(ActivityAction.USER_UPDATED, ActivityEntityType.USER)
                    .by(actor)
                    .subject(id)
                    .refused(violation)
                    .summary("User update refused -- " + violation)
                    .save();
            throw new BusinessRuleException(violation);
        }

        Boolean wasEnabled = userDao.findById(id).map(User::getEnabled).orElse(null);
        boolean beingDisabled = Boolean.TRUE.equals(wasEnabled) && Boolean.FALSE.equals(enabled);
        boolean beingEnabled = Boolean.FALSE.equals(wasEnabled) && Boolean.TRUE.equals(enabled);

        userDao.updateUser(id, firstName, lastName, departmentId, enabled);
        userDao.replaceRoles(id,roles);

        ActivityAction action = beingDisabled ? ActivityAction.USER_DISABLED
                              : beingEnabled  ? ActivityAction.USER_ENABLED
                                              : ActivityAction.USER_UPDATED;

        auditTrail.record(action, ActivityEntityType.USER)
                .by(actor)
                .subject(id)
                .summary(switch (action) {
                    case USER_DISABLED -> "Disabled user " + firstName + " " + lastName + " -- account retained, access withdrawn";
                    case USER_ENABLED -> "Re-enabled user " + firstName + " " + lastName;
                    default -> "Updated user " + firstName + " " + lastName;
                })
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
    @Transactional
    public Long createUser(String firstName, String lastName, String email, Long departmentId,
                           String password, Actor actor)
    {
        String violation = null;
        if (firstName == null || firstName.isBlank()) {
            violation = "First name is required";
        } else if (lastName == null || lastName.isBlank()) {
            violation = "Last name is required";
        } else if (email == null || email.isBlank()) {
            violation = "Email is required";
        } else if (departmentId == null) {
            violation = "Department is required";
        } else if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            violation = "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        } else if (Boolean.TRUE.equals(userDao.existsByEmailIgnoreCase(email))) {
            violation = "A user with that email already exists: " + email;
        }

        if (violation != null) {
            auditTrail.record(ActivityAction.USER_CREATED, ActivityEntityType.USER)
                    .by(actor)
                    .refused(violation)
                    .summary("User creation refused -- " + violation)
                    .save();
            throw new BusinessRuleException(violation);
        }

        Long newUserId = userDao.createUser(firstName, lastName, email,
                passwordEncoder.encode(password), departmentId);
        userDao.replaceRoles(newUserId, Set.of(RoleName.ROLE_EMPLOYEE));

        auditTrail.record(ActivityAction.USER_CREATED, ActivityEntityType.USER)
                .by(actor)
                .subject(newUserId)
                .summary("Created user " + firstName + " " + lastName + " (" + email + ")")
                .details(rolesJson(Set.of(RoleName.ROLE_EMPLOYEE)))
                .save();

        return newUserId;
    }

    @Transactional
    public void changeOwnPassword(Actor actor, String currentPassword, String newPassword, String confirmPassword)
    {
        User user = getUser(actor.userId());
        String currentHash = userDao.findPasswordHash(user.getId()).orElse(null);

        String violation = null;
        if (currentPassword == null || currentHash == null || !passwordEncoder.matches(currentPassword, currentHash)) {
            violation = "Current password is incorrect";
        } else if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            violation = "New password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        } else if (!newPassword.equals(confirmPassword)) {
            violation = "New password and confirmation do not match";
        } else if (passwordEncoder.matches(newPassword, currentHash)) {
            violation = "New password must be different from the current one";
        }

        if (violation != null) {
            auditTrail.record(ActivityAction.PASSWORD_CHANGED, ActivityEntityType.USER)
                    .by(actor)
                    .subject(user.getId())
                    .refused(violation)
                    .summary("Password change refused for " + user.getEmail())
                    .save();
            throw new BusinessRuleException(violation);
        }

        userDao.updatePassword(user.getId(), passwordEncoder.encode(newPassword));

        auditTrail.record(ActivityAction.PASSWORD_CHANGED, ActivityEntityType.USER)
                .by(actor)
                .subject(user.getId())
                .summary("Password changed for " + user.getEmail())
                .save();
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
