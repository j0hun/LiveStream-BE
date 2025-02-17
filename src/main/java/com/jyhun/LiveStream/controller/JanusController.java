package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.service.JanusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/janus")
@RequiredArgsConstructor
@Slf4j
public class JanusController {

    private final JanusService janusService;

    @PostMapping("/create-room")
    public ResponseEntity<?> createRoom(@RequestParam String sessionId,
                                        @RequestParam String handleId,
                                        @RequestParam Long roomId) {
        log.info("createRoom sessionId={}, handleId={}", sessionId, handleId);
        return ResponseEntity.ok(janusService.createRoom(sessionId,handleId,roomId));
    }

    @PostMapping("/join-room")
    public ResponseEntity<?> joinRoom(@RequestParam String sessionId,
                                      @RequestParam String handleId,
                                      @RequestParam Long roomId,
                                      @RequestParam String display,
                                      @RequestParam String ptype,
                                      @RequestParam Long feed) {
        log.info("joinRoom sessionId={}, handleId={}", sessionId, handleId);
        return ResponseEntity.ok(janusService.joinRoom(sessionId, handleId, roomId, display, ptype, feed));
    }

    @PostMapping("/get-publishers")
    public ResponseEntity<?> getPublishers(@RequestParam String sessionId, @RequestParam String handleId, @RequestParam Long roomId) {
        return ResponseEntity.ok(janusService.getPublishers(sessionId, handleId, roomId));
    }

}
