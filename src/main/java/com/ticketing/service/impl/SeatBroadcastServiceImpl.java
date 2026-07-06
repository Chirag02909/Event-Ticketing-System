package com.ticketing.service.impl;

import com.ticketing.dto.websocket.SeatUpdateMessage;
import com.ticketing.model.SeatVO;
import com.ticketing.service.interfaces.SeatBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes seat status changes to the STOMP topic for an event.
 *
 * SimpMessagingTemplate is Spring's class for sending messages to
 * WebSocket destinations from regular Java code (outside a @MessageMapping
 * controller). It is auto-configured once @EnableWebSocketMessageBroker
 * is active (done in WebSocketConfig).
 *
 * TOPIC NAMING CONVENTION:
 *   /topic/event/{eventId}/seats
 *
 * One topic per event. A client viewing event 42's seat map subscribes
 * to /topic/event/42/seats and receives ONLY updates for that event —
 * never updates for other events happening elsewhere on the platform.
 */
@Service
public class SeatBroadcastServiceImpl implements SeatBroadcastService {

    private static final String TOPIC_PREFIX = "/topic/event/";
    private static final String TOPIC_SUFFIX = "/seats";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastSeatUpdate(SeatVO seat, String eventType) {
        SeatUpdateMessage message = new SeatUpdateMessage(
            seat.getId(),
            seat.getSeatNumber(),
            seat.getStatus(),
            eventType,
            seat.getEventId()
        );

        String destination = TOPIC_PREFIX + seat.getEventId() + TOPIC_SUFFIX;

        // convertAndSend serializes the message to JSON automatically
        // and pushes it to every client subscribed to this destination.
        // If zero clients are subscribed, this is a harmless no-op.
        messagingTemplate.convertAndSend(destination, message);
    }
}