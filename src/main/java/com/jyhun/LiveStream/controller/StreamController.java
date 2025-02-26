package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.service.StreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @GetMapping("/hls/{streamId}")
    public ResponseEntity<?> getHlsUrl(@PathVariable String streamId) {
        return ResponseEntity.ok(streamService.getHlsUrl(streamId));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startStream() {
        return ResponseEntity.ok(streamService.startStream());
    }
}
