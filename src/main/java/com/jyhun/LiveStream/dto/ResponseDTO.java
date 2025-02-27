package com.jyhun.LiveStream.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseDTO<T> {

    private int status;
    private String message;
    private T data;

}
