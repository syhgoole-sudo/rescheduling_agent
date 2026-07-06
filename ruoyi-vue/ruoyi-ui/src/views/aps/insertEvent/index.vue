<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="事件编码" prop="eventCode">
        <el-input v-model="queryParams.eventCode" placeholder="请输入事件编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="插单订单ID" prop="insertOrderId">
        <el-input v-model="queryParams.insertOrderId" placeholder="请输入插单订单ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="事件状态" prop="eventStatus">
        <el-input v-model="queryParams.eventStatus" placeholder="NEW / ANALYZED" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:insertEvent:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-search" size="mini" @click="handleCreateAnalyze" v-hasPermi="['aps:insertEvent:add']">创建并分析插单</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:insertEvent:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:insertEvent:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:insertEvent:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="insertEventList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="事件ID" align="center" prop="eventId" width="90" />
      <el-table-column label="事件编码" align="center" prop="eventCode" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="插单订单ID" align="center" prop="insertOrderId" width="110" />
      <el-table-column label="原方案ID" align="center" prop="sourcePlanId" width="100" />
      <el-table-column label="新方案ID" align="center" prop="newPlanId" width="100" />
      <el-table-column label="事件时间" align="center" prop="eventTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.eventTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="eventStatus" width="110" />
      <el-table-column label="策略类型" align="center" prop="strategyType" width="210" :show-overflow-tooltip="true" />
      <el-table-column label="影响摘要" align="center" min-width="240" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ impactSummary(scope.row.impactJson) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="360">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewImpact(scope.row)">查看分析</el-button>
          <el-button v-if="scope.row.eventStatus === 'ANALYZED'" size="mini" type="text" icon="el-icon-s-operation" @click="handleRecommendStrategy(scope.row)" v-hasPermi="['aps:insertEvent:add']">推荐策略</el-button>
          <el-button v-if="(scope.row.eventStatus === 'ANALYZED' || scope.row.strategyType) && scope.row.eventStatus !== 'RESCHEDULED'" size="mini" type="text" icon="el-icon-refresh-right" @click="handleGenerateLocalReschedule(scope.row)" v-hasPermi="['aps:insertEvent:add']">生成重调度</el-button>
          <el-button v-if="canShowExplanation(scope.row)" size="mini" type="text" icon="el-icon-document" @click="handleExplanationReport(scope.row)" v-hasPermi="['aps:insertEvent:list']">解释报告</el-button>
          <el-button v-if="canShowExplanation(scope.row)" size="mini" type="text" icon="el-icon-chat-dot-round" @click="handleAiExplanationReport(scope.row)" v-hasPermi="['aps:insertEvent:list']">AI 分析报告</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:insertEvent:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:insertEvent:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="创建并分析插单" :visible.sync="analyzeOpen" width="420px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="插单订单ID">
          <el-input v-model="analyzeInsertOrderId" placeholder="例如：9" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAnalyze">确 定</el-button>
        <el-button @click="analyzeOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="影响分析结果" :visible.sync="impactOpen" width="720px" append-to-body>
      <el-descriptions v-if="impactDetail" :column="2" border size="small">
        <el-descriptions-item label="影响等级">{{ impactDetail.impactLevel }}</el-descriptions-item>
        <el-descriptions-item label="影响窗口">{{ impactDetail.windowStart }} ~ {{ impactDetail.windowEnd }}</el-descriptions-item>
        <el-descriptions-item label="受影响任务数">{{ impactDetail.affectedTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="受影响订单数">{{ impactDetail.affectedOrderCount }}</el-descriptions-item>
        <el-descriptions-item label="受影响设备数">{{ impactDetail.affectedEquipmentCount }}</el-descriptions-item>
        <el-descriptions-item label="插单订单">{{ impactDetail.insertOrder ? impactDetail.insertOrder.orderCode : '' }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ impactDetail.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-input class="mt12" type="textarea" :rows="10" :value="prettyImpactJson" readonly />
    </el-dialog>

    <el-dialog title="策略推荐结果" :visible.sync="strategyOpen" width="640px" append-to-body>
      <el-descriptions v-if="strategyDetail" :column="2" border size="small">
        <el-descriptions-item label="策略类型">{{ strategyDetail.strategyType }}</el-descriptions-item>
        <el-descriptions-item label="策略名称">{{ strategyDetail.strategyName }}</el-descriptions-item>
        <el-descriptions-item label="冻结任务数">{{ strategyDetail.frozenTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="可调整任务数">{{ strategyDetail.adjustableTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="插单任务数">{{ strategyDetail.insertTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="推荐原因" :span="2">{{ strategyDetail.recommendedReason }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog title="生成局部重调度方案" :visible.sync="algorithmOpen" width="460px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="算法类型">
          <el-radio-group v-model="selectedAlgorithmType">
            <el-radio label="RULE">规则算法</el-radio>
            <el-radio label="GA">遗传算法</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitLocalReschedule">确 定</el-button>
        <el-button @click="algorithmOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="局部重调度结果" :visible.sync="rescheduleOpen" width="640px" append-to-body>
      <el-descriptions v-if="rescheduleDetail" :column="2" border size="small">
        <el-descriptions-item label="新方案ID">{{ rescheduleDetail.newPlanId }}</el-descriptions-item>
        <el-descriptions-item label="原方案ID">{{ rescheduleDetail.sourcePlanId }}</el-descriptions-item>
        <el-descriptions-item label="事件ID">{{ rescheduleDetail.eventId }}</el-descriptions-item>
        <el-descriptions-item label="任务数量">{{ rescheduleDetail.taskCount }}</el-descriptions-item>
        <el-descriptions-item label="KPI" :span="2">{{ formatKpi(rescheduleDetail.kpi) }}</el-descriptions-item>
        <el-descriptions-item label="提示" :span="2">可到“调度方案”页面查看新方案、任务明细和甘特图。</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog title="插单重调度解释报告" :visible.sync="explanationOpen" width="860px" append-to-body>
      <el-descriptions v-if="explanationDetail" :column="2" border size="small">
        <el-descriptions-item label="事件编码">{{ explanationDetail.eventCode }}</el-descriptions-item>
        <el-descriptions-item label="新方案ID">{{ explanationDetail.newPlanId }}</el-descriptions-item>
        <el-descriptions-item label="插单事件概述" :span="2">{{ explanationDetail.eventSummary }}</el-descriptions-item>
        <el-descriptions-item label="影响范围说明" :span="2">{{ explanationDetail.impactSummary }}</el-descriptions-item>
        <el-descriptions-item label="策略推荐说明" :span="2">{{ explanationDetail.strategySummary }}</el-descriptions-item>
        <el-descriptions-item label="重调度结果说明" :span="2">{{ explanationDetail.rescheduleSummary }}</el-descriptions-item>
        <el-descriptions-item label="KPI 对比说明" :span="2">{{ explanationDetail.kpiSummary }}</el-descriptions-item>
        <el-descriptions-item label="方案收益" :span="2">{{ explanationDetail.benefit }}</el-descriptions-item>
        <el-descriptions-item label="方案代价" :span="2">{{ explanationDetail.cost }}</el-descriptions-item>
        <el-descriptions-item label="调度建议" :span="2">{{ explanationDetail.recommendation }}</el-descriptions-item>
      </el-descriptions>
      <el-input class="mt12" type="textarea" :rows="12" :value="explanationDetail ? explanationDetail.fullReport : ''" readonly />
    </el-dialog>

    <el-dialog title="AI 分析报告" :visible.sync="aiExplanationOpen" width="900px" append-to-body>
      <el-descriptions v-if="aiExplanationDetail" :column="2" border size="small">
        <el-descriptions-item label="模型名称">{{ aiExplanationDetail.modelName }}</el-descriptions-item>
        <el-descriptions-item label="Fallback">{{ aiExplanationDetail.fallbackUsed ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="事件概述" :span="2">{{ aiExplanationDetail.eventSummary }}</el-descriptions-item>
        <el-descriptions-item label="影响分析" :span="2">{{ aiExplanationDetail.impactAnalysis }}</el-descriptions-item>
        <el-descriptions-item label="策略解释" :span="2">{{ aiExplanationDetail.strategyExplanation }}</el-descriptions-item>
        <el-descriptions-item label="KPI 解读" :span="2">{{ aiExplanationDetail.kpiInterpretation }}</el-descriptions-item>
        <el-descriptions-item label="风险提示" :span="2">{{ aiExplanationDetail.riskWarning }}</el-descriptions-item>
        <el-descriptions-item label="调度建议" :span="2">{{ aiExplanationDetail.recommendation }}</el-descriptions-item>
      </el-descriptions>
      <el-input class="mt12" type="textarea" :rows="12" :value="aiExplanationDetail ? aiExplanationDetail.fullReport : ''" readonly />
    </el-dialog>

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="120px">
        <el-form-item label="事件编码" prop="eventCode">
          <el-input v-model="form.eventCode" placeholder="请输入事件编码" />
        </el-form-item>
        <el-form-item label="插单订单ID" prop="insertOrderId">
          <el-input v-model="form.insertOrderId" placeholder="请输入插单订单ID" />
        </el-form-item>
        <el-form-item label="原方案ID" prop="sourcePlanId">
          <el-input v-model="form.sourcePlanId" placeholder="请输入原方案ID" />
        </el-form-item>
        <el-form-item label="新方案ID" prop="newPlanId">
          <el-input v-model="form.newPlanId" placeholder="请输入新方案ID" />
        </el-form-item>
        <el-form-item label="事件时间" prop="eventTime">
          <el-date-picker clearable v-model="form.eventTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择事件时间" />
        </el-form-item>
        <el-form-item label="事件状态" prop="eventStatus">
          <el-input v-model="form.eventStatus" placeholder="请输入事件状态" />
        </el-form-item>
        <el-form-item label="影响JSON" prop="impactJson">
          <el-input v-model="form.impactJson" type="textarea" :rows="5" placeholder="请输入影响分析JSON" />
        </el-form-item>
        <el-form-item label="策略类型" prop="strategyType">
          <el-input v-model="form.strategyType" placeholder="请输入策略类型" />
        </el-form-item>
        <el-form-item label="插单原因" prop="eventReason">
          <el-input v-model="form.eventReason" type="textarea" placeholder="请输入插单原因" />
        </el-form-item>
        <el-form-item label="删除标识" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="0 / 1" />
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
import { listInsertEvent, getInsertEvent, delInsertEvent, addInsertEvent, updateInsertEvent, createAndAnalyzeInsertEvent, recommendStrategy, generateLocalReschedule, generateExplanationReport, generateAiExplanationReport } from "@/api/aps/insertEvent";

export default {
  name: "InsertEvent",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      insertEventList: [],
      title: "",
      open: false,
      analyzeOpen: false,
      impactOpen: false,
      strategyOpen: false,
      algorithmOpen: false,
      rescheduleOpen: false,
      explanationOpen: false,
      aiExplanationOpen: false,
      analyzeInsertOrderId: null,
      pendingRescheduleEvent: null,
      selectedAlgorithmType: "RULE",
      aiExplanationLoading: null,
      impactDetail: null,
      strategyDetail: null,
      rescheduleDetail: null,
      explanationDetail: null,
      aiExplanationDetail: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        eventCode: null,
        insertOrderId: null,
        eventStatus: null
      },
      form: {}
    };
  },
  computed: {
    prettyImpactJson() {
      return this.impactDetail ? JSON.stringify(this.impactDetail, null, 2) : "";
    }
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listInsertEvent(this.queryParams).then(response => {
        this.insertEventList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        eventId: null,
        eventCode: null,
        insertOrderId: null,
        sourcePlanId: null,
        newPlanId: null,
        eventTime: null,
        eventStatus: null,
        impactJson: null,
        strategyType: null,
        eventReason: null,
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
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.eventId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增插单事件";
    },
    handleCreateAnalyze() {
      this.analyzeInsertOrderId = this.queryParams.insertOrderId || null;
      this.analyzeOpen = true;
    },
    submitAnalyze() {
      if (!this.analyzeInsertOrderId) {
        this.$modal.msgError("请输入插单订单ID");
        return;
      }
      createAndAnalyzeInsertEvent(this.analyzeInsertOrderId).then(response => {
        this.$modal.msgSuccess("插单影响分析完成");
        this.analyzeOpen = false;
        this.impactDetail = this.parseImpactJson(response.data.impactJson);
        this.impactOpen = true;
        this.getList();
      });
    },
    handleViewImpact(row) {
      this.impactDetail = this.parseImpactJson(row.impactJson);
      this.impactOpen = true;
    },
    handleRecommendStrategy(row) {
      recommendStrategy(row.eventId).then(response => {
        this.strategyDetail = response.data;
        this.strategyOpen = true;
        this.$modal.msgSuccess("策略推荐完成");
        this.getList();
      });
    },
    handleGenerateLocalReschedule(row) {
      this.pendingRescheduleEvent = row;
      this.selectedAlgorithmType = "RULE";
      this.algorithmOpen = true;
    },
    submitLocalReschedule() {
      const row = this.pendingRescheduleEvent;
      if (!row) {
        this.algorithmOpen = false;
        return;
      }
      const algorithmType = this.selectedAlgorithmType || "RULE";
      this.$modal.confirm('确认基于事件 "' + row.eventCode + '" 使用 ' + (algorithmType === "GA" ? "遗传算法" : "规则算法") + ' 生成局部重调度方案？').then(() => {
        return generateLocalReschedule(row.eventId, algorithmType);
      }).then(response => {
        this.algorithmOpen = false;
        this.rescheduleDetail = response.data;
        this.rescheduleOpen = true;
        this.$modal.msgSuccess("局部重调度方案已生成");
        this.getList();
      }).catch(() => {});
    },
    canShowExplanation(row) {
      return row.eventStatus === "RESCHEDULED" || row.eventStatus === "CONFIRMED" || row.eventStatus === "REJECTED";
    },
    handleExplanationReport(row) {
      generateExplanationReport(row.eventId).then(response => {
        this.explanationDetail = response.data;
        this.explanationOpen = true;
      });
    },
    handleAiExplanationReport(row) {
      this.aiExplanationLoading = this.$loading({
        lock: true,
        text: "AI 分析报告生成中...",
        spinner: "el-icon-loading",
        background: "rgba(0, 0, 0, 0.3)"
      });
      generateAiExplanationReport(row.eventId).then(response => {
        this.aiExplanationDetail = response.data;
        this.aiExplanationOpen = true;
      }).catch(() => {
        this.$modal.msgError("AI 服务暂不可用，已建议使用解释报告或稍后重试。");
      }).finally(() => {
        if (this.aiExplanationLoading) {
          this.aiExplanationLoading.close();
          this.aiExplanationLoading = null;
        }
      });
    },
    impactSummary(impactJson) {
      const impact = this.parseImpactJson(impactJson);
      if (!impact) {
        return "";
      }
      return `${impact.impactLevel || ""} / 任务${impact.affectedTaskCount || 0} / 订单${impact.affectedOrderCount || 0} / 设备${impact.affectedEquipmentCount || 0}`;
    },
    parseImpactJson(impactJson) {
      if (!impactJson) {
        return null;
      }
      try {
        return typeof impactJson === "string" ? JSON.parse(impactJson) : impactJson;
      } catch (e) {
        return { reason: impactJson };
      }
    },
    formatKpi(kpi) {
      if (!kpi) {
        return "";
      }
      return JSON.stringify(kpi);
    },
    handleUpdate(row) {
      this.reset();
      const eventId = row.eventId || this.ids;
      getInsertEvent(eventId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改插单事件";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.eventId != null) {
            updateInsertEvent(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addInsertEvent(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const eventIds = row.eventId || this.ids;
      this.$modal.confirm('是否确认删除插单事件编号为"' + eventIds + '"的数据项？').then(function() {
        return delInsertEvent(eventIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download("aps/insertEvent/export", {
        ...this.queryParams
      }, `insertEvent_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.mt12 {
  margin-top: 12px;
}
</style>
