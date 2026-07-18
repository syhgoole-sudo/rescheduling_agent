<template>
  <div class="app-container schedule-task-page">
    <el-card shadow="never" class="plan-context-card">
      <el-tabs v-model="activePlanCategory" class="plan-category-tabs" @tab-click="handlePlanCategoryChange">
        <el-tab-pane label="当前有效方案" name="active" />
        <el-tab-pane label="候选方案" name="pending" />
        <el-tab-pane label="已确认方案" name="confirmed" />
        <el-tab-pane label="历史方案" name="history" />
        <el-tab-pane label="全部方案" name="all" />
      </el-tabs>
      <div slot="header" class="context-header">
        <span>方案上下文</span>
        <div class="context-actions">
          <el-select
            v-model="selectedPlanId"
            filterable
            placeholder="请先选择调度方案"
            class="plan-selector"
            :loading="planLoading"
            @change="handlePlanChange"
          >
            <el-option
              v-for="plan in filteredPlanOptions"
              :key="plan.planId"
              :label="formatPlanLabel(plan)"
              :value="plan.planId"
            />
          </el-select>
          <el-button
            type="primary"
            plain
            icon="el-icon-data-analysis"
            :disabled="!selectedPlanId"
            @click="openGantt"
          >查看甘特图</el-button>
        </div>
      </div>

      <el-descriptions v-if="selectedPlan" :column="5" border size="small">
        <el-descriptions-item label="方案编码">{{ selectedPlan.planCode }}</el-descriptions-item>
        <el-descriptions-item label="方案名称">{{ selectedPlan.planName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="方案类型"><el-tag size="mini" :type="planTypeTag(selectedPlan.planType)">{{ selectedPlan.planType }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="方案状态"><el-tag size="mini" :type="planStatusTag(selectedPlan.planStatus)">{{ selectedPlan.planStatus }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="任务数">{{ allScheduleTaskList.length }}</el-descriptions-item>
        <el-descriptions-item label="算法名称">{{ selectedPlan.algorithmName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="策略类型">{{ selectedPlan.strategyType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="方案生成时间">{{ formatTime(selectedPlan.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="计划起始时间">{{ formatTime(selectedPlan.scheduleStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="计划结束时间">{{ formatTime(selectedPlan.scheduleEndTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="请先选择调度方案，再查看该方案任务" :image-size="72" />
      <el-alert
        v-if="selectedPlan"
        class="time-note"
        type="info"
        :closable="false"
        show-icon
        title="任务记录生成时间表示任务写入系统的时间，不代表实际或计划加工开始时间。"
      />
    </el-card>

    <el-card v-if="selectedPlan" shadow="never" class="task-product-card">
      <div class="task-product-row">
        <div>
          <div class="filter-label">当前方案产品</div>
          <div class="filter-tip">产品只过滤当前全局方案中的任务，不改变方案范围或方案状态。</div>
        </div>
        <el-select v-model="selectedTaskProductId" filterable clearable placeholder="全部参与产品" class="product-selector" @change="handleTaskProductChange">
          <el-option v-for="product in currentPlanProducts" :key="product.productId" :label="product.productCode || product.productId" :value="product.productId" />
        </el-select>
      </div>
    </el-card>

    <el-tabs v-model="activeTaskType" class="task-type-tabs" @tab-click="handleTaskTypeChange">
      <el-tab-pane label="全部任务" name="all" />
      <el-tab-pane label="插单任务" name="inserted" />
      <el-tab-pane label="变更任务" name="changed" />
      <el-tab-pane label="冻结任务" name="frozen" />
      <el-tab-pane label="普通任务" name="normal" />
    </el-tabs>

    <el-form v-show="showSearch && selectedPlanId" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="90px" class="search-form">
      <el-form-item label="订单编码" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入订单编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="设备ID" prop="equipmentId">
        <el-input v-model="queryParams.equipmentId" placeholder="请输入设备ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-input v-model="queryParams.taskStatus" placeholder="PLANNED / FROZEN" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 task-toolbar">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="!selectedPlanId" @click="handleAdd" v-hasPermi="['aps:scheduleTask:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:scheduleTask:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:scheduleTask:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" :disabled="!selectedPlanId" @click="handleExport" v-hasPermi="['aps:scheduleTask:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="scheduleTaskList" :empty-text="selectedPlanId ? '当前方案暂无调度任务' : '请先选择调度方案'" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案编码" align="center" min-width="145" show-overflow-tooltip>
        <template slot-scope="scope">{{ selectedPlan ? selectedPlan.planCode : scope.row.planId }}</template>
      </el-table-column>
      <el-table-column label="订单编码" align="center" prop="orderCode" min-width="130" show-overflow-tooltip />
      <el-table-column label="产品编码" align="center" prop="productCode" min-width="110" show-overflow-tooltip />
      <el-table-column label="工序序号" align="center" prop="processSeq" width="90" />
      <el-table-column label="工序编码" align="center" prop="processCode" min-width="110" show-overflow-tooltip />
      <el-table-column label="设备编码" align="center" prop="equipmentCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="计划开始时间" align="center" prop="plannedStartTime" width="170"><template slot-scope="scope">{{ formatTime(scope.row.plannedStartTime) }}</template></el-table-column>
      <el-table-column label="计划结束时间" align="center" prop="plannedEndTime" width="170"><template slot-scope="scope">{{ formatTime(scope.row.plannedEndTime) }}</template></el-table-column>
      <el-table-column label="任务类型" align="center" width="105"><template slot-scope="scope"><el-tag size="mini" :type="taskTypeTag(scope.row)">{{ taskTypeLabel(scope.row) }}</el-tag></template></el-table-column>
      <el-table-column label="任务记录生成时间" align="center" prop="createTime" width="170"><template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:scheduleTask:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:scheduleTask:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="selectedPlanId && total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="clearSelectionState" />

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="120px">
        <el-form-item label="方案ID" prop="planId"><el-input v-model="form.planId" disabled /></el-form-item>
        <el-form-item label="订单ID" prop="orderId"><el-input v-model="form.orderId" placeholder="请输入订单ID" /></el-form-item>
        <el-form-item label="订单编码" prop="orderCode"><el-input v-model="form.orderCode" placeholder="请输入订单编码" /></el-form-item>
        <el-form-item label="产品ID" prop="productId"><el-input v-model="form.productId" placeholder="请输入产品ID" /></el-form-item>
        <el-form-item label="产品编码" prop="productCode"><el-input v-model="form.productCode" placeholder="请输入产品编码" /></el-form-item>
        <el-form-item label="工序序号" prop="processSeq"><el-input v-model="form.processSeq" placeholder="请输入工序序号" /></el-form-item>
        <el-form-item label="工序编码" prop="processCode"><el-input v-model="form.processCode" placeholder="请输入工序编码" /></el-form-item>
        <el-form-item label="工序名称" prop="processName"><el-input v-model="form.processName" placeholder="请输入工序名称" /></el-form-item>
        <el-form-item label="设备组ID" prop="equipmentGroupId"><el-input v-model="form.equipmentGroupId" placeholder="请输入设备组ID" /></el-form-item>
        <el-form-item label="设备ID" prop="equipmentId"><el-input v-model="form.equipmentId" placeholder="请输入设备ID" /></el-form-item>
        <el-form-item label="设备编码" prop="equipmentCode"><el-input v-model="form.equipmentCode" placeholder="请输入设备编码" /></el-form-item>
        <el-form-item label="计划开始时间" prop="plannedStartTime"><el-date-picker v-model="form.plannedStartTime" clearable type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划开始时间" /></el-form-item>
        <el-form-item label="计划结束时间" prop="plannedEndTime"><el-date-picker v-model="form.plannedEndTime" clearable type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划结束时间" /></el-form-item>
        <el-form-item label="时长(分钟)" prop="duration"><el-input v-model="form.duration" placeholder="请输入加工时长" /></el-form-item>
        <el-form-item label="任务状态" prop="taskStatus"><el-input v-model="form.taskStatus" placeholder="PLANNED / FROZEN" /></el-form-item>
        <el-form-item label="冻结" prop="isFrozen"><el-input v-model="form.isFrozen" placeholder="Y / N" /></el-form-item>
        <el-form-item label="插单" prop="isInserted"><el-input v-model="form.isInserted" placeholder="Y / N" /></el-form-item>
        <el-form-item label="变更" prop="isChanged"><el-input v-model="form.isChanged" placeholder="Y / N" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入内容" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listSchedulePlan, getSchedulePlan } from "@/api/aps/schedulePlan";
import { listScheduleTaskByPlan, getScheduleTask, delScheduleTask, addScheduleTask, updateScheduleTask } from "@/api/aps/scheduleTask";

export default {
  name: "ScheduleTask",
  data() {
    return {
      loading: false,
      planLoading: false,
      planOptions: [],
      activePlanCategory: "active",
      selectedPlanId: null,
      activeTaskType: "all",
      selectedTaskProductId: null,
      requestedProductId: this.$route.query.productId || null,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      allScheduleTaskList: [],
      title: "",
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, planId: null, orderCode: null, equipmentId: null, taskStatus: null },
      form: {}
    };
  },
  computed: {
    selectedPlan() {
      return this.planOptions.find(item => String(item.planId) === String(this.selectedPlanId)) || null;
    },
    filteredPlanOptions() {
      return this.planOptions.filter(plan => this.planMatchesCategory(plan, this.activePlanCategory));
    },
    filteredTaskList() {
      const orderCode = String(this.queryParams.orderCode || "").toLowerCase();
      const equipmentId = String(this.queryParams.equipmentId || "");
      const taskStatus = String(this.queryParams.taskStatus || "").toLowerCase();
      return this.allScheduleTaskList.filter(task => {
        return this.taskMatchesType(task, this.activeTaskType)
          && (!this.selectedTaskProductId || String(task.productId) === String(this.selectedTaskProductId))
          && (!orderCode || String(task.orderCode || "").toLowerCase().includes(orderCode))
          && (!equipmentId || String(task.equipmentId || "") === equipmentId)
          && (!taskStatus || String(task.taskStatus || "").toLowerCase().includes(taskStatus));
      });
    },
    total() {
      return this.filteredTaskList.length;
    },
    scheduleTaskList() {
      const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize;
      return this.filteredTaskList.slice(start, start + this.queryParams.pageSize);
    },
    currentPlanProducts() {
      const products = new Map();
      this.allScheduleTaskList.forEach(task => {
        if (task.productId !== null && task.productId !== undefined) {
          products.set(String(task.productId), { productId: task.productId, productCode: task.productCode });
        }
      });
      return Array.from(products.values()).sort((a, b) => String(a.productCode || "").localeCompare(String(b.productCode || "")));
    }
  },
  created() {
    this.loadPlanOptions(this.$route.query.planId);
  },
  watch: {
    "$route.query.planId"(planId) {
      if (planId && String(planId) !== String(this.selectedPlanId)) {
        this.selectPlanFromRoute(planId);
      }
    },
    "$route.query.productId"(productId) {
      this.requestedProductId = productId || null;
      this.applyRequestedProduct();
    }
  },
  methods: {
    loadPlanOptions(routePlanId) {
      this.planLoading = true;
      listSchedulePlan({ pageNum: 1, pageSize: 200 }).then(response => {
        this.planOptions = (response.rows || []).sort(this.comparePlanCreateTimeDesc);
        if (routePlanId) return this.selectPlanFromRoute(routePlanId);
        if (!this.filteredPlanOptions.length && this.planOptions.length) this.activePlanCategory = "all";
        this.selectedPlanId = (this.filteredPlanOptions[0] || {}).planId || null;
        this.handlePlanChange();
        return null;
      }).finally(() => {
        this.planLoading = false;
      });
    },
    selectPlanFromRoute(planId) {
      const existing = this.planOptions.find(item => String(item.planId) === String(planId));
      if (existing) {
        this.activePlanCategory = this.categoryForPlan(existing);
        this.selectedPlanId = existing.planId;
        this.handlePlanChange();
        return Promise.resolve();
      }
      return getSchedulePlan(planId).then(response => {
        if (response.data) this.planOptions.unshift(response.data);
        if (response.data) this.activePlanCategory = this.categoryForPlan(response.data);
        this.selectedPlanId = response.data ? response.data.planId : null;
        this.handlePlanChange();
      });
    },
    handlePlanChange() {
      this.queryParams.planId = this.selectedPlanId || null;
      this.queryParams.pageNum = 1;
      this.activeTaskType = "all";
      this.selectedTaskProductId = null;
      this.allScheduleTaskList = [];
      this.clearSelectionState();
      if (this.selectedPlanId) this.getList();
    },
    handlePlanCategoryChange() {
      this.selectedPlanId = (this.filteredPlanOptions[0] || {}).planId || null;
      this.handlePlanChange();
    },
    handleTaskTypeChange() {
      this.queryParams.pageNum = 1;
      this.clearSelectionState();
    },
    handleTaskProductChange() {
      this.queryParams.pageNum = 1;
      this.clearSelectionState();
    },
    getList() {
      if (!this.selectedPlanId) {
        this.allScheduleTaskList = [];
        this.loading = false;
        return;
      }
      this.loading = true;
      this.queryParams.planId = this.selectedPlanId;
      listScheduleTaskByPlan(this.selectedPlanId).then(response => {
        this.allScheduleTaskList = response.data || [];
        this.applyRequestedProduct();
      }).finally(() => {
        this.loading = false;
      });
    },
    applyRequestedProduct() {
      if (!this.requestedProductId) return;
      const product = this.currentPlanProducts.find(item => String(item.productId) === String(this.requestedProductId));
      this.selectedTaskProductId = product ? product.productId : null;
      this.requestedProductId = null;
    },
    planMatchesCategory(plan, category) {
      if (category === "active") return plan.activeFlag === "Y";
      if (category === "pending") return plan.planType === "RESCHEDULE" && plan.planStatus === "PENDING";
      if (category === "confirmed") return plan.planStatus === "CONFIRMED";
      if (category === "history") return plan.planStatus === "HISTORY";
      return true;
    },
    categoryForPlan(plan) {
      if (plan.activeFlag === "Y") return "active";
      if (plan.planType === "RESCHEDULE" && plan.planStatus === "PENDING") return "pending";
      if (plan.planStatus === "CONFIRMED") return "confirmed";
      if (plan.planStatus === "HISTORY") return "history";
      return "all";
    },
    taskMatchesType(task, type) {
      if (type === "inserted") return task.isInserted === "Y";
      if (type === "changed") return task.isChanged === "Y";
      if (type === "frozen") return task.isFrozen === "Y";
      if (type === "normal") return task.isInserted !== "Y" && task.isChanged !== "Y" && task.isFrozen !== "Y";
      return true;
    },
    taskTypeLabel(task) {
      if (task.isInserted === "Y") return "插单任务";
      if (task.isChanged === "Y") return "变更任务";
      if (task.isFrozen === "Y") return "冻结任务";
      return "普通任务";
    },
    taskTypeTag(task) {
      if (task.isInserted === "Y") return "danger";
      if (task.isChanged === "Y") return "warning";
      if (task.isFrozen === "Y") return "success";
      return "primary";
    },
    formatPlanLabel(plan) {
      return `${plan.planCode || plan.planId} / ${plan.planType || '-'} / ${plan.planStatus || '-'}`;
    },
    formatTime(value) {
      return value ? this.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "-";
    },
    comparePlanCreateTimeDesc(a, b) {
      return new Date(b.createTime || b.updateTime || 0).getTime() - new Date(a.createTime || a.updateTime || 0).getTime();
    },
    planTypeTag(type) {
      return type === "RESCHEDULE" ? "warning" : "primary";
    },
    planStatusTag(status) {
      const tags = { ACTIVE: "success", PENDING: "warning", CONFIRMED: "success", REJECTED: "danger", HISTORY: "info" };
      return tags[status] || "info";
    },
    openGantt() {
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId: this.selectedPlanId } });
    },
    cancel() { this.open = false; this.reset(); },
    reset() {
      this.form = {
        taskId: null, planId: this.selectedPlanId || null, orderId: null, orderCode: null,
        productId: null, productCode: null, processSeq: null, processCode: null, processName: null,
        equipmentGroupId: null, equipmentId: null, equipmentCode: null, plannedStartTime: null,
        plannedEndTime: null, duration: null, taskStatus: "PLANNED", isFrozen: "N", isInserted: "N",
        isChanged: "N", sourceTaskId: null, originalStartTime: null, originalEndTime: null,
        originalEquipmentId: null, delFlag: "0", remark: null
      };
      this.resetForm("form");
    },
    handleQuery() { this.queryParams.pageNum = 1; this.clearSelectionState(); },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.planId = this.selectedPlanId;
      this.handleQuery();
    },
    clearSelectionState() { this.ids = []; this.single = true; this.multiple = true; },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.taskId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() { if (!this.selectedPlanId) return; this.reset(); this.open = true; this.title = "新增调度任务"; },
    handleUpdate(row) {
      this.reset();
      const taskId = row.taskId || this.ids;
      getScheduleTask(taskId).then(response => { this.form = response.data; this.open = true; this.title = "修改调度任务"; });
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return;
        const request = this.form.taskId != null ? updateScheduleTask(this.form) : addScheduleTask(this.form);
        request.then(() => { this.$modal.msgSuccess(this.form.taskId != null ? "修改成功" : "新增成功"); this.open = false; this.getList(); });
      });
    },
    handleDelete(row) {
      const taskIds = row.taskId || this.ids;
      this.$modal.confirm(`删除后该任务将在业务列表中隐藏，但系统仍保留追溯记录。是否继续？任务编号：${taskIds}`).then(() => delScheduleTask(taskIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      if (!this.selectedPlanId) return;
      this.download("aps/scheduleTask/export", { ...this.queryParams, planId: this.selectedPlanId }, `scheduleTask_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.plan-context-card { margin-bottom: 12px; border-radius: 4px; }
.context-header, .context-actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.plan-selector { width: 360px; }
.plan-category-tabs, .time-note, .task-type-tabs, .search-form, .task-toolbar { margin-top: 12px; }
.task-product-card { margin-bottom: 12px; border-radius: 4px; }
.task-product-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.filter-label { color: #303133; font-weight: 600; }
.filter-tip { margin-top: 5px; color: #909399; font-size: 12px; }
.product-selector { width: 300px; }
@media (max-width: 768px) {
  .context-header, .context-actions { align-items: stretch; flex-direction: column; }
  .plan-selector { width: 100%; }
}
</style>
