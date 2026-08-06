package com.sil.asset_tagging_system.bean;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.exception.DbFetchException;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
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
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String idParam = params.get("id");

        if( idParam==null || idParam.isBlank())
        {
            user = null;
        }
        else{
            this.id = Long.valueOf(idParam);
            user = userDao.findById(id).orElseThrow(()->new DbFetchException("User fetching error from DB"));
        }
    }

    public boolean hasRole(RoleName roleName)
    {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

}
