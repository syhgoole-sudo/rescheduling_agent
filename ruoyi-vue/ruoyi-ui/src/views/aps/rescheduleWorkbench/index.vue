<template>
  <div class="app-container reschedule-workbench">
    <el-card shadow="never" class="workbench-header">
      <div class="title-row">
        <div>
          <div class="page-title">插单重调度工作台</div>
          <div class="page-subtitle">统一串联产品工艺、Hot Lot、初始调度、插单分析、策略推荐、重调度候选和人工确认。</div>
        </div>
        <el-button icon="el-icon-refresh" size="mini" @click="handleRestart">重新开始</el-button>
      </div>
      <el-steps :active="activeStep" finish-status="success" process-status="process" simple>
        <el-step title="选择产品" />
        <el-step title="选择 Hot Lot" />
        <el-step title="初始调度" />
        <el-step title="插单分析" />
        <el-step title="策略推荐" />
        <el-step title="重调度候选" />
        <el-step title="对比确认" />
      </el-steps>
    </el-card>

    <el-alert
      class="global-tip"
      type="info"
      :closable="false"
      show-icon
      title="当前初始调度为全局调度，产品选择仅用于展示工艺路线和筛选 Hot Lot。"
    />

    <el-row :gutter="12" class="main-grid">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>业务上下文</span>
            <el-tag size="mini" type="info">{{ workbenchStage }}</el-tag>
          </div>

          <el-form label-width="86px" size="small">
            <el-form-item label="产品">
              <el-select
                v-model="selectedProductId"
                filterable
                clearable
                placeholder="请选择产品"
                style="width: 100%"
                @change="handleProductChange"
              >
                <el-option
                  v-for="item in productList"
                  :key="item.productId"
                  :label="formatProductLabel(item)"
                  :value="item.productId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Hot Lot">
              <el-select
                v-model="selectedHotLotId"
                filterable
                clearable
                placeholder="请选择插单订单"
                style="width: 100%"
                :disabled="!selectedProductId"
                @change="handleHotLotChange"
              >
                <el-option
                  v-for="item in hotLotList"
                  :key="item.orderId"
                  :label="formatHotLotLabel(item)"
                  :value="item.orderId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="算法">
              <el-radio-group v-model="algorithmType">
                <el-radio-button label="RULE">RULE</el-radio-button>
                <el-radio-button label="GA">GA</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="algorithmType === 'GA'" label="randomSeed">
              <el-input-number
                v-model="randomSeed"
                :min="0"
                :step="1"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>

          <el-descriptions v-if="selectedProduct" :column="1" border size="small" class="section-gap">
            <el-descriptions-item label="产品编码">{{ selectedProduct.productCode }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ selectedProduct.productName }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ selectedProduct.status }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">可重入工艺路线</div>
          <el-alert
            v-if="reentrantGroupIds.length"
            class="route-alert"
            type="warning"
            :closable="false"
            show-icon
            :title="'存在可重入设备组：' + reentrantGroupIds.join(', ')"
          />
          <el-table
            v-loading="routeLoading"
            :data="routeOperationList"
            size="mini"
            height="260"
            border
            empty-text="请选择产品后加载工艺路线"
          >
            <el-table-column label="序号" prop="processSeq" width="64" align="center" />
            <el-table-column label="工序编码" prop="processCode" min-width="105" show-overflow-tooltip />
            <el-table-column label="工序名称" prop="processName" min-width="110" show-overflow-tooltip />
            <el-table-column label="设备组" prop="equipmentGroupId" width="82" align="center">
              <template slot-scope="scope">
                <el-tag v-if="isReentrantGroup(scope.row.equipmentGroupId)" size="mini" type="warning">
                  {{ scope.row.equipmentGroupId }}
                </el-tag>
                <span v-else>{{ scope.row.equipmentGroupId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="时长" prop="standardDuration" width="72" align="center" />
          </el-table>

          <div class="section-title">Hot Lot 列表</div>
          <el-table
            v-loading="hotLotLoading"
            :data="hotLotList"
            size="mini"
            height="220"
            border
            highlight-current-row
            empty-text="当前产品下暂无 INSERT 订单"
            @current-change="handleHotLotRowChange"
          >
            <el-table-column label="ID" prop="orderId" width="62" align="center" />
            <el-table-column label="订单编码" prop="orderCode" min-width="120" show-overflow-tooltip />
            <el-table-column label="产品" prop="productCode" min-width="95" show-overflow-tooltip />
            <el-table-column label="优先级" prop="priorityLevel" width="72" align="center" />
            <el-table-column label="交期" prop="dueTime" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" prop="orderStatus" width="86" align="center" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>流程操作</span>
            <el-tag size="mini" :type="isTerminalStage ? 'success' : 'primary'">{{ stageText }}</el-tag>
          </div>

          <el-form label-width="106px" size="small">
            <el-form-item label="当前原方案">
              <el-select
                v-model="sourcePlanId"
                filterable
                clearable
                placeholder="请选择 ACTIVE / INITIAL 方案"
                style="width: 100%"
                :disabled="!canSelectSourcePlan"
                @change="handleSourcePlanChange"
              >
                <el-option
                  v-for="item in initialActivePlans"
                  :key="item.planId"
                  :label="formatPlanLabel(item)"
                  :value="item.planId"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <div class="action-list">
            <el-button
              type="primary"
              icon="el-icon-video-play"
              :loading="initialLoading"
              :disabled="!canGenerateInitial"
              @click="handleGenerateInitial"
            >
              生成初始调度方案
            </el-button>
            <el-button
              type="success"
              icon="el-icon-search"
              :loading="eventLoading"
              :disabled="!canAnalyzeInsert"
              @click="handleCreateAndAnalyze"
            >
              创建并分析插单
            </el-button>
            <el-button
              type="warning"
              icon="el-icon-s-operation"
              :loading="strategyLoading"
              :disabled="!canRecommendStrategy"
              @click="handleRecommendStrategy"
            >
              推荐策略
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-refresh-right"
              :loading="rescheduleLoading"
              :disabled="!canGenerateReschedule"
              @click="handleGenerateLocalReschedule"
            >
              生成重调度候选方案
            </el-button>
          </div>

          <el-divider content-position="left">操作说明</el-divider>
          <el-timeline>
            <el-timeline-item timestamp="1" :type="selectedProduct ? 'success' : 'info'">选择产品后加载工艺路线和 Hot Lot。</el-timeline-item>
            <el-timeline-item timestamp="2" :type="selectedHotLot ? 'success' : 'info'">选择 INSERT 订单作为插单 Hot Lot。</el-timeline-item>
            <el-timeline-item timestamp="3" :type="sourcePlan ? 'success' : 'info'">生成或选择当前 ACTIVE 初始方案。</el-timeline-item>
            <el-timeline-item timestamp="4" :type="insertEvent ? 'success' : 'info'">创建插单事件并完成影响分析。</el-timeline-item>
            <el-timeline-item timestamp="5" :type="strategyDetail ? 'success' : 'info'">推荐局部重调度策略。</el-timeline-item>
            <el-timeline-item timestamp="6" :type="newPlan ? 'success' : 'info'">生成 PENDING 重调度候选方案。</el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>结果摘要</span>
            <el-button-group v-if="newPlan && newPlan.planStatus === 'PENDING'">
              <el-button size="mini" :disabled="!canConfirmOrReject" @click="handleConfirmPlan">采用</el-button>
              <el-button size="mini" :disabled="!canConfirmOrReject" @click="handleRejectPlan">拒绝</el-button>
            </el-button-group>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="原方案">{{ sourcePlan ? formatPlanLabel(sourcePlan) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="插单事件">{{ insertEvent ? formatEventLabel(insertEvent) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="推荐策略">{{ displayStrategyType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="候选方案">{{ newPlan ? formatPlanLabel(newPlan) : '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">影响分析</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="影响等级">{{ displayImpact.impactLevel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事件状态">{{ insertEvent ? insertEvent.eventStatus : '-' }}</el-descriptions-item>
            <el-descriptions-item label="影响任务">{{ valueOrDash(displayImpact.affectedTaskCount) }}</el-descriptions-item>
            <el-descriptions-item label="影响订单">{{ valueOrDash(displayImpact.affectedOrderCount) }}</el-descriptions-item>
            <el-descriptions-item label="影响设备">{{ valueOrDash(displayImpact.affectedEquipmentCount) }}</el-descriptions-item>
            <el-descriptions-item label="新方案ID">{{ insertEvent ? valueOrDash(insertEvent.newPlanId) : '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">策略结果</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="策略名称">{{ strategyDetail.strategyName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="策略类型">{{ displayStrategyType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="冻结任务">{{ valueOrDash(strategyDetail.frozenTaskCount) }}</el-descriptions-item>
            <el-descriptions-item label="可调任务">{{ valueOrDash(strategyDetail.adjustableTaskCount) }}</el-descriptions-item>
            <el-descriptions-item label="插单任务">{{ valueOrDash(strategyDetail.insertTaskCount) }}</el-descriptions-item>
            <el-descriptions-item label="算法">{{ displayAlgorithmName }}</el-descriptions-item>
            <el-descriptions-item v-if="displayRandomSeed !== null" label="randomSeed">{{ displayRandomSeed }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">KPI 摘要</div>
          <el-table :data="kpiRows" size="mini" border height="260" empty-text="生成候选方案后展示 KPI">
            <el-table-column label="指标" prop="label" min-width="145" />
            <el-table-column label="值" prop="value" min-width="120" show-overflow-tooltip />
          </el-table>
          <div class="evidence-actions">
            <el-button size="mini" icon="el-icon-s-order" :disabled="!newPlanId" @click="openEvidence('orders')">查看受影响 Lot</el-button>
            <el-button size="mini" icon="el-icon-s-operation" :disabled="!newPlanId" @click="openEvidence('tasks')">查看变更任务</el-button>
            <el-button size="mini" icon="el-icon-document-checked" :disabled="!newPlanId" @click="openEvidence('decision')">查看决策证据</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="entry-card">
      <div slot="header" class="panel-header">
        <span>结果入口</span>
        <span class="entry-tip">甘特图使用现有页面，工作台仅做流程整合。</span>
      </div>
      <el-button icon="el-icon-data-analysis" :disabled="!sourcePlanId" @click="openGantt(sourcePlanId)">查看原方案甘特图</el-button>
      <el-button icon="el-icon-data-analysis" :disabled="!newPlanId" @click="openGantt(newPlanId)">查看候选方案甘特图</el-button>
      <el-button icon="el-icon-data-line" :disabled="!newPlanId" @click="openGanttCompare">新旧甘特图对比</el-button>
      <el-button icon="el-icon-s-data" :disabled="!newPlanId" @click="openKpiDialog">查看 KPI 对比</el-button>
      <el-button icon="el-icon-document" :loading="explainLoading" :disabled="!insertEventId" @click="handleExplanationReport">解释报告</el-button>
      <el-button icon="el-icon-chat-dot-round" :loading="aiExplainLoading" :disabled="!insertEventId" @click="handleAiExplanationReport">AI 分析报告</el-button>
    </el-card>

    <gantt-preview ref="ganttPreview" :source-plan-id="sourcePlanId" :new-plan-id="newPlanId" />

    <el-dialog title="KPI 对比" :visible.sync="kpiOpen" width="860px" append-to-body>
      <el-table :data="kpiRows" size="small" border>
        <el-table-column label="指标" prop="label" width="240" />
        <el-table-column label="字段" prop="key" width="220" />
        <el-table-column label="值" prop="value" />
      </el-table>
      <el-input class="section-gap" type="textarea" :rows="12" :value="prettyJson(compareDetail)" readonly />
    </el-dialog>

    <el-dialog :title="evidenceTitle" :visible.sync="evidenceOpen" width="1120px" append-to-body>
      <el-table
        v-if="evidenceView === 'orders'"
        :data="compareDetail && compareDetail.affectedOrderDetails ? compareDetail.affectedOrderDetails : []"
        border
        size="small"
        max-height="500"
        empty-text="没有实际受影响的 Lot"
      >
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

      <el-table
        v-if="evidenceView === 'tasks'"
        :data="compareDetail && compareDetail.changedTaskDetails ? compareDetail.changedTaskDetails : []"
        border
        size="small"
        max-height="500"
        empty-text="没有任务发生变化"
      >
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

      <template v-if="evidenceView === 'decision' && compareDetail && compareDetail.decisionEvidence">
        <el-alert
          :title="'辅助建议：' + compareDetail.decisionEvidence.recommendationLevel + '。' + compareDetail.decisionEvidence.recommendationReason"
          :type="recommendationAlertType(compareDetail.decisionEvidence.recommendationLevel)"
          :closable="false"
          show-icon
        />
        <el-descriptions class="section-gap" :column="3" border size="small">
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
    </el-dialog>

    <el-dialog title="解释报告" :visible.sync="explanationOpen" width="860px" append-to-body>
      <el-input type="textarea" :rows="18" :value="formatReport(explanationDetail)" readonly />
    </el-dialog>

    <el-dialog title="AI 分析报告" :visible.sync="aiExplanationOpen" width="900px" append-to-body>
      <el-alert
        v-if="aiExplanationDetail && aiExplanationDetail.fallbackUsed"
        class="section-gap"
        type="warning"
        :closable="false"
        show-icon
        title="AI 服务使用了 fallback 结果，建议结合解释报告复核。"
      />
      <el-input type="textarea" :rows="18" :value="formatReport(aiExplanationDetail)" readonly />
    </el-dialog>
  </div>
</template>

<script>
import { listProduct } from "@/api/aps/product";
import { listRouteOperation } from "@/api/aps/routeOperation";
import { listOrder } from "@/api/aps/order";
import { listSchedulePlan, getSchedulePlan, generateInitialSchedule, compareSchedulePlan, confirmSchedulePlan, rejectSchedulePlan } from "@/api/aps/schedulePlan";
import { getInsertEvent, createAndAnalyzeInsertEvent, recommendStrategy, generateLocalReschedule, generateExplanationReport, generateAiExplanationReport } from "@/api/aps/insertEvent";
import GanttPreview from "./GanttPreview.vue";

const STAGE = {
  EMPTY: "EMPTY",
  PRODUCT_SELECTED: "PRODUCT_SELECTED",
  HOT_LOT_SELECTED: "HOT_LOT_SELECTED",
  INITIAL_READY: "INITIAL_READY",
  EVENT_ANALYZED: "EVENT_ANALYZED",
  STRATEGY_RECOMMENDED: "STRATEGY_RECOMMENDED",
  RESCHEDULE_GENERATED: "RESCHEDULE_GENERATED",
  CONFIRMED: "CONFIRMED",
  REJECTED: "REJECTED"
};

export default {
  name: "RescheduleWorkbench",
  components: { GanttPreview },
  data() {
    return {
      productLoading: false,
      routeLoading: false,
      hotLotLoading: false,
      initialLoading: false,
      eventLoading: false,
      strategyLoading: false,
      rescheduleLoading: false,
      explainLoading: false,
      aiExplainLoading: false,
      productList: [],
      routeOperationList: [],
      hotLotList: [],
      initialActivePlans: [],
      selectedProductId: null,
      selectedHotLotId: null,
      sourcePlanId: null,
      algorithmType: "RULE",
      randomSeed: 42,
      workbenchStage: STAGE.EMPTY,
      sourcePlan: null,
      insertEvent: null,
      strategyDetail: {},
      rescheduleResult: {},
      newPlan: null,
      compareDetail: null,
      explanationDetail: null,
      aiExplanationDetail: null,
      kpiOpen: false,
      evidenceOpen: false,
      evidenceView: "orders",
      explanationOpen: false,
      aiExplanationOpen: false
    };
  },
  computed: {
    selectedProduct() {
      const id = this.selectedProductId;
      return this.productList.find(item => item.productId === id) || null;
    },
    selectedHotLot() {
      const id = this.selectedHotLotId;
      return this.hotLotList.find(item => item.orderId === id) || null;
    },
    insertEventId() {
      return this.insertEvent ? this.insertEvent.eventId : null;
    },
    newPlanId() {
      if (this.newPlan && this.newPlan.planId) {
        return this.newPlan.planId;
      }
      if (this.insertEvent && this.insertEvent.newPlanId) {
        return this.insertEvent.newPlanId;
      }
      return null;
    },
    activeStep() {
      const map = {};
      map[STAGE.EMPTY] = 0;
      map[STAGE.PRODUCT_SELECTED] = 1;
      map[STAGE.HOT_LOT_SELECTED] = 2;
      map[STAGE.INITIAL_READY] = 3;
      map[STAGE.EVENT_ANALYZED] = 4;
      map[STAGE.STRATEGY_RECOMMENDED] = 5;
      map[STAGE.RESCHEDULE_GENERATED] = 6;
      map[STAGE.CONFIRMED] = 7;
      map[STAGE.REJECTED] = 7;
      return map[this.workbenchStage] || 0;
    },
    stageText() {
      const map = {};
      map[STAGE.EMPTY] = "等待选择产品";
      map[STAGE.PRODUCT_SELECTED] = "已选择产品";
      map[STAGE.HOT_LOT_SELECTED] = "已选择 Hot Lot";
      map[STAGE.INITIAL_READY] = "初始方案就绪";
      map[STAGE.EVENT_ANALYZED] = "插单已分析";
      map[STAGE.STRATEGY_RECOMMENDED] = "策略已推荐";
      map[STAGE.RESCHEDULE_GENERATED] = "候选方案已生成";
      map[STAGE.CONFIRMED] = "候选方案已采用";
      map[STAGE.REJECTED] = "候选方案已拒绝";
      return map[this.workbenchStage] || this.workbenchStage;
    },
    isTerminalStage() {
      return this.workbenchStage === STAGE.CONFIRMED || this.workbenchStage === STAGE.REJECTED;
    },
    canSelectSourcePlan() {
      return !!this.selectedHotLot && !this.isTerminalStage;
    },
    canGenerateInitial() {
      return !!this.selectedHotLot && !this.isTerminalStage;
    },
    canAnalyzeInsert() {
      return !!this.selectedHotLot && !!this.sourcePlanId && !this.insertEvent && !this.isTerminalStage;
    },
    canRecommendStrategy() {
      return !!this.insertEvent && this.workbenchStage === STAGE.EVENT_ANALYZED && !this.isTerminalStage;
    },
    canGenerateReschedule() {
      return !!this.insertEvent && this.workbenchStage === STAGE.STRATEGY_RECOMMENDED && !this.newPlanId && !this.isTerminalStage;
    },
    canConfirmOrReject() {
      return this.newPlan && this.newPlan.planStatus === "PENDING" && this.workbenchStage === STAGE.RESCHEDULE_GENERATED;
    },
    routeGroupCount() {
      const count = {};
      this.routeOperationList.forEach(item => {
        if (item.equipmentGroupId !== null && item.equipmentGroupId !== undefined) {
          const key = String(item.equipmentGroupId);
          count[key] = (count[key] || 0) + 1;
        }
      });
      return count;
    },
    reentrantGroupIds() {
      return Object.keys(this.routeGroupCount).filter(key => this.routeGroupCount[key] > 1);
    },
    displayImpact() {
      if (!this.insertEvent || !this.insertEvent.impactJson) {
        return {};
      }
      return this.parseJson(this.insertEvent.impactJson) || {};
    },
    displayStrategyType() {
      return this.strategyDetail.strategyType || (this.insertEvent ? this.insertEvent.strategyType : null);
    },
    displayAlgorithmName() {
      return this.rescheduleResult.algorithmType || this.rescheduleResult.algorithmName ||
        (this.newPlan ? this.newPlan.algorithmName : null) || this.algorithmType;
    },
    displayRandomSeed() {
      if (this.rescheduleResult.randomSeed !== undefined && this.rescheduleResult.randomSeed !== null) {
        return this.rescheduleResult.randomSeed;
      }
      const planKpi = this.newPlan ? this.parseJson(this.newPlan.kpiJson) : null;
      return planKpi && planKpi.randomSeed !== undefined ? planKpi.randomSeed : null;
    },
    kpiRows() {
      const detail = this.compareDetail || {};
      const summary = detail.summary || {};
      const legacyDelay = detail.delayCompare || {};
      const delay = detail.trueDelayCompare || {
        newTrueDelayOrderCount: legacyDelay.newDelayOrderCount,
        trueTotalDelayMinutesDiff: legacyDelay.totalDelayMinutesDiff
      };
      const stability = detail.stabilityCompare || {};
      const insertOrder = detail.insertOrderCompare || {};
      const makespan = detail.makespanCompare || {};
      const rows = [
        ["insertOrderTrueDelayMinutes", "插单真实延期分钟", insertOrder.insertOrderTrueDelayMinutes || insertOrder.insertOrderDelayMinutes],
        ["newTrueDelayOrderCount", "新方案真实延期订单数", delay.newTrueDelayOrderCount],
        ["trueTotalDelayMinutesDiff", "真实总延期变化", delay.trueTotalDelayMinutesDiff],
        ["stabilityDelayOrderCount", "计划后移订单数", stability.stabilityDelayOrderCount],
        ["stabilityTotalDelayMinutes", "计划总后移分钟", stability.stabilityTotalDelayMinutes],
        ["changedTaskCount", "变更任务数", summary.changedTaskCount],
        ["frozenTaskCount", "冻结任务数", summary.frozenTaskCount],
        ["makespanDiffMinutes", "Makespan 变化分钟", makespan.makespanDiffMinutes],
        ["changedTaskRatio", "变更比例", summary.changedTaskRatio],
        ["insertedTaskCount", "插单任务数", summary.insertedTaskCount],
        ["insertOrderFinishTime", "插单完成时间", insertOrder.insertOrderFinishTime],
        ["trueDelayCompare", "真实交期延期", this.pickCompareSummary(detail.trueDelayCompare)],
        ["stabilityCompare", "计划稳定性扰动", this.pickCompareSummary(stability)]
      ];
      return rows.map(item => ({
        key: item[0],
        label: item[1],
        value: this.valueOrDash(item[2])
      }));
    },
    evidenceTitle() {
      const titles = { orders: "受影响 Lot", tasks: "变更任务", decision: "决策证据" };
      return titles[this.evidenceView] || "方案差异";
    }
  },
  created() {
    this.loadProducts();
    this.refreshInitialActivePlans();
  },
  methods: {
    loadProducts() {
      this.productLoading = true;
      listProduct({ pageNum: 1, pageSize: 200, status: "0" }).then(response => {
        this.productList = response.rows || [];
      }).finally(() => {
        this.productLoading = false;
      });
    },
    handleProductChange() {
      this.routeOperationList = [];
      this.hotLotList = [];
      this.selectedHotLotId = null;
      this.sourcePlanId = null;
      this.sourcePlan = null;
      this.insertEvent = null;
      this.strategyDetail = {};
      this.rescheduleResult = {};
      this.newPlan = null;
      this.compareDetail = null;
      if (!this.selectedProductId) {
        this.workbenchStage = STAGE.EMPTY;
        return;
      }
      this.workbenchStage = STAGE.PRODUCT_SELECTED;
      this.loadRouteOperations();
      this.loadHotLots();
    },
    loadRouteOperations() {
      this.routeLoading = true;
      listRouteOperation({
        pageNum: 1,
        pageSize: 500,
        productId: this.selectedProductId,
        status: "0"
      }).then(response => {
        const rows = response.rows || [];
        this.routeOperationList = rows.sort((a, b) => (a.processSeq || 0) - (b.processSeq || 0));
      }).finally(() => {
        this.routeLoading = false;
      });
    },
    loadHotLots() {
      this.hotLotLoading = true;
      listOrder({
        pageNum: 1,
        pageSize: 200,
        productId: this.selectedProductId,
        orderType: "INSERT"
      }).then(response => {
        this.hotLotList = response.rows || [];
      }).finally(() => {
        this.hotLotLoading = false;
      });
    },
    handleHotLotChange() {
      this.insertEvent = null;
      this.strategyDetail = {};
      this.rescheduleResult = {};
      this.newPlan = null;
      this.compareDetail = null;
      if (this.selectedHotLotId) {
        this.workbenchStage = this.sourcePlanId ? STAGE.INITIAL_READY : STAGE.HOT_LOT_SELECTED;
        this.refreshInitialActivePlans();
      } else {
        this.workbenchStage = this.selectedProductId ? STAGE.PRODUCT_SELECTED : STAGE.EMPTY;
      }
    },
    handleHotLotRowChange(row) {
      if (row) {
        this.selectedHotLotId = row.orderId;
        this.handleHotLotChange();
      }
    },
    refreshInitialActivePlans() {
      return listSchedulePlan({
        pageNum: 1,
        pageSize: 100,
        planType: "INITIAL",
        planStatus: "ACTIVE"
      }).then(response => {
        const rows = response.rows || [];
        this.initialActivePlans = rows.sort(this.comparePlanCreateTimeDesc);
        if (!this.sourcePlanId && this.initialActivePlans.length > 0 && this.selectedHotLotId) {
          this.sourcePlanId = this.initialActivePlans[0].planId;
          this.sourcePlan = this.initialActivePlans[0];
          this.workbenchStage = STAGE.INITIAL_READY;
        }
      });
    },
    handleSourcePlanChange() {
      this.sourcePlan = this.initialActivePlans.find(item => item.planId === this.sourcePlanId) || null;
      if (this.sourcePlan && this.selectedHotLotId && !this.insertEvent) {
        this.workbenchStage = STAGE.INITIAL_READY;
      }
    },
    handleGenerateInitial() {
      this.initialLoading = true;
      generateInitialSchedule().then(response => {
        const taskCount = response.data && response.data.taskCount ? response.data.taskCount : 0;
        this.$modal.msgSuccess("初始调度生成成功，任务数：" + taskCount);
        return this.refreshInitialActivePlans();
      }).then(() => {
        if (this.initialActivePlans.length > 0) {
          this.sourcePlanId = this.initialActivePlans[0].planId;
          this.sourcePlan = this.initialActivePlans[0];
          this.workbenchStage = STAGE.INITIAL_READY;
        }
      }).finally(() => {
        this.initialLoading = false;
      });
    },
    handleCreateAndAnalyze() {
      this.eventLoading = true;
      createAndAnalyzeInsertEvent(this.selectedHotLotId).then(response => {
        this.insertEvent = response.data;
        this.workbenchStage = STAGE.EVENT_ANALYZED;
        this.$modal.msgSuccess("插单影响分析完成");
      }).finally(() => {
        this.eventLoading = false;
      });
    },
    handleRecommendStrategy() {
      this.strategyLoading = true;
      recommendStrategy(this.insertEvent.eventId).then(response => {
        this.strategyDetail = response.data || {};
        return this.refreshInsertEvent();
      }).then(() => {
        this.workbenchStage = STAGE.STRATEGY_RECOMMENDED;
        this.$modal.msgSuccess("策略推荐完成");
      }).finally(() => {
        this.strategyLoading = false;
      });
    },
    handleGenerateLocalReschedule() {
      this.rescheduleLoading = true;
      generateLocalReschedule(this.insertEvent.eventId, this.algorithmType, this.randomSeed).then(response => {
        const data = response.data || {};
        this.rescheduleResult = data;
        this.$modal.msgSuccess("局部重调度候选方案已生成");
        return this.afterRescheduleGenerated(data);
      }).finally(() => {
        this.rescheduleLoading = false;
      });
    },
    afterRescheduleGenerated(data) {
      const planId = data.newPlanId || data.planId || (data.newPlan ? data.newPlan.planId : null);
      return this.refreshInsertEvent().then(() => {
        const id = planId || (this.insertEvent ? this.insertEvent.newPlanId : null);
        if (!id) {
          this.workbenchStage = STAGE.RESCHEDULE_GENERATED;
          return null;
        }
        return getSchedulePlan(id).then(planResponse => {
          this.newPlan = planResponse.data;
          this.workbenchStage = STAGE.RESCHEDULE_GENERATED;
          return this.loadKpiCompare();
        });
      });
    },
    refreshInsertEvent() {
      if (!this.insertEvent || !this.insertEvent.eventId) {
        return Promise.resolve();
      }
      return getInsertEvent(this.insertEvent.eventId).then(response => {
        this.insertEvent = response.data;
      });
    },
    loadKpiCompare() {
      if (!this.newPlanId) {
        return Promise.resolve();
      }
      return compareSchedulePlan(this.newPlanId).then(response => {
        this.compareDetail = response.data || {};
      });
    },
    handleConfirmPlan() {
      this.$modal.confirm("确认采用该重调度候选方案？采用后原方案将转为历史方案。").then(() => {
        return confirmSchedulePlan(this.newPlanId);
      }).then(() => {
        this.$modal.msgSuccess("候选方案已采用");
        return this.refreshAfterDecision(STAGE.CONFIRMED);
      }).catch(() => {});
    },
    handleRejectPlan() {
      this.$modal.confirm("确认拒绝该重调度候选方案？").then(() => {
        return rejectSchedulePlan(this.newPlanId);
      }).then(() => {
        this.$modal.msgSuccess("候选方案已拒绝");
        return this.refreshAfterDecision(STAGE.REJECTED);
      }).catch(() => {});
    },
    refreshAfterDecision(stage) {
      const tasks = [this.refreshInsertEvent(), this.refreshInitialActivePlans()];
      if (this.sourcePlanId) {
        tasks.push(getSchedulePlan(this.sourcePlanId).then(response => {
          this.sourcePlan = response.data;
        }));
      }
      if (this.newPlanId) {
        tasks.push(getSchedulePlan(this.newPlanId).then(response => {
          this.newPlan = response.data;
        }));
      }
      return Promise.all(tasks).then(() => {
        this.workbenchStage = stage;
      });
    },
    openGantt(planId) {
      this.$router.push({ path: "/aps/scheduleTask/gantt", query: { planId: planId } });
    },
    openGanttCompare() {
      this.$router.push({ path: "/aps/scheduleTask/ganttCompare", query: { planId: this.newPlanId } });
    },
    openKpiDialog() {
      this.loadKpiCompare().then(() => {
        this.kpiOpen = true;
      });
    },
    openEvidence(view) {
      this.loadKpiCompare().then(() => {
        this.evidenceView = view;
        this.evidenceOpen = true;
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
    handleExplanationReport() {
      this.explainLoading = true;
      generateExplanationReport(this.insertEventId).then(response => {
        this.explanationDetail = response.data || {};
        this.explanationOpen = true;
      }).finally(() => {
        this.explainLoading = false;
      });
    },
    handleAiExplanationReport() {
      this.aiExplainLoading = true;
      generateAiExplanationReport(this.insertEventId).then(response => {
        this.aiExplanationDetail = response.data || {};
        this.aiExplanationOpen = true;
      }).catch(() => {
        this.$modal.msgError("AI 分析报告生成失败或超时，请稍后重试，或先查看普通解释报告。");
      }).finally(() => {
        this.aiExplainLoading = false;
      });
    },
    handleRestart() {
      if (this.$refs.ganttPreview) {
        this.$refs.ganttPreview.resetPreview();
      }
      this.selectedProductId = null;
      this.selectedHotLotId = null;
      this.sourcePlanId = null;
      this.algorithmType = "RULE";
      this.randomSeed = 42;
      this.routeOperationList = [];
      this.hotLotList = [];
      this.sourcePlan = null;
      this.insertEvent = null;
      this.strategyDetail = {};
      this.rescheduleResult = {};
      this.newPlan = null;
      this.compareDetail = null;
      this.explanationDetail = null;
      this.aiExplanationDetail = null;
      this.workbenchStage = STAGE.EMPTY;
      this.refreshInitialActivePlans();
    },
    isReentrantGroup(equipmentGroupId) {
      return this.reentrantGroupIds.indexOf(String(equipmentGroupId)) !== -1;
    },
    parseJson(value) {
      if (!value) {
        return null;
      }
      if (typeof value === "object") {
        return value;
      }
      try {
        return JSON.parse(value);
      } catch (e) {
        return { raw: value };
      }
    },
    prettyJson(value) {
      if (!value) {
        return "";
      }
      return JSON.stringify(value, null, 2);
    },
    formatReport(value) {
      if (!value) {
        return "";
      }
      if (typeof value === "string") {
        return value;
      }
      if (value.fullReport) {
        return value.fullReport;
      }
      return JSON.stringify(value, null, 2);
    },
    valueOrDash(value) {
      return value === null || value === undefined || value === "" ? "-" : value;
    },
    pickCompareSummary(value) {
      if (!value) {
        return null;
      }
      if (typeof value === "string" || typeof value === "number") {
        return value;
      }
      const keys = Object.keys(value);
      if (keys.length === 0) {
        return null;
      }
      return keys.map(key => key + ": " + value[key]).join("; ");
    },
    formatProductLabel(item) {
      return item.productCode + " / " + item.productName;
    },
    formatHotLotLabel(item) {
      return item.orderCode + " / P" + item.priorityLevel + " / " + item.orderStatus;
    },
    formatPlanLabel(item) {
      if (!item) {
        return "-";
      }
      return item.planId + " / " + item.planCode + " / " + item.planStatus;
    },
    formatEventLabel(item) {
      return item.eventId + " / " + item.eventCode + " / " + item.eventStatus;
    },
    comparePlanCreateTimeDesc(a, b) {
      const aTime = new Date(a.createTime || a.updateTime || a.scheduleStartTime || 0).getTime();
      const bTime = new Date(b.createTime || b.updateTime || b.scheduleStartTime || 0).getTime();
      return bTime - aTime;
    }
  }
};
</script>

<style scoped>
.reschedule-workbench {
  background: #f5f7fa;
}

.workbench-header,
.panel-card,
.entry-card {
  border-radius: 4px;
}

.title-row,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}

.page-subtitle {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.global-tip {
  margin-top: 12px;
}

.main-grid {
  margin-top: 12px;
}

.panel-card {
  min-height: 760px;
  margin-bottom: 12px;
}

.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #303133;
}

.section-gap {
  margin-top: 12px;
}

.route-alert {
  margin-bottom: 8px;
}

.action-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.action-list .el-button {
  margin-left: 0;
  width: 100%;
}

.entry-card {
  margin-top: 12px;
}

.entry-tip {
  color: #909399;
  font-size: 12px;
}

.evidence-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.evidence-actions .el-button {
  margin-left: 0;
}

.evidence-note {
  margin-top: 12px;
  color: #606266;
  font-size: 12px;
}
</style>
