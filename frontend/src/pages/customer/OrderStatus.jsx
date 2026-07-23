import { useParams } from 'react-router-dom'
import { useOrderStatus } from '../../hooks/useOrderStatus'
import { CheckCircle2, Clock, ChefHat, Bell, XCircle, UtensilsCrossed } from 'lucide-react'

const STATUS_CONFIG = {
  PLACED:    { icon: Clock,        color: 'text-yellow-400', bg: 'bg-yellow-400/10', label: 'Order received!', sub: 'Your order is in the queue.' },
  CONFIRMED: { icon: CheckCircle2, color: 'text-blue-400',   bg: 'bg-blue-400/10',   label: 'Confirmed!',     sub: 'The restaurant confirmed your order.' },
  PREPARING: { icon: ChefHat,      color: 'text-orange-400', bg: 'bg-orange-400/10', label: 'Being prepared', sub: 'The kitchen is working on your food.' },
  READY:     { icon: Bell,         color: 'text-green-400',  bg: 'bg-green-400/10',  label: 'Ready! 🎉',      sub: 'Your order is ready. Bon appétit!' },
  SERVED:    { icon: UtensilsCrossed, color: 'text-gray-400', bg: 'bg-gray-700',    label: 'Served!',        sub: 'Enjoy your meal!' },
  CANCELLED: { icon: XCircle,      color: 'text-red-400',    bg: 'bg-red-400/10',    label: 'Cancelled',      sub: 'Your order was cancelled. Please contact staff.' },
}

const STEPS = ['PLACED','CONFIRMED','PREPARING','READY','SERVED']

export default function OrderStatus() {
  const { orderId } = useParams()
  const { status, loading } = useOrderStatus(orderId)

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center text-gray-500">
      Loading…
    </div>
  )

  const cfg = STATUS_CONFIG[status?.status] || STATUS_CONFIG['PLACED']
  const Icon = cfg.icon
  const currentStep = STEPS.indexOf(status?.status)

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center px-4 text-center">
      {/* Glow decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className={`absolute top-1/4 left-1/2 -translate-x-1/2 w-80 h-80 rounded-full blur-3xl opacity-10 ${cfg.bg}`} />
      </div>

      {/* Icon */}
      <div className={`w-24 h-24 rounded-3xl ${cfg.bg} flex items-center justify-center mb-6 relative`}>
        <Icon size={44} className={`${cfg.color} ${status?.status === 'PREPARING' ? 'animate-pulse' : ''}`} />
      </div>

      {/* Status text */}
      <h1 className={`text-3xl font-bold mb-2 ${cfg.color}`}>{cfg.label}</h1>
      <p className="text-gray-400 mb-8">{cfg.sub}</p>

      {/* Order ID */}
      <div className="glass-card px-6 py-3 mb-8 inline-flex">
        <span className="text-gray-500 text-sm">Order</span>
        <span className="text-white font-bold ml-2">#{orderId}</span>
      </div>

      {/* Progress stepper (excludes CANCELLED) */}
      {status?.status !== 'CANCELLED' && (
        <div className="flex items-center gap-2 w-full max-w-xs">
          {STEPS.slice(0, -1).map((step, i) => (
            <div key={step} className="flex-1 flex items-center gap-2">
              <div className={`flex-1 h-1.5 rounded-full transition-all duration-500
                ${i <= currentStep - 1 ? 'bg-brand-500' : 'bg-gray-800'}`} />
            </div>
          ))}
        </div>
      )}

      <p className="text-gray-600 text-xs mt-8">This page refreshes automatically every 5 seconds</p>
    </div>
  )
}
