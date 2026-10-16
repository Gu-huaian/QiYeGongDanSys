// ============================================
// 主布局组件
// ============================================
qigd.register('main-layout', {
    template: `
    <div class="layout">
        <div class="mobile-mask" :class="{ open: sidebarOpen }" @click="sidebarOpen=false"></div>

        <!-- 侧边栏 -->
        <aside class="sidebar" :class="{ open: sidebarOpen }">
            <div class="sidebar-logo"><i class="el-icon"><component :is="ticketIcon"></component></i> 工单系统</div>
            <nav class="sidebar-menu">
                <div v-for="m in menus" :key="m.key"
                     class="menu-item" :class="{ active: store.activeMenu === m.key }"
                     @click="go(m.key)">
                    <i class="el-icon"><component :is="m.icon"></component></i>
                    <span>{{ m.label }}</span>
                </div>
            </nav>
            <div class="sidebar-user">
                <div style="margin-bottom:6px;font-size:14px;"><i class="el-icon"><component :is="userIcon"></component></i> {{ (store.user && store.user.realName) || '用户' }}</div>
                <div style="font-size:12px;color:rgba(255,255,255,.5)">{{ roleLabel }}</div>
            </div>
        </aside>

        <!-- 主内容 -->
        <div class="main">
            <header class="topbar">
                <div style="display:flex;align-items:center;gap:12px;">
                    <el-button class="menu-toggle" text @click="sidebarOpen=true" :icon="menuIcon"></el-button>
                    <span class="topbar-title">{{ currentTitle }}</span>
                </div>
                <div class="topbar-right">
                    <el-tag v-if="qigd.isAdmin()" type="danger" size="small" effect="dark">管理员</el-tag>
                    <el-dropdown @command="onUserCommand">
                        <span style="cursor:pointer;display:flex;align-items:center;gap:6px;color:#333;outline:none;">
                            <el-avatar :size="30" style="background:#1e3c72;">{{ avatarChar }}</el-avatar>
                            <span style="font-size:14px;">{{ (store.user && store.user.realName) || '用户' }}</span>
                            <el-icon><component :is="arrowDownIcon"></component></el-icon>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="logout" :icon="logoutIcon">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </header>

            <main class="content">
                <component :is="activeComponent"></component>
            </main>
        </div>
    </div>`,
    setup() {
        const { store } = qigd;
        const { ref, computed } = qigd;
        const sidebarOpen = ref(false);

        const ticketIcon = qigd.h(ElementPlusIconsVue.Tickets);
        const userIcon = qigd.h(ElementPlusIconsVue.User);
        const menuIcon = qigd.h(ElementPlusIconsVue.Menu);
        const arrowDownIcon = qigd.h(ElementPlusIconsVue.ArrowDown);
        const logoutIcon = qigd.h(ElementPlusIconsVue.SwitchButton);
        const I = ElementPlusIconsVue;

        const menus = [
            { key: 'dashboard', label: '数据看板', icon: qigd.h(I.DataAnalysis) },
            { key: 'tickets',   label: '工单管理', icon: qigd.h(I.Tickets) },
            { key: 'users',     label: '用户管理', icon: qigd.h(I.User), adminOnly: true },
            { key: 'roles',     label: '角色权限', icon: qigd.h(I.Lock), adminOnly: true },
            { key: 'logs',      label: '操作日志', icon: qigd.h(I.Document), adminOnly: true },
            { key: 'config',    label: '系统配置', icon: qigd.h(I.Setting), adminOnly: true }
        ];

        const visibleMenus = computed(() => menus.filter(m => !m.adminOnly || qigd.isAdmin()));

        const menuTitleMap = {
            dashboard: '数据看板',
            tickets: '工单管理',
            users: '用户管理',
            roles: '角色权限',
            logs: '操作日志',
            config: '系统配置'
        };
        const currentTitle = computed(() => menuTitleMap[store.activeMenu] || '工单系统');

        const roleLabel = computed(() => {
            const r = qigd.getUserRole();
            const map = { ADMIN: '超级管理员', MANAGER: '部门经理', AGENT: '客服工程师', USER: '普通用户' };
            return map[r] || r;
        });

        const avatarChar = computed(() => {
            const name = (store.user && store.user.realName) || '用';
            return name.charAt(0);
        });

        const activeComponent = computed(() => {
            const map = {
                dashboard: 'dashboard-view',
                tickets: 'ticket-list',
                users: 'user-manage',
                roles: 'role-manage',
                logs: 'log-manage',
                config: 'config-manage'
            };
            return qigd.components[map[store.activeMenu]] || qigd.components['dashboard-view'];
        });

        function go(key) {
            store.activeMenu = key;
            localStorage.setItem(qigd.LAST_VIEW, key);
            sidebarOpen.value = false;
        }

        function onUserCommand(cmd) {
            if (cmd === 'logout') {
                qigd.ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
                    .then(() => { qigd.logout(); })
                    .catch(() => {});
            }
        }

        return { store, menus: visibleMenus, sidebarOpen, go, onUserCommand,
            currentTitle, activeComponent, roleLabel, avatarChar, qigd,
            ticketIcon, userIcon, menuIcon, arrowDownIcon, logoutIcon };
    }
});