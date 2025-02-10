package com.jyhun.LiveStream.service;

import com.jyhun.LiveStream.entity.Room;
import com.jyhun.LiveStream.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;

    public Long createRoom() {
        Room room = Room.builder()
                .build();
        roomRepository.save(room);
        return room.getId();
    }

}
