<template>
  <div class="app-container">
    <el-card shadow="never" class="product-context-card">
      <div class="product-context-row">
        <div>
          <div class="context-title">订单产品上下文</div>
          <div class="context-tip">先选择产品，再查看该产品下的普通 Lot、Hot Lot 和订单阶段。</div>
        </div>
        <el-select v-model="selectedProductId" filterable placeholder="请选择产品" class="product-selector" :loading="productLoading" @change="handleProductChange">
          <el-option v-for="product in productList" :key="product.productId" :label="formatProductLabel(product)" :value="product.productId" />
        </el-select>
      </div>
    </el-card>

    <el-tabs v-model="activeOrderTab" class="business-tabs" @tab-click="handleOrderTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="普通 Lot" name="normal" />
      <el-tab-pane label="Hot Lot" name="insert" />
      <el-tab-pane label="待调度" name="new" />
      <el-tab-pane label="已排程" name="scheduled" />
      <el-tab-pane label="已完成" name="closed" />
    </el-tabs>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="订单编码" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入订单编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="Lot 类型" prop="orderType">
        <el-select v-model="queryParams.orderType" placeholder="全部类型" clearable>
          <el-option label="普通 Lot" value="NORMAL" />
          <el-option label="Hot Lot" value="INSERT" />
        </el-select>
      </el-form-item>
      <el-form-item label="Lot 状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="全部状态" clearable>
          <el-option label="待调度" value="NEW" />
          <el-option label="已排程" value="SCHEDULED" />
          <el-option label="已完成" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="!selectedProductId" @click="handleAdd" v-hasPermi="['aps:order:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:order:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:order:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aps:order:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" :empty-text="selectedProductId ? '当前产品下暂无符合条件的 Lot' : '请先选择产品'" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="Lot ID" align="center" prop="orderId" width="85" />
      <el-table-column label="Lot 编码" align="center" prop="orderCode" min-width="145" show-overflow-tooltip />
      <el-table-column label="Lot 类型" align="center" prop="orderType" width="100">
        <template slot-scope="scope"><el-tag size="mini" :type="scope.row.orderType === 'INSERT' ? 'danger' : 'primary'">{{ orderTypeLabel(scope.row.orderType) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="产品编码" align="center" prop="productCode" min-width="110" show-overflow-tooltip />
      <el-table-column label="数量" align="center" prop="quantity" width="85" />
      <el-table-column label="优先级" align="center" prop="priorityLevel" width="85" />
      <el-table-column label="Lot 释放时间" align="center" prop="releaseTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.releaseTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交期" align="center" prop="dueTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.dueTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Lot 状态" align="center" prop="orderStatus" width="105">
        <template slot-scope="scope"><el-tag size="mini" :type="orderStatusTag(scope.row.orderStatus)">{{ orderStatusLabel(scope.row.orderStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="记录创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
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
        <el-form-item label="Lot 类型" prop="orderType">
          <el-input v-model="form.orderType" placeholder="请输入订单类型：NORMAL普通订单，INSERT插单订单" />
        </el-form-item>
        <el-form-item label="当前产品" prop="productId">
          <el-input :value="selectedProduct ? formatProductLabel(selectedProduct) : ''" disabled />
        </el-form-item>
        <el-form-item label="订单数量" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入订单数量" />
        </el-form-item>
        <el-form-item label="优先级，数值越小优先级越高" prop="priorityLevel">
          <el-input v-model="form.priorityLevel" placeholder="请输入优先级，数值越小优先级越高" />
        </el-form-item>
        <el-form-item label="Lot 释放时间" prop="releaseTime">
          <el-date-picker clearable v-model="form.releaseTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择 Lot 释放时间" />
        </el-form-item>
        <el-form-item label="交期" prop="dueTime">
          <el-date-picker clearable v-model="form.dueTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择交期" />
        </el-form-item>
        <el-form-item label="Lot 状态" prop="orderStatus">
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
import { listProduct } from "@/api/aps/product";

export default {
  name: "Order",
  data() {
    return {
      loading: false,
      productLoading: false,
      productList: [],
      selectedProductId: null,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      orderList: [],
      activeOrderTab: "normal",
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
  computed: {
    selectedProduct() {
      return this.productList.find(item => String(item.productId) === String(this.selectedProductId)) || null;
    }
  },
  created() {
    this.loadProducts();
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
      this.orderList = [];
      this.total = 0;
      if (this.selectedProductId) this.getList();
    },
    getList() {
      if (!this.selectedProductId) {
        this.orderList = [];
        this.total = 0;
        return;
      }
      this.loading = true;
      listOrder(this.buildOrderQuery()).then(response => {
        this.orderList = response.rows || [];
        this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    buildOrderQuery() {
      const query = { ...this.queryParams };
      const filters = {
        all: {},
        normal: { orderType: "NORMAL" },
        insert: { orderType: "INSERT" },
        new: { orderStatus: "NEW" },
        scheduled: { orderStatus: "SCHEDULED" },
        closed: { orderStatus: "CLOSED" }
      };
      return { ...query, ...(filters[this.activeOrderTab] || {}), productId: this.selectedProductId };
    },
    handleOrderTabChange() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    orderStatusTag(status) {
      const tags = { NEW: "warning", SCHEDULED: "primary", CLOSED: "success" };
      return tags[status] || "info";
    },
    orderTypeLabel(type) {
      return type === "INSERT" ? "Hot Lot" : type === "NORMAL" ? "普通 Lot" : type || "-";
    },
    orderStatusLabel(status) {
      const labels = { NEW: "待调度", SCHEDULED: "已排程", CLOSED: "已完成" };
      return labels[status] || status || "-";
    },
    formatProductLabel(product) {
      return `${product.productCode} / ${product.productName}`;
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
        productId: this.selectedProductId,
        productCode: this.selectedProduct ? this.selectedProduct.productCode : '',
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
      this.queryParams.productId = this.selectedProductId;
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.orderId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      if (!this.selectedProductId) return;
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
          this.form.productId = this.selectedProductId;
          this.form.productCode = this.selectedProduct ? this.selectedProduct.productCode : this.form.productCode;
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
        ...this.buildOrderQuery()
      }, `order_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style scoped>
.business-tabs {
  margin-bottom: 12px;
}

.product-context-card { margin-bottom: 12px; border-radius: 4px; }
.product-context-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.context-title { color: #303133; font-size: 16px; font-weight: 600; }
.context-tip { margin-top: 5px; color: #909399; font-size: 12px; }
.product-selector { width: 360px; }
</style>
