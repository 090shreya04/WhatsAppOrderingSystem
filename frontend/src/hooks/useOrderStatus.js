import { useEffect, useState } from 'react'
import { orderApi } from '../api'

/** Polls order status every 5 seconds for customer-facing tracking page */
export function useOrderStatus(orderId) {
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!orderId) return

    let stopped = false

    const poll = async () => {
      try {
        const res = await orderApi.getStatus(orderId)
        if (!stopped) setStatus(res.data)
      } catch (e) {
        console.error('Status poll failed:', e)
      } finally {
        if (!stopped) setLoading(false)
      }
    }

    poll()
    const interval = setInterval(poll, 5000)
    return () => { stopped = true; clearInterval(interval) }
  }, [orderId])

  return { status, loading }
}
