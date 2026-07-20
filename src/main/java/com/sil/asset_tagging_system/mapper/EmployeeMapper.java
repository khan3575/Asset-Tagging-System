package com.sil.asset_tagging_system.mapper;

import com.sil.asset_tagging_system.dto.response.EmployeeResponse;
import com.sil.asset_tagging_system.dto.response.EmployeeSummaryResponse;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(User employee) {

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getEnabled(),
                extractRoles(employee),
                employee.getCreatedAt()
        );
    }

    public EmployeeSummaryResponse toSummaryResponse(User employee) {

        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getEnabled(),
                employee.getCreatedAt()
        );
    }

    private List<String> extractRoles(User employee) {

        return employee.getRoles()
                .stream()
                .map(Role::getName)
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}