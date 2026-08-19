package com.sil.asset_tagging_system.bean;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContextHolder;

import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.security.CustomUserDetails;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Named
@RequestScoped
public class AssetFormBean {

    private final AssetDao assetDao;

    // form fields, bound from add-asset.xhtml
    private String assetTag;
    private String name;
    private Long categoryId;
    private LocalDate purchaseDate;
    private BigDecimal value;

    @Inject
    public AssetFormBean(AssetDao assetDao)
    {
        this.assetDao = assetDao;
    }

    public String save()
    {
        if (assetDao.existsByAssetTagIgnoreCase(assetTag))
        {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Asset tag already exists", null));
            return null;
        }

        Long currentUserId = getCurrentUserId();

        Long newAssetId = assetDao.createAsset(assetTag, name, categoryId, purchaseDate, value, currentUserId);

       /* auditLogDao.log(currentUserId, "CREATE", "Asset", newAssetId,
                "Created asset " + assetTag, getRemoteAddress());  
                
            commented the old audit log here new audit will replace this part        
        */

        try
        {
            FacesContext.getCurrentInstance().getExternalContext()
                    .redirect(getRequest().getContextPath() + "/assets");
        }
        catch (IOException e)
        {
            log.error("Failed to redirect after creating asset {}", assetTag, e);
        }
        return null;
    }

    private Long getCurrentUserId()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId() : null;
    }

    private HttpServletRequest getRequest()
    {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    private String getRemoteAddress()
    {
        return getRequest().getRemoteAddr();
    }
}
