package com.jyhun.LiveStream.config;

import com.jyhun.LiveStream.dto.SignalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> broadcasters = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionToRoom = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalMessage signalMessage = SignalMessage.fromJson(message.getPayload());
        log.info(signalMessage.getRole());
        switch (signalMessage.getType()) {
            case "join":
                joinRoom(signalMessage.getRoomId(), session, signalMessage.getRole());
                break;
            case "signal":
                forwardSignal(signalMessage.getRoomId(), signalMessage.getSignalData(), session);
                break;
            default:
                log.error("알 수 없는 메시지 타입: {}", signalMessage.getType());
                break;
        }
    }

    private void joinRoom(String roomId, WebSocketSession session, String role) throws IOException {
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session, roomId);

        if ("broadcaster".equals(role)) {
            // 이미 방송자가 있으면 에러 메시지 전송
            if (broadcasters.containsKey(roomId)) {
                session.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                        "type", "error",
                        "message", "이미 방송자가 있는 방입니다."
                ))));
                return;
            }
            broadcasters.put(roomId, session);
            log.info("📢 방송자 등록: {}", roomId);
        } else {
            // 시청자 등록: 방송자가 있다면 방송자에게 새 시청자 알림 전송
            WebSocketSession broadcaster = broadcasters.get(roomId);
            if (broadcaster != null && broadcaster.isOpen()) {
                broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                        "type", "newViewer",
                        "roomId", roomId,
                        "viewerId", session.getId()
                ))));
                log.info("👀 새로운 시청자 입장: {}", roomId);
            }
        }
    }

    private void forwardSignal(String roomId, Object signalData, WebSocketSession sender) throws IOException {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null || roomSessions.isEmpty()) {
            log.error("해당 roomId에 세션이 없습니다: {}", roomId);
            return;
        }
        if (signalData instanceof Map) {
            Map<String, Object> signalMap = (Map<String, Object>) signalData;
            String signalType = (String) signalMap.get("type");

            String forwardPayload = SignalMessage.toJson(Map.of(
                    "type", "signal",
                    "roomId", roomId,
                    "signalData", signalData
            ));

            WebSocketSession broadcaster = broadcasters.get(roomId);
            if ("offer".equals(signalType)) {
                // 방송자가 보낸 offer를 방송자가 아닌 모든 시청자에게 전달
                for (WebSocketSession session : roomSessions) {
                    if (broadcaster != null && !session.getId().equals(broadcaster.getId()) && session.isOpen()) {
                        session.sendMessage(new TextMessage(forwardPayload));
                        log.info("📡 방송자가 offer 전송 -> 시청자: {}", session.getId());
                    }
                }
            } else if ("answer".equals(signalType)) {
                if (broadcaster != null && broadcaster.isOpen()) {
                    broadcaster.sendMessage(new TextMessage(forwardPayload));
                    log.info("📡 시청자가 answer 전송 -> 방송자");
                }
            } else if ("candidate".equals(signalType)) {
                if (broadcaster == null) {
                    log.error("방송자가 없는 roomId: {}", roomId);
                    return;
                }
                if (sender.getId().equals(broadcaster.getId())) {
                    // 방송자에서 보낸 candidate -> 모든 시청자에게
                    for (WebSocketSession session : roomSessions) {
                        if (!session.getId().equals(broadcasters.get(roomId).getId()) && session.isOpen()) {
                            session.sendMessage(new TextMessage(forwardPayload));
                        }
                    }
                    log.info("❄ 방송자 candidate 전송 -> 시청자");
                } else {
                    // 시청자에서 보낸 candidate -> 방송자에게 전달
                    if (broadcaster.isOpen()) {
                        broadcaster.sendMessage(new TextMessage(forwardPayload));
                    }
                    log.info("❄ 시청자 candidate 전송 -> 방송자");
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.get(session);
        if (roomId != null) {
            Set<WebSocketSession> sessions = rooms.getOrDefault(roomId, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            sessions.remove(session);
            sessionToRoom.remove(session);

            if (broadcasters.get(roomId) == session) {
                broadcasters.remove(roomId);
                // 방송 종료 시 모든 시청자에게 방송 종료 알림 전송
                String endPayload = SignalMessage.toJson(Map.of(
                        "type", "broadcastEnded",
                        "roomId", roomId,
                        "message", "방송이 종료되었습니다."
                ));
                for (WebSocketSession viewer : sessions) {
                    if (viewer.isOpen()) {
                        viewer.sendMessage(new TextMessage(endPayload));
                    }
                }
                log.info("🚫 방송 종료: " + roomId);
            }
        }
    }
}
