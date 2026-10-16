// ============================================
// 应用挂载入口
// ============================================

// 注册所有组件（各文件已定义）
function ensureComponents() {
    const required = ['login-view', 'main-layout', 'dashboard-view', 'ticket-list',
        'user-manage', 'role-manage', 'log-manage', 'config-manage'];
    for (const name of required) {
        if (!qigd.components[name]) {
            console.error('缺少组件: ' + name);
        }
    }
}

const RootComponent = {
    setup() {
        const store = qigd.store;
        const activeView = Vue.ref('main');
        const currentHash = Vue.ref(window.location.hash);

        // 简单 hash 路由
        function parseHash() {
            const h = currentHash.value || '#/dashboard';
            const m = h.match(/^#\/([\w-]+)/);
            const view = m ? m[1] : 'dashboard';
            if (view === 'login') {
                qigd.clearSession();
            }
            if (store.logged && view !== 'login') {
                const map = { dashboard: 'dashboard', tickets: 'tickets', users: 'users', config: 'config', roles: 'roles', logs: 'logs' };
                if (map[view]) {
                    store.activeMenu = map[view];
                    localStorage.setItem(qigd.LAST_VIEW, view);
                }
                activeView.value = 'main';
            } else if (!store.logged) {
                activeView.value = 'login';
            } else if (store.logged && view === 'login') {
                activeView.value = 'main';
            }
        }

        Vue.watch(() => store.logged, () => parseHash());
        Vue.watch(currentHash, () => parseHash());

        Vue.onMounted(() => {
            window.addEventListener('hashchange', () => {
                currentHash.value = window.location.hash;
            });
            parseHash();
            ensureComponents();
        });

        return { activeView };
    },
    template: `<login-view v-if="activeView==='login'"></login-view><main-layout v-else></main-layout>`
};

const app = Vue.createApp(RootComponent);

// 全局注册 Element Plus 图标
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, comp);
}

// 注册所有业务组件
for (const [name, comp] of Object.entries(qigd.components)) {
    app.component(name, comp);
}

app.use(ElementPlus);
app.mount('#app');