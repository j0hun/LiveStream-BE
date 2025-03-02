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
public class WebRTCP2PWebSocketHandler extends TextWebSocketHandler {

    // 각 방(roomId)에 연결된 WebSocket 세션 집합
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // 각 방의 방송자 세션 저장
    private final Map<String, WebSocketSession> broadcasters = new ConcurrentHashMap<>();
    // 각 세션이 속한 방의 roomId 매핑
    private final Map<WebSocketSession, String> sessionToRoom = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalMessage signalMessage = SignalMessage.fromJson(message.getPayload());
        String type = signalMessage.getType();
        String roomId = signalMessage.getRoomId();

        switch (type) {
            case "join":
                handleJoinMessage(roomId, session, signalMessage.getRole());
                break;
            case "signal":
                handleSignalMessage(roomId, signalMessage.getSignalData(), signalMessage.getViewerId(), session);
                break;
            default:
                log.error("알 수 없는 메시지 타입: {}", type);
                break;
        }
    }

    /**
     * 클라이언트가 방에 참여할 때 호출. 방송자와 시청자를 구분하여 처리한다.
     *
     * @param roomId  참여할 방 ID
     * @param session 클라이언트의 WebSocket 세션
     * @param role    클라이언트 역할 ("broadcaster" 또는 기타)
     */
    private void handleJoinMessage(String roomId, WebSocketSession session, String role) throws IOException {
        // 방에 세션 추가 및 매핑
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session, roomId);

        if ("broadcaster".equalsIgnoreCase(role)) {
            if (broadcasters.containsKey(roomId)) {
                sendErrorMessage(session, "이미 방송자가 있는 방입니다.", roomId);
                return;
            }
            broadcasters.put(roomId, session);
            log.info("방송자가 방에 등록되었습니다: {}", roomId);
        } else {
            WebSocketSession broadcaster = broadcasters.get(roomId);
            if (broadcaster != null && broadcaster.isOpen()) {
                sendMessage(broadcaster, Map.of(
                        "type", "newViewer",
                        "roomId", roomId,
                        "viewerId", session.getId()
                ));
                log.info("새로운 시청자가 방에 입장했습니다: {} (시청자 ID: {})", roomId, session.getId());
            }
        }
    }

    /**
     * WebRTC 시그널링 메시지를 전달한다.
     *
     * @param roomId     메시지 대상 방 ID
     * @param signalData 시그널 데이터 (offer, answer, candidate)
     * @param viewerId   메시지 대상 시청자 ID (필요한 경우)
     * @param sender     메시지 발신 세션
     */
    private void handleSignalMessage(String roomId, Object signalData, String viewerId, WebSocketSession sender) throws IOException {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null || roomSessions.isEmpty()) {
            log.error("해당 방({})에 연결된 세션이 없습니다.", roomId);
            return;
        }

        if (!(signalData instanceof Map)) {
            log.error("signalData 형식이 올바르지 않습니다: {}", signalData);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> signalMap = (Map<String, Object>) signalData;
        String signalType = (String) signalMap.get("type");

        Map<String, Object> forwardMessage = new ConcurrentHashMap<>();
        forwardMessage.put("type", "signal");
        forwardMessage.put("roomId", roomId);
        forwardMessage.put("signalData", signalData);

        WebSocketSession broadcaster = broadcasters.get(roomId);
        switch (signalType) {
            case "offer":
                if (viewerId == null) {
                    log.error("offer 메시지에 시청자 ID가 누락되었습니다.");
                    return;
                }
                forwardMessage.put("viewerId", viewerId);
                WebSocketSession targetViewer = roomSessions.stream()
                        .filter(sess -> !sess.equals(broadcaster) && sess.getId().equals(viewerId))
                        .findFirst()
                        .orElse(null);
                if (targetViewer != null && targetViewer.isOpen()) {
                    sendMessage(targetViewer, forwardMessage);
                    log.info("방송자에서 시청자({})로 offer 메시지를 전송했습니다.", viewerId);
                } else {
                    log.error("시청자 세션을 찾을 수 없습니다: {}", viewerId);
                }
                break;
            case "answer":
                forwardMessage.put("viewerId", sender.getId());
                if (broadcaster != null && broadcaster.isOpen()) {
                    sendMessage(broadcaster, forwardMessage);
                    log.info("시청자({})에서 방송자로 answer 메시지를 전송했습니다.", sender.getId());
                }
                break;
            case "candidate":
                if (sender.equals(broadcasters.get(roomId))) {
                    if (viewerId == null) {
                        log.error("방송자 candidate 메시지에 시청자 ID가 누락되었습니다.");
                        return;
                    }
                    forwardMessage.put("viewerId", viewerId);
                    WebSocketSession targetSession = roomSessions.stream()
                            .filter(sess -> !sess.equals(broadcaster) && sess.getId().equals(viewerId))
                            .findFirst()
                            .orElse(null);
                    if (targetSession != null && targetSession.isOpen()) {
                        sendMessage(targetSession, forwardMessage);
                        log.info("방송자에서 시청자({})로 candidate 메시지를 전송했습니다.", viewerId);
                    } else {
                        log.error("candidate 메시지용 시청자 세션을 찾을 수 없습니다: {}", viewerId);
                    }
                } else {
                    forwardMessage.put("viewerId", sender.getId());
                    if (broadcaster != null && broadcaster.isOpen()) {
                        sendMessage(broadcaster, forwardMessage);
                        log.info("시청자({})에서 방송자로 candidate 메시지를 전송했습니다.", sender.getId());
                    }
                }
                break;
            default:
                log.error("알 수 없는 signal 타입: {}", signalType);
                break;
        }
    }

    /**
     * WebSocket 연결 종료 시 후처리.
     * 연결이 종료된 세션이 속한 방에서 세션을 제거하고,
     * 만약 종료된 세션이 방송자라면, 모든 시청자에게 방송 종료 메시지를 전송한다.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.remove(session);
        if (roomId != null) {
            Set<WebSocketSession> sessions = rooms.getOrDefault(roomId, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            sessions.remove(session);
            if (session.equals(broadcasters.get(roomId))) {
                broadcasters.remove(roomId);
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
                log.info("방송이 종료되었습니다. 방: {}", roomId);
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> messageData) throws IOException {
        session.sendMessage(new TextMessage(SignalMessage.toJson(messageData)));
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage, String roomId) throws IOException {
        Map<String, Object> errorPayload = Map.of(
                "type", "error",
                "roomId", roomId,
                "message", errorMessage
        );
        session.sendMessage(new TextMessage(SignalMessage.toJson(errorPayload)));
    }
}
