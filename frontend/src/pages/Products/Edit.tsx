import React, { useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { Spin, message } from 'antd';
import ProductForm from './components/ProductForm';
import { getProduct, updateProduct } from '@/services/products';
import type { ProductDetail } from '@/services/types';

const ProductEdit: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const [product, setProduct] = useState<ProductDetail>();
  const [loading, setLoading] = useState(true);

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

  return (
    <PageContainer title="编辑商品" onBack={() => history.back()}>
      <Spin spinning={loading}>
        {product && (
          <ProductForm
            initialValues={product}
            submitText="保存修改"
            onSubmit={async (values) => {
              await updateProduct(productId, values);
              message.success('商品已更新');
              history.push('/products');
            }}
          />
        )}
      </Spin>
    </PageContainer>
  );
};

export default ProductEdit;
