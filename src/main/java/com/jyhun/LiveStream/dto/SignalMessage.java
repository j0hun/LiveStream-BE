package com.jyhun.LiveStream.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class SignalMessage {

    private String type;
    private String roomId;
    private String role;
    private Object signalData;
    private String viewerId;

    // JSON 파싱 메소드
    public static SignalMessage fromJson(String json) throws JsonProcessingException {
        return new ObjectMapper().readValue(json, SignalMessage.class);
    }

    // JSON 직렬화 메소드
    public static String toJson(Object data) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(data);
    }

}
