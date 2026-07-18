<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="方案ID" prop="planId">
        <el-input v-model="queryParams.planId" placeholder="请输入方案ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:scheduleTask:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:scheduleTask:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:scheduleTask:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:scheduleTask:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="scheduleTaskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案ID" align="center" prop="planId" width="90" />
      <el-table-column label="订单编码" align="center" prop="orderCode" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="产品编码" align="center" prop="productCode" width="110" />
      <el-table-column label="工序序号" align="center" prop="processSeq" width="90" />
      <el-table-column label="工序编码" align="center" prop="processCode" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="工序名称" align="center" prop="processName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="设备编码" align="center" prop="equipmentCode" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="计划开始" align="center" prop="plannedStartTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.plannedStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计划结束" align="center" prop="plannedEndTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.plannedEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时长(分钟)" align="center" prop="duration" width="100" />
      <el-table-column label="插单" align="center" prop="isInserted" width="80" />
      <el-table-column label="冻结" align="center" prop="isFrozen" width="80" />
      <el-table-column label="变更" align="center" prop="isChanged" width="80" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:scheduleTask:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:scheduleTask:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="方案ID" prop="planId">
          <el-input v-model="form.planId" placeholder="请输入方案ID" />
        </el-form-item>
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入订单ID" />
        </el-form-item>
        <el-form-item label="订单编码" prop="orderCode">
          <el-input v-model="form.orderCode" placeholder="请输入订单编码" />
        </el-form-item>
        <el-form-item label="产品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入产品ID" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="工序序号" prop="processSeq">
          <el-input v-model="form.processSeq" placeholder="请输入工序序号" />
        </el-form-item>
        <el-form-item label="工序编码" prop="processCode">
          <el-input v-model="form.processCode" placeholder="请输入工序编码" />
        </el-form-item>
        <el-form-item label="工序名称" prop="processName">
          <el-input v-model="form.processName" placeholder="请输入工序名称" />
        </el-form-item>
        <el-form-item label="设备组ID" prop="equipmentGroupId">
          <el-input v-model="form.equipmentGroupId" placeholder="请输入设备组ID" />
        </el-form-item>
        <el-form-item label="设备ID" prop="equipmentId">
          <el-input v-model="form.equipmentId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="设备编码" prop="equipmentCode">
          <el-input v-model="form.equipmentCode" placeholder="请输入设备编码" />
        </el-form-item>
        <el-form-item label="计划开始" prop="plannedStartTime">
          <el-date-picker clearable v-model="form.plannedStartTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划开始时间" />
        </el-form-item>
        <el-form-item label="计划结束" prop="plannedEndTime">
          <el-date-picker clearable v-model="form.plannedEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择计划结束时间" />
        </el-form-item>
        <el-form-item label="时长" prop="duration">
          <el-input v-model="form.duration" placeholder="请输入加工时长，单位分钟" />
        </el-form-item>
        <el-form-item label="任务状态" prop="taskStatus">
          <el-input v-model="form.taskStatus" placeholder="PLANNED / FROZEN" />
        </el-form-item>
        <el-form-item label="冻结" prop="isFrozen">
          <el-input v-model="form.isFrozen" placeholder="Y / N" />
        </el-form-item>
        <el-form-item label="插单" prop="isInserted">
          <el-input v-model="form.isInserted" placeholder="Y / N" />
        </el-form-item>
        <el-form-item label="变更" prop="isChanged">
          <el-input v-model="form.isChanged" placeholder="Y / N" />
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
import { listScheduleTask, getScheduleTask, delScheduleTask, addScheduleTask, updateScheduleTask } from "@/api/aps/scheduleTask";

export default {
  name: "ScheduleTask",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      scheduleTaskList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planId: null,
        orderCode: null,
        equipmentId: null,
        taskStatus: null
      },
      form: {}
    };
  },
  created() {
    if (this.$route.query.planId) {
      this.queryParams.planId = this.$route.query.planId;
    }
    this.getList();
  },
  watch: {
    "$route.query.planId"(planId) {
      this.queryParams.planId = planId || null;
      this.handleQuery();
    }
  },
  methods: {
    getList() {
      this.loading = true;
      listScheduleTask(this.queryParams).then(response => {
        this.scheduleTaskList = response.rows;
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
        taskId: null,
        planId: this.queryParams.planId || null,
        orderId: null,
        orderCode: null,
        productId: null,
        productCode: null,
        processSeq: null,
        processCode: null,
        processName: null,
        equipmentGroupId: null,
        equipmentId: null,
        equipmentCode: null,
        plannedStartTime: null,
        plannedEndTime: null,
        duration: null,
        taskStatus: "PLANNED",
        isFrozen: "N",
        isInserted: "N",
        isChanged: "N",
        sourceTaskId: null,
        originalStartTime: null,
        originalEndTime: null,
        originalEquipmentId: null,
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
      this.queryParams.planId = null;
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.taskId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增调度任务";
    },
    handleUpdate(row) {
      this.reset();
      const taskId = row.taskId || this.ids;
      getScheduleTask(taskId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改调度任务";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.taskId != null) {
            updateScheduleTask(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addScheduleTask(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const taskIds = row.taskId || this.ids;
      this.$modal.confirm('删除后该任务将在业务列表中隐藏，但系统仍保留追溯记录。是否继续？任务编号：' + taskIds).then(function() {
        return delScheduleTask(taskIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download("aps/scheduleTask/export", {
        ...this.queryParams
      }, `scheduleTask_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>
