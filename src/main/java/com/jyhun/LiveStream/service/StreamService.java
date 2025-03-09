package com.jyhun.LiveStream.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StreamService {

    @Value("${rtmp.base.url}")
    private String RTMP_BASE_URL;

    public String getHlsUrl(String streamId) {
        return RTMP_BASE_URL + streamId + ".m3u8";
    }

}
