// ============================================
// 企业工单管理系统 - 核心工具层
// ============================================

const qigd = {
    Vue,
    axios,
    echarts,
    ElementPlus,

    ref: Vue.ref,
    reactive: Vue.reactive,
    computed: Vue.computed,
    onMounted: Vue.onMounted,
    onBeforeUnmount: Vue.onBeforeUnmount,
    watch: Vue.watch,
    h: Vue.h,

    // Element 工具
    ElMessage: ElementPlus.ElMessage,
    ElMessageBox: ElementPlus.ElMessageBox,
    ElNotification: ElementPlus.ElNotification,

    // 存储 key
    TOKEN_KEY: 'qigd_token',
    USER_KEY: 'qigd_user',
    LAST_VIEW: 'qigd_lastview',

    // 组件注册表
    components: {},

    // 状态
    store: Vue.reactive({
        logged: !!localStorage.getItem('qigd_token'),
        user: JSON.parse(localStorage.getItem('qigd_user') || 'null'),
        activeMenu: localStorage.getItem('qigd_lastview') || 'dashboard'
    }),

    register(name, component) {
        this.components[name] = component;
    },

    getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    getUser() {
        try { return JSON.parse(localStorage.getItem(this.USER_KEY) || 'null'); }
        catch (e) { return null; }
    },

    saveSession(token, user) {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        this.store.logged = true;
        this.store.user = user;
    },

    clearSession() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
        this.store.logged = false;
        this.store.user = null;
    },

    isAdmin() {
        return this.getUserRole() === 'ADMIN';
    },

    getUserRole() {
        const u = qigd.getUser();
        if (u && Array.isArray(u.trustedRoles) && u.trustedRoles.length) return u.trustedRoles[0];
        if (u && Array.isArray(u.roles) && u.roles.length) return u.roles[0];
        return 'USER';
    },

    // API 客户端
    api: axios.create({ baseURL: '/api', timeout: 30000 }),

    fmtTime(t) {
        if (!t) return '-';
        const s = String(t).replace('T', ' ').replace(/\.\d+$/, '');
        return s.substring(0, 16);
    },
    fmtDateTime(t) {
        if (!t) return '-';
        const s = String(t).replace('T', ' ').replace(/\.\d+$/, '');
        return s.substring(0, 19);
    },
    fmtSize(bytes) {
        if (bytes == null) return '-';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    },

    STATUS_META: {
        PENDING:    { label: '待处理',   type: 'warning' },
        ASSIGNED:   { label: '已分配',   type: 'primary' },
        PROCESSING: { label: '处理中',   type: 'danger' },
        PAUSED:     { label: '已暂停',   type: 'info' },
        RESOLVED:   { label: '已解决',   type: 'success' },
        CLOSED:     { label: '已关闭',   type: 'info' },
        REOPENED:   { label: '重新打开', type: 'danger' }
    },

    ASSIGN_META: {
        MANUAL: '手动分配',
        AUTO: '自动分配',
        ROTATION: '轮询分配',
        SKILL_BASED: '技能匹配'
    },

    statusMeta(s) {
        return this.STATUS_META[s] || { label: s, type: 'info' };
    },

    // 登录跳转
    showLogin() {
        this.clearSession();
        this.store.activeMenu = 'dashboard';
        window.location.hash = '#/login';
        if (window.__rootActive && false) {}
    },

    logout() {
        this.clearSession();
        window.location.hash = '#/login';
    }
};

// API 拦截器
qigd.api.interceptors.request.use(config => {
    const token = qigd.getToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

qigd.api.interceptors.response.use(
    response => {
        if (response.config.responseType === 'blob') return response;
        const r = response.data;
        if (r && typeof r === 'object' && 'code' in r) {
            if (r.code === 200) return r;
            const msg = r.message || '操作失败';
            if (r.code === 401) {
                qigd.ElMessage.error('登录已过期，请重新登录');
                qigd.logout();
            } else {
                qigd.ElMessage.error(msg);
            }
            return Promise.reject(new Error(msg));
        }
        return r;
    },
    error => {
        const status = error.response ? error.response.status : 0;
        if (status === 401) {
            qigd.ElMessage.error('登录已过期，请重新登录');
            qigd.logout();
        } else if (status === 403) {
            qigd.ElMessage.error('没有权限执行此操作');
        } else {
            const msg = (error.response && error.response.data && error.response.data.message) || '网络异常，请稍后重试';
            qigd.ElMessage.error(msg);
        }
        return Promise.reject(error);
    }
);