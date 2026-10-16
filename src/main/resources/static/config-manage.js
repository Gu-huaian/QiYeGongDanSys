// ============================================
// 系统配置组件（分类 + 优先级管理）
// ============================================
qigd.register('config-manage', {
    template: `
    <div>
        <el-tabs v-model="activeTab">
            <!-- 分类管理 -->
            <el-tab-pane label="工单分类" name="categories">
                <div class="page-card">
                    <div class="card-header">
                        <div style="font-weight:600;">工单分类</div>
                        <el-button v-if="qigd.isAdmin()" type="primary" :icon="plusIcon" @click="openCategoryCreate(null)">新增分类</el-button>
                    </div>
                    <div class="card-body">
                        <el-table :data="categoryTree" v-loading="catLoading" row-key="id" default-expand-all stripe style="width:100%">
                            <el-table-column label="名称" min-width="180" prop="name"></el-table-column>
                            <el-table-column label="编码" width="130" prop="code"></el-table-column>
                            <el-table-column label="描述" min-width="220" prop="description"></el-table-column>
                            <el-table-column label="状态" width="90">
                                <template #default="{row}">
                                    <el-tag size="small" :type="row.enabled?'success':'info'">{{ row.enabled?'启用':'禁用' }}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column v-if="qigd.isAdmin()" label="操作" width="180" fixed="right">
                                <template #default="{row}">
                                    <el-button link type="primary" size="small" @click="openCategoryCreate(row)">编辑</el-button>
                                    <el-button link type="success" size="small" @click="openCategoryCreate(null)">新增子类</el-button>
                                    <el-button link type="danger" size="small" @click="removeCategory(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </div>
            </el-tab-pane>

            <!-- 优先级管理 -->
            <el-tab-pane label="优先级" name="priorities">
                <div class="page-card">
                    <div class="card-header">
                        <div style="font-weight:600;">工单优先级</div>
                        <el-button v-if="qigd.isAdmin()" type="primary" :icon="plusIcon" @click="openPriorityCreate(null)">新增优先级</el-button>
                    </div>
                    <div class="card-body">
                        <el-table :data="priorityList" v-loading="priLoading" stripe style="width:100%">
                            <el-table-column label="名称" width="140">
                                <template #default="{row}">
                                    <el-tag :color="row.color || '#409eff'" style="color:#fff;border:none;">{{ row.name }}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column label="编码" width="130" prop="code"></el-table-column>
                            <el-table-column label="级别" width="80" prop="level"></el-table-column>
                            <el-table-column label="描述" min-width="220" prop="description"></el-table-column>
                            <el-table-column label="状态" width="90">
                                <template #default="{row}">
                                    <el-tag size="small" :type="row.enabled?'success':'info'">{{ row.enabled?'启用':'禁用' }}</el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column v-if="qigd.isAdmin()" label="操作" width="130" fixed="right">
                                <template #default="{row}">
                                    <el-button link type="primary" size="small" @click="openPriorityCreate(row)">编辑</el-button>
                                    <el-button link type="danger" size="small" @click="removePriority(row)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 分类对话框 -->
        <el-dialog v-model="catVisible" :title="categoryEditing ? '编辑分类' : '新增分类'" width="480px" :close-on-click-modal="false">
            <el-form :model="catForm" label-width="80px">
                <el-form-item label="父分类">
                    <el-select v-model="catForm.parentId" placeholder="选择父分类(可选)" clearable filterable style="width:100%" :disabled="!!catEditing && catEditing.parentId===null && catForm.parentId===null">
                        <el-option v-for="c in categoryFlat" :key="c.id" :label="c.name" :value="c.id" :disabled="c.id===catEditing?.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="名称" required>
                    <el-input v-model="catForm.name" placeholder="分类名称"></el-input>
                </el-form-item>
                <el-form-item label="编码">
                    <el-input v-model="catForm.code" placeholder="分类编码"></el-input>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="catForm.description" type="textarea" :rows="3" placeholder="分类描述"></el-input>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="catForm.enabled"></el-switch>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="catVisible=false">取消</el-button>
                    <el-button type="primary" :loading="catSaving" @click="saveCategory">保存</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 优先级对话框 -->
        <el-dialog v-model="priVisible" :title="priorityEditing ? '编辑优先级' : '新增优先级'" width="480px" :close-on-click-modal="false">
            <el-form :model="priForm" label-width="80px">
                <el-form-item label="名称" required>
                    <el-input v-model="priForm.name" placeholder="优先级名称"></el-input>
                </el-form-item>
                <el-form-item label="编码">
                    <el-input v-model="priForm.code" placeholder="编码，如 HIGH"></el-input>
                </el-form-item>
                <el-form-item label="级别">
                    <el-input-number v-model="priForm.level" :min="1" :max="10"></el-input-number>
                    <span style="margin-left:8px;color:#909399;font-size:12px;">数字越大优先级越高</span>
                </el-form-item>
                <el-form-item label="颜色">
                    <el-color-picker v-model="priForm.color"></el-color-picker>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="priForm.description" type="textarea" :rows="3"></el-input>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="priForm.enabled"></el-switch>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="priVisible=false">取消</el-button>
                    <el-button type="primary" :loading="priSaving" @click="savePriority">保存</el-button>
                </div>
            </template>
        </el-dialog>
    </div>`,
    setup() {
        const { ref, reactive, onMounted } = qigd;
        const api = qigd.api;
        const ElMessage = qigd.ElMessage;
        const ElMessageBox = qigd.ElMessageBox;

        const activeTab = ref('categories');
        const categoryTree = ref([]);
        const categoryFlat = ref([]);
        const priorityList = ref([]);
        const catLoading = ref(false);
        const priLoading = ref(false);

        // 分类
        const catVisible = ref(false);
        const catSaving = ref(false);
        const catEditing = ref(null);
        const catForm = reactive({ id: null, name: '', code: '', description: '', parentId: null, enabled: true });

        // 优先级
        const priVisible = ref(false);
        const priSaving = ref(false);
        const priorityEditing = ref(null);
        const priForm = reactive({ id: null, name: '', code: '', level: 3, color: '#409eff', description: '', enabled: true });

        const plusIcon = qigd.h(ElementPlusIconsVue.Plus);

        async function loadCategories() {
            catLoading.value = true;
            try {
                const [treeResp, flatResp] = await Promise.all([
                    api.get('/categories/tree'),
                    api.get('/categories')
                ]);
                categoryTree.value = treeResp.data || [];
                categoryFlat.value = flatResp.data || [];
            } catch (e) {} finally {
                catLoading.value = false;
            }
        }

        async function loadPriorities() {
            priLoading.value = true;
            try {
                const resp = await api.get('/priorities/all');
                priorityList.value = resp.data || [];
            } catch (e) {} finally {
                priLoading.value = false;
            }
        }

        function openCategoryCreate(row) {
            catEditing.value = row || null;
            // 新增子分类时 parentId 指向当前分类
            catForm.parentId = row ? row.id : null;
            catForm.name = row ? row.name : '';
            catForm.code = row ? row.code : '';
            catForm.description = row ? row.description : '';
            catForm.enabled = row ? !!row.enabled : true;
            catForm.id = row ? row.id : null;
            catVisible.value = true;
        }

        async function saveCategory() {
            if (!catForm.name.trim()) {
                ElMessage.warning('请输入分类名称');
                return;
            }
            catSaving.value = true;
            try {
                const payload = {
                    name: catForm.name,
                    code: catForm.code,
                    description: catForm.description,
                    parentId: catForm.parentId,
                    enabled: catForm.enabled
                };
                if (catForm.id) {
                    await api.put('/categories/' + catForm.id, payload);
                    ElMessage.success('分类已更新');
                } else {
                    await api.post('/categories', payload);
                    ElMessage.success('分类已创建');
                }
                catVisible.value = false;
                loadCategories();
            } catch (e) {} finally {
                catSaving.value = false;
            }
        }

        async function removeCategory(row) {
            try {
                await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', { type: 'warning' });
                await api.delete('/categories/' + row.id);
                ElMessage.success('删除成功');
                loadCategories();
            } catch (e) {}
        }

        function openPriorityCreate(row) {
            priorityEditing.value = row || null;
            priForm.name = row ? row.name : '';
            priForm.code = row ? row.code : '';
            priForm.level = row ? row.level : 3;
            priForm.color = row ? row.color : '#409eff';
            priForm.description = row ? row.description : '';
            priForm.enabled = row ? !!row.enabled : true;
            priForm.id = row ? row.id : null;
            priVisible.value = true;
        }

        async function savePriority() {
            if (!priForm.name.trim()) {
                ElMessage.warning('请输入优先级名称');
                return;
            }
            priSaving.value = true;
            try {
                const payload = {
                    name: priForm.name,
                    code: priForm.code,
                    level: priForm.level,
                    color: priForm.color,
                    description: priForm.description,
                    enabled: priForm.enabled
                };
                if (priForm.id) {
                    await api.put('/priorities/' + priForm.id, payload);
                    ElMessage.success('优先级已更新');
                } else {
                    await api.post('/priorities', payload);
                    ElMessage.success('优先级已创建');
                }
                priVisible.value = false;
                loadPriorities();
            } catch (e) {} finally {
                priSaving.value = false;
            }
        }

        async function removePriority(row) {
            try {
                await ElMessageBox.confirm(`确定删除优先级「${row.name}」吗？`, '删除确认', { type: 'warning' });
                await api.delete('/priorities/' + row.id);
                ElMessage.success('删除成功');
                loadPriorities();
            } catch (e) {}
        }

        onMounted(() => {
            loadCategories();
            loadPriorities();
        });

        return { activeTab, categoryTree, categoryFlat, priorityList, catLoading, priLoading,
            catVisible, catSaving, catEditing, catForm,
            priVisible, priSaving, priorityEditing, priForm,
            plusIcon, qigd,
            openCategoryCreate, saveCategory, removeCategory,
            openPriorityCreate, savePriority, removePriority };
    }
});