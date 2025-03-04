package com.jyhun.LiveStream.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AmsStreamService {

    @Value("${ams.base.url:http://antmedia:5080/LiveApp}")
    private String AMS_BASE_URL;

    private final RestTemplate restTemplate;

    public String getHlsUrl(String streamId) {
        return String.format("%s/streams/%s.m3u8", "http://localhost:5080/LiveApp", streamId);
    }

    public Map<String, Object> startStream() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(new HashMap<>(), headers);

        String createBroadcastUrl = String.format("%s/rest/v2/broadcasts/create", AMS_BASE_URL);
        ResponseEntity<Map> response = restTemplate.postForEntity(createBroadcastUrl, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("streamId")) {
            throw new RuntimeException("방송 생성 실패: 응답에 streamId가 없습니다.");
        }
        String streamId = (String) body.get("streamId");
        String hlsUrl = getHlsUrl(streamId);

        Map<String, Object> result = new HashMap<>();
        result.put("streamId", streamId);
        result.put("hlsUrl", hlsUrl);

        return result;
    }
}
