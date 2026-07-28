import React, { useEffect, useState } from 'react';
import { PageContainer, ProCard, ProForm, ProFormDigit, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Descriptions, Tag, message } from 'antd';
import { listCategories, validateCategoryDraft } from '@/services/categories';
import type { CategoryDraftResponse, CategorySummary } from '@/services/types';
import { formatDateTime } from '@/utils/format';

const CategoryList: React.FC = () => {
  const [categories, setCategories] = useState<CategorySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [validateResult, setValidateResult] = useState<CategoryDraftResponse>();

  const loadCategories = async () => {
    setLoading(true);
    try {
      setCategories(await listCategories());
    } catch (error: any) {
      message.error(error?.message || '分类加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCategories();
  }, []);

  const columns: ProColumns<CategorySummary>[] = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '分类名称', dataIndex: 'name' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (_, record) => (record.status === 1 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
    },
    { title: '排序', dataIndex: 'sortOrder' },
    { title: '创建时间', dataIndex: 'createdAt', render: (_, record) => formatDateTime(record.createdAt) },
    { title: '更新时间', dataIndex: 'updatedAt', render: (_, record) => formatDateTime(record.updatedAt) },
  ];

  return (
    <PageContainer title="分类管理" subTitle="本期接入分类列表展示和分类草稿校验">
      <ProCard split="horizontal" bordered>
        <ProCard title="分类基础展示">
          <ProTable<CategorySummary>
            rowKey="id"
            search={false}
            loading={loading}
            columns={columns}
            dataSource={categories}
            pagination={false}
            options={{ reload: loadCategories }}
          />
        </ProCard>
        <ProCard title="分类草稿校验">
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message="这里调用后端 /api/categories/validate，只校验并返回规范化结果，不写入数据库。"
          />
          <ProForm
            layout="horizontal"
            submitter={{ searchConfig: { submitText: '提交校验', resetText: '重置' } }}
            onFinish={async (values: any) => {
              const result = await validateCategoryDraft({
                name: values.name,
                description: values.description,
                sortOrder: values.sortOrder,
              });
              setValidateResult(result);
              message.success('分类草稿校验通过');
              return true;
            }}
          >
            <ProFormText
              name="name"
              label="分类名称"
              rules={[{ required: true, message: '请输入分类名称' }, { max: 30, message: '分类名称不能超过 30 个字符' }]}
            />
            <ProFormTextArea
              name="description"
              label="分类描述"
              fieldProps={{ maxLength: 200, showCount: true }}
              rules={[{ max: 200, message: '分类描述不能超过 200 个字符' }]}
            />
            <ProFormDigit
              name="sortOrder"
              label="排序值"
              fieldProps={{ min: 0, max: 9999, precision: 0 }}
              rules={[{ required: true, message: '请输入排序值' }]}
            />
          </ProForm>
          {validateResult && (
            <Descriptions
              bordered
              column={1}
              title="后端规范化结果"
              items={[
                { key: 'name', label: '分类名称', children: validateResult.name },
                { key: 'description', label: '分类描述', children: validateResult.description || '-' },
                { key: 'sortOrder', label: '排序值', children: validateResult.sortOrder },
              ]}
            />
          )}
        </ProCard>
      </ProCard>
    </PageContainer>
  );
};

export default CategoryList;
