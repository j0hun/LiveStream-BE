package com.jyhun.LiveStream.config;

import com.jyhun.LiveStream.dto.SignalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//@Component
@Slf4j
public class WebRTCP2PWebSocketHandler extends TextWebSocketHandler {

    // 각 방(roomId)에 연결된 모든 WebSocket 세션 관리
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // 각 방의 방송자(broadcaster) 세션 저장
    private final Map<String, WebSocketSession> broadcasters = new ConcurrentHashMap<>();

    // 각 세션이 속한 방의 roomId를 매핑
    private final Map<WebSocketSession, String> sessionToRoom = new ConcurrentHashMap<>();

    /**
     * 클라이언트로부터 텍스트 메시지를 수신하면 호출
     * 메시지를 파싱하여 join 또는 signal 타입에 따라 분기 처리
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 JSON을 SignalMessage 객체로 변환
        SignalMessage signalMessage = SignalMessage.fromJson(message.getPayload());

        // 메시지 타입에 따라 분기 처리
        switch (signalMessage.getType()) {
            case "join":
                // join 메시지: 클라이언트가 방에 입장
                joinRoom(signalMessage.getRoomId(), session, signalMessage.getRole());
                break;
            case "signal":
                // signal 메시지: WebRTC 시그널링 정보 전달
                forwardSignal(signalMessage.getRoomId(), signalMessage.getSignalData(), signalMessage.getViewerId(), session);
                break;
            default:
                log.error("알 수 없는 메시지 타입: {}", signalMessage.getType());
                break;
        }
    }

    /**
     * 클라이언트가 방에 참여할 때 호출됨.
     * 방송자(broadcaster) 와 시청자(viewer)를 구분하여 처리함.
     * @param roomId 참여할 방의 ID
     * @param session 클라이언트의 WebSocket 세션
     * @param role 클라이언트 역할 ("broadcaster" 또는 그 외)
     */
    private void joinRoom(String roomId, WebSocketSession session, String role) throws IOException {
        // 방에 세션 추가 및 세션-방 매핑 등
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session, roomId);

