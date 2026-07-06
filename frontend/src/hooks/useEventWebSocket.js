import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Custom hook to subscribe to real-time seat updates for a specific event.
 * Uses @stomp/stompjs Client API over SockJS.
 * 
 * @param {number|string} eventId - The ID of the event to watch.
 * @param {function} onSeatUpdate - Callback triggered when a seat update message is received.
 */
export const useEventWebSocket = (eventId, onSeatUpdate) => {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!eventId) return;

    // Create STOMP client using the modern Client class (not Stomp.over)
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => {
        console.log('[STOMP] ', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
      console.log('STOMP connected successfully to event: ' + eventId);
      
      // Subscribe to the event-specific seat topic
      stompClient.subscribe(`/topic/event/${eventId}/seats`, (message) => {
        if (message.body) {
          try {
            const seatUpdateMessage = JSON.parse(message.body);
            console.log('Received seat update via WebSocket:', seatUpdateMessage);
            onSeatUpdate(seatUpdateMessage);
          } catch (err) {
            console.error('Error parsing WebSocket message body:', err);
          }
        }
      });
    };

    stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    stompClient.onDisconnect = () => {
      console.log('STOMP disconnected from event: ' + eventId);
    };

    stompClient.activate();
    clientRef.current = stompClient;

    // Cleanup connection on unmount or eventId change
    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [eventId, onSeatUpdate]);

  return clientRef.current;
};
export default useEventWebSocket;
