// ============================================
// 操作日志组件
// ============================================
qigd.register('log-manage', {
    template: `
    <div class="page-card">
        <div class="card-header">
            <div style="font-weight:600;font-size:16px;">操作日志</div>
            <div>
                <el-button :icon="refreshIcon" @click="load">刷新</el-button>
            </div>
        </div>
        <div class="card-body">
            <div class="toolbar">
                <el-input v-model="query.username" placeholder="操作人" clearable style="width:140px" @clear="load" @keyup.enter="load"></el-input>
                <el-input v-model="query.module" placeholder="模块" clearable style="width:130px" @clear="load" @keyup.enter="load"></el-input>
                <el-input v-model="query.operation" placeholder="操作" clearable style="width:140px" @clear="load" @keyup.enter="load"></el-input>
                <el-select v-model="query.status" placeholder="结果" clearable style="width:110px" @change="load">
                    <el-option label="成功" :value="1"></el-option>
                    <el-option label="失败" :value="0"></el-option>
                </el-select>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="reset">重置</el-button>
            </div>

            <el-table :data="list" v-loading="loading" stripe style="width:100%">
                <el-table-column label="操作人" width="110">
                    <template #default="{row}">{{ row.user ? (row.user.realName || row.user.username) : '系统' }}</template>
                </el-table-column>
                <el-table-column label="模块" prop="module" width="110"></el-table-column>
                <el-table-column label="操作" prop="operation" width="130"></el-table-column>
                <el-table-column label="请求方式" prop="requestMethod" width="90">
                    <template #default="{row}">
                        <el-tag size="small" :type="row.requestMethod==='POST'||row.requestMethod==='PUT'||row.requestMethod==='DELETE'?'warning':'primary'">{{ row.requestMethod }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="请求地址" prop="requestUrl" min-width="200" show-overflow-tooltip></el-table-column>
                <el-table-column label="耗时" width="90">
                    <template #default="{row}">{{ row.costTime != null ? row.costTime + 'ms' : '-' }}</template>
                </el-table-column>
                <el-table-column label="IP" prop="ip" width="130"></el-table-column>
                <el-table-column label="结果" width="80">
                    <template #default="{row}">
                        <el-tag size="small" :type="row.status===1?'success':'danger'">{{ row.status===1?'成功':'失败' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="时间" width="150">
                    <template #default="{row}">{{ qigd.fmtDateTime(row.createdAt) }}</template>
                </el-table-column>
            </el-table>

            <div style="display:flex;justify-content:flex-end;margin-top:16px;">
                <el-pagination background layout="total, sizes, prev, pager, next"
                    :total="total" :page-size="query.size" :current-page="query.page+1"
                    :page-sizes="[10,20,50]" @current-change="onPage" @size-change="onSize"></el-pagination>
            </div>
        </div>
    </div>`,
    setup() {
        const { ref, reactive, onMounted } = qigd;
        const api = qigd.api;
        const I = ElementPlusIconsVue;

        const list = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const query = reactive({ username: '', module: '', operation: '', status: null, page: 0, size: 10 });

        async function load(page) {
            if (typeof page === 'number') query.page = page;
            loading.value = true;
            try {
                const resp = await api.get('/logs', { params: {
                    username: query.username || undefined,
                    module: query.module || undefined,
                    operation: query.operation || undefined,
                    status: query.status == null ? undefined : query.status,
                    page: query.page, size: query.size
                }});
                const r = resp.data || {};
                list.value = r.content || [];
                total.value = r.totalElements || 0;
            } catch (e) {} finally {
                loading.value = false;
            }
        }

        function search() { query.page = 0; load(); }
        function reset() { Object.assign(query, { username: '', module: '', operation: '', status: null, page: 0 }); load(); }
        function onPage(p) { load(p - 1); }
        function onSize(s) { query.size = s; query.page = 0; load(); }

        onMounted(() => load());

        return { list, total, loading, query, load, search, reset, onPage, onSize,
            refreshIcon: qigd.h(I.Refresh) };
    }
});