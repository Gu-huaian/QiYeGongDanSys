package com.example.qigd_ljl.config;

import com.example.qigd_ljl.entity.Permission;
import com.example.qigd_ljl.entity.Role;
import com.example.qigd_ljl.entity.User;
import com.example.qigd_ljl.repository.PermissionRepository;
import com.example.qigd_ljl.repository.RoleRepository;
import com.example.qigd_ljl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initPermissions();
        initRoles();
        initAdminUser();
        initDemoUsers();
        log.info("========== 数据初始化完成 ==========");
    }

    private void initPermissions() {
        String[][] perms = {
                {"ticket:view", "查看工单", "工单管理"},
                {"ticket:create", "创建工单", "工单管理"},
                {"ticket:edit", "编辑工单", "工单管理"},
                {"ticket:delete", "删除工单", "工单管理"},
                {"ticket:assign", "分配工单", "工单管理"},
                {"ticket:close", "关闭工单", "工单管理"},
                {"ticket:comment", "评论工单", "工单管理"},
                {"ticket:stats", "统计分析", "工单管理"},
                {"user:view", "查看用户", "用户管理"},
                {"user:create", "创建用户", "用户管理"},
                {"user:edit", "编辑用户", "用户管理"},
                {"user:delete", "删除用户", "用户管理"},
                {"role:view", "查看角色", "角色管理"},
                {"role:create", "创建角色", "角色管理"},
                {"role:edit", "编辑角色", "角色管理"},
                {"role:delete", "删除角色", "角色管理"},
                {"category:view", "查看分类", "基础配置"},
                {"category:manage", "管理分类", "基础配置"},
                {"priority:view", "查看优先级", "基础配置"},
                {"priority:manage", "管理优先级", "基础配置"},
                {"log:view", "查看操作日志", "系统日志"}
        };

        for (String[] p : perms) {
            if (!permissionRepository.existsByCode(p[0])) {
                Permission permission = new Permission();
                permission.setCode(p[0]);
                permission.setName(p[1]);
                permission.setModule(p[2]);
                permissionRepository.save(permission);
            }
        }
        log.info("权限初始化完成，共 {} 项", permissionRepository.count());
    }

    private void initRoles() {
        createRoleIfNotExists("ADMIN", "超级管理员", "拥有所有权限", true, null);
        createRoleIfNotExists("MANAGER", "部门经理", "管理部门工单和用户", false,
                Set.of("ticket:view", "ticket:create", "ticket:edit", "ticket:assign", "ticket:close",
                        "ticket:comment", "ticket:stats", "user:view", "category:view", "priority:view", "log:view"));
        createRoleIfNotExists("AGENT", "客服工程师", "处理分配给自己的工单", false,
                Set.of("ticket:view", "ticket:create", "ticket:edit", "ticket:close",
                        "ticket:comment", "ticket:stats", "category:view", "priority:view"));
        createRoleIfNotExists("USER", "普通用户", "创建和查看自己的工单", false,
                Set.of("ticket:view", "ticket:create", "ticket:comment"));
    }

    private void createRoleIfNotExists(String code, String name, String desc, boolean allPerms, Set<String> permCodes) {
        if (!roleRepository.existsByCode(code)) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setDescription(desc);
            role.setEnabled(true);

            if (allPerms) {
                role.setPermissions(new HashSet<>(permissionRepository.findAll()));
            } else if (permCodes != null) {
                Set<Permission> perms = new HashSet<>();
                for (Permission p : permissionRepository.findAll()) {
                    if (permCodes.contains(p.getCode())) {
                        perms.add(p);
                    }
                }
                role.setPermissions(perms);
            }

            roleRepository.save(role);
            log.info("角色已创建: {}", code);
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@qigd.com");
            admin.setDepartment("信息中心");
            admin.setEnabled(true);

            Role adminRole = roleRepository.findByCodeWithPermissions("ADMIN").orElse(null);
            if (adminRole != null) {
                admin.setRoles(new HashSet<>(Set.of(adminRole)));
            }

            userRepository.save(admin);
            log.info("管理员用户已创建: admin / admin123");
        }
    }

    private void initDemoUsers() {
        // 建立组织上下级关系，覆盖完整层级：
        // admin(信息中心) <-> manager(运营部经理) 管理 agent(工程师) 和 各普通用户
        Object[][] demos = {
                {"manager", "manager123", "经理小王", "manager@qigd.com", "运营部", "MANAGER", "admin"},
                {"agent",   "agent123",   "工程师小李", "agent@qigd.com", "客服中心", "AGENT", "manager"},
                {"agent2",  "agent123",   "工程师小赵", "agent2@qigd.com", "客服中心", "AGENT", "manager"},
                {"user01",  "user123",    "普通用户小张", "user01@qigd.com", "财务部", "USER", "manager"},
                {"user02",  "user123",    "普通用户小王", "user02@qigd.com", "市场部", "USER", "manager"},
                {"user03",  "user123",    "普通用户小李", "user03@qigd.com", "技术部", "USER", "agent"}
        };
        for (Object[] d : demos) {
            String username = (String) d[0];
            String supervisorName = (String) d[6];

            // 已存在用户：仅补设缺失的上级关系，不覆盖其他数据
            if (userRepository.existsByUsername(username)) {
                userRepository.findByUsername(username).ifPresent(u -> {
                    if (u.getSupervisor() == null && !"manager".equals(username)) {
                        userRepository.findByUsername(supervisorName).ifPresent(u::setSupervisor);
                    }
                });
                continue;
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode((String) d[1]));
            user.setRealName((String) d[2]);
            user.setEmail((String) d[3]);
            user.setDepartment((String) d[4]);
            user.setEnabled(true);

            Role role = roleRepository.findByCodeWithPermissions((String) d[5]).orElse(null);
            if (role != null) {
                user.setRoles(new HashSet<>(Set.of(role)));
            }

            userRepository.findByUsername(supervisorName).ifPresent(user::setSupervisor);

            userRepository.save(user);
            log.info("演示用户已创建: {} / {}，上级: {}", username, d[1], supervisorName);
        }
    }
}