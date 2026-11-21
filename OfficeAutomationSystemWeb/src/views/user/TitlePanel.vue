<template>
  <div class="pt-panel">
    <div class="pt-toolbar">
      <el-button v-permission="'title:manage'" type="primary" size="small" @click="openDialog(null)">新增职称</el-button>
    </div>
    <el-table :data="list" v-loading="loading" size="small" border>
      <el-table-column prop="title_name" label="职称名称" min-width="140" />
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column label="等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ levelText(row.title_level) }}</el-tag>
        </template>
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
          <el-button v-permission="'title:manage'" link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-permission="'title:manage'" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑职称' : '新增职称'" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="职称名称" required>
          <el-input v-model="form.title_name" placeholder="如：工程师、高级工程师、教授" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="如：eng、senior_eng" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.title_level" style="width: 200px">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
            <el-option label="正高级" :value="4" />
          </el-select>
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
  listTitles,
  createTitle,
  updateTitle,
  deleteTitle,
  type TitleItem,
} from "@/api/user";

const loading = ref(false);
const saving = ref(false);
const list = ref<TitleItem[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive<TitleItem>({
  title_name: "",
  code: "",
  title_level: 1,
  sort_order: 0,
  status: 1,
  remark: "",
});

function levelText(level: number) {
  return ["", "初级", "中级", "高级", "正高级"][level] || "未知";
}

async function load() {
  loading.value = true;
  try {
    list.value = await listTitles();
  } finally {
    loading.value = false;
  }
}

function openDialog(row: TitleItem | null) {
  isEdit.value = !!row;
  if (row) {
    Object.assign(form, row);
  } else {
    form.id = undefined;
    form.title_name = "";
    form.code = "";
    form.title_level = 1;
    form.sort_order = 0;
    form.status = 1;
    form.remark = "";
  }
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.title_name.trim()) {
    ElMessage.warning("请输入职称名称");
    return;
  }
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateTitle(form);
      ElMessage.success("已更新");
    } else {
      await createTitle(form);
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

async function handleDelete(row: TitleItem) {
  try {
    await ElMessageBox.confirm(`确定删除职称「${row.title_name}」吗？`, "删除职称", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await deleteTitle(row.id!);
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
</style>
