package com.sil.asset_tagging_system.bean;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.AssetCategoryDao;
import com.sil.asset_tagging_system.dao.DepartmentDao;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.enums.RoleName;

import lombok.Getter;

@Getter
@Named
@ApplicationScoped
public class LookupBean {
    private final DepartmentDao departmentDao;
    private final AssetCategoryDao assetCategoryDao;
    private List<Department> departmentList;
    private List<AssetCategory> assetCategoryList;

    @Inject
    public LookupBean(DepartmentDao departmentDao, AssetCategoryDao assetCategoryDao)
    {
        this.departmentDao = departmentDao;
        this.assetCategoryDao = assetCategoryDao;
    }

    @PostConstruct
    public void init()
    {
        departmentList = departmentDao.findAllDepartments();
        assetCategoryList = assetCategoryDao.findAll();
    }

    public RoleName[] getRoleOptions()
    {
        return RoleName.values();
    }

}
