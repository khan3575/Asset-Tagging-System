package com.sil.asset_tagging_system.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    
    @Transactional
    public void updateUser(Long id, String firstName, String lastName, Long departmentId, Boolean enabled, Set<RoleName> roles)
    {
        userDao.updateUser(id, firstName, lastName, departmentId, enabled);
        userDao.replaceRoles(id,roles);
    }
    public User getUser(Long id)
    {
        return OptionalUtils.orThrowDbFetch(userDao.findById(id), "User");
    }
}
