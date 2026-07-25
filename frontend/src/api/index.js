import api from './axios'

export const authApi = {
  signup: (data) => api.post('/auth/signup', data),
  login:  (data) => api.post('/auth/login', data),
}

export const restaurantApi = {
  create: (data) => api.post('/restaurants', data),
  getMe:  ()     => api.get('/restaurants/me'),
  update: (data) => api.put('/restaurants/me', data),
}

export const menuApi = {
  // Categories
  getCategories:    ()           => api.get('/restaurants/me/categories'),
  createCategory:   (data)       => api.post('/restaurants/me/categories', data),
  updateCategory:   (id, data)   => api.put(`/categories/${id}`, data),
  deleteCategory:   (id)         => api.delete(`/categories/${id}`),

  // Items
  getMenuItems:     ()           => api.get('/restaurants/me/menu-items'),
  createMenuItem:   (data)       => api.post('/restaurants/me/menu-items', data),
  updateMenuItem:   (id, data)   => api.put(`/menu-items/${id}`, data),
  toggleAvailability: (id, available) =>
    api.patch(`/menu-items/${id}/availability`, { available }),
  deleteMenuItem:   (id)         => api.delete(`/menu-items/${id}`),

  // Image upload
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/menu-items/upload-image', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
}

const getBackendBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL
  return envUrl ? `${envUrl}/api/v1` : '/api/v1'
}

export const tableApi = {
  getTables:    ()           => api.get('/restaurants/me/tables'),
  createTable:  (data)       => api.post('/restaurants/me/tables', data),
  updateTable:  (id, data)   => api.put(`/tables/${id}`, data),
  deleteTable:  (id)         => api.delete(`/tables/${id}`),
  updateStatus: (id, status) => api.patch(`/tables/${id}/status`, { status }),
  getQrUrl:     (id)         => `${getBackendBaseUrl()}/tables/${id}/qr`,
}

export const orderApi = {
  getOrders:     (params) => api.get('/restaurants/me/orders', { params }),
  updateStatus:  (id, status, reason) => api.patch(`/orders/${id}/status`, { status, reason }),
  placeOrder:    (data)   => api.post('/public/orders', data),
  getStatus:     (id)     => api.get(`/public/orders/${id}/status`),
}

export const publicMenuApi = {
  getMenu: (qrSecret, tableId) =>
    api.get(`/public/menu/${qrSecret}/${tableId}`),
}

export const analyticsApi = {
  getSummary:   (from, to) => api.get('/restaurants/me/analytics/summary', { params: { from, to } }),
  getTopItems:  (from, to) => api.get('/restaurants/me/analytics/top-items', { params: { from, to } }),
  getPeakHours: (from, to) => api.get('/restaurants/me/analytics/peak-hours', { params: { from, to } }),
}
