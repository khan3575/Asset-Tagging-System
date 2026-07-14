package com.sil.asset_tagging_system.service;

import com.sil.asset_tagging_system.dto.request.CreateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.ResetEmployeePasswordRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeRequest;
import com.sil.asset_tagging_system.dto.request.UpdateEmployeeStatusRequest;

import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.dto.response.PageResponse;


public interface EmployeeService {


    EmployeeResponse createEmployee(
            CreateEmployeeRequest request
    );


    EmployeeResponse getEmployeeById(
            Long employeeId
    );

    PageResponse<EmployeeSummaryResponse> getAllEmployees(

            int page,

            int size,

            String search,

            Long departmentId,

            Boolean enabled,

            String sortBy,

            String direction
    );

    EmployeeResponse updateEmployee(

            Long employeeId,

            UpdateEmployeeRequest request
    );

    EmployeeResponse updateEmployeeStatus(

            Long employeeId,

            UpdateEmployeeStatusRequest request
    );


    void resetEmployeePassword(

            Long employeeId,

            ResetEmployeePasswordRequest request
    );

}