package com.sil.asset_tagging_system.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.service.UserService;
import com.sil.asset_tagging_system.util.FacesMessages;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Named
@RequestScoped
public class UserFormBean {

    private final UserService userService;

    private String firstName;
    private String lastName;
    private String email;
    private Long departmentId;
    private String password;

    @Inject
    public UserFormBean(UserService userService)
    {
        this.userService = userService;
    }

    public String save()
    {
        try {
            userService.createUser(firstName, lastName, email, departmentId, password, Actor.current());
        }
        catch (BusinessRuleException e) {
            log.warn("user creation refused: {}", e.getMessage());
            FacesMessages.error(e.getMessage());
            return null;
        }
        finally {
            password = null;
        }
        return "/user/list?faces-redirect=true";
    }
}
