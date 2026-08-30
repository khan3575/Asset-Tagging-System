package com.sil.asset_tagging_system.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.service.LookupService;

import lombok.Getter;

@Getter
@Named
@RequestScoped
public class LookupBean {
    private final LookupService lookupService;
    private List<Department> departmentList;
    private List<AssetCategory> assetCategoryList;

    @Inject
    public LookupBean(LookupService lookupService)
    {
        this.lookupService = lookupService;
    }

    @PostConstruct
    public void init()
    {
        departmentList = lookupService.findAllDepartments();
        assetCategoryList = lookupService.findAllAssetCategories();
    }

    public RoleName[] getRoleOptions()
    {
        return RoleName.values();
    }

    public Map<String, String> getDepartmentOptionsMap()
    {
        Map<String, String> options = new LinkedHashMap<>();
        for (Department dept : departmentList) {
            options.put(String.valueOf(dept.getId()), dept.getName());
        }
        return options;
    }

}
