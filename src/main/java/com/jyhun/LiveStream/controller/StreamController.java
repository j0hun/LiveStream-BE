package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.service.StreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamService streamService;

    @GetMapping("/hls/{streamId}")
    public ResponseEntity<?> getHlsUrl(@PathVariable String streamId) {
        return ResponseEntity.ok(streamService.getHlsUrl(streamId));
    }

}
