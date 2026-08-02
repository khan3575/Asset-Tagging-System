package com.sil.asset_tagging_system.dao;

import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;



@Repository
public class UserDao {

    private final EntityManager entityManager;

    UserDao(EntityManager entityManager)
    {
        this.entityManager= entityManager;
    }

    public Optional<User> findByEmailIgnoreCase(String email)
    {
        String sql = """
                SELECT u.id , u.first_name, u.last_name, u.email, u.password, u.enabled, u.created_at, d.id as dept_id
                       ,d.name as dept_name, d.enabled as dept_enabled
                FROM users u
                JOIN departments d
                ON u.dept_id = d.id
                WHERE LOWER(email) = LOWER(:email)
                """;
        List<Object[]> userRows = entityManager.createNativeQuery(sql)
                .setParameter("email", email)
                .getResultList();

        if(userRows.isEmpty())
        {
            return Optional.empty();
        }
        Object[] rows = userRows.getFirst();
        Long userId = ((Number) rows[0]).longValue();
        Department dept = Department.builder().id(((Number) rows[7]).longValue())
                .name((String) rows[8]).enabled((Boolean)rows[9]).build();
        User user = User.builder().id(userId)
                .firstName((String)rows[1])
                .lastName((String)rows[2])
                .email((String) rows[3])
                .password((String) rows[4])
                .department(dept)
                .enabled((Boolean) rows[5])
                .createdAt((LocalDateTime)rows[6])
                .roles(new HashSet<>(findRolesForUser(userId)))
                .build();

        return Optional.of(user);
    }

    public List<Role> findRolesForUser(Long userId)
    {
        String sql = """
                SELECT r.id , r.name
                FROM roles r
                JOIN user_role u ON r.id = u.role_id
                WHERE u.user_id = :userId
                """;
        List<Object[]> roleRows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId).getResultList();

