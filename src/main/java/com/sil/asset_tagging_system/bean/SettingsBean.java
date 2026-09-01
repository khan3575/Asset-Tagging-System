package com.sil.asset_tagging_system.bean;

import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.UserService;
import com.sil.asset_tagging_system.util.FacesMessages;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@ViewScoped
public class SettingsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final transient UserService userService;

    private User user;

    @Setter
    private String currentPassword;
    @Setter
    private String newPassword;
    @Setter
    private String confirmPassword;

    @Inject
    public SettingsBean(UserService userService)
    {
        this.userService = userService;
    }

    public void load()
    {
        user = userService.getUser(SecurityUtil.currentUserId());
    }

    public String changePassword()
    {
        try {
            userService.changeOwnPassword(Actor.current(), currentPassword, newPassword, confirmPassword);
        }
        catch (BusinessRuleException e) {
            log.warn("password change refused: {}", e.getMessage());
            FacesMessages.error(e.getMessage());
            return null;
        }
        finally {
            currentPassword = null;
            newPassword = null;
            confirmPassword = null;
        }

        FacesMessages.info("Password changed.");
        return null;
    }
}
