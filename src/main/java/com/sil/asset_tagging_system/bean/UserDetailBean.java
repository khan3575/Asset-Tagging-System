package com.sil.asset_tagging_system.bean;

import com.sil.asset_tagging_system.dao.DepartmentDao;
import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.exception.DbFetchException;
import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Named
@RequestScoped
public class UserDetailBean {
    private final UserDao userDao;
    private final DepartmentDao deptDao;
    private Long id;
    private User user;
    private List<Department> departmentList;

    @Inject
    public UserDetailBean(UserDao userDao, DepartmentDao deptDao)
    {
        this.userDao = userDao;
        this.deptDao = deptDao;
    }


    @PostConstruct
    public void init()
    {
        initiateUser();
        initiateDepartments();

    }

    public void initiateDepartments()
    {
        this.departmentList = deptDao.findAllDepartments();
    }

    public void initiateUser(){
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String idParam = params.get("id");

        if( idParam==null || idParam.isBlank())
        {
            user = null;
        }
        else{
            this.id = Long.valueOf(idParam);
            user = userDao.findById(id).orElseThrow(()->new DbFetchException("User fetching error from DB"));
        }
    }

}
