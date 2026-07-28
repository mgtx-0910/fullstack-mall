import React from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { message } from 'antd';
import ProductForm from './components/ProductForm';
import { createProduct } from '@/services/products';

const ProductCreate: React.FC = () => {
  return (
    <PageContainer title="新建商品" onBack={() => history.back()}>
      <ProductForm
        submitText="创建商品"
        onSubmit={async (values) => {
          const product = await createProduct(values);
          message.success('商品已创建');
          history.push(`/products/${product.id}/skus`);
        }}
      />
    </PageContainer>
  );
};

export default ProductCreate;
