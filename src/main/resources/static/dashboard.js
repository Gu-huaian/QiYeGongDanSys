// ============================================
// 数据看板组件 (ECharts 可视化)
// ============================================
qigd.register('dashboard-view', {
    template: `
    <div>
        <div class="view-banner">
            <el-tag :type="isAdmin ? 'danger' : 'success'" effect="dark" round style="margin-right:10px;">
                {{ isAdmin ? '管理员视角 · 全部数据' : '我的视角 · 仅我创建的 / 分配给我的工单' }}
            </el-tag>
            <span style="color:#909399;font-size:13px;">
                {{ isAdmin ? '当前账号可查看并统计系统内全部工单' : '系统中其他人员的工单对您不可见，请联系上级申请查看' }}
            </span>
        </div>
        <div class="org-card">
            <div class="org-title">我的组织层级</div>
            <div class="org-body">
                <div v-if="myInfo" class="org-item">
                    <span class="org-label">我的角色</span>
                    <span class="org-strong">{{ getUserRoleCn() }}</span>
                    <span class="org-muted">（{{ qigd.getUser() ? qigd.getUser().realName : '' }}）</span>
                </div>
                <div v-if="myInfo && myInfo.supervisor" class="org-item">
                    <span class="org-label">直属上级</span>
                    <span class="org-strong">{{ myInfo.supervisor.realName }}（{{ myInfo.supervisor.department || '未知部门' }}）</span>
                    <el-button link type="primary" @click="goMyTickets()">创建工单沟通 &raquo;</el-button>
                </div>
                <div v-else-if="myInfo && !isAdmin" class="org-item">
                    <span class="org-label">直属上级</span><span class="org-strong">暂无，请联系管理员配置</span>
                </div>
                <div class="org-item" style="flex-direction:column;align-items:flex-start;gap:8px;">
                    <span class="org-label">直属下级</span>
                    <template v-if="subordinates.length">
                        <el-tag
                            v-for="s in subordinates"
                            :key="s.id"
                            size="small"
                            type="success"
                            style="margin-right:8px;margin-bottom:6px;">
                            {{ s.realName }}（{{ s.department || '未知部门' }}）
                        </el-tag>
                    </template>
                    <span v-else class="org-muted">{{ isAdmin ? '管理员无直属下级范围' : '暂无直属下级' }}</span>
                </div>
            </div>
        </div>
        <div class="stat-grid">
            <div class="stat-card">
                <div class="label">我的工单</div>
                <div class="value">{{ stats.totalCount ?? 0 }}</div>
                <div class="sub">{{ isAdmin ? '全部工单' : '与我相关' }}</div>
            </div>
            <div class="stat-card">
                <div class="label">待处理</div>
                <div class="value" style="color:#e6a23c;">{{ num('PENDING') }}</div>
                <div class="sub">等待分配</div>
            </div>
            <div class="stat-card">
                <div class="label">处理中</div>
                <div class="value" style="color:#f56c6c;">{{ num('PROCESSING') + num('ASSIGNED') }}</div>
                <div class="sub">正在处理</div>
            </div>
            <div class="stat-card">
                <div class="label">已解决</div>
                <div class="value" style="color:#67c23a;">{{ num('RESOLVED') }}</div>
                <div class="sub">待确认关闭</div>
            </div>
            <div class="stat-card">
                <div class="label">已关闭</div>
                <div class="value" style="color:#909399;">{{ num('CLOSED') }}</div>
                <div class="sub">已完成归档</div>
            </div>
            <div class="stat-card">
                <div class="label">逾期工单</div>
                <div class="value" style="color:#f56c6c;">{{ stats.overdueCount ?? 0 }}</div>
                <div class="sub">已超期</div>
            </div>
        </div>

        <div class="chart-grid">
            <div class="chart-card full">
                <div class="chart-title">近7日新增 / 解决趋势</div>
                <div ref="trendChartRef" class="chart-box" style="height:260px;"></div>
            </div>
            <div class="chart-card">
                <div class="chart-title">工单状态分布</div>
                <div ref="statusChartRef" class="chart-box"></div>
            </div>
            <div class="chart-card">
                <div class="chart-title">分类分布</div>
                <div ref="categoryChartRef" class="chart-box"></div>
            </div>
            <div class="chart-card">
                <div class="chart-title">优先级分布</div>
                <div ref="priorityChartRef" class="chart-box"></div>
            </div>
            <div class="chart-card">
                <div class="chart-title">处理人负载</div>
                <div ref="assigneeChartRef" class="chart-box"></div>
            </div>
        </div>
    </div>`,
    setup() {
        const { ref, onMounted, onBeforeUnmount } = qigd;
        const api = qigd.api;
        const echarts = qigd.echarts;

        const stats = ref({});
        const isAdmin = qigd.isAdmin();
        const myInfo = ref(null);
        const subordinates = ref([]);
        const myUserId = (qigd.getUser() && qigd.getUser().userId) || null;
        const trendChartRef = ref();
        const statusChartRef = ref();
        const categoryChartRef = ref();
        const priorityChartRef = ref();
        const assigneeChartRef = ref();
        const charts = [];

        function num(k) {
            const sc = (stats.value.statusCount || {});
            return sc[k] || 0;
        }

        function toPairs(obj) {
            if (!obj) return [];
            return Object.keys(obj).map(k => ({ name: k, value: obj[k] }));
        }

        async function loadStats() {
            try {
                const resp = await api.get('/tickets/statistics');
                stats.value = resp.data || {};
                renderCharts();
            } catch (e) {}
        }

        async function loadMyInfo() {
            if (!myUserId) return;
            try {
                const resp = await api.get('/users/' + myUserId);
                myInfo.value = resp.data || null;
            } catch (e) {}
            try {
                const subResp = await api.get('/users/' + myUserId + '/subordinates');
                subordinates.value = subResp.data || [];
            } catch (e) {}
        }

        function getUserRoleCn() {
            return { ADMIN: '超级管理员', MANAGER: '部门经理', AGENT: '客服工程师', USER: '普通用户' }[qigd.getUserRole()] || qigd.getUserRole();
        }

        function goMyTickets() {
            window.location.hash = '#/tickets';
        }

        function renderCharts() {
            // 近7日趋势 - 折线图
            renderTrend(trendChartRef.value, stats.value.dailyTrend || []);

            // 状态分布 - 环状玫瑰图
            const statusData = Object.keys(qigd.STATUS_META).map(k => {
                const v = num(k);
                return { name: qigd.STATUS_META[k].label, value: v };
            }).filter(i => i.value > 0);
            renderRose(statusChartRef.value, statusData, '工单状态');

            // 分类分布 - 横向条形图
            renderBar(categoryChartRef.value, toPairs(stats.value.categoryCount), '分类', true);

            // 优先级分布 - 半透明渐变柱状图
            renderGradientBar(priorityChartRef.value, toPairs(stats.value.priorityCount), '优先级');

            // 处理人负载 - 雷达/条形选择：用堆叠横向条形
            renderBar(assigneeChartRef.value, toPairs(stats.value.assigneeCount), '处理人', true);
        }

        function renderTrend(el, data) {
            if (!el) return;
            if (el._chart) el._chart.dispose();
            const chart = echarts.init(el);
            el._chart = chart;
            charts.push(chart);
            chart.setOption({
                tooltip: { trigger: 'axis' },
                legend: { data: ['新增', '解决'], top: 0 },
                grid: { left: 10, right: 15, top: 32, bottom: 10, containLabel: true },
                xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { fontSize: 11 } },
                yAxis: { type: 'value', minInterval: 1 },
                series: [
                    {
                        name: '新增', type: 'line', smooth: true, data: data.map(d => d.created),
                        itemStyle: { color: '#409eff' }, areaStyle: { opacity: 0.12 }
                    },
                    {
                        name: '解决', type: 'line', smooth: true, symbol: 'diamond', data: data.map(d => d.resolved),
                        itemStyle: { color: '#67c23a' }, areaStyle: { opacity: 0.12 }
                    }
                ]
            });
        }

        function renderRose(el, data, name) {
            if (!el) return;
            if (el._chart) el._chart.dispose();
            const chart = echarts.init(el);
            el._chart = chart;
            chart.setOption({
                tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
                legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { fontSize: 11 } },
                color: ['#e6a23c', '#409eff', '#f56c6c', '#67c23a', '#909399', '#9254de', '#ff7e67'],
                series: [{
                    name, type: 'pie', roseType: 'radius',
                    radius: ['20%', '72%'], center: ['40%', '50%'],
                    label: { show: false },
                    itemStyle: { borderRadius: 4 },
                    data: data.length ? data : [{ name: '暂无数据', value: 0 }]
                }]
            });
        }

        function renderGradientBar(el, data, name) {
            if (!el) return;
            if (el._chart) el._chart.dispose();
            const chart = echarts.init(el);
            el._chart = chart;
            const sorted = data.slice().sort((a, b) => b.value - a.value).slice(0, 10);
            chart.setOption({
                tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                grid: { left: 10, right: 20, top: 20, bottom: 10, containLabel: true },
                xAxis: { type: 'category', data: sorted.map(i => i.name), axisLabel: { fontSize: 11, interval: 0, rotate: sorted.length > 4 ? 20 : 0 } },
                yAxis: { type: 'value', minInterval: 1 },
                series: [{
                    name, type: 'bar',
                    data: sorted.map(i => i.value),
                    barMaxWidth: 34,
                    itemStyle: {
                        borderRadius: [6, 6, 0, 0],
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: '#f56c6c' },
                            { offset: 1, color: '#e6a23c' }
                        ])
                    }
                }]
            });
        }

        function renderBar(el, data, name, horizontal) {
            if (!el) return;
            if (el._chart) el._chart.dispose();
            const chart = echarts.init(el);
            el._chart = chart;
            const sorted = data.slice().sort((a, b) => b.value - a.value).slice(0, 10);
            chart.setOption({
                tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                grid: { left: 10, right: 20, top: 20, bottom: 10, containLabel: true },
                xAxis: horizontal ? { type: 'value', minInterval: 1 } : { type: 'category', data: sorted.map(i => i.name), axisLabel: { fontSize: 11 } },
                yAxis: horizontal ? { type: 'category', data: sorted.map(i => i.name), axisLabel: { fontSize: 11 } } : { type: 'value', minInterval: 1 },
                series: [{
                    name, type: 'bar',
                    data: sorted.map(i => i.value),
                    itemStyle: { color: '#409eff', borderRadius: horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0] },
                    barMaxWidth: 22
                }]
            });
        }

        function resizeHandler() {
            charts.forEach(c => c && c.resize && c.resize());
        }

        onMounted(() => {
            loadStats();
            loadMyInfo();
            window.addEventListener('resize', resizeHandler);
        });
        onBeforeUnmount(() => {
            window.removeEventListener('resize', resizeHandler);
        });

        return { stats, isAdmin, myInfo, subordinates, trendChartRef, statusChartRef, categoryChartRef, priorityChartRef, assigneeChartRef, num, getUserRoleCn, goMyTickets };
    }
});