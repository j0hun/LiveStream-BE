package com.jyhun.LiveStream.config;

import com.jyhun.LiveStream.dto.SignalMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> rooms = new HashMap<>();
    private final Map<String, WebSocketSession> broadcasters = new HashMap<>();
    private final Map<WebSocketSession, String> sessionToRoom = new HashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalMessage signalMessage = SignalMessage.fromJson(message.getPayload());
        switch (signalMessage.getType()) {
            case "join":
                joinRoom(signalMessage.getRoomId(), session, signalMessage.getRole());
                break;
            case "signal":
                forwardSignal(signalMessage.getRoomId(), signalMessage.getSignalData(), session);
                break;
            default:
                break;
        }
    }

    private void joinRoom(String roomId, WebSocketSession session, String role) throws IOException {
        rooms.computeIfAbsent(roomId, k -> new HashSet<>()).add(session);
        sessionToRoom.put(session, roomId);
        if ("broadcaster".equals(role)) {
            // 방송자 등록
            if (broadcasters.containsKey(roomId)) {
                // 이미 방송자가 있으면 거부하거나 적절한 처리가 필요
                session.sendMessage(new TextMessage(SignalMessage.toJson(Map.of("type", "error", "message", "Room already has a broadcaster"))));
                return;
            }
            broadcasters.put(roomId, session);
            System.out.println("📢 방송자 등록: " + roomId);
        } else {
            // 시청자 등록
            WebSocketSession broadcaster = broadcasters.get(roomId);
            if (broadcaster != null && broadcaster.isOpen()) {
                // 방송자가 있다면 시청자에게 방송 시작 메시지 전송
                broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                        "type", "newViewer",
                        "viewerId", session.getId()
                ))));
                System.out.println("👀 새로운 시청자 입장: " + roomId);
            }
        }
    }

    private void forwardSignal(String roomId, Object signalData, WebSocketSession sender) throws IOException {
        WebSocketSession broadcaster = broadcasters.get(roomId);

        if (broadcaster == null || !broadcaster.isOpen()) {
            return;
        }

        if (signalData instanceof Map) {
            String signalType = (String) ((Map<?, ?>) signalData).get("type");

            if ("offer".equals(signalType)) {
                // 방송자가 시청자에게 offer 전송
                sender.sendMessage(new TextMessage(SignalMessage.toJson(signalData)));
                System.out.println("📡 방송자가 offer 전송 -> 시청자");
            } else if ("answer".equals(signalType)) {
                // 시청자가 방송자에게 answer 전송
                broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(signalData)));
                System.out.println("📡 시청자가 answer 전송 -> 방송자");
            } else if ("candidate".equals(signalType)) {
                // ICE Candidate 교환
                sender.sendMessage(new TextMessage(SignalMessage.toJson(signalData)));
                System.out.println("❄ ICE Candidate 교환 완료");
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.get(session);
        if (roomId != null) {
            rooms.getOrDefault(roomId, Collections.emptySet()).remove(session);
            sessionToRoom.remove(session);

            if (broadcasters.get(roomId) == session) {
                broadcasters.remove(roomId);
                // 방송 종료 시 모든 시청자에게 방송 종료 알림 전송
                for (WebSocketSession viewer : rooms.getOrDefault(roomId, Collections.emptySet())) {
                    if (viewer.isOpen()) {
                        viewer.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                                "type", "broadcastEnded",
                                "message", "Broadcast has ended"
                        ))));
                    }
                }
                System.out.println("🚫 방송 종료: " + roomId);
            }
        }
    }
}
