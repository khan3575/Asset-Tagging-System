package com.sil.asset_tagging_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sil.asset_tagging_system.dao.AssetCategoryDao;
import com.sil.asset_tagging_system.dao.DepartmentDao;
import com.sil.asset_tagging_system.model.AssetCategory;
import com.sil.asset_tagging_system.model.Department;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LookupService {
    private final DepartmentDao departmentDao;
    private final AssetCategoryDao assetCategoryDao;

    public List<Department> findAllDepartments() {
        return departmentDao.findAllDepartments();
    }

    public List<AssetCategory> findAllAssetCategories() {
        return assetCategoryDao.findAll();
    }
}
