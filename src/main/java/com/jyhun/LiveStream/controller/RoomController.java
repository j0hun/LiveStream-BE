package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.dto.ResponseDTO;
import com.jyhun.LiveStream.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ResponseDTO> createRoom() {
        return new ResponseEntity<>(roomService.createRoom(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllRooms() {
        return new ResponseEntity<>(roomService.getAllRooms(), HttpStatus.OK);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDTO> getRoom(@PathVariable Long roomId) {
        return new ResponseEntity<>(roomService.getRoomById(roomId), HttpStatus.OK);
    }

    @GetMapping("/{roomId}/checkBroadcaster")
    public ResponseEntity<ResponseDTO> checkBroadcaster(@PathVariable Long roomId) {
        return new ResponseEntity<>(roomService.checkBroadcaster(roomId), HttpStatus.OK);
    }

}
