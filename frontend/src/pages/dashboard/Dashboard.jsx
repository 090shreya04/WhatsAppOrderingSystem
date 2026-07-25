import { useEffect, useState, useRef } from 'react'
import { restaurantApi, orderApi } from '../../api'
import { useOrderStore } from '../../store/orderStore'
import { useAuthStore } from '../../store/authStore'
import { useOrderWebSocket } from '../../hooks/useOrderWebSocket'
import toast from 'react-hot-toast'
import { ChevronRight, Wifi, UtensilsCrossed, MessageCircle, Clock, XCircle, X } from 'lucide-react'

const NEXT_STATUS = { PLACED: 'CONFIRMED', CONFIRMED: 'PREPARING', PREPARING: 'READY', READY: 'SERVED' }

function StatusBadge({ status }) {
  const map = {
    PLACED:    { label: 'Placed',    cls: 'status-placed' },
    CONFIRMED: { label: 'Confirmed', cls: 'status-confirmed' },
    PREPARING: { label: 'Preparing', cls: 'status-preparing' },
    READY:     { label: 'Ready',     cls: 'status-ready' },
    SERVED:    { label: 'Served',    cls: 'status-served' },
    CANCELLED: { label: 'Cancelled', cls: 'status-cancelled' },
  }
  const { label, cls } = map[status] || { label: status, cls: '' }
  return <span className={`status-chip ${cls}`}>{label}</span>
}

function ChannelBadge({ channel }) {
  if (channel === 'DINE_IN') return (
    <span className="badge-dine-in"><UtensilsCrossed size={10} />Table</span>
  )
  return (
    <span className="badge-whatsapp"><MessageCircle size={10} />WhatsApp</span>
  )
}

function OrderCard({ order, onAdvance, onCancel, isNew }) {
  const [loading, setLoading] = useState(false)
  const nextStatus = NEXT_STATUS[order.status]

  const handleAdvance = async () => {
    if (!nextStatus) return
    setLoading(true)
    try {
      await onAdvance(order.id, nextStatus)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`glass-card p-4 flex items-start gap-4 ${isNew ? 'order-pop border-brand-500/40' : ''}`}>
      {/* Left: info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap mb-2">
          <ChannelBadge channel={order.channel} />
          <StatusBadge status={order.status} />
          <span className="text-gray-500 text-xs ml-auto flex items-center gap-1">
            <Clock size={11} />
            {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>

        <div className="flex items-baseline gap-2 mb-1">
          <span className="font-semibold text-white">
            {order.channel === 'DINE_IN' ? `Table ${order.tableNumber}` : `${order.customerPhone || 'WhatsApp'}`}
          </span>
          <span className="text-brand-400 font-semibold text-sm">₹{Number(order.totalAmount).toFixed(0)}</span>
        </div>

        <p className="text-gray-400 text-sm truncate">
          {order.items?.map(i => `${i.menuItemName} x${i.quantity}`).join(', ') || '…'}
        </p>
      </div>

      {/* Right: Actions (Advance + Cancel) */}
      <div className="flex items-center gap-2 flex-shrink-0">
        {order.status !== 'CANCELLED' && order.status !== 'SERVED' && (
          <button
            onClick={() => onCancel(order)}
            title="Cancel Order"
            className="px-2.5 py-2 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 text-xs font-medium flex items-center gap-1 transition-colors"
          >
            <XCircle size={14} />
            Cancel
          </button>
        )}

        {nextStatus && order.status !== 'CANCELLED' && (
          <button
            id={`advance-order-${order.id}`}
            onClick={handleAdvance}
            disabled={loading}
            className="btn-primary flex items-center gap-1 whitespace-nowrap text-sm px-3 py-2"
          >
            {loading ? '…' : nextStatus.charAt(0) + nextStatus.slice(1).toLowerCase()}
            <ChevronRight size={14} />
          </button>
        )}
      </div>
    </div>
  )
}

