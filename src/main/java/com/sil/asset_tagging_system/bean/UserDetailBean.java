package com.sil.asset_tagging_system.bean;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.FacesUtil;
import com.sil.asset_tagging_system.util.OptionalUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;

import java.util.Map;

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


    @PostConstruct
    public void init()
    {
        initiateUser();

    }

    public void initiateUser(){
        Map<String, String> params = FacesUtil.getRequestParams();

        String idParam = params.get("id");

        if( idParam==null || idParam.isBlank())
        {
            user = null;
        }
        else{
            this.id = Long.valueOf(idParam);
            user = OptionalUtils.orThrowDbFetch(userDao.findById(id), "User");
        }
    }

    public boolean hasRole(RoleName roleName)
    {
        return user != null && user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

}
