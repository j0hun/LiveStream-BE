package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.service.AmsStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ams-stream")
@RequiredArgsConstructor
public class AmsStreamController {

    private final AmsStreamService AmsStreamService;

    @GetMapping("/hls/{streamId}")
    public ResponseEntity<?> getHlsUrl(@PathVariable String streamId) {
        return ResponseEntity.ok(AmsStreamService.getHlsUrl(streamId));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startStream() {
        return ResponseEntity.ok(AmsStreamService.startStream());
    }
}
