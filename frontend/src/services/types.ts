export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
  traceId?: string;
  timestamp?: string;
};

export type PageResponse<T> = {
  pageNum: number;
  pageSize: number;
  total: number;
  pages: number;
  records: T[];
};

export type UserRole = 'USER' | 'ADMIN';

export type CurrentUser = {
  id: number;
  username: string;
  nickname: string;
  role: UserRole;
};

export type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  currentUser: CurrentUser;
};

export type ProductStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SHELF';

export type ProductSummary = {
  id: number;
  categoryId: number;
  categoryName?: string;
  title: string;
  status: ProductStatus;
  createdAt: string;
};

export type ProductDetail = ProductSummary & {
  subtitle?: string;
  description?: string;
  createdBy: number;
  updatedAt: string;
};

export type CategorySummary = {
  id: number;
  name: string;
  status: number;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type CategoryDraftResponse = {
  name: string;
  description?: string;
  sortOrder: number;
};

export type Sku = {
  id: number;
  productId: number;
  skuCode: string;
  specText: string;
  salePrice: number;
  availableStock: number;
  lockedStock: number;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type UserSummary = {
  id: number;
  username: string;
  nickname: string;
  status: number;
  createdAt: string;
};

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'CLOSED';

export type OrderItem = {
  id: number;
  skuId: number;
  productId: number;
  productTitle: string;
  skuCode: string;
  specText: string;
  unitPrice: number;
  quantity: number;
  lineAmount: number;
};

export type OrderSummary = {
  id: number;
  orderNo: string;
  userId: number;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
  paidAt?: string;
  cancelledAt?: string;
  closedAt?: string;
};

export type OrderDetail = Omit<OrderSummary, 'userId'> & {
  userId?: number;
  items: OrderItem[];
};
