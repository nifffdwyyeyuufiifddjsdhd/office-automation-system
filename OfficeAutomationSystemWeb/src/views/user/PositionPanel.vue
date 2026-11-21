<template>
  <div class="pt-panel">
    <div class="pt-toolbar">
      <el-button v-permission="'position:manage'" type="primary" size="small" @click="openDialog(null)">新增职位</el-button>
    </div>
    <el-table :data="list" v-loading="loading" size="small" border>
      <el-table-column prop="position_name" label="职位名称" min-width="140" />
      <el-table-column prop="position_code" label="编码" width="120" />
      <el-table-column prop="level" label="级别" width="70" align="center">
        <template #default="{ row }">{{ row.level }}</template>
      </el-table-column>
      <el-table-column prop="sort_order" label="排序" width="70" align="center" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'position:manage'" link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-permission="'position:manage'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑职位' : '新增职位'" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="职位名称" required>
          <el-input v-model="form.position_name" placeholder="如：Java工程师、部门主管" />
        </el-form-item>
        <el-form-item label="职位编码">
          <el-input v-model="form.position_code" placeholder="如：java_dev" />
        </el-form-item>
        <el-form-item label="级别">
          <el-input-number v-model="form.level" :min="1" :max="10" />
          <span class="muted"> 1-10，用于审批流自动匹配审核人</span>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort_order" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listPositions,
  createPosition,
  updatePosition,
  deletePosition,
  type PositionItem,
} from "@/api/user";

const loading = ref(false);
const saving = ref(false);
const list = ref<PositionItem[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive<PositionItem>({
  position_name: "",
  position_code: "",
  level: 1,
  sort_order: 0,
  status: 1,
  remark: "",
});

async function load() {
  loading.value = true;
  try {
    list.value = await listPositions();
  } finally {
    loading.value = false;
  }
}

function openDialog(row: PositionItem | null) {
  isEdit.value = !!row;
  if (row) {
    Object.assign(form, row);
  } else {
    form.id = undefined;
    form.position_name = "";
    form.position_code = "";
    form.level = 1;
    form.sort_order = 0;
    form.status = 1;
    form.remark = "";
  }
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.position_name.trim()) {
    ElMessage.warning("请输入职位名称");
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value) {
      await updatePosition(form);
      ElMessage.success("已更新");
    } else {
      await createPosition(form);
      ElMessage.success("已创建");
    }
    dialogVisible.value = false;
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: PositionItem) {
  try {
    await ElMessageBox.confirm(`确定删除职位「${row.position_name}」吗？`, "删除职位", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await deletePosition(row.id!);
    ElMessage.success("已删除");
    load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || "删除失败");
  }
}

onMounted(load);
</script>

<style scoped>
.pt-panel {
  padding: 4px;
}
.pt-toolbar {
  margin-bottom: 12px;
}
.muted {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
</style>
