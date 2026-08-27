package com.sil.asset_tagging_system.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;



@Repository
public class UserDao {

    private final EntityManager entityManager;

    UserDao(EntityManager entityManager)
    {
        this.entityManager= entityManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<User> findByEmailIgnoreCase(String email)
    {
        String sql = """
                SELECT u.id , u.first_name, u.last_name, u.email, u.password_hash, u.enabled, u.created_at, d.id as dept_id
                       ,d.name as dept_name, d.closed_at as dept_closed_at
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
                .name((String) rows[8]).closedAt((LocalDateTime)rows[9]).build();
        User user = User.builder().id(userId)
                .firstName((String)rows[1])
                .lastName((String)rows[2])
                .email((String) rows[3])
                .passwordHash((String) rows[4])
                .department(dept)
                .enabled((Boolean) rows[5])
                .createdAt((LocalDateTime)rows[6])
                .roles(new HashSet<>(findRolesForUser(userId)))
                .build();

        return Optional.of(user);
    }

    @SuppressWarnings("unchecked")
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
        return DaoUtils.exists(entityManager, sql, Map.of("email", email));
    }

    public Boolean existsByEmailIgnoreCaseAndIdNot(String email, Long userId)
    {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE LOWER(email)= LOWER(:email) and id != :userId
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("email", email, "userId", userId));
    }

    @SuppressWarnings("unchecked")
    public Optional<User> findByIdAndRoleName(Long userId, RoleName roleName)
    {
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email, u.password_hash
                , u.enabled, u.created_at, d.id as dept_id, d.name as dept_name
                , d.closed_at as closed_at
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
        Department dept = Department.builder().id(((Number)rows[7])
        .longValue()).name((String)rows[8]).closedAt((LocalDateTime)rows[9]).build();

        User user = User.builder()
                .id( ((Number)rows[0]).longValue() )
                .firstName((String) rows[1])
                .lastName((String) rows[2])
                .email((String) rows[3])
                .passwordHash((String) rows[4])
                .department(dept)
                .enabled((Boolean) rows[5])
                .createdAt((LocalDateTime) rows[6])
                .build();

        return Optional.of(user);
    }
    private void appendUserFilters(StringBuilder sql, RoleName roleName, String search, Long deptId, Boolean enabled)
    {
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
    }

    private void bindUserFilters(Query query, RoleName roleName, String search, Long deptId, Boolean enabled)
    {
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
    }

    @SuppressWarnings("unchecked")
    public List<User> findUsers(RoleName roleName, String search, Long deptId, Boolean enabled, int limit , int offset)
    {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.first_name, u.last_name, u.email
                , u.password_hash, u.dept_id, u.enabled, u.created_at, d.id as dept_id, d.name as dept_name, d.closed_at as closed_at
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                LEFT JOIN user_role ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE 1=1
                """);

        appendUserFilters(sql, roleName, search, deptId, enabled);

        sql.append(" ORDER BY u.id LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());

        bindUserFilters(query, roleName, search, deptId, enabled);
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
                    .closedAt((LocalDateTime)rows[10])
                    .build();

            Long userId = ((Number)rows[0]).longValue();
            User user = User.builder()
                    .id(userId)
                    .firstName((String)rows[1])
                    .lastName((String) rows[2])
                    .email((String) rows[3])
                    .passwordHash((String) rows[4])
                    .department(dept)
                    .enabled((Boolean) rows[6])
                    .createdAt((LocalDateTime) rows[7])
                    .roles(roleMap.getOrDefault(userId, Set.of()))
                    .build();
            userList.add(user);
        }

        return userList;
    }

    public long countUsers(RoleName roleName, String search, Long departmentId, Boolean enabled)
    {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                LEFT JOIN user_role ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE 1=1
                """);

        appendUserFilters(sql, roleName, search, departmentId, enabled);

        Query query = entityManager.createNativeQuery(sql.toString());

        bindUserFilters(query, roleName, search, departmentId, enabled);

        return ((Number) query.getSingleResult()).longValue();
    }


    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public Optional<User> findById(Long id)
    {
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email,
                u.enabled, u.created_at, u.dept_id, d.id as dept_id, d.name as dept_name, d.closed_at as dept_closed_at
                FROM users u
                JOIN departments d ON u.dept_id = d.id
                WHERE u.id = :id
                """;

        List<Object[]> rowList = entityManager.createNativeQuery(sql)
                .setParameter("id", id).getResultList();

        if(rowList.isEmpty())
        {
            return Optional.empty();
        }

        Object[] row = rowList.getFirst();
        Long userId = ((Number)row[0]).longValue();

        Department dept = Department.builder().id(((Number)row[7])
                .longValue()).name((String)row[8]).closedAt((LocalDateTime)row[9]).build();


        User user = User.builder().id(userId)
                .firstName((String)row[1])
                .lastName((String)row[2])
                .email((String)row[3])
                .department(dept)
                .enabled((Boolean) row[4])
                .roles(new HashSet<>( findRolesForUser(userId)))
                .createdAt((LocalDateTime)row[5]).build();
        return Optional.of(user);
    }


    public void updateUser(Long id , String firstName, String lastName, Long departmentId, Boolean enabled)
    {
        String sql = """
                UPDATE users
                SET first_name = :firstName, last_name = :lastName, dept_id = :departmentId, enabled = :enabled
                WHERE id = :id
                """;
        entityManager.createNativeQuery(sql)
            .setParameter("id", id)
            .setParameter("firstName", firstName)
            .setParameter("lastName", lastName)
            .setParameter("departmentId",departmentId)
            .setParameter("enabled", enabled)
            .executeUpdate();
    }

    public void replaceRoles(Long userId, Set<RoleName> roleNames)
    {
        // deleting old roles
        entityManager.createNativeQuery("DELETE FROM user_role WHERE user_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
        // assigning new roles;
        String insertSql = "INSERT INTO user_role (user_id, role_id) SELECT :userId, id FROM roles WHERE name = :roleName";
        for (RoleName roleName : roleNames) {
            entityManager.createNativeQuery(insertSql)
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName.name())
                    .executeUpdate();
        }

    }
}


