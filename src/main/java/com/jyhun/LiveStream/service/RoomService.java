package com.jyhun.LiveStream.service;

import com.jyhun.LiveStream.dto.ResponseDTO;
import com.jyhun.LiveStream.dto.RoomResponseDTO;
import com.jyhun.LiveStream.entity.Room;
import com.jyhun.LiveStream.entity.User;
import com.jyhun.LiveStream.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserService userService;

    public ResponseDTO createRoom() {
        User user = userService.getLoggedUser();
        Room room = Room.builder()
                .user(user)
                .build();
        roomRepository.save(room);

        return ResponseDTO.builder()
                .status(200)
                .message("방 생성 성공")
                .data(room.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO getAllRooms() {
        List<Room> roomList = roomRepository.findAll();
        List<RoomResponseDTO> roomResponseDTOList = new ArrayList<>();
        for (Room room : roomList) {
            RoomResponseDTO roomResponseDTO = RoomResponseDTO.toDTO(room);
            roomResponseDTOList.add(roomResponseDTO);
        }

        return ResponseDTO.builder()
                .status(200)
                .message("방 목록 조회 성공")
                .data(roomResponseDTOList)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id).orElse(null);
        RoomResponseDTO roomResponseDTO = RoomResponseDTO.toDTO(room);

        return ResponseDTO.builder()
                .status(200)
                .message("방 조회 성공")
                .data(roomResponseDTO)
                .build();
    }

    public ResponseDTO checkBroadcaster(Long roomId) {
        User user = userService.getLoggedUser();
        boolean checkBroadcaster = false;
        if (user != null) {
            checkBroadcaster = roomRepository.existsByIdAndUserId(roomId, user.getId());
        }
        return ResponseDTO.builder()
                .status(200)
                .message("방송자 여부 확인 성공")
                .data(checkBroadcaster)
                .build();
    }
}
