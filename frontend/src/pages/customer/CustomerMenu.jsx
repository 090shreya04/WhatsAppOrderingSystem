import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { publicMenuApi, orderApi } from '../../api'
import toast from 'react-hot-toast'
import { Plus, Minus, ShoppingCart, ImageIcon, X } from 'lucide-react'

function ItemCard({ item, quantity, onAdd, onRemove, onSetQty }) {
  return (
    <div className="flex items-center gap-3 py-3 border-b border-gray-800/50 last:border-0">
      {/* Image */}
      <div className="w-16 h-16 rounded-xl bg-gray-800 flex-shrink-0 overflow-hidden">
        {item.imageUrl
          ? <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
          : <div className="w-full h-full flex items-center justify-center text-gray-600">
              <ImageIcon size={20} />
            </div>
        }
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-white text-sm">{item.name}</p>
        {item.description && <p className="text-gray-500 text-xs mt-0.5 truncate">{item.description}</p>}
        <p className="text-brand-400 font-bold text-sm mt-1">₹{Number(item.price).toFixed(0)}</p>
      </div>

      {/* Qty control */}
      <div className="flex items-center gap-1 flex-shrink-0">
        {quantity > 0 ? (
          <>
            <button id={`remove-${item.id}`} onClick={() => onRemove(item)}
              className="w-7 h-7 rounded-full bg-gray-800 flex items-center justify-center text-gray-300 hover:bg-gray-700">
              <Minus size={14} />
            </button>
            <input
              type="number"
              value={quantity}
              onChange={(e) => onSetQty(item, parseInt(e.target.value) || 0)}
              className="w-9 h-7 bg-gray-900 border border-gray-700 rounded text-center text-white text-sm font-semibold focus:outline-none focus:border-brand-500"
            />
          </>
        ) : <div className="w-14" />}
        <button id={`add-${item.id}`} onClick={() => onAdd(item)}
          className="w-7 h-7 rounded-full bg-brand-500 flex items-center justify-center text-white hover:bg-brand-600">
          <Plus size={14} />
        </button>
      </div>
    </div>
  )
}

export default function CustomerMenu() {
  const { qrSecret, tableId } = useParams()
  const navigate = useNavigate()
  const [menu, setMenu] = useState(null)
  const [cart, setCart] = useState({})   // { menuItemId: { item, quantity } }
  const [loading, setLoading] = useState(true)
  const [placing, setPlacing] = useState(false)
  const [showCart, setShowCart] = useState(false)

  useEffect(() => {
    publicMenuApi.getMenu(qrSecret, tableId)
      .then(res => setMenu(res.data))
      .catch(() => toast.error('Menu not found'))
      .finally(() => setLoading(false))
  }, [qrSecret, tableId])

  const addItem = (item) => setCart(c => ({
    ...c,
    [item.id]: { item, quantity: (c[item.id]?.quantity || 0) + 1 }
  }))

  const removeItem = (item) => setCart(c => {
    const curr = c[item.id]?.quantity || 0
    if (curr <= 1) { const n = { ...c }; delete n[item.id]; return n }
    return { ...c, [item.id]: { item, quantity: curr - 1 } }
  })

  const setQty = (item, qty) => setCart(c => {
    if (!qty || qty <= 0) { const n = { ...c }; delete n[item.id]; return n }
    return { ...c, [item.id]: { item, quantity: Math.min(qty, 99) } }
  })

  const cartItems = Object.values(cart)
  const cartTotal = cartItems.reduce((s, { item, quantity }) => s + Number(item.price) * quantity, 0)
  const cartCount = cartItems.reduce((s, { quantity }) => s + quantity, 0)

  const placeOrder = async () => {
    if (cartItems.length === 0) return
    setPlacing(true)
    try {
      const res = await orderApi.placeOrder({
        qrSecret,
        tableId: Number(tableId),
        items: cartItems.map(({ item, quantity }) => ({ menuItemId: item.id, quantity })),
      })
      toast.success('Order placed! 🎉')
      navigate(`/order/${qrSecret}/${tableId}/status/${res.data.id}`)
    } catch (e) {
      toast.error(e.response?.data?.message || 'Order failed')
    } finally {
      setPlacing(false)
    }
  }

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center text-gray-500">
      Loading menu…
    </div>
  )
  if (!menu) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center text-red-400">
      Invalid QR code
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-950">
      {/* Header */}
      <div className="sticky top-0 z-20 bg-gray-950/90 backdrop-blur border-b border-gray-800 px-4 py-4">
        <h1 className="text-xl font-bold text-white">{menu.restaurantName}</h1>
        <p className="text-gray-500 text-sm">Table {menu.tableNumber}</p>
      </div>

      {/* Menu */}
      <div className="px-4 pb-32">
        {menu.categories.map(cat => (
          <div key={cat.id || 'other'} className="mt-6">
            <h2 className="text-xs font-bold text-gray-500 uppercase tracking-widest mb-2">{cat.name}</h2>
            {cat.items.map(item => (
              <ItemCard key={item.id} item={item}
                quantity={cart[item.id]?.quantity || 0}
                onAdd={addItem} onRemove={removeItem} onSetQty={setQty} />
            ))}
          </div>
        ))}
      </div>

      {/* Cart bar */}
      {cartCount > 0 && (
        <div className="fixed bottom-0 left-0 right-0 p-4 bg-gray-950/95 backdrop-blur border-t border-gray-800">
          {showCart && (
            <div className="glass-card p-4 mb-3 max-h-60 overflow-y-auto">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold text-white">Your order</h3>
                <button onClick={() => setShowCart(false)} className="text-gray-500"><X size={16} /></button>
              </div>
              {cartItems.map(({ item, quantity }) => (
                <div key={item.id} className="flex justify-between text-sm py-1.5 border-b border-gray-800/50 last:border-0">
                  <span className="text-gray-300">{item.name} × {quantity}</span>
                  <span className="text-brand-400 font-medium">₹{(Number(item.price) * quantity).toFixed(0)}</span>
                </div>
              ))}
              <div className="flex justify-between font-bold text-white mt-3 pt-2 border-t border-gray-700">
                <span>Total</span>
                <span className="text-brand-400">₹{cartTotal.toFixed(0)}</span>
              </div>
            </div>
          )}
          <button id="place-order-btn" onClick={placeOrder} disabled={placing}
            className="btn-primary w-full flex items-center justify-between"
            onContextMenu={e => { e.preventDefault(); setShowCart(!showCart) }}>
            <div className="flex items-center gap-2">
              <ShoppingCart size={18} />
              <span>{cartCount} item{cartCount > 1 ? 's' : ''}</span>
            </div>
            <span>{placing ? 'Placing order…' : `Place order • ₹${cartTotal.toFixed(0)}`}</span>
          </button>
          <p className="text-center text-gray-600 text-xs mt-2">Long-press to review cart</p>
        </div>
      )}
    </div>
  )
}
