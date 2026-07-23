import { useState, useEffect } from 'react'
import { menuApi } from '../../api'
import toast from 'react-hot-toast'
import { Plus, Pencil, Trash2, ToggleLeft, ToggleRight, ImageIcon, X, Check } from 'lucide-react'

function ItemRow({ item, onToggle, onEdit, onDelete }) {
  const [toggling, setToggling] = useState(false)

  const handleToggle = async () => {
    setToggling(true)
    try {
      await onToggle(item.id, !item.available)
    } finally {
      setToggling(false)
    }
  }

  return (
    <div className="flex items-center gap-3 py-3 px-4 hover:bg-gray-800/50 rounded-xl group transition-colors">
      {/* Image thumbnail */}
      <div className="w-10 h-10 rounded-lg bg-gray-800 flex-shrink-0 overflow-hidden">
        {item.imageUrl
          ? <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
          : <div className="w-full h-full flex items-center justify-center text-gray-600"><ImageIcon size={16} /></div>
        }
      </div>

      {/* Details */}
      <div className="flex-1 min-w-0">
        <p className={`font-medium ${item.available ? 'text-white' : 'text-gray-500 line-through'}`}>
          {item.name}
        </p>
        <p className="text-xs text-gray-500">{item.categoryName || 'Uncategorised'}</p>
      </div>

      <span className="font-semibold text-brand-400 text-sm">₹{Number(item.price).toFixed(0)}</span>

      {/* Availability toggle — most important action per UX brief */}
      <button id={`toggle-item-${item.id}`} onClick={handleToggle} disabled={toggling}
        title={item.available ? 'Mark unavailable' : 'Mark available'}
        className={`flex items-center gap-1 text-xs font-medium px-2.5 py-1 rounded-lg transition-colors
          ${item.available
            ? 'bg-green-500/15 text-green-400 hover:bg-green-500/25'
            : 'bg-gray-700 text-gray-500 hover:bg-gray-600'}`}>
        {item.available ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
        {item.available ? 'Available' : 'Out of stock'}
      </button>

      {/* Edit / delete (secondary) */}
      <div className="opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
        <button id={`edit-item-${item.id}`} onClick={() => onEdit(item)}
          className="p-1.5 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-gray-200">
          <Pencil size={14} />
        </button>
        <button id={`delete-item-${item.id}`} onClick={() => onDelete(item.id)}
          className="p-1.5 rounded-lg hover:bg-red-900/40 text-gray-500 hover:text-red-400">
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  )
}

function ItemModal({ item, categories, onSave, onClose }) {
  const [form, setForm] = useState(item || {
    name: '', description: '', price: '', categoryId: '', imageUrl: '', available: true
  })
  const [uploading, setUploading] = useState(false)
  const [saving, setSaving] = useState(false)

  const handleFile = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setUploading(true)
    try {
      const res = await menuApi.uploadImage(file)
      setForm(f => ({ ...f, imageUrl: res.data.imageUrl }))
      toast.success('Image uploaded')
    } catch { toast.error('Image upload failed') }
    finally { setUploading(false) }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await onSave({ ...form, price: parseFloat(form.price), categoryId: form.categoryId || null })
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-card w-full max-w-md p-6 shadow-2xl">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">{item ? 'Edit item' : 'Add menu item'}</h2>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300"><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Item name</label>
            <input className="form-input" placeholder="Paneer Tikka" required
              value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="form-label">Price (₹)</label>
              <input className="form-input" type="number" min="0" step="0.5" placeholder="180" required
                value={form.price} onChange={e => setForm(f => ({ ...f, price: e.target.value }))} />
            </div>
            <div>
              <label className="form-label">Category</label>
              <select className="form-input" value={form.categoryId || ''}
                onChange={e => setForm(f => ({ ...f, categoryId: e.target.value || null }))}>
                <option value="">— None —</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="form-label">Description (optional)</label>
            <input className="form-input" placeholder="Short description"
              value={form.description || ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          </div>
          <div>
            <label className="form-label">Image</label>
            {form.imageUrl && <img src={form.imageUrl} className="w-16 h-16 object-cover rounded-lg mb-2" />}
            <input type="file" accept="image/*" onChange={handleFile}
              className="text-sm text-gray-400 file:mr-3 file:btn-ghost file:border-0 file:py-1 file:px-3" />
            {uploading && <p className="text-xs text-gray-500 mt-1">Uploading…</p>}
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="item-available" checked={form.available}
              onChange={e => setForm(f => ({ ...f, available: e.target.checked }))}
              className="w-4 h-4 rounded accent-brand-500" />
            <label htmlFor="item-available" className="text-sm text-gray-400">Available</label>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Cancel</button>
            <button type="submit" disabled={saving} className="btn-primary flex-1">
              {saving ? 'Saving…' : 'Save item'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default function MenuManagement() {
  const [categories, setCategories] = useState([])
  const [items, setItems] = useState([])
  const [activeTab, setActiveTab] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [noRestaurant, setNoRestaurant] = useState(false)
  const [newCatName, setNewCatName] = useState('')
  const [addingCat, setAddingCat] = useState(false)

  const load = async () => {
    try {
      const [cats, its] = await Promise.all([menuApi.getCategories(), menuApi.getMenuItems()])
      setCategories(cats.data)
      setItems(its.data)
      if (!activeTab && cats.data.length > 0) setActiveTab(cats.data[0].id)
    } catch (e) {
      if (e.response?.status === 404 || e.response?.status === 403) setNoRestaurant(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const filteredItems = activeTab
    ? items.filter(i => (i.categoryId === activeTab) || (activeTab === 'none' && !i.categoryId))
    : items

  const handleSave = async (data) => {
    try {
      if (editItem) {
        await menuApi.updateMenuItem(editItem.id, data)
        toast.success('Item updated')
      } else {
        await menuApi.createMenuItem(data)
        toast.success('Item added')
      }
      setShowModal(false); setEditItem(null); load()
    } catch (e) { toast.error(e.response?.data?.message || 'Save failed') }
  }

  const handleToggle = async (id, available) => {
    await menuApi.toggleAvailability(id, available)
    setItems(prev => prev.map(i => i.id === id ? { ...i, available } : i))
    toast.success(available ? 'Item marked available' : 'Item marked out of stock')
  }

  const handleDeleteCategory = async (id) => {
    if (!confirm('Delete this category? Items will become uncategorised.')) return
    try {
      await menuApi.deleteCategory(id)
      if (activeTab === id) setActiveTab(null)
      load()
      toast.success('Category deleted')
    } catch { toast.error('Failed to delete category') }
  }

  const handleAddCategory = async (e) => {
    e.preventDefault()
    if (!newCatName.trim()) return
    try {
      await menuApi.createCategory({ name: newCatName.trim() })
      setNewCatName('')
      setAddingCat(false)
      load()
      toast.success('Category added!')
    } catch { toast.error('Failed to add category') }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this item?')) return
    await menuApi.deleteMenuItem(id)
    setItems(prev => prev.filter(i => i.id !== id))
    toast.success('Item deleted')
  }

  if (loading) return <div className="p-6 text-gray-500">Loading menu…</div>

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
          <h1 className="text-2xl font-bold text-white">Menu Management</h1>
          <p className="text-gray-500 text-sm mt-0.5">{items.length} items across {categories.length} categories</p>
        </div>
        <button id="add-item-btn" onClick={() => { setEditItem(null); setShowModal(true) }} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Add item
        </button>
      </div>

      {/* Category tabs + add category */}
      <div className="flex gap-2 mb-4 overflow-x-auto pb-1 items-center">
        <button onClick={() => setActiveTab(null)}
          className={`px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-colors
            ${activeTab === null ? 'bg-brand-500 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'}`}>
          All ({items.length})
        </button>
        {categories.map(c => (
          <button key={c.id} onClick={() => setActiveTab(c.id)}
            className={`px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-colors
              ${activeTab === c.id ? 'bg-brand-500 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'}`}>
            {c.name} ({items.filter(i => i.categoryId === c.id).length})
          </button>
        ))}

        {/* Inline add category */}
        {addingCat ? (
          <form onSubmit={handleAddCategory} className="flex gap-1">
            <input
              autoFocus
              value={newCatName}
              onChange={e => setNewCatName(e.target.value)}
              placeholder="Category name"
              className="bg-gray-800 border border-gray-600 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-brand-500 w-36"
            />
            <button type="submit" className="bg-brand-500 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-brand-600">Add</button>
            <button type="button" onClick={() => { setAddingCat(false); setNewCatName('') }} className="bg-gray-800 text-gray-400 px-3 py-1.5 rounded-lg text-sm hover:bg-gray-700">✕</button>
          </form>
        ) : (
          <button
            id="add-category-btn"
            onClick={() => setAddingCat(true)}
            className="px-3 py-2 rounded-xl text-sm font-medium whitespace-nowrap text-gray-500 border border-dashed border-gray-700 hover:border-brand-500 hover:text-brand-400 transition-colors"
          >
            + Category
          </button>
        )}
      </div>

      {/* Items list */}
      <div className="glass-card divide-y divide-gray-800">
        {filteredItems.length === 0
          ? <p className="p-8 text-center text-gray-500">No items in this category</p>
          : filteredItems.map(item => (
            <ItemRow key={item.id} item={item}
              onToggle={handleToggle} onEdit={i => { setEditItem(i); setShowModal(true) }}
              onDelete={handleDelete} />
          ))
        }
      </div>

      {showModal && (
        <ItemModal item={editItem} categories={categories} onSave={handleSave}
          onClose={() => { setShowModal(false); setEditItem(null) }} />
      )}
    </div>
  )
}
