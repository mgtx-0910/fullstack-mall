import React, { useEffect, useMemo, useState } from 'react';
import { ProForm, ProFormSelect, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import { message } from 'antd';
import { listCategories } from '@/services/categories';
import type { ProductDetail } from '@/services/types';
import type { ProductFormValues } from '@/services/products';

type ProductFormProps = {
  initialValues?: Partial<ProductDetail>;
  submitText: string;
  onSubmit: (values: ProductFormValues) => Promise<void>;
};

const ProductForm: React.FC<ProductFormProps> = ({ initialValues, submitText, onSubmit }) => {
  const [categories, setCategories] = useState<{ label: string; value: number; disabled?: boolean }[]>([]);

  useEffect(() => {
    listCategories()
      .then((items) => {
        setCategories(
          items.map((item) => ({
            label: item.status === 1 ? item.name : `${item.name}（停用）`,
            value: item.id,
            disabled: item.status !== 1,
          })),
        );
      })
      .catch((error) => message.error(error?.message || '分类加载失败'));
  }, []);

  const formInitialValues = useMemo(
    () => ({
      categoryId: initialValues?.categoryId,
      title: initialValues?.title,
      description: initialValues?.description,
    }),
    [initialValues],
  );

  return (
    <ProForm<ProductFormValues>
      initialValues={formInitialValues}
      submitter={{ searchConfig: { submitText, resetText: '重置' } }}
      onFinish={async (values) => {
        await onSubmit(values);
        return true;
      }}
    >
      <ProFormSelect
        name="categoryId"
        label="商品分类"
        options={categories}
        rules={[{ required: true, message: '请选择商品分类' }]}
        placeholder="请选择启用分类"
      />
      <ProFormText
        name="title"
        label="商品标题"
        rules={[{ required: true, message: '请输入商品标题' }, { max: 100, message: '商品标题不能超过 100 个字符' }]}
      />
      <ProFormTextArea
        name="description"
        label="商品描述"
        fieldProps={{ rows: 6, showCount: true, maxLength: 2000 }}
        rules={[{ max: 2000, message: '商品描述不能超过 2000 个字符' }]}
      />
    </ProForm>
  );
};

export default ProductForm;
