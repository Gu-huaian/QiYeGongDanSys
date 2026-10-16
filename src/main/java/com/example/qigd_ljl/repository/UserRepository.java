package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(String username);

    List<User> findBySupervisorId(Long supervisorId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.enabled = :enabled AND r.code IN :roleCodes")
    List<User> findByEnabledAndRolesIn(@Param("enabled") boolean enabled,
                                       @Param("roleCodes") List<String> roleCodes);
}