package com.jyhun.LiveStream.controller;

import com.jyhun.LiveStream.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

}