function CancelModal({ order, onConfirm, onClose }) {
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      await onConfirm(order.id, reason)
    } finally {
      setSubmitting(false)
    }
  }

  const PRESETS = [
    'Item out of stock',
    'Kitchen closed for the day',
    'Customer requested cancellation',
    'High order surge / delay'
  ]

  return (
    <div className="fixed inset-0 bg-black/75 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-card w-full max-w-md p-6 shadow-2xl border border-gray-700">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-white">Cancel Order #{order.id}</h2>
            <p className="text-xs text-gray-400 mt-0.5">
              {order.channel === 'DINE_IN' ? `Table ${order.tableNumber}` : order.customerPhone}
            </p>
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300"><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Cancellation Reason (Optional)</label>
            <textarea
              className="form-input min-h-[80px] text-sm"
              placeholder="e.g. Sorry, Egg Roll is out of stock today. (If left blank, default apology is sent)"
              value={reason}
              onChange={e => setReason(e.target.value)}
            />
          </div>

          <div>
            <p className="text-xs text-gray-400 mb-2">Quick Reason Presets:</p>
            <div className="flex flex-wrap gap-1.5">
              {PRESETS.map(preset => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setReason(preset)}
                  className="text-xs px-2.5 py-1 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 border border-gray-700 transition-colors"
                >
                  + {preset}
                </button>
              ))}
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Back</button>
            <button
              type="submit"
              disabled={submitting}
              className="bg-red-600 hover:bg-red-700 text-white font-medium text-sm px-4 py-2 rounded-xl flex-1 transition-colors"
            >
              {submitting ? 'Cancelling…' : 'Cancel Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuthStore()
  const { orders, setOrders, updateOrderStatus } = useOrderStore()
  const [restaurant, setRestaurant] = useState(null)
  const [stats, setStats] = useState({ total: 0, dineIn: 0, whatsapp: 0 })
  const [filter, setFilter] = useState({ channel: '', status: '' })
  const [loading, setLoading] = useState(true)
  const [cancellingOrder, setCancellingOrder] = useState(null)
  const newOrderIds = useRef(new Set())

  useOrderWebSocket(restaurant?.id)

  // Load restaurant + initial order queue
  useEffect(() => {
    const init = async () => {
      try {
        const r = await restaurantApi.getMe()
        setRestaurant(r.data)
        const o = await orderApi.getOrders({})
        setOrders(o.data)
        setStats({
          total: o.data.length,
          dineIn: o.data.filter(x => x.channel === 'DINE_IN').length,
          whatsapp: o.data.filter(x => x.channel === 'WHATSAPP').length,
        })
      } catch {
        // No restaurant yet — redirect handled by app
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [])

  const handleAdvance = async (orderId, nextStatus) => {
    try {
      await orderApi.updateStatus(orderId, nextStatus)
      updateOrderStatus(orderId, nextStatus)
      toast.success(`Order #${orderId} → ${nextStatus}`)
    } catch (e) {
      toast.error('Failed to update status')
    }
  }

  const handleCancelConfirm = async (orderId, reason) => {
    try {
      await orderApi.updateStatus(orderId, 'CANCELLED', reason)
      updateOrderStatus(orderId, 'CANCELLED')
      toast.success(`Order #${orderId} cancelled`)
      setCancellingOrder(null)
    } catch (e) {
      toast.error('Failed to cancel order')
    }
  }

  const filteredOrders = orders.filter(o => {
    if (filter.channel && o.channel !== filter.channel) return false
    if (filter.status && o.status !== filter.status) return false
    return true
  })

  if (loading) return (
    <div className="flex items-center justify-center h-full text-gray-500">Loading dashboard…</div>
  )

  return (
    <div className="p-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">
            {restaurant?.name || 'Dashboard'}
          </h1>
          <p className="text-gray-500 text-sm mt-0.5">Live order queue</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-green-400 bg-green-400/10 px-3 py-1.5 rounded-full border border-green-400/20">
          <Wifi size={12} />
          Live
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Active orders', value: filteredOrders.length, color: 'text-white' },
          { label: 'Dine-in', value: filteredOrders.filter(o => o.channel === 'DINE_IN').length, color: 'text-blue-400' },
          { label: 'WhatsApp', value: filteredOrders.filter(o => o.channel === 'WHATSAPP').length, color: 'text-green-400' },
        ].map(({ label, value, color }) => (
          <div key={label} className="glass-card p-4">
            <p className="text-gray-500 text-xs mb-1">{label}</p>
            <p className={`text-3xl font-bold ${color}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex gap-2 mb-4 flex-wrap">
        {['', 'DINE_IN', 'WHATSAPP'].map(ch => (
          <button key={ch} onClick={() => setFilter(f => ({ ...f, channel: ch }))}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors
              ${filter.channel === ch ? 'bg-brand-500 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'}`}>
            {ch === '' ? 'All channels' : ch === 'DINE_IN' ? '🍽️ Dine-in' : '📱 WhatsApp'}
          </button>
        ))}
        <div className="w-px bg-gray-800 mx-1" />
        {['', 'PLACED', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED', 'CANCELLED'].map(st => (
          <button key={st} onClick={() => setFilter(f => ({ ...f, status: st }))}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors
              ${filter.status === st ? 'bg-brand-500 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'}`}>
            {st === '' ? 'All statuses' : st.charAt(0) + st.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Order queue */}
      <div className="space-y-3">
        {filteredOrders.length === 0 ? (
          <div className="glass-card p-12 text-center">
            <UtensilsCrossed size={40} className="text-gray-700 mx-auto mb-3" />
            <p className="text-gray-500">No active orders right now</p>
            <p className="text-gray-600 text-sm mt-1">New orders will appear here instantly</p>
          </div>
        ) : (
          filteredOrders.map(order => (
            <OrderCard
              key={order.id}
              order={order}
              onAdvance={handleAdvance}
              onCancel={setCancellingOrder}
              isNew={newOrderIds.current.has(order.id)}
            />
          ))
        )}
      </div>

      {/* Cancel Order Modal */}
      {cancellingOrder && (
        <CancelModal
          order={cancellingOrder}
          onConfirm={handleCancelConfirm}
          onClose={() => setCancellingOrder(null)}
        />
      )}
    </div>
  )
}
