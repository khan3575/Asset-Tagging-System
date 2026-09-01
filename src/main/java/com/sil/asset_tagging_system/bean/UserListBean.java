package com.sil.asset_tagging_system.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.service.UserService;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.Setter;

@Getter
@Named
@RequestScoped
public class UserListBean {
    private final Logger log = LoggerFactory.getLogger(UserListBean.class);

    private final UserService userService;
    private List<User> users;
    private Long totalRecords;
    private Integer totalPages;

    @Setter
    private Integer currentPage;
    @Setter
    private String search;
    @Setter
    private RoleName roleName;
    @Setter
    private Long departmentId;
    @Setter
    private Boolean enabled;
    private final Integer pageSize = 10;
    private Integer offset;

    @Inject
    public UserListBean(UserService userService)
    {
        this.userService = userService;
    }


    public void load()
    {

        currentPage = PageParams.clamp(currentPage);
        offset = PageParams.offset(currentPage,pageSize);

        log.info("UserListBean init -- Search {}", search);

       users = userService.findUsers(roleName, search, departmentId, enabled, pageSize, offset);
       totalRecords = userService.countUsers(roleName, search, departmentId, enabled);

       totalPages = PageParams.totalPages(totalRecords, pageSize);
    }

    public RoleName[] getRoleOptions()
    {
        return RoleName.values();
    }
    public Map<String, String> getRoleOptionsMap()
    {
        Map<String, String> options = new LinkedHashMap<>();
        for (RoleName role : RoleName.values()) {
            options.put(role.name(), role.name());
        }
        return options;
    }
    public Map<String, String> getExtraParams(){
        Map<String, String> params = new LinkedHashMap<>();
        if(search != null) params.put("search", search);
        if(roleName != null) params.put("roleName", roleName.name());
        if(departmentId != null) params.put("departmentId", String.valueOf(departmentId));
        if(enabled != null) params.put("enabled", String.valueOf(enabled));
        return params;
    }
    
    public String search()
    {
        currentPage = 1;
        return "/user/list?faces-redirect=true&includeViewParams=true";
    }

}
