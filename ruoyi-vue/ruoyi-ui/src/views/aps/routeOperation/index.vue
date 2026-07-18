<template>
  <div class="app-container route-page">
    <el-card shadow="never" class="product-context-card">
      <div class="product-select-row">
        <div>
          <div class="context-title">产品工艺路线</div>
          <div class="context-subtitle">选择产品后查看其完整工艺路线，工序按顺序号排列。</div>
        </div>
        <el-select
          v-model="selectedProductId"
          filterable
          placeholder="请选择产品"
          class="product-selector"
          :loading="productLoading"
          @change="handleProductChange"
        >
          <el-option v-for="product in productList" :key="product.productId" :label="formatProductLabel(product)" :value="product.productId" />
        </el-select>
      </div>

      <el-descriptions v-if="selectedProduct" :column="5" border size="small" class="route-summary">
        <el-descriptions-item label="产品编码">{{ selectedProduct.productCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ selectedProduct.productName }}</el-descriptions-item>
        <el-descriptions-item label="工序总数">{{ routeOperationList.length }}</el-descriptions-item>
        <el-descriptions-item label="路线总标准时长">{{ totalStandardDuration }} 分钟</el-descriptions-item>
        <el-descriptions-item label="可重入设备组">{{ reentrantGroupIds.length ? reentrantGroupIds.join('、') : '无' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="selectedProduct" class="route-chain">
        <span class="route-chain-label">完整路线</span>
        <span>{{ routeSummary || '当前产品暂无工序' }}</span>
      </div>
      <el-empty v-else description="请先选择产品" :image-size="72" />
    </el-card>

    <el-form v-show="showSearch && selectedProductId" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="86px" class="search-form">
      <el-form-item label="工序编码" prop="processCode"><el-input v-model="queryParams.processCode" placeholder="请输入工序编码" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="工序名称" prop="processName"><el-input v-model="queryParams.processName" placeholder="请输入工序名称" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="设备组ID" prop="equipmentGroupId"><el-input v-model="queryParams.equipmentGroupId" placeholder="请输入设备组ID" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable><el-option label="正常" value="0" /><el-option label="停用" value="1" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 route-toolbar">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="!selectedProductId" @click="handleAdd" v-hasPermi="['aps:routeOperation:add']">新增工序</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aps:routeOperation:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aps:routeOperation:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" :disabled="!selectedProductId" @click="handleExport" v-hasPermi="['aps:routeOperation:export']">导出当前路线</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert v-if="reentrantGroupIds.length" type="warning" :closable="false" show-icon :title="`存在可重入设备组：${reentrantGroupIds.join('、')}`" class="reentrant-alert" />

    <el-table v-loading="loading" :data="filteredRouteOperationList" :empty-text="selectedProductId ? '当前产品暂无符合条件的工序' : '请先选择产品'" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工序序号" align="center" prop="processSeq" width="90" />
      <el-table-column label="工序编码" align="center" prop="processCode" min-width="130" show-overflow-tooltip />
      <el-table-column label="工序名称" align="center" prop="processName" min-width="145" show-overflow-tooltip />
      <el-table-column label="设备组ID" align="center" prop="equipmentGroupId" width="120">
        <template slot-scope="scope"><el-tag v-if="isReentrantGroup(scope.row.equipmentGroupId)" size="mini" type="warning">{{ scope.row.equipmentGroupId }} / 可重入</el-tag><span v-else>{{ scope.row.equipmentGroupId }}</span></template>
      </el-table-column>
      <el-table-column label="标准时长(分钟)" align="center" prop="standardDuration" width="130" />
      <el-table-column label="状态" align="center" prop="status" width="85"><template slot-scope="scope"><el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope"><el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aps:routeOperation:edit']">修改</el-button><el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aps:routeOperation:remove']">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="当前产品" prop="productId"><el-input :value="selectedProduct ? formatProductLabel(selectedProduct) : ''" disabled /></el-form-item>
        <el-form-item label="工序序号" prop="processSeq"><el-input v-model="form.processSeq" placeholder="请输入工序顺序号" /></el-form-item>
        <el-form-item label="工序编码" prop="processCode"><el-input v-model="form.processCode" placeholder="请输入工序编码" /></el-form-item>
        <el-form-item label="工序名称" prop="processName"><el-input v-model="form.processName" placeholder="请输入工序名称" /></el-form-item>
        <el-form-item label="设备组ID" prop="equipmentGroupId"><el-input v-model="form.equipmentGroupId" placeholder="请输入设备组ID" /></el-form-item>
        <el-form-item label="标准时长(分钟)" prop="standardDuration"><el-input v-model="form.standardDuration" placeholder="请输入标准加工时长" /></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="form.status" style="width:100%"><el-option label="正常" value="0" /><el-option label="停用" value="1" /></el-select></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入内容" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listProduct } from "@/api/aps/product";
import { listRouteOperation, getRouteOperation, delRouteOperation, addRouteOperation, updateRouteOperation } from "@/api/aps/routeOperation";

export default {
  name: "RouteOperation",
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
      routeOperationList: [],
      title: "",
      open: false,
      queryParams: { pageNum: 1, pageSize: 500, productId: null, processCode: null, processName: null, equipmentGroupId: null, status: null },
      form: {},
      rules: {}
    };
  },
  computed: {
    selectedProduct() {
      return this.productList.find(item => String(item.productId) === String(this.selectedProductId)) || null;
    },
    equipmentGroupCounts() {
      return this.routeOperationList.reduce((counts, item) => {
        if (item.equipmentGroupId !== null && item.equipmentGroupId !== undefined) {
          const key = String(item.equipmentGroupId);
          counts[key] = (counts[key] || 0) + 1;
        }
        return counts;
      }, {});
    },
    reentrantGroupIds() {
      return Object.keys(this.equipmentGroupCounts).filter(key => this.equipmentGroupCounts[key] > 1);
    },
    totalStandardDuration() {
      return this.routeOperationList.reduce((total, item) => total + Number(item.standardDuration || 0), 0);
    },
    routeSummary() {
      return this.routeOperationList.map(item => item.processCode || item.processName).filter(Boolean).join(" → ");
    },
    filteredRouteOperationList() {
      const processCode = String(this.queryParams.processCode || "").toLowerCase();
      const processName = String(this.queryParams.processName || "").toLowerCase();
      const equipmentGroupId = String(this.queryParams.equipmentGroupId || "");
      const status = this.queryParams.status;
      return this.routeOperationList.filter(item => {
        return (!processCode || String(item.processCode || "").toLowerCase().includes(processCode))
          && (!processName || String(item.processName || "").toLowerCase().includes(processName))
          && (!equipmentGroupId || String(item.equipmentGroupId || "").includes(equipmentGroupId))
          && (!status || item.status === status);
      });
    }
  },
  created() {
    this.loadProducts();
  },
  methods: {
    loadProducts() {
      this.productLoading = true;
      listProduct({ pageNum: 1, pageSize: 200, status: "0" }).then(response => {
        this.productList = (response.rows || []).sort((a, b) => String(a.productCode || "").localeCompare(String(b.productCode || "")));
        const routeProductId = this.$route.query.productId;
        const routeProduct = this.productList.find(item => String(item.productId) === String(routeProductId));
        this.selectedProductId = (routeProduct || this.productList[0] || {}).productId || null;
        this.handleProductChange();
      }).finally(() => {
        this.productLoading = false;
      });
    },
    handleProductChange() {
      this.queryParams.productId = this.selectedProductId || null;
      this.queryParams.pageNum = 1;
      this.routeOperationList = [];
      this.total = 0;
      this.clearSelectionState();
      if (this.selectedProductId) this.getList();
    },
    getList() {
      if (!this.selectedProductId) {
        this.routeOperationList = [];
        this.total = 0;
        return;
      }
      this.loading = true;
      this.queryParams.productId = this.selectedProductId;
      listRouteOperation({ pageNum: 1, pageSize: 500, productId: this.selectedProductId }).then(response => {
        this.routeOperationList = (response.rows || []).sort((a, b) => Number(a.processSeq || 0) - Number(b.processSeq || 0));
        this.total = this.routeOperationList.length;
      }).finally(() => {
        this.loading = false;
      });
    },
    formatProductLabel(product) { return `${product.productCode} / ${product.productName}`; },
    isReentrantGroup(groupId) { return this.reentrantGroupIds.includes(String(groupId)); },
    cancel() { this.open = false; this.reset(); },
    reset() {
      this.form = {
        routeOperationId: null,
        productId: this.selectedProductId,
        productCode: this.selectedProduct ? this.selectedProduct.productCode : "",
        processSeq: null,
        processCode: "",
        processName: "",
        equipmentGroupId: null,
        standardDuration: null,
        status: "0",
        delFlag: "0",
        remark: null
      };
      this.resetForm("form");
    },
    handleQuery() { this.queryParams.pageNum = 1; this.total = this.filteredRouteOperationList.length; },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.productId = this.selectedProductId;
      this.queryParams.pageSize = 500;
      this.handleQuery();
    },
    clearSelectionState() { this.ids = []; this.single = true; this.multiple = true; },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.routeOperationId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      if (!this.selectedProductId) { this.$modal.msgWarning("请先选择产品"); return; }
      this.reset();
      this.open = true;
      this.title = "添加工艺路线工序";
    },
    handleUpdate(row) {
      this.reset();
      const routeOperationId = row.routeOperationId || this.ids;
      getRouteOperation(routeOperationId).then(response => { this.form = response.data; this.open = true; this.title = "修改工艺路线工序"; });
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return;
        this.form.productId = this.selectedProductId;
        this.form.productCode = this.selectedProduct ? this.selectedProduct.productCode : this.form.productCode;
        const request = this.form.routeOperationId != null ? updateRouteOperation(this.form) : addRouteOperation(this.form);
        request.then(() => { this.$modal.msgSuccess(this.form.routeOperationId != null ? "修改成功" : "新增成功"); this.open = false; this.getList(); });
      });
    },
    handleDelete(row) {
      const routeOperationIds = row.routeOperationId || this.ids;
      this.$modal.confirm(`是否确认删除工艺路线工序编号为"${routeOperationIds}"的数据项？`).then(() => delRouteOperation(routeOperationIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      if (!this.selectedProductId) return;
      this.download("aps/routeOperation/export", { ...this.queryParams, productId: this.selectedProductId }, `routeOperation_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.product-context-card { margin-bottom: 12px; border-radius: 4px; }
.product-select-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.context-title { color: #303133; font-size: 16px; font-weight: 600; }
.context-subtitle { margin-top: 5px; color: #909399; font-size: 12px; }
.product-selector { width: 360px; }
.route-summary, .route-chain, .search-form, .route-toolbar { margin-top: 12px; }
.route-chain { padding: 10px 12px; background: #f5f7fa; color: #606266; line-height: 22px; }
.route-chain-label { margin-right: 12px; color: #303133; font-weight: 600; }
.reentrant-alert { margin-bottom: 12px; }
@media (max-width: 768px) {
  .product-select-row { align-items: stretch; flex-direction: column; }
  .product-selector { width: 100%; }
}
</style>