        if ("broadcaster".equals(role)) {
            // 이미 방송자가 등록되어 있으면 에러 메시지 전송 후 종
            if (broadcasters.containsKey(roomId)) {
                session.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                        "type", "error",
                        "message", "이미 방송자가 있는 방입니다."
                ))));
                return;
            }
            // 방송자 등
            broadcasters.put(roomId, session);
            log.info("📢 방송자 등록: {}", roomId);
        } else {
            // 시청자인 경우, 방송자에게 새 시청자 입장을 알림
            WebSocketSession broadcaster = broadcasters.get(roomId);
            if (broadcaster != null && broadcaster.isOpen()) {
                broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(Map.of(
                        "type", "newViewer",
                        "roomId", roomId,
                        "viewerId", session.getId()
                ))));
                log.info("👀 새로운 시청자 입장: {} (viewerId: {})", roomId, session.getId());
            }
        }
    }

    /**
     * WebRTC 시그널링 메시지 전달
     * @param roomId 메시지 대상 방의 ID
     * @param signalData 시그널 데이터 (offer, answer, candidate)
     * @param viewerId 메시지 전달 대상 시청자 ID
     * @param sender 메시지를 보낸 세션
     */
    private void forwardSignal(String roomId, Object signalData, String viewerId, WebSocketSession sender) throws IOException {
        // 해당 방에 연결된 모든 세션 조회
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null || roomSessions.isEmpty()) {
            log.error("해당 roomId에 세션이 없습니다: {}", roomId);
            return;
        }

        // signalData가 Map 타입인지 확
        if (!(signalData instanceof Map)) {
            log.error("signalData의 형식이 올바르지 않습니다: {}", signalData);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> signalMap = (Map<String, Object>) signalData;
        String signalType = (String) signalMap.get("type");

        // 전달할 메시지 기본 구조 생
        Map<String, Object> forwardMessage = new ConcurrentHashMap<>();
        forwardMessage.put("type", "signal");
        forwardMessage.put("roomId", roomId);
        forwardMessage.put("signalData", signalData);

        // 해당 방의 방송자 세션 조회
        WebSocketSession broadcaster = broadcasters.get(roomId);

        // 시그널 타입에 따른 분기 처
        switch (signalType) {
            case "offer":
                // offer 메시지는 viewerId 가 필요함.
                if (viewerId == null) {
                    log.error("offer 메시지에 viewerId가 없습니다.");
                    return;
                }
                forwardMessage.put("viewerId", viewerId);

                // 방송자가 아닌, 특정 시청자 세션을 찾아 offer 메시지 전
                WebSocketSession viewer = roomSessions.stream()
                        .filter(sess -> !sess.getId().equals(broadcaster.getId()) && sess.getId().equals(viewerId))
                        .findFirst()
                        .orElse(null);
                if (viewer != null && viewer.isOpen()) {
                    viewer.sendMessage(new TextMessage(SignalMessage.toJson(forwardMessage)));
                    log.info("📡 방송자 offer 전송 -> 시청자: {}", viewerId);
                } else {
                    log.error("대상 시청자 세션을 찾을 수 없습니다: {}", viewerId);
                }
                break;
            case "answer":
                // answer 메시지는 시청자가 보낸 답변을 방송자에게 전
                forwardMessage.put("viewerId", sender.getId());
                if (broadcaster != null && broadcaster.isOpen()) {
                    broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(forwardMessage)));
                    log.info("📡 시청자 answer 전송 -> 방송자, viewerId: {}", sender.getId());
                }
                break;
            case "candidate":
                // candidate 메시지의 경우, 송신자가 방송자인지 시청자인지에 따라 분기 처
                if (sender.equals(broadcaster)) {
                    // 방송자가 candidate를 보낼 경우 대상 시청자를 지정해야 함
                    if (viewerId == null) {
                        log.error("candidate 메시지(방송자)에서 viewerId가 없습니다.");
                        return;
                    }
                    forwardMessage.put("viewerId", viewerId);
                    WebSocketSession targetSession = roomSessions.stream()
                            .filter(sess -> !sess.getId().equals(broadcaster.getId()) && sess.getId().equals(viewerId))
                            .findFirst()
                            .orElse(null);
                    if (targetSession != null && targetSession.isOpen()) {
                        targetSession.sendMessage(new TextMessage(SignalMessage.toJson(forwardMessage)));
                        log.info("❄ 방송자 candidate 전송 -> 시청자: {}", viewerId);
                    } else {
                        log.error("대상 시청자 세션(candidate) 찾을 수 없음: {}", viewerId);
                    }
                } else {
                    // 시청자가 candidate를 보낼 경우 방송자에게 전달
                    forwardMessage.put("viewerId", sender.getId());
                    if (broadcaster != null && broadcaster.isOpen()) {
                        broadcaster.sendMessage(new TextMessage(SignalMessage.toJson(forwardMessage)));
                        log.info("❄ 시청자 candidate 전송 -> 방송자, viewerId: {}", sender.getId());
                    }
                }
                break;
            default:
                log.error("알 수 없는 signal type: {}", signalType);
                break;
        }
    }


    /**
     * WebSocket 연결이 종료되었을때 호출함.
     * 종료된 세션이 속한 방과 역할에 따라 후처리함.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 세션이 속한 roomId 조회
        String roomId = sessionToRoom.get(session);
        if (roomId != null) {
            // 해당 방의 세션 목록에서 현재 세션 제거
            Set<WebSocketSession> sessions = rooms.getOrDefault(roomId, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            sessions.remove(session);
            sessionToRoom.remove(session);

            // 만약 종료된 세션이 방송자일때
            if (broadcasters.get(roomId) == session) {
                // 빙송자 제거 및 모든 시청자에게 방송 종료 메시지 전송
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
                log.info("🚫 방송 종료: {}", roomId);
            }
        }
    }
}
