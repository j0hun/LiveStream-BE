package com.jyhun.LiveStream.dto;

import com.jyhun.LiveStream.entity.Room;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponseDTO {

    private Long id;

    public static RoomResponseDTO toDTO(Room room) {
        return RoomResponseDTO.builder()
                .id(room.getId())
                .build();
    }

}
