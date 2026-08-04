package com.sil.asset_tagging_system.bean;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;

import lombok.Getter;

@Getter
@Named
@RequestScoped
public class UserListBean {
    private final Logger log = LoggerFactory.getLogger(UserListBean.class);

    private final UserDao userDao;
    private List<User> users;
    private Long totalCount;
    private Integer totalPageCount;
    private Integer page;
    private String search;
    private RoleName roleName;
    private Long departmentId;
    private Boolean status;
    private final Integer pageSize = 10;
    private Integer offset;

    @Inject
    public UserListBean(UserDao userDao)
    {
        this.userDao = userDao;
    }

    @PostConstruct
    public void init()
    {
        // here the xhtml initially doesnt get the parameters as the url are redirected from the controller. so we have to
        // first get the current instance, then from the external context them map it be used.
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        // getting parameter from the url to do query
        this.search = params.get("search");

        String roleNameParam = params.get("roleName");
        this.roleName = (roleNameParam == null || roleNameParam.isBlank()) ? null : RoleName.valueOf(roleNameParam);

        String departmentIdParam = params.get("departmentId");
        this.departmentId = (departmentIdParam == null || departmentIdParam.isBlank()) ? null : Long.valueOf(departmentIdParam);

        String statusParam = params.get("status");
        this.status = (statusParam == null || statusParam.isBlank()) ? null : Boolean.valueOf(statusParam);

        String pageParam = params.get("page");
        if (pageParam == null || pageParam.isBlank()) {
            page = 1;
            offset = 0;
        } else {
            if (pageParam.matches("\\d+")) {
                page = Integer.valueOf(pageParam);
                offset = (page - 1) * pageSize;
            } else {
                page = 1;
                offset = 0;
            }
        }
        log.info("UserListBean init -- Search {}", search);

        // getting user list
       users = userDao.findUsers(roleName, search, departmentId, status,pageSize,offset);

       totalCount = userDao.countUsers(roleName, search, departmentId, status);

        // total page count
        totalPageCount = (int) Math.ceil( (double) totalCount / pageSize);

    }

    public RoleName[] getRoleOptions()
    {
        return RoleName.values();
    }

}
