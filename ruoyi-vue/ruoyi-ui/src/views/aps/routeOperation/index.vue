<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="产品ID" prop="productId">
        <el-input v-model="queryParams.productId" placeholder="请输入产品ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="queryParams.productCode" placeholder="请输入产品编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="工序编码" prop="processCode">
        <el-input v-model="queryParams.processCode" placeholder="请输入工序编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="queryParams.processName" placeholder="请输入工序名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="设备组ID" prop="equipmentGroupId">
        <el-input v-model="queryParams.equipmentGroupId" placeholder="请输入设备组ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态：0正常，1停用" prop="status">
        <el-input v-model="queryParams.status" placeholder="请输入状态：0正常，1停用" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:routeOperation:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:routeOperation:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:routeOperation:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:routeOperation:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="routeOperationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="产品工艺路线工序ID" align="center" prop="routeOperationId" :show-overflow-tooltip="true" />
      <el-table-column label="产品ID" align="center" prop="productId" :show-overflow-tooltip="true" />
      <el-table-column label="产品编码" align="center" prop="productCode" :show-overflow-tooltip="true" />
      <el-table-column label="工序顺序号" align="center" prop="processSeq" :show-overflow-tooltip="true" />
      <el-table-column label="工序编码" align="center" prop="processCode" :show-overflow-tooltip="true" />
      <el-table-column label="工序名称" align="center" prop="processName" :show-overflow-tooltip="true" />
      <el-table-column label="设备组ID" align="center" prop="equipmentGroupId" :show-overflow-tooltip="true" />
      <el-table-column label="标准加工时长，单位分钟" align="center" prop="standardDuration" :show-overflow-tooltip="true" />
      <el-table-column label="状态：0正常，1停用" align="center" prop="status" :show-overflow-tooltip="true" />
      <el-table-column label="删除标志：0存在，2删除" align="center" prop="delFlag" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:routeOperation:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:routeOperation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="产品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入产品ID" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="工序顺序号" prop="processSeq">
          <el-input v-model="form.processSeq" placeholder="请输入工序顺序号" />
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
        <el-form-item label="标准加工时长，单位分钟" prop="standardDuration">
          <el-input v-model="form.standardDuration" placeholder="请输入标准加工时长，单位分钟" />
        </el-form-item>
        <el-form-item label="状态：0正常，1停用" prop="status">
          <el-input v-model="form.status" placeholder="请输入状态：0正常，1停用" />
        </el-form-item>
        <el-form-item label="删除标志：0存在，2删除" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志：0存在，2删除" />
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
import { listRouteOperation, getRouteOperation, delRouteOperation, addRouteOperation, updateRouteOperation } from "@/api/aps/routeOperation";

export default {
  name: "RouteOperation",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      routeOperationList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        productId: null,
        productCode: null,
        processCode: null,
        processName: null,
        equipmentGroupId: null,
        status: null
      },
      form: {},
      rules: {}
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listRouteOperation(this.queryParams).then(response => {
        this.routeOperationList = response.rows;
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

        routeOperationId: null,
        productId: null,
        productCode: '',
        processSeq: null,
        processCode: '',
        processName: '',
        equipmentGroupId: null,
        standardDuration: null,
        status: '',
        delFlag: ''
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
      this.ids = selection.map(item => item.routeOperationId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加工艺路线工序";
    },
    handleUpdate(row) {
      this.reset();
      const routeOperationId = row.routeOperationId || this.ids;
      getRouteOperation(routeOperationId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改工艺路线工序";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.routeOperationId != null) {
            updateRouteOperation(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRouteOperation(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const routeOperationIds = row.routeOperationId || this.ids;
      this.$modal.confirm('是否确认删除工艺路线工序编号为"' + routeOperationIds + '"的数据项？').then(function() {
        return delRouteOperation(routeOperationIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('aps/routeOperation/export', {
        ...this.queryParams
      }, `routeOperation_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
