// ============================================
// 登录页面组件
// ============================================
qigd.register('login-view', {
    template: `
    <div class="login-wrapper">
        <div class="login-card">
            <div class="login-title">
                <h1>企业工单管理系统</h1>
                <p>Enterprise Ticket Management System</p>
            </div>
            <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large" @submit.prevent>
                <el-form-item prop="username">
                    <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="userIcon" clearable></el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="lockIcon" show-password @keyup.enter="doLogin"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="loading" style="width:100%" @click="doLogin">登 录</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>`,
    setup() {
        const { ref, reactive } = qigd;
        const api = qigd.api;
        const ElMessage = qigd.ElMessage;

        const formRef = ref();
        const loading = ref(false);
        const form = reactive({ username: 'admin', password: '' });
        const rules = {
            username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
            password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
        };

        const userIcon = qigd.h(ElementPlusIconsVue.User);
        const lockIcon = qigd.h(ElementPlusIconsVue.Lock);

        async function doLogin() {
            try {
                await formRef.value.validate();
            } catch (e) {
                return;
            }
            loading.value = true;
            try {
                const resp = await api.post('/auth/login', {
                    username: form.username,
                    password: form.password
                });
                const d = resp.data;
                const user = {
                    id: d.userId,
                    username: d.username,
                    realName: d.realName,
                    roles: d.roles || [],
                    permissions: d.permissions || [],
                    trustedRoles: d.roles || []
                };
                qigd.saveSession(d.token, user);
                qigd.ElMessage.success('登录成功，欢迎 ' + (d.realName || d.username));
                window.location.hash = '#/dashboard';
            } catch (e) {
                // 拦截器已提示
            } finally {
                loading.value = false;
            }
        }

        return { formRef, form, rules, loading, userIcon, lockIcon, doLogin };
    }
});