<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="订单编码" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入订单编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单类型：NORMAL普通订单，INSERT插单订单" prop="orderType">
        <el-input v-model="queryParams.orderType" placeholder="请输入订单类型：NORMAL普通订单，INSERT插单订单" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="产品ID" prop="productId">
        <el-input v-model="queryParams.productId" placeholder="请输入产品ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="queryParams.productCode" placeholder="请输入产品编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭" prop="orderStatus">
        <el-input v-model="queryParams.orderStatus" placeholder="请输入订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aps:order:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:order:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:order:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:order:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单ID" align="center" prop="orderId" :show-overflow-tooltip="true" />
      <el-table-column label="订单编码" align="center" prop="orderCode" :show-overflow-tooltip="true" />
      <el-table-column label="订单类型：NORMAL普通订单，INSERT插单订单" align="center" prop="orderType" :show-overflow-tooltip="true" />
      <el-table-column label="产品ID" align="center" prop="productId" :show-overflow-tooltip="true" />
      <el-table-column label="产品编码" align="center" prop="productCode" :show-overflow-tooltip="true" />
      <el-table-column label="订单数量" align="center" prop="quantity" :show-overflow-tooltip="true" />
      <el-table-column label="优先级，数值越小优先级越高" align="center" prop="priorityLevel" :show-overflow-tooltip="true" />
      <el-table-column label="可开工时间" align="center" prop="releaseTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.releaseTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交期" align="center" prop="dueTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.dueTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭" align="center" prop="orderStatus" :show-overflow-tooltip="true" />
      <el-table-column label="删除标志：0存在，2删除" align="center" prop="delFlag" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:order:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:order:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="订单编码" prop="orderCode">
          <el-input v-model="form.orderCode" placeholder="请输入订单编码" />
        </el-form-item>
        <el-form-item label="订单类型：NORMAL普通订单，INSERT插单订单" prop="orderType">
          <el-input v-model="form.orderType" placeholder="请输入订单类型：NORMAL普通订单，INSERT插单订单" />
        </el-form-item>
        <el-form-item label="产品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入产品ID" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入产品编码" />
        </el-form-item>
        <el-form-item label="订单数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入订单数量" />
        </el-form-item>
        <el-form-item label="优先级，数值越小优先级越高" prop="priorityLevel">
          <el-input v-model="form.priorityLevel" placeholder="请输入优先级，数值越小优先级越高" />
        </el-form-item>
        <el-form-item label="可开工时间" prop="releaseTime">
          <el-date-picker clearable v-model="form.releaseTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择可开工时间" />
        </el-form-item>
        <el-form-item label="交期" prop="dueTime">
          <el-date-picker clearable v-model="form.dueTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择交期" />
        </el-form-item>
        <el-form-item label="订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭" prop="orderStatus">
          <el-input v-model="form.orderStatus" placeholder="请输入订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭" />
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
import { listOrder, getOrder, delOrder, addOrder, updateOrder } from "@/api/aps/order";

export default {
  name: "Order",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      orderList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderCode: null,
        orderType: null,
        productId: null,
        productCode: null,
        orderStatus: null
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
      listOrder(this.queryParams).then(response => {
        this.orderList = response.rows;
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

        orderId: null,
        orderCode: '',
        orderType: '',
        productId: null,
        productCode: '',
        quantity: null,
        priorityLevel: null,
        releaseTime: null,
        dueTime: null,
        orderStatus: '',
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
      this.ids = selection.map(item => item.orderId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加订单";
    },
    handleUpdate(row) {
      this.reset();
      const orderId = row.orderId || this.ids;
      getOrder(orderId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改订单";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.orderId != null) {
            updateOrder(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addOrder(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const orderIds = row.orderId || this.ids;
      this.$modal.confirm('是否确认删除订单编号为"' + orderIds + '"的数据项？').then(function() {
        return delOrder(orderIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('aps/order/export', {
        ...this.queryParams
      }, `order_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
