package com.sil.asset_tagging_system.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.Getter;

@Getter
@Named
@RequestScoped
public class UserDetailBean {
    private final UserDao userDao;
    private Long id;
    private User user;

    @Inject
    public UserDetailBean(UserDao userDao)
    {
        this.userDao = userDao;
    }


    public void load(){
        user = OptionalUtils.orThrowDbFetch(userDao.findById(id), "User");   
    }

    public boolean hasRole(RoleName roleName)
    {
        return user != null && user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    public void setId(Long id)
    {
        this.id = id;
    }
}
