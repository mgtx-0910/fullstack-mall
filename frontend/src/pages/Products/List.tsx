import React, { useEffect, useMemo, useRef, useState } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { history, Link } from '@umijs/max';
import { Button, Form, Input, Modal, Space, message } from 'antd';
import { listCategories } from '@/services/categories';
import { getProduct, queryProducts, updateProductStatus, updateProductSubtitle } from '@/services/products';
import type { CategorySummary, ProductStatus, ProductSummary } from '@/services/types';
import { toTableResult } from '@/services/common';
import { formatDateTime, productStatusText, productStatusValueEnum } from '@/utils/format';

const ProductList: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [subtitleForm] = Form.useForm<{ subtitle: string }>();
  const [categories, setCategories] = useState<CategorySummary[]>([]);
  const [subtitleProduct, setSubtitleProduct] = useState<ProductSummary>();
  const [subtitleLoading, setSubtitleLoading] = useState(false);

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch((error) => message.error(error?.message || '分类加载失败'));
  }, []);

  const categoryValueEnum = useMemo(() => {
    return categories.reduce<Record<number, { text: string }>>((acc, item) => {
      acc[item.id] = { text: item.status === 1 ? item.name : `${item.name}（停用）` };
      return acc;
    }, {});
  }, [categories]);

  const handleChangeStatus = (record: ProductSummary, targetStatus: ProductStatus) => {
    Modal.confirm({
      title: `${productStatusText[targetStatus]}商品`,
      content: `确定要将「${record.title}」调整为「${productStatusText[targetStatus]}」吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await updateProductStatus(record.id, targetStatus);
        message.success('商品状态已更新');
        actionRef.current?.reload();
      },
    });
  };

  const openSubtitleModal = async (record: ProductSummary) => {
    setSubtitleLoading(true);
    setSubtitleProduct(record);
    try {
      const detail = await getProduct(record.id);
      subtitleForm.setFieldsValue({ subtitle: detail.subtitle || '' });
    } catch (error: any) {
      message.error(error?.message || '商品详情加载失败');
      setSubtitleProduct(undefined);
    } finally {
      setSubtitleLoading(false);
    }
  };

  const columns: ProColumns<ProductSummary>[] = [
    { title: 'ID', dataIndex: 'id', width: 80, search: false },
    { title: '商品标题', dataIndex: 'keyword', hideInTable: true, fieldProps: { placeholder: '标题关键字' } },
    { title: '商品标题', dataIndex: 'title', search: false, ellipsis: true },
    {
      title: '分类',
      dataIndex: 'categoryId',
      valueType: 'select',
      valueEnum: categoryValueEnum,
      render: (_, record) => record.categoryName || record.categoryId,
    },
    { title: '状态', dataIndex: 'status', valueType: 'select', valueEnum: productStatusValueEnum },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      search: false,
      render: (_, record) => formatDateTime(record.createdAt),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 260,
      render: (_, record) => (
        <Space size="small" wrap>
          <Link to={`/products/${record.id}/edit`}>编辑</Link>
          <Link to={`/products/${record.id}/skus`}>SKU</Link>
          {record.status === 'ON_SALE' ? (
            <Button type="link" size="small" onClick={() => handleChangeStatus(record, 'OFF_SHELF')}>
              下架
            </Button>
          ) : (
            <Button type="link" size="small" onClick={() => handleChangeStatus(record, 'ON_SALE')}>
              上架
            </Button>
          )}
          <Button type="link" size="small" onClick={() => openSubtitleModal(record)}>
            改副标题
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="商品管理">
      <ProTable<ProductSummary>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => toTableResult(await queryProducts(params))}
        pagination={{ defaultPageSize: 10 }}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => history.push('/products/create')}>
            新建商品
          </Button>,
        ]}
      />
      <Modal
        title={`修改副标题${subtitleProduct ? `：${subtitleProduct.title}` : ''}`}
        open={Boolean(subtitleProduct)}
        confirmLoading={subtitleLoading}
        onCancel={() => setSubtitleProduct(undefined)}
        onOk={async () => {
          const values = await subtitleForm.validateFields();
          if (!subtitleProduct) return;
          setSubtitleLoading(true);
          try {
            await updateProductSubtitle(subtitleProduct.id, values.subtitle);
            message.success('副标题已更新');
            setSubtitleProduct(undefined);
            actionRef.current?.reload();
          } finally {
            setSubtitleLoading(false);
          }
        }}
      >
        <Form form={subtitleForm} layout="vertical">
          <Form.Item
            name="subtitle"
            label="副标题"
            rules={[{ required: true, message: '请输入商品副标题' }, { max: 100, message: '副标题不能超过 100 个字符' }]}
          >
            <Input placeholder="请输入商品副标题" maxLength={100} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default ProductList;
