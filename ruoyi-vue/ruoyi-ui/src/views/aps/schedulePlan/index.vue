<template>
  <div class="app-container">
    <el-card shadow="never" class="product-context-card">
      <div class="product-context-row">
        <div>
          <div class="context-title">方案产品查看上下文</div>
          <div class="context-tip">产品只用于筛选包含该产品任务的全局方案，不会将方案拆分为产品独立方案。</div>
        </div>
        <el-select v-model="selectedProductId" filterable placeholder="请选择产品" class="product-selector" :loading="productLoading" @change="handleProductChange">
          <el-option v-for="product in productList" :key="product.productId" :label="formatProductLabel(product)" :value="product.productId" />
        </el-select>
      </div>
    </el-card>

    <el-tabs v-model="activePlanTab" class="business-tabs" @tab-click="handlePlanTabChange">
      <el-tab-pane label="当前有效" name="active" />
      <el-tab-pane label="候选方案" name="pending" />
      <el-tab-pane label="已确认" name="confirmed" />
      <el-tab-pane label="已拒绝" name="rejected" />
      <el-tab-pane label="历史方案" name="history" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="方案编码" prop="planCode">
        <el-input v-model="queryParams.planCode" placeholder="请输入方案编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案名称" prop="planName">
        <el-input v-model="queryParams.planName" placeholder="请输入方案名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案类型" prop="planType">
        <el-input v-model="queryParams.planType" placeholder="INITIAL / RESCHEDULE" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="方案状态" prop="planStatus">
        <el-input v-model="queryParams.planStatus" placeholder="ACTIVE / HISTORY" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="有效标识" prop="activeFlag">
        <el-input v-model="queryParams.activeFlag" placeholder="Y / N" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:schedulePlan:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-video-play" size="mini" @click="handleGenerateInitial" v-hasPermi="['aps:schedulePlan:add']">生成初始调度</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:schedulePlan:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:schedulePlan:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:schedulePlan:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="schedulePlanList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案ID" align="center" prop="planId" width="90" />
      <el-table-column label="方案编码" align="center" prop="planCode" :show-overflow-tooltip="true" />
      <el-table-column label="方案名称" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="方案类型" align="center" prop="planType" width="110">
        <template slot-scope="scope"><el-tag size="mini" :type="planTypeTag(scope.row.planType)">{{ scope.row.planType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="方案状态" align="center" prop="planStatus" width="110">
        <template slot-scope="scope"><el-tag size="mini" :type="planStatusTag(scope.row.planStatus)">{{ scope.row.planStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column label="有效标识" align="center" prop="activeFlag" width="86">
        <template slot-scope="scope"><el-tag size="mini" :type="scope.row.activeFlag === 'Y' ? 'success' : 'info'">{{ scope.row.activeFlag }}</el-tag></template>
      </el-table-column>
      <el-table-column label="方案范围" align="center" prop="planScope" width="100">
        <template slot-scope="scope"><el-tag size="mini" type="info">{{ scope.row.planScope || 'GLOBAL' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="方案总任务数" align="center" prop="totalTaskCount" width="110" />
      <el-table-column label="当前产品任务数" align="center" prop="currentProductTaskCount" width="125" />
      <el-table-column label="参与产品数" align="center" prop="participatingProductCount" width="105" />
      <el-table-column label="原方案" align="center" min-width="135" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.sourcePlanCode || scope.row.sourcePlanId || '-' }}</template>
      </el-table-column>
      <el-table-column label="插单事件" align="center" min-width="125" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.eventCode || scope.row.eventId || '-' }}</template>
      </el-table-column>
      <el-table-column label="算法名称" align="center" prop="algorithmName" :show-overflow-tooltip="true" />
      <el-table-column label="策略类型" align="center" prop="strategyType" :show-overflow-tooltip="true" />
      <el-table-column label="方案生成时间" align="center" prop="createTime" width="170">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template>
      </el-table-column>
      <el-table-column label="计划起始时间" align="center" prop="scheduleStartTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.scheduleStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计划结束时间" align="center" prop="scheduleEndTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.scheduleEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-tickets" @click="handleViewTasks(scope.row)" v-hasPermi="['aps:scheduleTask:list']">查看任务</el-button>
          <el-button size="mini" type="text" icon="el-icon-data-analysis" @click="handleViewGantt(scope.row)" v-hasPermi="['aps:scheduleTask:list']">查看甘特图</el-button>
          <el-button v-if="scope.row.planType === 'RESCHEDULE'" size="mini" type="text" icon="el-icon-s-data" @click="handleCompare(scope.row)" v-hasPermi="['aps:schedulePlan:list']">方案对比</el-button>
          <el-button v-if="scope.row.planType === 'RESCHEDULE'" size="mini" type="text" icon="el-icon-data-line" @click="handleGanttCompare(scope.row)" v-hasPermi="['aps:scheduleTask:list']">甘特图对比</el-button>
          <el-button v-if="canConfirmOrReject(scope.row)" size="mini" type="text" icon="el-icon-check" @click="handleConfirmPlan(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">采用方案</el-button>
          <el-button v-if="canConfirmOrReject(scope.row)" size="mini" type="text" icon="el-icon-close" @click="handleRejectPlan(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">拒绝方案</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:schedulePlan:edit']">修改</el-button>
          <el-button v-if="canDeletePlan(scope.row)" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:schedulePlan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新旧方案对比" :visible.sync="compareOpen" width="1180px" append-to-body>
      <template v-if="compareDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="原方案">{{ compareDetail.sourcePlanId }} / {{ compareDetail.sourcePlanCode }}</el-descriptions-item>
          <el-descriptions-item label="新方案">{{ compareDetail.newPlanId }} / {{ compareDetail.newPlanCode }}</el-descriptions-item>
          <el-descriptions-item label="插单事件">{{ compareDetail.eventId }}</el-descriptions-item>
          <el-descriptions-item label="插单订单">{{ compareDetail.insertOrderCode }}</el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="compareTab" class="mt12">
          <el-tab-pane label="KPI 总览" name="kpi">
        <el-descriptions class="mt12" :column="3" border size="small" title="任务变化">
          <el-descriptions-item label="原任务数">{{ compareDetail.summary.originalTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="新任务数">{{ compareDetail.summary.newTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="变更任务数">{{ compareDetail.summary.changedTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="变更比例">{{ compareDetail.summary.changedTaskRatio }}</el-descriptions-item>
          <el-descriptions-item label="插单任务数">{{ compareDetail.summary.insertedTaskCount }}</el-descriptions-item>
          <el-descriptions-item label="冻结任务数">{{ compareDetail.summary.frozenTaskCount }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="3" border size="small" title="延期对比">
          <el-descriptions-item label="原延期订单数">{{ compareDetail.delayCompare.originalDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="新延期订单数">{{ compareDetail.delayCompare.newDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.delayOrderCountDiff }}</el-descriptions-item>
          <el-descriptions-item label="原总延期分钟">{{ compareDetail.delayCompare.originalTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新总延期分钟">{{ compareDetail.delayCompare.newTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.totalDelayMinutesDiff }}</el-descriptions-item>
          <el-descriptions-item label="原最大延期分钟">{{ compareDetail.delayCompare.originalMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新最大延期分钟">{{ compareDetail.delayCompare.newMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ compareDetail.delayCompare.maxDelayMinutesDiff }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="3" border size="small" title="真实交期延期">
          <el-descriptions-item label="原真实延期订单数">{{ trueDelayCompare.originalTrueDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="新真实延期订单数">{{ trueDelayCompare.newTrueDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ trueDelayCompare.trueDelayOrderCountDiff }}</el-descriptions-item>
          <el-descriptions-item label="原真实总延期分钟">{{ trueDelayCompare.originalTrueTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新真实总延期分钟">{{ trueDelayCompare.newTrueTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ trueDelayCompare.trueTotalDelayMinutesDiff }}</el-descriptions-item>
          <el-descriptions-item label="原真实最大延期分钟">{{ trueDelayCompare.originalTrueMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="新真实最大延期分钟">{{ trueDelayCompare.newTrueMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="变化">{{ trueDelayCompare.trueMaxDelayMinutesDiff }}</el-descriptions-item>
          <el-descriptions-item label="插单真实延期分钟">{{ trueDelayCompare.insertOrderTrueDelayMinutes }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="4" border size="small" title="计划稳定性扰动">
          <el-descriptions-item label="后移订单数">{{ stabilityCompare.stabilityDelayOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="总后移分钟">{{ stabilityCompare.stabilityTotalDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="最大后移分钟">{{ stabilityCompare.stabilityMaxDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="平均后移分钟">{{ stabilityCompare.averageStabilityDelayMinutes }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions class="mt12" :column="2" border size="small" title="完工时间">
          <el-descriptions-item label="原 makespan">{{ compareDetail.makespanCompare.originalMakespan }}</el-descriptions-item>
          <el-descriptions-item label="新 makespan">{{ compareDetail.makespanCompare.newMakespan }}</el-descriptions-item>
          <el-descriptions-item label="makespan 变化分钟">{{ compareDetail.makespanCompare.makespanDiffMinutes }}</el-descriptions-item>
          <el-descriptions-item label="插单完工时间">{{ compareDetail.insertOrderCompare.insertOrderFinishTime }}</el-descriptions-item>
          <el-descriptions-item label="插单真实延期分钟">{{ insertOrderCompare.insertOrderTrueDelayMinutes }}</el-descriptions-item>
          <el-descriptions-item label="结论">{{ compareDetail.conclusion }}</el-descriptions-item>
        </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="受影响 Lot" name="orders">
            <el-table :data="compareDetail.affectedOrderDetails || []" border size="small" max-height="480" empty-text="没有实际受影响的 Lot">
              <el-table-column label="Lot 编码" prop="orderCode" min-width="130" fixed="left" />
              <el-table-column label="类型" prop="orderType" width="90" />
              <el-table-column label="优先级" prop="priorityLevel" width="80" />
              <el-table-column label="原完工时间" prop="originalFinishTime" min-width="160" />
              <el-table-column label="新完工时间" prop="newFinishTime" min-width="160" />
              <el-table-column label="真实延期" prop="trueDelayMinutes" width="95" />
              <el-table-column label="计划后移" prop="stabilityDelayMinutes" width="95" />
              <el-table-column label="变更任务" prop="changedTaskCount" width="90" />
              <el-table-column label="影响类型" prop="impactType" width="105" fixed="right">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="impactTagType(scope.row.impactType)">{{ scope.row.impactType }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="变更任务" name="tasks">
            <el-table :data="compareDetail.changedTaskDetails || []" border size="small" max-height="480" empty-text="没有任务发生变化">
              <el-table-column label="Lot 编码" prop="orderCode" min-width="125" fixed="left" />
              <el-table-column label="工序" min-width="115">
                <template slot-scope="scope">{{ scope.row.processSeq }} / {{ scope.row.processCode }}</template>
              </el-table-column>
              <el-table-column label="原设备" prop="originalEquipmentCode" min-width="110" />
              <el-table-column label="新设备" prop="equipmentCode" min-width="110" />
              <el-table-column label="原开始" prop="originalStartTime" min-width="160" />
              <el-table-column label="新开始" prop="newStartTime" min-width="160" />
              <el-table-column label="位移分钟" prop="startShiftMinutes" width="95" />
              <el-table-column label="变更类型" prop="changeType" min-width="185" fixed="right">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="changeTagType(scope.row.changeType)">{{ scope.row.changeType }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="决策证据" name="decision">
            <template v-if="compareDetail.decisionEvidence">
              <el-alert
                :title="'辅助建议：' + compareDetail.decisionEvidence.recommendationLevel + '。' + compareDetail.decisionEvidence.recommendationReason"
                :type="recommendationAlertType(compareDetail.decisionEvidence.recommendationLevel)"
                :closable="false"
                show-icon
              />
              <el-descriptions class="mt12" :column="3" border size="small">
                <el-descriptions-item label="插单按期">{{ compareDetail.decisionEvidence.insertOrderOnTime ? '是' : '否' }}</el-descriptions-item>
                <el-descriptions-item label="插单真实延期">{{ compareDetail.decisionEvidence.insertOrderTrueDelayMinutes }} 分钟</el-descriptions-item>
                <el-descriptions-item label="受影响 Lot">{{ compareDetail.decisionEvidence.affectedOrderCount }}</el-descriptions-item>
                <el-descriptions-item label="后移 Lot">{{ compareDetail.decisionEvidence.delayedOrderCount }}</el-descriptions-item>
                <el-descriptions-item label="提前 Lot">{{ compareDetail.decisionEvidence.advancedOrderCount }}</el-descriptions-item>
                <el-descriptions-item label="变更任务">{{ compareDetail.decisionEvidence.changedTaskCount }}</el-descriptions-item>
                <el-descriptions-item label="变更比例">{{ compareDetail.decisionEvidence.changedTaskRatio }}</el-descriptions-item>
                <el-descriptions-item label="最大计划后移">{{ compareDetail.decisionEvidence.maxStabilityDelayMinutes }} 分钟</el-descriptions-item>
                <el-descriptions-item label="总计划后移">{{ compareDetail.decisionEvidence.totalStabilityDelayMinutes }} 分钟</el-descriptions-item>
                <el-descriptions-item label="受影响设备组">{{ compareDetail.decisionEvidence.affectedEquipmentGroupCount }}</el-descriptions-item>
                <el-descriptions-item label="设备组" :span="2">{{ formatEquipmentGroups(compareDetail.decisionEvidence.affectedEquipmentGroups) }}</el-descriptions-item>
              </el-descriptions>
              <div class="evidence-note">该建议仅用于辅助调度员判断，不会自动采用或拒绝方案。</div>
            </template>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="方案编码" prop="planCode">
          <el-input v-model="form.planCode" placeholder="请输入方案编码" />
        </el-form-item>
        <el-form-item label="方案名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入方案名称" />
        </el-form-item>
        <el-form-item label="方案类型" prop="planType">
          <el-input v-model="form.planType" placeholder="INITIAL / RESCHEDULE" />
        </el-form-item>
        <el-form-item label="方案状态" prop="planStatus">
          <el-input v-model="form.planStatus" placeholder="PENDING / ACTIVE / HISTORY" />
        </el-form-item>
        <el-form-item label="算法名称" prop="algorithmName">
          <el-input v-model="form.algorithmName" placeholder="请输入算法名称" />
        </el-form-item>
        <el-form-item label="策略类型" prop="strategyType">
          <el-input v-model="form.strategyType" placeholder="请输入策略类型" />
        </el-form-item>
        <el-form-item label="计划起始时间" prop="scheduleStartTime">
          <el-date-picker clearable v-model="form.scheduleStartTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划起始时间" />
        </el-form-item>
        <el-form-item label="计划结束时间" prop="scheduleEndTime">
          <el-date-picker clearable v-model="form.scheduleEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划结束时间" />
        </el-form-item>
        <el-form-item label="KPI" prop="kpiJson">
          <el-input v-model="form.kpiJson" type="textarea" placeholder="请输入KPI JSON" />
        </el-form-item>
        <el-form-item label="有效标识" prop="activeFlag">
          <el-input v-model="form.activeFlag" placeholder="Y / N" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSchedulePlan, getSchedulePlan, delSchedulePlan, addSchedulePlan, updateSchedulePlan, generateInitialSchedule, compareSchedulePlan, confirmSchedulePlan, rejectSchedulePlan } from "@/api/aps/schedulePlan";
import { listProduct } from "@/api/aps/product";

export default {
  name: "SchedulePlan",
  data() {
    return {
      loading: true,
      productLoading: false,
      productList: [],
      selectedProductId: null,
      ids: [],
      selectedPlans: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      schedulePlanList: [],
      title: "",
      open: false,
      compareOpen: false,
      compareTab: "kpi",
      compareDetail: null,
      activePlanTab: "active",
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planCode: null,
        planName: null,
        planType: null,
        planStatus: null,
        activeFlag: null,
        productId: null
      },
      form: {}
    };
  },
  created() {
    this.loadProducts();
  },
  computed: {
    selectedProduct() {
      return this.productList.find(item => String(item.productId) === String(this.selectedProductId)) || null;
    },
    trueDelayCompare() {
      return this.compareDetail && this.compareDetail.trueDelayCompare
        ? this.compareDetail.trueDelayCompare
        : this.legacyTrueDelayCompare;
    },
    stabilityCompare() {
      return this.compareDetail && this.compareDetail.stabilityCompare
        ? this.compareDetail.stabilityCompare
        : {};
    },
    insertOrderCompare() {
      const compare = this.compareDetail && this.compareDetail.insertOrderCompare ? this.compareDetail.insertOrderCompare : {};
      return {
        ...compare,
        insertOrderTrueDelayMinutes: compare.insertOrderTrueDelayMinutes || compare.insertOrderDelayMinutes || 0
      };
    },
    legacyTrueDelayCompare() {
      const delay = this.compareDetail && this.compareDetail.delayCompare ? this.compareDetail.delayCompare : {};
      const insertOrder = this.compareDetail && this.compareDetail.insertOrderCompare ? this.compareDetail.insertOrderCompare : {};
      return {
        originalTrueDelayOrderCount: delay.originalDelayOrderCount,
        newTrueDelayOrderCount: delay.newDelayOrderCount,
        trueDelayOrderCountDiff: delay.delayOrderCountDiff,
        originalTrueTotalDelayMinutes: delay.originalTotalDelayMinutes,
        newTrueTotalDelayMinutes: delay.newTotalDelayMinutes,
        trueTotalDelayMinutesDiff: delay.totalDelayMinutesDiff,
        originalTrueMaxDelayMinutes: delay.originalMaxDelayMinutes,
        newTrueMaxDelayMinutes: delay.newMaxDelayMinutes,
        trueMaxDelayMinutesDiff: delay.maxDelayMinutesDiff,
        insertOrderTrueDelayMinutes: insertOrder.insertOrderTrueDelayMinutes || insertOrder.insertOrderDelayMinutes || 0
      };
    }
  },
  methods: {
    loadProducts() {
      this.productLoading = true;
      listProduct({ pageNum: 1, pageSize: 200, status: "0" }).then(response => {
        this.productList = response.rows || [];
        this.selectedProductId = (this.productList[0] || {}).productId || null;
        this.handleProductChange();
      }).finally(() => {
        this.productLoading = false;
      });
    },
    handleProductChange() {
      this.queryParams.productId = this.selectedProductId || null;
      this.queryParams.pageNum = 1;
      this.clearPlanSelection();
      this.getList();
    },
    formatProductLabel(product) {
      return `${product.productCode} / ${product.productName}`;
    },
    getList() {
      this.loading = true;
      listSchedulePlan(this.buildPlanQuery()).then(response => {
        this.schedulePlanList = (response.rows || []).sort(this.comparePlanCreateTimeDesc);
        this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    buildPlanQuery() {
      const query = { ...this.queryParams };
      const filters = {
        active: { activeFlag: "Y" },
        pending: { planType: "RESCHEDULE", planStatus: "PENDING" },
        confirmed: { planStatus: "CONFIRMED" },
        rejected: { planStatus: "REJECTED" },
        history: { planStatus: "HISTORY" },
        all: {}
      };
      return { ...query, ...(filters[this.activePlanTab] || {}), productId: this.selectedProductId };
    },
    handlePlanTabChange() {
      this.queryParams.pageNum = 1;
      this.clearPlanSelection();
      this.getList();
    },
    clearPlanSelection() {
      this.ids = [];
      this.selectedPlans = [];
      this.single = true;
      this.multiple = true;
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
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        planId: null,
        planCode: null,
        planName: null,
        planType: null,
        planStatus: null,
        sourcePlanId: null,
        eventId: null,
        algorithmName: null,
        strategyType: null,
        scheduleStartTime: null,
        scheduleEndTime: null,
        kpiJson: null,
        aiExplanation: null,
        activeFlag: null,
        delFlag: "0",
        remark: null
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.productId = this.selectedProductId;
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planId);
      this.selectedPlans = selection;
      this.single = selection.length !== 1;
      this.multiple = !selection.length || selection.some(item => !this.canDeletePlan(item));
    },
    canDeletePlan(plan) {
      const protectedStatuses = ["ACTIVE", "CONFIRMED", "HISTORY"];
      return plan.activeFlag !== "Y"
        && !protectedStatuses.includes((plan.planStatus || "").toUpperCase())
        && !plan.eventId;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增调度方案";
    },
    handleGenerateInitial() {
      this.$modal.confirm("确认基于当前普通 NEW 订单生成初始调度方案？").then(() => {
        return generateInitialSchedule();
      }).then(response => {
        const taskCount = response.data && response.data.taskCount ? response.data.taskCount : 0;
        this.$modal.msgSuccess("初始调度生成成功，任务数：" + taskCount);
        this.getList();
      }).catch(() => {});
    },
    handleUpdate(row) {
      this.reset();
      const planId = row.planId || this.ids;
      getSchedulePlan(planId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改调度方案";
      });
    },
    handleViewTasks(row) {
      this.$router.push({ path: "/aps/scheduleTask", query: { planId: row.planId, productId: this.selectedProductId } });
    },
    handleViewGantt(row) {
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId: row.planId } });
    },
    handleCompare(row) {
      compareSchedulePlan(row.planId).then(response => {
        this.compareDetail = response.data;
        this.compareTab = "kpi";
        this.compareOpen = true;
      });
    },
    impactTagType(type) {
      const types = { INSERTED: "danger", DELAYED: "warning", ADVANCED: "success", CHANGED: "info" };
      return types[type] || "info";
    },
    changeTagType(type) {
      if (type === "INSERTED") return "danger";
      if (type === "FROZEN") return "info";
      if (type === "EQUIPMENT_CHANGED" || type === "TIME_AND_EQUIPMENT_CHANGED") return "warning";
      return "";
    },
    recommendationAlertType(level) {
      if (level === "RECOMMENDED") return "success";
      if (level === "NOT_RECOMMENDED") return "error";
      return "warning";
    },
    formatEquipmentGroups(groups) {
      if (!groups || !groups.length) return "-";
      return groups.map(item => item.equipmentGroupCode || item.equipmentGroupName || item.equipmentGroupId).join("、");
    },
    handleGanttCompare(row) {
      this.$router.push({ path: "/aps/scheduleTask/ganttCompare", query: { planId: row.planId } });
    },
    canConfirmOrReject(row) {
      return row.planType === "RESCHEDULE" && row.planStatus === "PENDING";
    },
    handleConfirmPlan(row) {
      this.$modal.confirm("确定采用该重调度方案吗？采用后原方案将转为历史方案。").then(() => {
        return confirmSchedulePlan(row.planId);
      }).then(() => {
        this.$modal.msgSuccess("方案已确认采用");
        this.getList();
      }).catch(() => {});
    },
    handleRejectPlan(row) {
      this.$modal.confirm("确定拒绝该重调度方案吗？拒绝后系统将恢复原初始方案为当前有效方案。").then(() => {
        return rejectSchedulePlan(row.planId);
      }).then(() => {
        this.$modal.msgSuccess("方案已拒绝");
        this.getList();
      }).catch(() => {});
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.planId != null) {
            updateSchedulePlan(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addSchedulePlan(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const planIds = row.planId || this.ids;
      const plans = row.planId ? [row] : this.selectedPlans;
      if (plans.some(item => !this.canDeletePlan(item))) {
        this.$modal.msgWarning("激活、确认、历史或已关联插单事件的方案禁止删除");
        return;
      }
      this.$modal.confirm('删除后该方案将在业务列表中隐藏，但系统仍保留追溯记录。是否继续？方案编号：' + planIds).then(function() {
        return delSchedulePlan(planIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download("aps/schedulePlan/export", {
        ...this.buildPlanQuery()
      }, `schedulePlan_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.mt12 {
  margin-top: 12px;
}

.business-tabs {
  margin-bottom: 12px;
}

.product-context-card { margin-bottom: 12px; border-radius: 4px; }
.product-context-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.context-title { color: #303133; font-size: 16px; font-weight: 600; }
.context-tip { margin-top: 5px; color: #909399; font-size: 12px; }
.product-selector { width: 360px; }

.evidence-note {
  margin-top: 12px;
  color: #606266;
  font-size: 12px;
}
</style>
