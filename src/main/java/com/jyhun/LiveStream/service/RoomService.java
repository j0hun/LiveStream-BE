package com.jyhun.LiveStream.service;

import com.jyhun.LiveStream.dto.ResponseDTO;
import com.jyhun.LiveStream.entity.Room;
import com.jyhun.LiveStream.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;

    public ResponseDTO createRoom() {
        Room room = Room.builder()
                .build();
        roomRepository.save(room);

        return ResponseDTO.builder()
                .status(200)
                .message("방 생성 성공")
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO getAllRooms() {
        List<Room> roomList = roomRepository.findAll();

        return ResponseDTO.builder()
                .status(200)
                .message("방 목록 조회 성공")
                .data(roomList)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id).orElse(null);

        return ResponseDTO.builder()
                .status(200)
                .message("방 조회 성공")
                .data(room)
                .build();
    }

}
