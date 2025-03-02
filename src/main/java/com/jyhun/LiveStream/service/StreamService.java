package com.jyhun.LiveStream.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StreamService {

    public String getHlsUrl(String streamId) {
        return "http://localhost:8081/hls/" + streamId + ".m3u8";
    }

}
