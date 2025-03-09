package com.jyhun.LiveStream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JanusStreamService {

    private final RestTemplate restTemplate;

    @Value("${janus.server.url:https://janus-gateway:8089/janus}")
    private String janusServerUrl;

    public ResponseEntity<Map> createRoom(String sessionId, String handleId, Long roomId) {
        String url = janusServerUrl + "/" + sessionId + "/" + handleId;
        Map<String, Object> body = new HashMap<>();
        body.put("janus", "message");
        String transactionId = UUID.randomUUID().toString();
        body.put("transaction", transactionId);
        Map<String, Object> request = new HashMap<>();
        request.put("request", "create");
        request.put("room", roomId);
        request.put("permanent", false);
        body.put("body", request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        log.info("response: {}", response);
        return response;
    }

    public ResponseEntity<Map> joinRoom(String sessionId, String handleId, Long roomId, String display, String ptype, Long feed) {
        String url = janusServerUrl + "/" + sessionId + "/" + handleId;
        Map<String, Object> body = new HashMap<>();
        body.put("janus", "message");
        String transactionId = UUID.randomUUID().toString();
        body.put("transaction", transactionId);
        Map<String, Object> request = new HashMap<>();
        request.put("request", "join");
        request.put("room", roomId);
        request.put("ptype", ptype);
        request.put("display", display);
        if (feed != null) {
            request.put("feed", feed);
        }
        body.put("body", request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        log.info("response: {}", response);
        return response;
    }

    public Long getPublishers(String sessionId, String handleId, Long roomId) {
        String url = janusServerUrl + "/" + sessionId + "/" + handleId;
        Map<String, Object> body = new HashMap<>();
        body.put("janus", "message");
        body.put("transaction", UUID.randomUUID().toString());
        Map<String, Object> request = new HashMap<>();
        request.put("request", "listparticipants");
        request.put("room", roomId);
        body.put("body", request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            log.error("Janus 응답이 null입니다.");
            return null;
        }
        Map<String, Object> pluginData = (Map<String, Object>) responseBody.get("plugindata");
        if (pluginData == null) {
            log.error("plugindata가 없습니다: {}", responseBody);
            return null;
        }
        Map<String, Object> data = (Map<String, Object>) pluginData.get("data");
        if (data == null) {
            log.error("data 필드가 없습니다: {}", pluginData);
            return null;
        }
        List<Map<String, Object>> participants = (List<Map<String, Object>>) data.get("participants");
        if (participants == null || participants.isEmpty()) {
            log.warn("참여자가 없습니다. Room ID: {}", roomId);
            return null;
        }
        // 방송자 찾기 (display가 "Broadcaster"인 경우)
        for (Map<String, Object> participant : participants) {
            log.info("participant: {}", participant);
            if ("Broadcaster".equals(participant.get("display"))) {
                log.info("방송자 찾음: {}", participant);
                Number id = (Number) participant.get("id");
                return id.longValue();
            }
        }
        log.warn("방송자가 없습니다. Room ID: {}", roomId);
        return null;
    }
}
