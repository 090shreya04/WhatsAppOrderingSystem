import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Login from './pages/auth/Login'
import Signup from './pages/auth/Signup'
import DashboardLayout from './components/layout/DashboardLayout'
import Dashboard from './pages/dashboard/Dashboard'
import MenuManagement from './pages/dashboard/MenuManagement'
import TableManagement from './pages/dashboard/TableManagement'
import Analytics from './pages/dashboard/Analytics'
import CustomerMenu from './pages/customer/CustomerMenu'
import OrderStatus from './pages/customer/OrderStatus'
import Settings from './pages/dashboard/Settings'

function ProtectedRoute({ children }) {
  const token = useAuthStore(s => s.token)
  return token ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Auth */}
        <Route path="/login"  element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        {/* Owner dashboard (protected) */}
        <Route path="/dashboard" element={<ProtectedRoute><DashboardLayout /></ProtectedRoute>}>
          <Route index        element={<Dashboard />} />
          <Route path="menu"   element={<MenuManagement />} />
          <Route path="tables" element={<TableManagement />} />
          <Route path="analytics" element={<Analytics />} />
          <Route path="settings" element={<Settings />} />
        </Route>

        {/* Customer-facing (public) */}
        <Route path="/order/:qrSecret/:tableId"          element={<CustomerMenu />} />
        <Route path="/order/:qrSecret/:tableId/status/:orderId" element={<OrderStatus />} />

        {/* Default */}
        <Route path="/"  element={<Navigate to="/dashboard" replace />} />
        <Route path="*"  element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
