package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByModule(String module);

    boolean existsByCode(String code);
}