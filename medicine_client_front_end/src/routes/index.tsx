import { Suspense, lazy } from 'react'
import type { RouteObject } from 'react-router-dom'
import { useRoutes, Navigate } from 'react-router-dom'
import TabLayout from '@/layout/TabBar'
import { useAuthStore } from '@/store/auth'

const HomePage = lazy(() => import('@/pages/Home'))
const CategoryPage = lazy(() => import('@/pages/Category'))
const Assistant = lazy(() => import('@/pages/Assistant'))
const MePage = lazy(() => import('@/pages/Me'))
const CartPage = lazy(() => import('@/pages/Cart'))
const LoginPage = lazy(() => import('@/pages/Login'))
const AgreementPage = lazy(() => import('@/pages/Agreement'))
const SearchPage = lazy(() => import('@/pages/SearchPage'))
const SearchResultPage = lazy(() => import('@/pages/SearchResult'))
const CategoryResultPage = lazy(() => import('@/pages/CategoryResult'))
const MallDetailPage = lazy(() => import('@/pages/MallDetail'))
const CheckoutPage = lazy(() => import('@/pages/Checkout'))
const OrderPaymentPage = lazy(() => import('@/pages/OrderPayment'))
const PaymentResultPage = lazy(() => import('@/pages/PaymentResult'))
const OrderDetailPage = lazy(() => import('@/pages/OrderDetail'))
const OrdersPage = lazy(() => import('@/pages/Orders'))
const WalletPage = lazy(() => import('@/pages/Me/components/Wallet'))
const WalletBillPage = lazy(() => import('@/pages/Me/components/WalletBill'))
/** 钱包账单详情页路由组件。 */
const WalletBillDetailPage = lazy(() => import('@/pages/Me/components/WalletBillDetail'))
const AddressListPage = lazy(() => import('@/pages/AddressList'))
const ProfileEditPage = lazy(() => import('@/pages/Me/components/ProfileEdit'))
const PasswordChangePage = lazy(() => import('@/pages/Me/components/PasswordChange'))
const SecuritySettingsPage = lazy(() => import('@/pages/Me/components/SecuritySettings'))
const PhoneChangePage = lazy(() => import('@/pages/Me/components/PhoneChange'))
const PatientListPage = lazy(() => import('@/pages/PatientList'))
const AfterSalePage = lazy(() => import('@/pages/AfterSale'))
const AfterSaleDetailPage = lazy(() => import('@/pages/AfterSaleDetail'))
const AfterSaleApplyPage = lazy(() => import('@/pages/AfterSaleApply'))
const AfterSaleReapplyPage = lazy(() => import('@/pages/AfterSaleReapply'))
const ViewHistoryPage = lazy(() => import('@/pages/viewHistor'))
const CouponPage = lazy(() => import('@/pages/Coupon'))
const CouponActivationPage = lazy(() => import('@/pages/CouponActivation'))
const NotFoundPage = lazy(() => import('@/pages/NotFound'))

/**
 * 路由守卫组件
 * 检查用户是否已登录，未登录跳转到登录页
 */
const ProtectedRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const isAuthenticated = useAuthStore(state => state.isAuthenticated())

  if (!isAuthenticated) {
    return <Navigate to='/login' replace />
  }

  return children
}

const routes: RouteObject[] = [
  // 公开路由 - 无需登录
  {
    path: '/login',
    element: <LoginPage />
  },
  // 协议页 - 无需登录
  {
    path: '/agreement/software',
    element: <AgreementPage />
  },
  {
    path: '/agreement/privacy',
    element: <AgreementPage />
  },
  // 首页相关 - 无需登录即可浏览
  {
    path: '/',
    element: <TabLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'home', element: <HomePage /> },
      {
        path: 'category',
        element: <CategoryPage />
      },
      {
        path: 'assistant',
        element: (
          <ProtectedRoute>
            <Assistant />
          </ProtectedRoute>
        )
      },
      {
        path: 'me',
        element: (
          <ProtectedRoute>
            <MePage />
          </ProtectedRoute>
        )
      }
    ]
  },
  // 搜索页 - 无需登录
  {
    path: '/search',
    element: <SearchPage />
  },
  // 搜索结果页 - 无需登录
  {
    path: '/search-result',
    element: <SearchResultPage />
  },
  // 分类结果页 - 无需登录
  {
    path: '/category-result',
    element: <CategoryResultPage />
  },
  // 商品详情 - 需要登录
  {
    path: '/product/:id',
    element: (
      <ProtectedRoute>
        <MallDetailPage />
      </ProtectedRoute>
    )
  },
  // 以下路由需要登录
  {
    path: '/cart',
    element: (
      <ProtectedRoute>
        <CartPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/checkout',
    element: (
      <ProtectedRoute>
        <CheckoutPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/order/payment/:orderNo',
    element: (
      <ProtectedRoute>
        <OrderPaymentPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/payment/result',
    element: (
      <ProtectedRoute>
        <PaymentResultPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/orders/:orderNo',
    element: (
      <ProtectedRoute>
        <OrderDetailPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/orders',
    element: (
      <ProtectedRoute>
        <OrdersPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/wallet',
    element: (
      <ProtectedRoute>
        <WalletPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/wallet/bill',
    element: (
      <ProtectedRoute>
        <WalletBillPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/wallet/bill/:billId',
    element: (
      <ProtectedRoute>
        <WalletBillDetailPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/address/list',
    element: (
      <ProtectedRoute>
        <AddressListPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/profile/edit',
    element: (
      <ProtectedRoute>
        <ProfileEditPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/password/change',
    element: (
      <ProtectedRoute>
        <PasswordChangePage />
      </ProtectedRoute>
    )
  },
  {
    path: '/security/settings',
    element: (
      <ProtectedRoute>
        <SecuritySettingsPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/phone/change',
    element: (
      <ProtectedRoute>
        <PhoneChangePage />
      </ProtectedRoute>
    )
  },
  {
    path: '/patient/list',
    element: (
      <ProtectedRoute>
        <PatientListPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/after-sale',
    element: (
      <ProtectedRoute>
        <AfterSalePage />
      </ProtectedRoute>
    )
  },
  {
    path: '/after-sale/detail/:afterSaleNo',
    element: (
      <ProtectedRoute>
        <AfterSaleDetailPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/after-sale/apply',
    element: (
      <ProtectedRoute>
        <AfterSaleApplyPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/after-sale/reapply',
    element: (
      <ProtectedRoute>
        <AfterSaleReapplyPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/view-history',
    element: (
      <ProtectedRoute>
        <ViewHistoryPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/coupon',
    element: (
      <ProtectedRoute>
        <CouponPage />
      </ProtectedRoute>
    )
  },
  {
    path: '/coupon/activation',
    element: (
      <ProtectedRoute>
        <CouponActivationPage />
      </ProtectedRoute>
    )
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
]

const AppRoutes: React.FC = () => {
  const element = useRoutes(routes)
  return <Suspense fallback={null}>{element}</Suspense>
}

export default AppRoutes
