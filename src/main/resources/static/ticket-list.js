// ============================================
// 工单管理组件（列表、创建、详情、分配、状态流转、评论、附件）
// ============================================
qigd.register('ticket-list', {
    template: `
    <div class="page-card">
        <div class="card-header">
            <div style="font-weight:600;font-size:16px;">工单管理</div>
            <div>
                <el-button type="primary" :icon="plusIcon" @click="openCreate">新建工单</el-button>
            </div>
        </div>
        <div class="card-body">
            <!-- 筛选工具栏 -->
            <div class="toolbar">
                <el-input v-model="query.keyword" placeholder="搜索标题/描述/工单号" clearable style="width:200px" @clear="load" @keyup.enter="load" :prefix-icon="searchIcon"></el-input>
                <el-select v-model="query.status" placeholder="状态" clearable style="width:130px" @change="load">
                    <el-option v-for="(m,k) in qigd.STATUS_META" :key="k" :label="m.label" :value="k"></el-option>
                </el-select>
                <el-select v-model="query.categoryId" placeholder="分类" clearable filterable style="width:140px" @change="load">
                    <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
                </el-select>
                <el-select v-model="query.priorityId" placeholder="优先级" clearable style="width:130px" @change="load">
                    <el-option v-for="p in priorities" :key="p.id" :label="p.name" :value="p.id"></el-option>
                </el-select>
                <el-select v-model="query.assigneeId" placeholder="处理人" clearable filterable style="width:140px" @change="load">
                    <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id"></el-option>
                </el-select>
                <el-button @click="load">查询</el-button>
                <el-button @click="resetQuery">重置</el-button>
            </div>

            <!-- 表格 -->
            <el-table :data="list" v-loading="loading" stripe style="width:100%">
                <el-table-column label="工单号" prop="ticketNo" width="150">
                    <template #default="{row}"><span style="font-family:monospace;color:#409eff;">{{ row.ticketNo }}</span></template>
                </el-table-column>
                <el-table-column label="标题" min-width="200">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDetail(row.id)">{{ row.title }}</el-link>
                    </template>
                </el-table-column>
                <el-table-column label="分类" width="110">
                    <template #default="{row}">{{ row.category ? row.category.name : '-' }}</template>
                </el-table-column>
                <el-table-column label="优先级" width="100">
                    <template #default="{row}">
                        <el-tag v-if="row.priority" size="small" :color="row.priority.color || '#409eff'" style="color:#fff;border:none;">{{ row.priority.name }}</el-tag>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                    <template #default="{row}">
                        <el-tag size="small" :type="qigd.statusMeta(row.status).type">{{ qigd.statusMeta(row.status).label }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="处理人" width="110">
                    <template #default="{row}">{{ row.assignee ? row.assignee.realName : '-' }}</template>
                </el-table-column>
                <el-table-column label="创建人" width="110">
                    <template #default="{row}">{{ row.creator ? row.creator.realName : '-' }}</template>
                </el-table-column>
                <el-table-column label="逾期" width="80">
                    <template #default="{row}">
                        <el-tag v-if="row.overdue" type="danger" size="small">逾期</el-tag>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" width="140">
                    <template #default="{row}">{{ qigd.fmtDateTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{row}">
                        <el-button link type="primary" size="small" @click="openDetail(row.id)">详情</el-button>
                        <el-button v-if="qigd.isAdmin() || canManage(row)" link type="danger" size="small" @click="removeTicket(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div style="display:flex;justify-content:flex-end;margin-top:16px;">
                <el-pagination
                    background layout="total, sizes, prev, pager, next" 
                    :total="total" :page-size="query.size" :current-page="query.page + 1"
                    :page-sizes="[10,20,50]" @current-change="onPage" @size-change="onSize"></el-pagination>
            </div>
        </div>

        <!-- 创建/编辑工单对话框 -->
        <el-dialog v-model="createVisible" :title="editingTicket ? '编辑工单' : '新建工单'" width="600px" :close-on-click-modal="false">
            <el-form :model="ticketForm" label-width="90px" ref="ticketFormRef" :rules="ticketRules">
                <el-form-item label="标题" prop="title">
                    <el-input v-model="ticketForm.title" placeholder="请输入工单标题"></el-input>
                </el-form-item>
                <el-form-item label="描述" prop="description">
                    <el-input v-model="ticketForm.description" type="textarea" :rows="4" placeholder="请输入工单详细描述"></el-input>
                </el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="ticketForm.categoryId" placeholder="选择分类" clearable filterable style="width:100%">
                        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="优先级">
                    <el-select v-model="ticketForm.priorityId" placeholder="选择优先级" clearable style="width:100%">
                        <el-option v-for="p in priorities" :key="p.id" :label="p.name" :value="p.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="处理人">
                    <el-select v-model="ticketForm.assigneeId" placeholder="选择处理人" clearable filterable style="width:100%">
                        <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="分配方式">
                    <el-radio-group v-model="ticketForm.assignmentMode">
                        <el-radio value="MANUAL">手动</el-radio>
                        <el-radio value="AUTO">自动</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="截止时间">
                    <el-date-picker v-model="ticketForm.dueTime" type="datetime" placeholder="选择截止时间" style="width:100%"></el-date-picker>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="createVisible=false">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="saveTicket">保存</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 工单详情抽屉 -->
        <el-drawer v-model="detailVisible" :title="detailTicket ? detailTicket.ticketNo : '工单详情'" size="80%" :close-on-click-modal="false">
            <template v-if="detailTicket">
                <div style="display:flex;flex-wrap:wrap;gap:16px;margin-bottom:20px;">
                    <el-descriptions :column="2" border style="flex:1;min-width:300px;">
                        <el-descriptions-item label="标题"><b>{{ detailTicket.title }}</b></el-descriptions-item>
                        <el-descriptions-item label="状态">
                            <el-tag :type="qigd.statusMeta(detailTicket.status).type">{{ qigd.statusMeta(detailTicket.status).label }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="分类">{{ detailTicket.category ? detailTicket.category.name : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="优先级">
                            <el-tag v-if="detailTicket.priority" size="small" :color="detailTicket.priority.color || '#409eff'" style="color:#fff;border:none;">{{ detailTicket.priority.name }}</el-tag>
                            <span v-else>-</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="创建人">{{ detailTicket.creator ? detailTicket.creator.realName : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="处理人">{{ detailTicket.assignee ? detailTicket.assignee.realName : '-' }}</el-descriptions-item>
                        <el-descriptions-item label="创建时间">{{ qigd.fmtDateTime(detailTicket.createdAt) }}</el-descriptions-item>
                        <el-descriptions-item label="截止时间">{{ qigd.fmtDateTime(detailTicket.dueTime) }}</el-descriptions-item>
                        <el-descriptions-item label="解决时间">{{ qigd.fmtDateTime(detailTicket.resolvedTime) }}</el-descriptions-item>
                        <el-descriptions-item label="关闭时间">{{ qigd.fmtDateTime(detailTicket.closedTime) }}</el-descriptions-item>
                        <el-descriptions-item label="分配方式">{{ qigd.ASSIGN_META[detailTicket.assignmentMode] || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="逾期">
                            <el-tag v-if="detailTicket.overdue" type="danger" size="small">已逾期</el-tag>
                            <span v-else>否</span>
                        </el-descriptions-item>
                        <el-descriptions-item label="描述" :span="2">
                            <span style="white-space:pre-wrap;">{{ detailTicket.description || '暂无描述' }}</span>
                        </el-descriptions-item>
                    </el-descriptions>
                </div>

                <!-- 操作区 -->
                <div style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:20px;">
                    <el-button v-if="detailTicket.status==='PENDING'" type="primary" @click="openAssign">分配工单</el-button>
                    <el-button v-if="detailTicket.status==='ASSIGNED'" type="warning" @click="changeStatus('PROCESSING')">开始处理</el-button>
                    <el-button v-if="detailTicket.status==='PROCESSING'" type="warning" @click="changeStatus('PAUSED')">暂停</el-button>
                    <el-button v-if="detailTicket.status==='PAUSED'" type="warning" @click="changeStatus('PROCESSING')">恢复处理</el-button>
                    <el-button v-if="detailTicket.status==='PROCESSING'" type="success" @click="changeStatus('RESOLVED')">标记解决</el-button>
                    <el-button v-if="detailTicket.status==='RESOLVED'" type="success" @click="changeStatus('CLOSED')">关闭工单</el-button>
                    <el-button v-if="detailTicket.status==='CLOSED'" type="danger" @click="changeStatus('REOPENED')">重新打开</el-button>
                    <el-button v-if="isTicketOpen()" type="warning" plain @click="openTransfer">工单转出</el-button>
                    <el-button v-if="isTicketOpen()" type="success" plain @click="openInvite">邀请协作</el-button>
                    <el-button v-if="isTicketOpen()" type="danger" plain @click="escalateTicket">向上级反馈</el-button>
                </div>

                <!-- 协作成员 -->
                <el-divider content-position="left">协作成员 ({{ detailTicket.collaborators ? detailTicket.collaborators.length : 0 }})</el-divider>
                <div style="margin-bottom:16px;">
                    <template v-if="detailTicket.collaborators && detailTicket.collaborators.length">
                        <el-tag v-for="c in detailTicket.collaborators" :key="c.id" closable @close="removeCollab(c)" type="info" style="margin-right:8px;margin-bottom:6px;">
                            {{ c.realName }}{{ c.id === currentUserId ? '（我）' : '' }}
                        </el-tag>
                    </template>
                    <span v-else style="color:#c0c4cc;">暂无协作成员，可使用「邀请协作」添加同事协助处理</span>
                </div>

                <el-divider content-position="left">评论交流</el-divider>
                <div style="margin-bottom:12px;">
                    <el-input v-model="commentContent" type="textarea" :rows="2" placeholder="发表评论..."></el-input>
                    <div style="margin-top:8px;display:flex;justify-content:space-between;align-items:center;">
                        <div>
                            <el-checkbox v-model="commentInternal">内部评论</el-checkbox>
                        </div>
                        <el-button type="primary" :loading="commentSaving" @click="submitComment">发表评论</el-button>
                    </div>
                </div>
                <div v-loading="commentLoading">
                    <div v-if="!comments.length" style="color:#c0c4cc;text-align:center;padding:20px;">暂无评论</div>
                    <div v-for="c in comments" :key="c.id" class="comment-item">
                        <div class="comment-head">
                            <span class="comment-user">{{ c.user ? c.user.realName : '系统' }}
                                <el-tag v-if="c.internal" type="warning" size="small" style="margin-left:6px;">内部</el-tag>
                            </span>
                            <span class="comment-time">{{ qigd.fmtDateTime(c.createdAt) }}</span>
                            <el-button v-if="canDeleteComment(c)" link type="danger" size="small" style="margin-left:auto;" @click="deleteComment(c)">删除</el-button>
                        </div>
                        <div class="comment-content">{{ c.content }}</div>
                    </div>
                </div>

                <el-divider content-position="left">附件 ({{ attachments.length }})</el-divider>
                <div style="margin-bottom:12px;">
                    <el-upload ref="uploadRef" :show-file-list="false" :auto-upload="false" :on-change="onSelectFile" multiple>
                        <el-button :icon="uploadIcon">选择文件上传</el-button>
                    </el-upload>
                    <el-button v-if="selectedFiles.length" type="primary" :loading="uploading" @click="uploadFiles" style="margin-left:8px;">
                        上传 {{ selectedFiles.length }} 个文件
                    </el-button>
                    <el-button v-if="selectedFiles.length" @click="clearFiles">清空选择</el-button>
                </div>
                <div v-loading="attachLoading">
                    <div v-if="!attachments.length" style="color:#c0c4cc;text-align:center;padding:20px;">暂无附件</div>
                    <div v-for="a in attachments" :key="a.id" class="attach-item">
                        <el-icon style="color:#409eff;"><Document /></el-icon>
                        <span class="attach-name">{{ a.originalName }}</span>
                        <span style="color:#c0c4cc;font-size:12px;">{{ qigd.fmtSize(a.fileSize) }} · {{ a.uploader ? a.uploader.realName : '-' }} · {{ qigd.fmtDateTime(a.createdAt) }}</span>
                        <el-button link type="danger" size="small" @click="deleteAttachment(a)">删除</el-button>
                    </div>
                </div>
            </template>
        </el-drawer>

        <!-- 分配工单对话框 -->
        <el-dialog v-model="assignVisible" title="分配工单" width="420px">
            <el-form label-width="80px">
                <el-form-item label="处理人">
                    <el-select v-model="assignForm.assigneeId" placeholder="选择处理人" filterable style="width:100%">
                        <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="分配方式">
                    <el-radio-group v-model="assignForm.mode">
                        <el-radio value="MANUAL">手动</el-radio>
                        <el-radio value="AUTO">自动</el-radio>
                        <el-radio value="ROTATION">轮询</el-radio>
                        <el-radio value="SKILL_BASED">技能</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="assignVisible=false">取消</el-button>
                    <el-button type="primary" :loading="assignSaving" @click="doAssign">确认分配</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 工单转出对话框 -->
        <el-dialog v-model="transferVisible" title="工单转出" width="420px">
            <el-form label-width="90px">
                <el-form-item label="转给处理人">
                    <el-select v-model="transferForm.assigneeId" placeholder="选择新的处理人" filterable style="width:100%">
                        <el-option v-for="u in transferCandidates" :key="u.id" :label="u.realName || u.username" :value="u.id"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="transferVisible=false">取消</el-button>
                    <el-button type="warning" :loading="transferSaving" @click="doTransfer">确认转出</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 邀请协作对话框 -->
        <el-dialog v-model="inviteVisible" title="邀请协作" width="420px">
            <el-form label-width="90px">
                <el-form-item label="选择同事">
                    <el-select v-model="inviteForm.userId" placeholder="选择要邀请协助的同事" filterable style="width:100%">
                        <el-option v-for="u in inviteCandidates" :key="u.id" :label="(u.realName || u.username) + '（' + (u.department || '') + '）'" :value="u.id"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="form-footer">
                    <el-button @click="inviteVisible=false">取消</el-button>
                    <el-button type="success" :loading="inviteSaving" @click="doInvite">确认邀请</el-button>
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
        const total = ref(0);
        const loading = ref(false);
        const categories = ref([]);
        const priorities = ref([]);
        const users = ref([]);

        const query = reactive({
            keyword: '', status: '', categoryId: null, priorityId: null,
            assigneeId: null, page: 0, size: 10
        });

        // 创建/编辑
        const createVisible = ref(false);
        const editingTicket = ref(null);
        const saving = ref(false);
        const ticketFormRef = ref();
        const ticketForm = reactive({
            id: null, title: '', description: '', categoryId: null,
            priorityId: null, assigneeId: null, assignmentMode: 'MANUAL', dueTime: null
        });
        const ticketRules = {
            title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }]
        };

        // 详情
        const detailVisible = ref(false);
        const detailTicket = ref(null);
        const comments = ref([]);
        const attachments = ref([]);
        const currentUserId = (qigd.getUser() && qigd.getUser().userId) || null;
        const commentContent = ref('');
        const commentInternal = ref(false);
        const commentLoading = ref(false);
        const commentSaving = ref(false);
        const attachLoading = ref(false);
        const uploading = ref(false);
        const selectedFiles = ref([]);
        const uploadRef = ref();

        // 分配
        const assignVisible = ref(false);
        const assignSaving = ref(false);
        const assignForm = reactive({ assigneeId: null, mode: 'MANUAL' });

        // 转出 / 邀请协作
        const transferVisible = ref(false);
        const transferSaving = ref(false);
        const transferForm = reactive({ assigneeId: null });
        const transferCandidates = ref([]);
        const inviteVisible = ref(false);
        const inviteSaving = ref(false);
        const inviteForm = reactive({ userId: null });
        const inviteCandidates = ref([]);

        const searchIcon = qigd.h(ElementPlusIconsVue.Search);
        const plusIcon = qigd.h(ElementPlusIconsVue.Plus);
        const uploadIcon = qigd.h(ElementPlusIconsVue.Upload);

        async function load() {
            loading.value = true;
            try {
                const params = { page: query.page, size: query.size };
                if (query.keyword) params.keyword = query.keyword;
                if (query.status) params.status = query.status;
                if (query.categoryId != null) params.categoryId = query.categoryId;
                if (query.priorityId != null) params.priorityId = query.priorityId;
                if (query.assigneeId != null) params.assigneeId = query.assigneeId;
                const resp = await api.get('/tickets', { params });
                list.value = resp.data.content || [];
                total.value = resp.data.totalElements || 0;
            } catch (e) {} finally {
                loading.value = false;
            }
        }

        async function loadMeta() {
            try {
                const [catResp, priResp, userResp] = await Promise.all([
                    api.get('/categories'),
                    api.get('/priorities'),
                    api.get('/users/all')
                ]);
                categories.value = catResp.data || [];
                priorities.value = priResp.data || [];
                users.value = userResp.data || [];
            } catch (e) {}
        }

        function onPage(p) {
            query.page = p - 1;
            load();
        }
        function onSize(s) {
            query.size = s;
            query.page = 0;
            load();
        }
        function resetQuery() {
            query.keyword = ''; query.status = ''; query.categoryId = null;
            query.priorityId = null; query.assigneeId = null; query.page = 0;
            load();
        }

        function openCreate() {
            editingTicket.value = null;
            Object.assign(ticketForm, {
                id: null, title: '', description: '', categoryId: null,
                priorityId: null, assigneeId: null, assignmentMode: 'MANUAL', dueTime: null
            });
            createVisible.value = true;
        }

        function openEdit(row) {
            editingTicket.value = row;
            ticketForm.title = row.title;
            ticketForm.description = row.description || '';
            ticketForm.categoryId = row.category ? row.category.id : null;
            ticketForm.priorityId = row.priority ? row.priority.id : null;
            ticketForm.assigneeId = row.assignee ? row.assignee.id : null;
            ticketForm.assignmentMode = row.assignmentMode || 'MANUAL';
            ticketForm.dueTime = row.dueTime ? new Date(row.dueTime) : null;
            ticketForm.id = row.id;
            createVisible.value = true;
        }

        async function saveTicket() {
            try {
                await ticketFormRef.value.validate();
            } catch (e) { return; }
            saving.value = true;
            try {
                const payload = {
                    title: ticketForm.title,
                    description: ticketForm.description,
                    categoryId: ticketForm.categoryId,
                    priorityId: ticketForm.priorityId,
                    assigneeId: ticketForm.assigneeId,
                    assignmentMode: ticketForm.assignmentMode,
                    dueTime: ticketForm.dueTime
                };
                if (editingTicket.value) {
                    await api.put('/tickets/' + editingTicket.value.id, payload);
                    ElMessage.success('工单已更新');
                } else {
                    await api.post('/tickets', payload);
                    ElMessage.success('工单创建成功');
                }
                createVisible.value = false;
                load();
            } catch (e) {} finally {
                saving.value = false;
            }
        }

        function canManage(row) {
            const u = qigd.getUser();
            return row.creator && u && row.creator.id === u.id;
        }

        async function removeTicket(row) {
            try {
                await ElMessageBox.confirm(`确定删除工单 ${row.ticketNo} 吗？此操作不可恢复！`, '删除确认', { type: 'warning' });
                await api.delete('/tickets/' + row.id);
                ElMessage.success('删除成功');
                load();
            } catch (e) {}
        }

        async function openDetail(id) {
            detailVisible.value = true;
            detailTicket.value = null;
            comments.value = [];
            attachments.value = [];
            try {
                const resp = await api.get('/tickets/' + id);
                detailTicket.value = resp.data;
                detailVisible.value = true;
                loadComments(id);
                loadAttachments(id);
            } catch (e) {
                detailVisible.value = false;
            }
        }

        async function changeStatus(status) {
            const id = detailTicket.value.id;
            try {
                await api.post(`/tickets/${id}/status`, { status });
                ElMessage.success('状态已更新');
                openDetail(id);
            } catch (e) {}
        }

        async function loadComments(id) {
            commentLoading.value = true;
            try {
                const resp = await api.get(`/tickets/${id}/comments`);
                comments.value = resp.data || [];
            } catch (e) {} finally {
                commentLoading.value = false;
            }
        }

        async function submitComment() {
            if (!commentContent.value.trim()) {
                ElMessage.warning('请输入评论内容');
                return;
            }
            commentSaving.value = true;
            try {
                await api.post(`/tickets/${detailTicket.value.id}/comments`, {
                    content: commentContent.value,
                    internal: commentInternal.value
                });
                commentContent.value = '';
                commentInternal.value = false;
                ElMessage.success('评论发表成功');
                loadComments(detailTicket.value.id);
            } catch (e) {} finally {
                commentSaving.value = false;
            }
        }

        function canDeleteComment(c) {
            if (!c) return false;
            if (qigd.isAdmin()) return true;
            if (!c.user || !c.user.id) return false;
            return String(c.user.id) === String(currentUserId);
        }

        async function deleteComment(c) {
            try {
                await ElMessageBox.confirm('确定删除这条评论吗？删除后不可恢复。', '删除评论', { type: 'warning' });
                await api.delete(`/tickets/comments/${c.id}`);
                ElMessage.success('评论已删除');
                loadComments(detailTicket.value.id);
            } catch (e) {}
        }

        async function loadAttachments(id) {
            attachLoading.value = true;
            try {
                const resp = await api.get(`/tickets/${id}/attachments`);
                attachments.value = resp.data || [];
            } catch (e) {} finally {
                attachLoading.value = false;
            }
        }

        function onSelectFile(file, fileList) {
            selectedFiles.value = fileList.slice(-10).map(f => f.raw);
        }
        function clearFiles() {
            selectedFiles.value = [];
            if (uploadRef.value) uploadRef.value.clearFiles();
        }
        function deleteAttachment(a) {
            api.delete('/tickets/attachments/' + a.id).then(() => {
                ElMessage.success('附件已删除');
                loadAttachments(detailTicket.value.id);
            }).catch(() => {});
        }

        async function uploadFiles() {
            const id = detailTicket.value.id;
            uploading.value = true;
            try {
                for (const f of selectedFiles.value) {
                    const fd = new FormData();
                    fd.append('file', f);
                    await api.post(`/tickets/${id}/attachments`, fd);
                }
                ElMessage.success('附件上传成功');
                selectedFiles.value = [];
                if (uploadRef.value) uploadRef.value.clearFiles();
                loadAttachments(id);
            } catch (e) {} finally {
                uploading.value = false;
            }
        }

        function openAssign() {
            assignForm.assigneeId = detailTicket.value.assignee ? detailTicket.value.assignee.id : null;
            assignForm.mode = 'MANUAL';
            assignVisible.value = true;
        }

        async function doAssign() {
            if (!assignForm.assigneeId) {
                ElMessage.warning('请选择处理人');
                return;
            }
            // 自动/技能/轮询可不需要处理人，但也支持
            assignSaving.value = true;
            try {
                await api.post(`/tickets/${detailTicket.value.id}/assign`, {
                    assigneeId: assignForm.assigneeId,
                    mode: assignForm.mode
                });
                ElMessage.success('工单分配成功');
                assignVisible.value = false;
                openDetail(detailTicket.value.id);
            } catch (e) {} finally {
                assignSaving.value = false;
            }
        }

        function isTicketOpen() {
            return detailTicket.value && detailTicket.value.status !== 'CLOSED';
        }

        function openTransfer() {
            transferForm.assigneeId = null;
            // 转出对象：除当前处理人外的其他启用用户
            const curId = detailTicket.value.assignee ? String(detailTicket.value.assignee.id) : null;
            transferCandidates.value = users.value.filter(u => String(u.id) !== curId);
            transferVisible.value = true;
        }

        async function doTransfer() {
            if (!transferForm.assigneeId) {
                ElMessage.warning('请选择要转给的处理人');
                return;
            }
            transferSaving.value = true;
            try {
                await api.post(`/tickets/${detailTicket.value.id}/transfer`, {
                    assigneeId: transferForm.assigneeId
                });
                ElMessage.success('工单已转出');
                transferVisible.value = false;
                openDetail(detailTicket.value.id);
            } catch (e) {} finally {
                transferSaving.value = false;
            }
        }

        function openInvite() {
            inviteForm.userId = null;
            const relatedIds = new Set([
                detailTicket.value.creator ? String(detailTicket.value.creator.id) : null,
                detailTicket.value.assignee ? String(detailTicket.value.assignee.id) : null,
                String(currentUserId)
            ]);
            (detailTicket.value.collaborators || []).forEach(c => relatedIds.add(String(c.id)));
            inviteCandidates.value = users.value.filter(u => !relatedIds.has(String(u.id)));
            inviteVisible.value = true;
        }

        async function doInvite() {
            if (!inviteForm.userId) {
                ElMessage.warning('请选择要邀请协助的同事');
                return;
            }
            inviteSaving.value = true;
            try {
                await api.post(`/tickets/${detailTicket.value.id}/collaborators`, {
                    userId: inviteForm.userId
                });
                ElMessage.success('邀请成功，已加入协作成员');
                inviteVisible.value = false;
                openDetail(detailTicket.value.id);
            } catch (e) {} finally {
                inviteSaving.value = false;
            }
        }

        async function removeCollab(c) {
            try {
                await ElMessageBox.confirm(`确定移除协作成员 ${c.realName} 吗？`, '移除协作', { type: 'warning' });
                await api.delete(`/tickets/${detailTicket.value.id}/collaborators/${c.id}`);
                ElMessage.success('已移除协作成员');
                openDetail(detailTicket.value.id);
            } catch (e) {}
        }

        async function escalateTicket() {
            try {
                await ElMessageBox.confirm('确定将该工单向上级反馈吗？系统会将工单转交给您的直属上级处理。', '向上级反馈', { type: 'warning' });
                await api.post(`/tickets/${detailTicket.value.id}/escalate`);
                ElMessage.success('已向上级反馈工单');
                openDetail(detailTicket.value.id);
            } catch (e) {}
        }

        onMounted(() => {
            loadMeta();
            load();
        });

        return {
            list, total, loading, categories, priorities, users, query,
            createVisible, editingTicket, saving, ticketForm, ticketFormRef, ticketRules,
            detailVisible, detailTicket, comments, attachments, commentContent, commentInternal,
            commentLoading, commentSaving, attachLoading, uploading, selectedFiles, uploadRef,
            assignVisible, assignSaving, assignForm,
            transferVisible, transferSaving, transferForm, transferCandidates,
            inviteVisible, inviteSaving, inviteForm, inviteCandidates,
            searchIcon, plusIcon, uploadIcon, qigd,
            load, onPage, onSize, resetQuery, openCreate, openEdit, saveTicket,
            canManage, removeTicket, openDetail, changeStatus, submitComment, canDeleteComment, deleteComment,
            onSelectFile, clearFiles, deleteAttachment, uploadFiles, openAssign, doAssign,
            isTicketOpen, openTransfer, doTransfer, openInvite, doInvite, removeCollab, escalateTicket, currentUserId
        };
    }
});