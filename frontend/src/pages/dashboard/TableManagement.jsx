import { useState, useEffect } from 'react'
import { tableApi } from '../../api'
import toast from 'react-hot-toast'
import { Plus, QrCode, Download, Circle } from 'lucide-react'

function TableCard({ table, onStatusToggle }) {
  const isOccupied = table.status === 'OCCUPIED'

  const handleDownloadQr = () => {
    const token = JSON.parse(sessionStorage.getItem('auth-storage') || '{}')?.state?.token || ''
    const link = document.createElement('a')
    const urlWithParam = `${tableApi.getQrUrl(table.id)}?frontendUrl=${encodeURIComponent(window.location.origin)}`
    link.href = urlWithParam
    // Attach auth header via fetch
    fetch(urlWithParam, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.blob())
      .then(blob => {
        const url = URL.createObjectURL(blob)
        link.href = url
        link.download = `table-${table.tableNumber}-qr.png`
        link.click()
        URL.revokeObjectURL(url)
      })
      .catch(() => toast.error('QR download failed'))
  }

  return (
    <div className={`glass-card p-5 flex flex-col items-center gap-3 transition-all
      ${isOccupied ? 'border-orange-500/30' : 'border-gray-800'}`}>
      {/* Table icon */}
      <div className={`w-16 h-16 rounded-2xl flex items-center justify-center text-2xl font-bold
        ${isOccupied ? 'bg-orange-500/20 text-orange-400' : 'bg-gray-800 text-gray-400'}`}>
        {table.tableNumber}
      </div>

      {/* Status */}
      <button id={`toggle-table-${table.id}`}
        onClick={() => onStatusToggle(table.id, isOccupied ? 'FREE' : 'OCCUPIED')}
        className={`flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full transition-colors
          ${isOccupied
            ? 'bg-orange-500/15 text-orange-400 border border-orange-500/20 hover:bg-orange-500/25'
            : 'bg-green-500/15 text-green-400 border border-green-500/20 hover:bg-green-500/25'}`}>
        <Circle size={6} className="fill-current" />
        {isOccupied ? 'Occupied' : 'Free'}
      </button>

      {/* QR download */}
      <button id={`download-qr-${table.id}`} onClick={handleDownloadQr}
        className="btn-ghost flex items-center gap-1.5 text-xs w-full justify-center py-2">
        <Download size={13} /> Download QR
      </button>
    </div>
  )
}

export default function TableManagement() {
  const [tables, setTables] = useState([])
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [newNumber, setNewNumber] = useState('')
  const [noRestaurant, setNoRestaurant] = useState(false)

  const load = async () => {
    try {
      const res = await tableApi.getTables()
      setTables(res.data)
    } catch (e) {
      if (e.response?.status === 404 || e.response?.status === 403) setNoRestaurant(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleAdd = async (e) => {
    e.preventDefault()
    try {
      await tableApi.createTable({ tableNumber: newNumber.trim() })
      toast.success(`Table ${newNumber} added`)
      setNewNumber('')
      setAdding(false)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to add table')
    }
  }

  const handleStatusToggle = async (id, status) => {
    await tableApi.updateStatus(id, status)
    setTables(prev => prev.map(t => t.id === id ? { ...t, status } : t))
    toast.success(`Table marked ${status.toLowerCase()}`)
  }

  if (loading) return <div className="p-6 text-gray-500">Loading tables…</div>

  if (noRestaurant) return (
    <div className="p-6 flex flex-col items-center justify-center h-64 text-center">
      <p className="text-gray-400 text-lg font-medium mb-2">Restaurant not set up yet</p>
      <p className="text-gray-600 text-sm mb-4">Go to <strong className="text-brand-400">Settings</strong> and save your restaurant profile first.</p>
      <a href="/dashboard/settings" className="btn-primary px-4 py-2 rounded-lg text-sm">Go to Settings →</a>
    </div>
  )

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">Table Management</h1>
          <p className="text-gray-500 text-sm mt-0.5">
            {tables.filter(t => t.status === 'OCCUPIED').length} occupied · {tables.filter(t => t.status === 'FREE').length} free
          </p>
        </div>
        <button id="add-table-btn" onClick={() => setAdding(!adding)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Add table
        </button>
      </div>

      {/* Add table form */}
      {adding && (
        <div className="glass-card p-4 mb-6 flex items-center gap-3">
          <form onSubmit={handleAdd} className="flex gap-3 flex-1">
            <input className="form-input flex-1" placeholder="Table number (e.g. 7 or A1)"
              value={newNumber} onChange={e => setNewNumber(e.target.value)} required autoFocus />
            <button type="submit" className="btn-primary">Add</button>
            <button type="button" onClick={() => setAdding(false)} className="btn-ghost">Cancel</button>
          </form>
        </div>
      )}

      {/* Table grid */}
      {tables.length === 0 ? (
        <div className="glass-card p-12 text-center">
          <QrCode size={40} className="text-gray-700 mx-auto mb-3" />
          <p className="text-gray-500">No tables yet. Add your first table.</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {tables.map(table => (
            <TableCard key={table.id} table={table} onStatusToggle={handleStatusToggle} />
          ))}
        </div>
      )}
    </div>
  )
}
