import { create } from 'zustand'

export const useOrderStore = create((set, get) => ({
  orders: [],

  setOrders: (orders) => set({ orders }),

  addOrder: (order) => set(state => ({
    orders: [order, ...state.orders.filter(o => o.id !== order.id)]
  })),

  updateOrderStatus: (orderId, status) => set(state => ({
    orders: state.orders.map(o => o.id === orderId ? { ...o, status } : o)
  })),

  removeTerminalOrders: () => set(state => ({
    orders: state.orders.filter(o => !['SERVED', 'CANCELLED'].includes(o.status))
  })),
}))
