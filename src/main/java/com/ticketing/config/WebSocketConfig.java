package com.ticketing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the WebSocket + STOMP messaging infrastructure.
 *
 * TWO THINGS THIS CLASS SETS UP:
 *
 * 1. THE HANDSHAKE ENDPOINT (/ws)
 *    Browsers connect here first to upgrade from HTTP to WebSocket.
 *    withSockJS() adds a fallback for browsers/networks that block
 *    raw WebSocket — SockJS transparently falls back to HTTP long-polling
 *    while keeping the same client-side API.
 *
 * 2. THE MESSAGE BROKER (topics)
 *    enableSimpleBroker("/topic") activates Spring's built-in in-memory
 *    broker for any destination starting with /topic. This is sufficient
 *    for a single-server deployment. For multi-server horizontal scaling,
 *    you would swap this for a full STOMP broker relay (e.g. RabbitMQ) —
 *    worth mentioning in interviews as the next scaling step.
 *
 * setApplicationDestinationPrefixes("/app") is not used in this chunk
 * since clients only SUBSCRIBE (receive), they never SEND messages
 * through the app — all seat changes originate from REST API calls,
 * not from WebSocket messages sent by clients.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")   // restrict to your frontend domain in production
            .withSockJS();                   // fallback for restrictive networks/browsers
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Activates the in-memory broker for all /topic/** destinations.
        // SeatBroadcastServiceImpl publishes here; clients subscribe here.
        registry.enableSimpleBroker("/topic");
    }
}