package com.sil.asset_tagging_system.bean;

import java.util.HashSet;
import java.util.Set;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.service.UserService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Named
@ViewScoped
public class UserDetailBean {
    private final UserService userService;
    @Setter
    private Long id;
    private User user;

    private boolean editing;
    @Setter
    private Long editorDepartmentId;
    @Setter
    private boolean editorIsAdmin;
    @Setter
    private boolean editorIsEmployee;


    @Inject
    public UserDetailBean(UserService userService)
    {
        this.userService = userService;
    }

    public void load(){
        user = userService.getUser(id); 
    }

    public void update(Long id, String firstName, String lastName, Long departmentId, Boolean enabled, Set<RoleName> roleNames)
    {
        userService.updateUser(id, firstName, lastName, departmentId, enabled, roleNames, Actor.current());
    }

    public boolean hasRole(RoleName roleName)
    {
        return user != null && user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    public void edit()
    {
        editorDepartmentId = user.getDepartment().getId();
        editorIsAdmin = hasRole(RoleName.ROLE_ADMIN);
        editorIsEmployee = hasRole(RoleName.ROLE_EMPLOYEE);
        editing = true;
    }

    public void save()
    {
        Set<RoleName> roles = new HashSet<>();
        if(editorIsAdmin)
        {
            roles.add(RoleName.ROLE_ADMIN);
        }
        if(editorIsEmployee)
        {
            roles.add(RoleName.ROLE_EMPLOYEE);
        }
        update(id, user.getFirstName() , user.getLastName(), editorDepartmentId, user.getEnabled(), roles);
        load();
        editing = false;
    }

    public void cancel()
    {
        load();
        editing = false;
    }
}