        List<Role> roles = new ArrayList<>();
        for(Object[] rows : roleRows)
        {
            Role role = Role.builder().id(((Number)rows[0]).longValue())
                    .name(RoleName.valueOf((String)rows[1])).build();
            roles.add(role);
        }
        return roles;
    }

    public Boolean existsByEmailIgnoreCase(String email)
    {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE LOWER(email) = LOWER(:email)
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("email", email)
                .getSingleResult();

        return (count.longValue() > 0);
    }

    public Boolean existsByEmailIgnoreCaseAndIdNot(String email, Long userId)
    {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE LOWER(email)= LOWER(:email) and id != :userId
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("email",email )
                .setParameter("userId", userId)
                .getSingleResult();

        return count.longValue() > 0;
    }

    public Optional<User> findByIdAndRoleName(Long userId, RoleName roleName)
    {
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email, u.password, u.enabled, u.created_at, d.id as dept_id, d.name as dept_name
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                JOIN user_role ur ON ur.user_id = u.id
                JOIN roles r ON ur.role_id = r.id
                WHERE u.id = :userId AND r.name = :roleName
                """;
        List<Object[]> rowList = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("roleName", roleName.name())
                .getResultList();
        if(rowList.isEmpty())
        {
            return Optional.empty();
        }

        Object[] rows = rowList.getFirst();
        Department dept = Department.builder().id(((Number)rows[7]).longValue()).name((String)rows[8]).build();

        User user = User.builder()
                .id( ((Number)rows[0]).longValue() )
                .firstName((String) rows[1])
                .lastName((String) rows[2])
                .email((String) rows[3])
                .password((String) rows[4])
                .department(dept)
                .enabled((Boolean) rows[5])
                .createdAt((LocalDateTime) rows[6])
                .build();

        return Optional.of(user);
    }
    public List<User> findEmployees(RoleName roleName, String search, Long deptId, Boolean enabled, int limit , int offset)
    {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.first_name, u.last_name, u.email
                , u.password, u.dept_id, u.enabled, u.created_at, d.id as dept_id, d.name as dept_name, d.enabled as dept_enabled
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                LEFT JOIN user_role ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE 1=1
                """);

        if(roleName != null)
        {
            sql.append(" AND r.name = :roleName");
        }
        if(search != null && !search.trim().isEmpty())
        {
            sql.append(" AND (LOWER(u.first_name) LIKE :search " +
                    "OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search)");
        }
        if(deptId != null)
        {
            sql.append(" AND u.dept_id = :deptId");
        }
        if(enabled != null)
        {
            sql.append(" AND u.enabled = :enabled");
        }

        sql.append(" ORDER BY u.id LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (roleName != null) {
            query.setParameter("roleName", roleName.name());
        }
        if (search != null && !search.trim().isEmpty()) {
            // Wrap search string in wildcards and convert to lowercase for LIKE comparison
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        }
        if (deptId != null) {
            query.setParameter("deptId", deptId);
        }
        if (enabled != null) {
            query.setParameter("enabled", enabled);
        }
        query.setParameter("limit", limit);
        query.setParameter("offset",offset);


        List<Object[]> rowList = query.getResultList();

        if(rowList.isEmpty())
        {
            return new ArrayList<>();
        }
        List<User> userList = new ArrayList<>();

        List<Long> userIds = new ArrayList<>();
        for(Object[] rows: rowList)
        {
            userIds.add(((Number)rows[0]).longValue());
        }

        Map<Long,Set<Role>> roleMap = findRolesForUsers(userIds);

        for(Object[] rows : rowList)
        {
            Department dept =  Department.builder()
                    .id(((Number)rows[8]).longValue())
                    .name((String) rows[9])
                    .enabled((Boolean) rows[10])
                    .build();

            Long userId = ((Number)rows[0]).longValue();
            User user = User.builder()
                    .id(userId)
                    .firstName((String)rows[1])
                    .lastName((String) rows[2])
                    .email((String) rows[3])
                    .password((String) rows[4])
                    .department(dept)
                    .enabled((Boolean) rows[6])
                    .createdAt((LocalDateTime) rows[7])
                    .roles(roleMap.getOrDefault(userId, Set.of()))
                    .build();
            userList.add(user);
        }

        return userList;
    }

    public long countEmployees(RoleName roleName, String search, Long departmentId, Boolean enabled)
    {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT (DISTINCT u.id)
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                LEFT JOIN user_role ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE 1=1
                """);
        if(roleName != null)
        {
            sql.append(" AND r.name = :roleName");
        }
        if(search != null && !search.trim().isEmpty())
        {
            sql.append(" AND (LOWER(u.first_name) LIKE :search OR LOWER(u.last_name) LIKE :search OR LOWER(u.email) LIKE :search)");
        }
        if(departmentId != null)
        {
            sql.append(" AND u.dept_id = :deptId");
        }
        if(enabled != null)
        {
            sql.append(" AND u.enabled = :enabled");
        }

        Query query = entityManager.createNativeQuery(sql.toString());

        if (roleName != null) {
            query.setParameter("roleName", roleName.name());
        }
        if (search != null && !search.trim().isEmpty()) {
            // Wrap search string in wildcards and convert to lowercase for LIKE comparison
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        }
        if (departmentId != null) {
            query.setParameter("deptId", departmentId);
        }
        if (enabled != null) {
            query.setParameter("enabled", enabled);
        }

        return ((Number) query.getSingleResult()).longValue();
    }


    public Map<Long , Set<Role>> findRolesForUsers(List<Long> userList)
    {
        if(userList.isEmpty())
        {
            return Map.of();
        }
        String sql = """
                SELECT ur.user_id, r.id, r.name
                FROM roles r
                JOIN user_role ur ON ur.role_id = r.id
                WHERE ur.user_id IN (:userIds)
                """;

        List<Object[]> resultList = entityManager.createNativeQuery(sql)
                .setParameter("userIds", userList).getResultList();


        Map<Long, Set<Role> > roleMap = new HashMap<>();
        for(Object[] row : resultList)
        {
            Long userId = ((Number)row[0]).longValue();
            Role role = Role.builder().id(((Number)row[1]).longValue())
                    .name(RoleName.valueOf((String)row[2])).build();

            roleMap.computeIfAbsent(userId,k-> new HashSet<>()).add(role);
        }

        return roleMap;
    }

}


