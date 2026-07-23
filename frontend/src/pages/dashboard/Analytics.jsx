import { useState, useEffect } from 'react'
import { analyticsApi } from '../../api'
import { format, subDays } from 'date-fns'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts'

const COLORS = { DINE_IN: '#3b82f6', WHATSAPP: '#22c55e' }

export default function Analytics() {
  const today = format(new Date(), 'yyyy-MM-dd')
  const weekAgo = format(subDays(new Date(), 7), 'yyyy-MM-dd')

  const [from, setFrom] = useState(weekAgo)
  const [to, setTo] = useState(today)
  const [summary, setSummary] = useState(null)
  const [topItems, setTopItems] = useState([])
  const [peakHours, setPeakHours] = useState([])
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const [s, t, p] = await Promise.all([
        analyticsApi.getSummary(from, to),
        analyticsApi.getTopItems(from, to),
        analyticsApi.getPeakHours(from, to),
      ])
      setSummary(s.data)
      setTopItems(t.data.slice(0, 8))
      // Fill missing hours with 0
      const hourMap = {}
      p.data.forEach(r => { hourMap[r.hour] = r.orderCount })
      setPeakHours(Array.from({ length: 24 }, (_, h) => ({
        hour: `${h.toString().padStart(2, '0')}:00`,
        orders: hourMap[h] || 0,
      })))
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const pieData = summary ? [
    { name: 'Dine-in', value: summary.dineInOrders },
    { name: 'WhatsApp', value: summary.whatsappOrders },
  ] : []

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6 flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">Analytics</h1>
          <p className="text-gray-500 text-sm mt-0.5">Orders and revenue insights</p>
        </div>

        {/* Date range picker */}
        <div className="flex items-center gap-2">
          <input type="date" value={from} onChange={e => setFrom(e.target.value)}
            className="form-input text-sm py-2 px-3 w-auto" />
          <span className="text-gray-500 text-sm">to</span>
          <input type="date" value={to} onChange={e => setTo(e.target.value)}
            className="form-input text-sm py-2 px-3 w-auto" />
          <button onClick={load} disabled={loading} className="btn-primary text-sm py-2">
            {loading ? '…' : 'Apply'}
          </button>
        </div>
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total orders', value: summary.totalOrders },
            { label: 'Dine-in', value: summary.dineInOrders, color: 'text-blue-400' },
            { label: 'WhatsApp', value: summary.whatsappOrders, color: 'text-green-400' },
            { label: 'Revenue', value: `₹${Number(summary.totalRevenue).toFixed(0)}`, color: 'text-brand-400' },
          ].map(({ label, value, color = 'text-white' }) => (
            <div key={label} className="glass-card p-4">
              <p className="text-gray-500 text-xs mb-1">{label}</p>
              <p className={`text-2xl font-bold ${color}`}>{value}</p>
            </div>
          ))}
        </div>
      )}

      <div className="grid md:grid-cols-3 gap-4 mb-4">
        {/* Channel split pie */}
        <div className="glass-card p-5">
          <h2 className="text-sm font-semibold text-gray-400 mb-4">Channel Split</h2>
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={50} outerRadius={75}
                dataKey="value" label={({ name, percent }) => `${name} ${(percent*100).toFixed(0)}%`}
                labelLine={false} fontSize={11} fill="#8884d8">
                {pieData.map((_, i) => (
                  <Cell key={i} fill={i === 0 ? COLORS.DINE_IN : COLORS.WHATSAPP} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ background: '#1f2937', border: '1px solid #374151', borderRadius: '8px' }} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Top items */}
        <div className="glass-card p-5 md:col-span-2">
          <h2 className="text-sm font-semibold text-gray-400 mb-4">Top Selling Items</h2>
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={topItems} layout="vertical" margin={{ left: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#374151" horizontal={false} />
              <XAxis type="number" tick={{ fill: '#9ca3af', fontSize: 11 }} />
              <YAxis type="category" dataKey="itemName" width={100} tick={{ fill: '#9ca3af', fontSize: 11 }} />
              <Tooltip contentStyle={{ background: '#1f2937', border: '1px solid #374151', borderRadius: '8px' }} />
              <Bar dataKey="quantitySold" fill="#fc7c0f" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Peak hours */}
      <div className="glass-card p-5">
        <h2 className="text-sm font-semibold text-gray-400 mb-4">Peak Ordering Hours</h2>
        <ResponsiveContainer width="100%" height={180}>
          <BarChart data={peakHours}>
            <CartesianGrid strokeDasharray="3 3" stroke="#374151" vertical={false} />
            <XAxis dataKey="hour" tick={{ fill: '#9ca3af', fontSize: 10 }} interval={1} />
            <YAxis tick={{ fill: '#9ca3af', fontSize: 11 }} allowDecimals={false} />
            <Tooltip contentStyle={{ background: '#1f2937', border: '1px solid #374151', borderRadius: '8px' }} />
            <Bar dataKey="orders" fill="#3b82f6" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
