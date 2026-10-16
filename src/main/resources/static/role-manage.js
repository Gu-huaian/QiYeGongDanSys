// ============================================
// 角色权限管理组件
// ============================================
qigd.register('role-manage', {
    template: `
    <div class="page-card">
        <div class="card-header">
            <div style="font-weight:600;font-size:16px;">角色权限管理</div>
            <div>
                <el-button type="primary" :icon="plusIcon" @click="openCreate">新增角色</el-button>
            </div>
        </div>
        <div class="card-body">
            <el-table :data="list" v-loading="loading" stripe style="width:100%">
                <el-table-column label="角色编码" prop="code" width="130">
                    <template #default="{row}">
                        <el-tag :type="row.code==='ADMIN'?'danger':'primary'" effect="dark" size="small">{{ row.code }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="角色名称" prop="name" width="140"></el-table-column>
                <el-table-column label="描述" prop="description" min-width="180"></el-table-column>
                <el-table-column label="权限数" width="90">
                    <template #default="{row}">{{ row.permissions ? row.permissions.length : 0 }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{row}">
                        <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="{row}">
                        <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button link type="danger" size="small" :disabled="row.code==='ADMIN'" @click="removeRole(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 新增/编辑角色 -->
        <el-dialog v-model="visible" :title="editing ? '编辑角色' : '新增角色'" width="600px" :close-on-click-modal="false">
            <el-form :model="form" label-width="80px" ref="formRef" :rules="rules">
                <el-form-item label="角色名称" prop="name">
                    <el-input v-model="form.name" placeholder="角色名称"></el-input>
                </el-form-item>
                <el-form-item label="角色编码" prop="code">
                    <el-input v-model="form.code" :disabled="!!editing" placeholder="如 MANAGER，大写字母" style="text-transform:uppercase"></el-input>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述"></el-input>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用"></el-switch>
                </el-form-item>
                <el-form-item label="权限">
                    <div style="width:100%;max-height:280px;overflow:auto;border:1px solid #ebeef5;border-radius:6px;padding:12px;">
                        <div v-for="mod in permissionGroups" :key="mod.module" style="margin-bottom:12px;">
                            <div style="font-weight:600;margin-bottom:6px;color:#1e3c72;">{{ mod.module }}</div>
                            <el-checkbox :model-value="isModuleAllChecked(mod)" :indeterminate="isModuleIndeterminate(mod)" @change="toggleModule(mod, $event)">
                                {{ mod.module == '全选' ? '全选' : mod.module }}
                            </el-checkbox>
                            <div style="margin-top:4px;">
                                <el-checkbox v-for="p in mod.items" :key="p.id" v-model="form.permissionIds" :label="p.id" style="margin-right:12px;">{{ p.name }}</el-checkbox>
                            </div>
                        </div>
                    </div>
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
        const { ref, reactive, computed, onMounted } = qigd;
        const api = qigd.api;
        const ElMessage = qigd.ElMessage;
        const ElMessageBox = qigd.ElMessageBox;
        const I = ElementPlusIconsVue;

        const list = ref([]);
        const loading = ref(false);
        const permissions = ref([]);
        const visible = ref(false);
        const editing = ref(false);
        const saving = ref(false);
        const formRef = ref();
        const form = reactive({
            id: null, name: '', code: '', description: '', enabled: true, permissionIds: []
        });
        const rules = {
            name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
            code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
        };

        const permissionGroups = computed(() => {
            const byModule = {};
            perms: for (const p of permissions.value) {
                const mod = p.module || '其他';
                if (!byModule[mod]) byModule[mod] = [];
                byModule[mod].push(p);
            }
            const groups = Object.keys(byModule).map(mod => ({ module: mod, items: byModule[mod] }));
            return groups;
        });

        function isModuleAllChecked(mod) {
            return mod.items.every(p => form.permissionIds.includes(p.id));
        }
        function isModuleIndeterminate(mod) {
            const checked = mod.items.some(p => form.permissionIds.includes(p.id));
            return checked && !isModuleAllChecked(mod);
        }
        function toggleModule(mod, checked) {
            if (checked) {
                mod.items.forEach(p => { if (!form.permissionIds.includes(p.id)) form.permissionIds.push(p.id); });
            } else {
                form.permissionIds = form.permissionIds.filter(id => !mod.items.some(p => p.id === id));
            }
        }

        async function load() {
            loading.value = true;
            try {
                const resp = await api.get('/roles');
                list.value = resp.data || [];
            } catch (e) {} finally {
                loading.value = false;
            }
        }

        async function loadPermissions() {
            try {
                const resp = await api.get('/roles/permissions');
                permissions.value = resp.data || [];
            } catch (e) {}
        }

        function openCreate() {
            editing.value = false;
            Object.assign(form, { id: null, name: '', code: '', description: '', enabled: true, permissionIds: [] });
            visible.value = true;
        }

        function openEdit(row) {
            editing.value = true;
            Object.assign(form, {
                id: row.id, name: row.name, code: row.code,
                description: row.description, enabled: row.enabled,
                permissionIds: row.permissions ? row.permissions.map(p => p.id) : []
            });
            visible.value = true;
        }

        async function save() {
            await formRef.value.validate();
            const payload = {
                name: form.name, code: form.code,
                description: form.description, enabled: form.enabled,
                permissionIds: form.permissionIds
            };
            saving.value = true;
            try {
                if (editing.value) {
                    await api.put('/roles/' + form.id, payload);
                    ElMessage.success('角色已更新');
                } else {
                    await api.post('/roles', payload);
                    ElMessage.success('角色已创建');
                }
                visible.value = false;
                load();
            } catch (e) {} finally {
                saving.value = false;
            }
        }

        function removeRole(row) {
            ElMessageBox.confirm('确定要删除角色「' + row.name + '」吗？', '提示', { type: 'warning' })
                .then(async () => {
                    try {
                        await api.delete('/roles/' + row.id);
                        ElMessage.success('角色已删除');
                        load();
                    } catch (e) {}
                })
                .catch(() => {});
        }

        onMounted(() => { load(); loadPermissions(); });

        return { list, loading, visible, editing, saving, form, formRef, rules,
            permissionGroups, isModuleAllChecked, isModuleIndeterminate, toggleModule,
            openCreate, openEdit, save, removeRole,
            plusIcon: qigd.h(I.Plus) };
    }
});