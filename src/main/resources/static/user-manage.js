// ============================================
// 用户管理组件
// ============================================
qigd.register('user-manage', {
    template: `
    <div class="page-card">
        <div class="card-header">
            <div style="font-weight:600;font-size:16px;">用户管理</div>
            <div>
                <el-button v-if="qigd.isAdmin()" type="primary" :icon="plusIcon" @click="openCreate">新增用户</el-button>
            </div>
        </div>
        <div class="card-body">
            <div class="toolbar">
                <el-input v-model="query.keyword" placeholder="搜索用户名/姓名/邮箱" clearable style="width:220px" @clear="load" @keyup.enter="load" :prefix-icon="searchIcon"></el-input>
                <el-button @click="load">查询</el-button>
            </div>

            <el-table :data="list" v-loading="loading" stripe style="width:100%">
                <el-table-column label="用户名" prop="username" width="130"></el-table-column>
                <el-table-column label="姓名" prop="realName" width="120"></el-table-column>
                <el-table-column label="部门" prop="department" width="140"></el-table-column>
                <el-table-column label="直属上级" width="130">
                    <template #default="{row}">
                        <span v-if="row.supervisor">{{ row.supervisor.realName }}（{{ row.supervisor.department || '无部门' }}）</span>
                        <span v-else style="color:#c0c4cc;">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="邮箱" prop="email" min-width="180"></el-table-column>
                <el-table-column label="手机" prop="phone" width="130"></el-table-column>
                <el-table-column label="角色" min-width="140">
                    <template #default="{row}">
                        <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin:2px;" :type="r.code==='ADMIN'?'danger':'primary'">{{ r.name || r.code }}</el-tag>
                        <span v-if="!row.roles || !row.roles.length">-</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{row}">
                        <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" width="140">
                    <template #default="{row}">{{ qigd.fmtDateTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column v-if="qigd.isAdmin()" label="操作" width="140" fixed="right">
                    <template #default="{row}">
                        <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" size="small" @click="removeUser(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div style="display:flex;justify-content:flex-end;margin-top:16px;">
                <el-pagination background layout="total, sizes, prev, pager, next"
                    :total="total" :page-size="query.size" :current-page="query.page+1"
                    :page-sizes="[10,20,50]" @current-change="onPage" @size-change="onSize"></el-pagination>
            </div>
        </div>

        <!-- 新增/编辑用户 -->
        <el-dialog v-model="visible" :title="editing ? '编辑用户' : '新增用户'" width="560px" :close-on-click-modal="false">
            <el-form :model="form" label-width="80px" ref="formRef" :rules="rules">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" :disabled="!!editing" placeholder="登录用户名"></el-input>
                </el-form-item>
                <el-form-item label="姓名" prop="realName">
                    <el-input v-model="form.realName" placeholder="真实姓名"></el-input>
                </el-form-item>
                <el-form-item v-if="!editing" label="密码" prop="password">
                    <el-input v-model="form.password" type="password" placeholder="初始密码" show-password></el-input>
                </el-form-item>
                <el-form-item v-else label="重置密码">
                    <el-input v-model="form.password" type="password" placeholder="留空则不修改" show-password></el-input>
                </el-form-item>
                <el-form-item label="部门">
                    <el-input v-model="form.department" placeholder="所属部门"></el-input>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="form.email" placeholder="邮箱地址"></el-input>
                </el-form-item>
                <el-form-item label="手机">
                    <el-input v-model="form.phone" placeholder="手机号码"></el-input>
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="form.roleIds" multiple placeholder="分配角色" style="width:100%">
                        <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="直属上级">
                    <el-select v-model="form.supervisorId" clearable placeholder="选择直属上级（用于层级沟通）" style="width:100%">
                        <el-option v-for="u in allUsers" :key="u.id" :value="u.id" :label="u.realName + '（' + (u.department||'') + '）'"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用"></el-switch>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="visible=false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                </div>
            </template>
        </el-dialog>
    </div>`,
    setup() {
        const { ref, reactive, onMounted } = qigd;
        const api = qigd.api;
        const ElMessage = qigd.ElMessage;
        const ElMessageBox = qigd.ElMessageBox;

        const list = ref([]);
        const allUsers = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const roles = ref([]);
        const query = reactive({ keyword: '', page: 0, size: 10 });

        const visible = ref(false);
        const editing = ref(false);
        const saving = ref(false);
        const formRef = ref();
        const form = reactive({
            id: null, username: '', realName: '', password: '', department: '',
            email: '', phone: '', roleIds: [], supervisorId: null, enabled: true
        });
        const rules = {
            username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
            realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
            password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
        };

        const searchIcon = qigd.h(ElementPlusIconsVue.Search);
        const plusIcon = qigd.h(ElementPlusIconsVue.Plus);

        async function load() {
            loading.value = true;
            try {
                const params = { page: query.page, size: query.size };
                if (query.keyword) params.keyword = query.keyword;
                const resp = await api.get('/users', { params });
                list.value = resp.data.content || [];
                total.value = resp.data.totalElements || 0;
            } catch (e) {} finally {
                loading.value = false;
            }
        }

        async function loadRoles() {
            try {
                const resp = await api.get('/roles');
                roles.value = resp.data || [];
            } catch (e) {}
        }

        async function loadAllUsers() {
            try {
                const resp = await api.get('/users/all');
                allUsers.value = resp.data || [];
            } catch (e) {}
        }

        function onPage(p) { query.page = p - 1; load(); }
        function onSize(s) { query.size = s; query.page = 0; load(); }

        function openCreate() {
            editing.value = false;
            Object.assign(form, { id: null, username: '', realName: '', password: '', department: '', email: '', phone: '', roleIds: [], supervisorId: null, enabled: true });
            visible.value = true;
        }

        function openEdit(row) {
            editing.value = true;
            Object.assign(form, {
                id: row.id, username: row.username, realName: row.realName, password: '',
                department: row.department, email: row.email, phone: row.phone,
                roleIds: (row.roles || []).map(r => r.id),
                supervisorId: row.supervisor ? row.supervisor.id : null,
                enabled: !!row.enabled
            });
            visible.value = true;
        }

        async function save() {
            try {
                await formRef.value.validate();
            } catch (e) { return; }
            saving.value = true;
            try {
                const payload = {
                    username: form.username,
                    realName: form.realName,
                    password: form.password,
                    department: form.department,
                    email: form.email,
                    phone: form.phone,
                    roleIds: form.roleIds,
                    supervisorId: form.supervisorId || null,
                    enabled: form.enabled
                };
                if (editing.value) {
                    await api.put('/users/' + form.id, payload);
                    ElMessage.success('用户已更新');
                } else {
                    await api.post('/users', payload);
                    ElMessage.success('用户已创建');
                }
                visible.value = false;
                load();
            } catch (e) {} finally {
                saving.value = false;
            }
        }

        async function removeUser(row) {
            try {
                await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '删除确认', { type: 'warning' });
                await api.delete('/users/' + row.id);
                ElMessage.success('删除成功');
                load();
            } catch (e) {}
        }

        onMounted(() => { loadRoles(); loadAllUsers(); load(); });

        return { list, allUsers, total, loading, roles, query, qigd, visible, editing, saving, form, formRef, rules,
            searchIcon, plusIcon, load, onPage, onSize, openCreate, openEdit, save, removeUser };
    }
});