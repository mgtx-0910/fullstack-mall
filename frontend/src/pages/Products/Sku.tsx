import React, { useEffect, useRef, useState } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { Button, Form, InputNumber, Modal, Space, Spin, message } from 'antd';
import { getProduct } from '@/services/products';
import { adjustSkuStock, createSku, querySkus, updateSkuPrice } from '@/services/skus';
import type { ProductDetail, Sku } from '@/services/types';
import { formatDateTime, formatMoney } from '@/utils/format';

const ProductSkuPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const actionRef = useRef<ActionType>();
  const [product, setProduct] = useState<ProductDetail>();
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [priceForm] = Form.useForm<{ salePrice: number }>();
  const [stockForm] = Form.useForm<{ delta: number }>();
  const [priceSku, setPriceSku] = useState<Sku>();
  const [stockSku, setStockSku] = useState<Sku>();
  const [modalLoading, setModalLoading] = useState(false);

  useEffect(() => {
    if (!productId) {
      message.error('商品 ID 不合法');
      history.push('/products');
      return;
    }
    getProduct(productId)
      .then(setProduct)
      .catch((error) => message.error(error?.message || '商品加载失败'))
      .finally(() => setLoading(false));
  }, [productId]);

  const columns: ProColumns<Sku>[] = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: 'SKU 编码', dataIndex: 'skuCode', ellipsis: true },
    { title: '规格', dataIndex: 'specText', ellipsis: true },
    { title: '销售价', dataIndex: 'salePrice', render: (_, record) => formatMoney(record.salePrice) },
    { title: '可售库存', dataIndex: 'availableStock' },
    { title: '锁定库存', dataIndex: 'lockedStock' },
    { title: '版本', dataIndex: 'version', width: 80 },
    { title: '更新时间', dataIndex: 'updatedAt', render: (_, record) => formatDateTime(record.updatedAt) },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => {
              priceForm.setFieldsValue({ salePrice: Number(record.salePrice) });
              setPriceSku(record);
            }}
          >
            改价
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              stockForm.setFieldsValue({ delta: 1 });
              setStockSku(record);
            }}
          >
            改库存
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title="SKU 管理" subTitle={product ? `商品：${product.title}` : undefined} onBack={() => history.back()}>
      <Spin spinning={loading}>
        <ProTable<Sku>
          rowKey="id"
          actionRef={actionRef}
          search={false}
          columns={columns}
          request={async () => ({ data: await querySkus(productId), success: true })}
          pagination={false}
          toolBarRender={() => [
            <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
              创建 SKU
            </Button>,
          ]}
        />
      </Spin>

      <ModalForm
        title="创建 SKU"
        open={createOpen}
        modalProps={{ destroyOnClose: true, onCancel: () => setCreateOpen(false) }}
        onFinish={async (values: any) => {
          await createSku(productId, {
            skuCode: values.skuCode,
            specText: values.specText,
            salePrice: values.salePrice,
            initialStock: values.initialStock,
          });
          message.success('SKU 已创建');
          setCreateOpen(false);
          actionRef.current?.reload();
          return true;
        }}
      >
        <ProFormText
          name="skuCode"
          label="SKU 编码"
          rules={[{ required: true, message: '请输入 SKU 编码' }, { max: 64, message: 'SKU 编码不能超过 64 个字符' }]}
        />
        <ProFormText
          name="specText"
          label="规格描述"
          placeholder="例如：红色 / 256GB"
          rules={[{ required: true, message: '请输入规格描述' }, { max: 200, message: '规格描述不能超过 200 个字符' }]}
        />
        <ProFormDigit
          name="salePrice"
          label="销售价"
          fieldProps={{ min: 0.01, precision: 2 }}
          rules={[{ required: true, message: '请输入销售价' }]}
        />
        <ProFormDigit
          name="initialStock"
          label="初始库存"
          fieldProps={{ min: 0, precision: 0 }}
          rules={[{ required: true, message: '请输入初始库存' }]}
        />
      </ModalForm>

      <Modal
        title={`改价${priceSku ? `：${priceSku.skuCode}` : ''}`}
        open={Boolean(priceSku)}
        confirmLoading={modalLoading}
        onCancel={() => setPriceSku(undefined)}
        onOk={async () => {
          const values = await priceForm.validateFields();
          if (!priceSku) return;
          setModalLoading(true);
          try {
            await updateSkuPrice(priceSku.id, values.salePrice, priceSku.version);
            message.success('价格已更新');
            setPriceSku(undefined);
            actionRef.current?.reload();
          } finally {
            setModalLoading(false);
          }
        }}
      >
        <Form form={priceForm} layout="vertical">
          <Form.Item name="salePrice" label="新销售价" rules={[{ required: true, message: '请输入新销售价' }]}>
            <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`改库存${stockSku ? `：${stockSku.skuCode}` : ''}`}
        open={Boolean(stockSku)}
        confirmLoading={modalLoading}
        onCancel={() => setStockSku(undefined)}
        onOk={async () => {
          const values = await stockForm.validateFields();
          if (!stockSku) return;
          setModalLoading(true);
          try {
            await adjustSkuStock(stockSku.id, values.delta, stockSku.version);
            message.success('库存已更新');
            setStockSku(undefined);
            actionRef.current?.reload();
          } finally {
            setModalLoading(false);
          }
        }}
      >
        <Form form={stockForm} layout="vertical">
          <Form.Item
            name="delta"
            label="库存变化量"
            extra="正数表示入库，负数表示扣减；后端会校验最终可售库存不能小于 0。"
            rules={[{ required: true, message: '请输入库存变化量' }]}
          >
            <InputNumber precision={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};


export default ProductSkuPage;
