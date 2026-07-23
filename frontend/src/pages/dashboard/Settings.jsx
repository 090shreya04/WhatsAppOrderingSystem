import { useEffect, useState } from 'react'
import { restaurantApi } from '../../api'
import toast from 'react-hot-toast'
import { Save, Store, Phone, Globe, MessageCircle, RefreshCw } from 'lucide-react'

export default function Settings() {
  const [loading, setLoading]   = useState(true)
  const [saving,  setSaving]    = useState(false)
  const [form,    setForm]      = useState({
    name:           '',
    whatsappNumber: '',
    description:    '',
    address:        '',
  })

  useEffect(() => {
    restaurantApi.getMe()
      .then(r => {
        const d = r.data
        setForm({
          name:           d.name           || '',
          whatsappNumber: d.whatsappNumber || '',
          description:    d.description   || '',
          address:        d.address        || '',
        })
      })
      .catch(() => {
        // No restaurant yet — that's OK, we'll create one on save
      })
      .finally(() => setLoading(false))
  }, [])

  const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSave = async e => {
    e.preventDefault()
    if (!form.name.trim()) { toast.error('Restaurant name is required'); return }
    setSaving(true)
    try {
      // Try update first; if 404 then create
      try {
        await restaurantApi.update(form)
      } catch (err) {
        if (err.response?.status === 404) {
          await restaurantApi.create(form)
        } else throw err
      }
      toast.success('Settings saved!')
    } catch {
      toast.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center h-full text-gray-500">Loading…</div>
  )

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-white">Restaurant Settings</h1>
        <p className="text-gray-500 text-sm mt-1">Configure your restaurant profile and WhatsApp integration</p>
      </div>

      <form onSubmit={handleSave} className="space-y-6">

        {/* Restaurant Info */}
        <div className="glass-card p-5">
          <div className="flex items-center gap-2 mb-4">
            <Store size={16} className="text-brand-400" />
            <h2 className="text-white font-semibold">Restaurant Profile</h2>
          </div>
          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-400 mb-1">Restaurant Name *</label>
              <input
                id="settings-name"
                name="name"
                value={form.name}
                onChange={handleChange}
                placeholder="e.g. Jasper's Kitchen"
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm text-gray-400 mb-1">Description</label>
              <textarea
                id="settings-description"
                name="description"
                value={form.description}
                onChange={handleChange}
                placeholder="A brief description of your restaurant"
                rows={3}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500 resize-none"
              />
            </div>
            <div>
              <label className="block text-sm text-gray-400 mb-1">Address</label>
              <input
                id="settings-address"
                name="address"
                value={form.address}
                onChange={handleChange}
                placeholder="123 Main St, City"
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-brand-500"
              />
            </div>
          </div>
        </div>

        {/* WhatsApp Integration */}
        <div className="glass-card p-5">
          <div className="flex items-center gap-2 mb-1">
            <MessageCircle size={16} className="text-green-400" />
            <h2 className="text-white font-semibold">WhatsApp Integration</h2>
          </div>
          <p className="text-xs text-gray-500 mb-4">
            Enter the WhatsApp number assigned to your restaurant in the Meta Developer Console.
            This links incoming WhatsApp messages to your restaurant.
          </p>
          <div>
            <label className="block text-sm text-gray-400 mb-1">WhatsApp Phone Number</label>
            <div className="relative">
              <Phone size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
              <input
                id="settings-whatsapp"
                name="whatsappNumber"
                value={form.whatsappNumber}
                onChange={handleChange}
                placeholder="15551828527  (digits only, no + or spaces)"
                className="w-full bg-gray-800 border border-gray-700 rounded-lg pl-9 pr-3 py-2 text-white text-sm focus:outline-none focus:border-green-500 font-mono"
              />
            </div>
            <p className="text-xs text-gray-600 mt-1">
              Found in Meta Developer Console → WhatsApp → API Setup → Phone Number ID row.
              Use the test number <span className="text-gray-400 font-mono">15551828527</span> for development.
            </p>
          </div>
        </div>

        <div className="sticky bottom-0 pt-4 pb-2 bg-gray-950">
        <button
          id="settings-save-btn"
          type="submit"
          disabled={saving}
          className="flex items-center gap-2 bg-brand-500 hover:bg-brand-600 disabled:opacity-50 text-white px-6 py-3 rounded-lg font-semibold text-sm transition-colors w-full justify-center"
        >
          {saving ? <RefreshCw size={16} className="animate-spin" /> : <Save size={16} />}
          {saving ? 'Saving…' : '💾 Save Settings'}
        </button>
      </div>
      </form>
    </div>
  )
}
