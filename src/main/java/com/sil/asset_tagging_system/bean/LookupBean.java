package com.sil.asset_tagging_system.bean;

import com.sil.asset_tagging_system.dao.DepartmentDao;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.enums.RoleName;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

@Getter
@Named
@ApplicationScoped
public class LookupBean {
    private final DepartmentDao departmentDao;
    private List<Department> departmentList;

    @Inject
    public LookupBean(DepartmentDao departmentDao)
    {
        this.departmentDao = departmentDao;
    }

    @PostConstruct
    public void init()
    {
        departmentList = departmentDao.findAllDepartments();
    }

    public RoleName[] getRoleOptions()
    {
        return RoleName.values();
    }

}
