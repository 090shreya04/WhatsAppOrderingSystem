import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'
import { useOrderStore } from '../store/orderStore'
import toast from 'react-hot-toast'

/**
 * Connects to the STOMP WebSocket and subscribes to the restaurant's order topic.
 * Call once from the Dashboard page.
 */
export function useOrderWebSocket(restaurantId) {
  const clientRef = useRef(null)
  const addOrder = useOrderStore(s => s.addOrder)
  const updateOrderStatus = useOrderStore(s => s.updateOrderStatus)
  const token = useAuthStore(s => s.token)

  useEffect(() => {
    if (!restaurantId || !token) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected')
        client.subscribe(
          `/topic/restaurant/${restaurantId}/orders`,
          (message) => {
            const event = JSON.parse(message.body)
            if (event.type === 'ORDER_CREATED') {
              addOrder(event)
              toast.success(
                event.channel === 'DINE_IN'
                  ? `🍽️ New dine-in order — Table ${event.tableNumber}`
                  : `📱 New WhatsApp order!`,
                { duration: 5000 }
              )
            } else if (event.type === 'ORDER_STATUS_CHANGED') {
              updateOrderStatus(event.orderId, event.status)
            }
          }
        )
      },
      onStompError: (frame) => console.error('STOMP error:', frame),
      onDisconnect: () => console.log('WebSocket disconnected'),
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
    }
  }, [restaurantId, token])
}
