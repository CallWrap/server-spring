package com.callwrap.audio.controller;

import com.callwrap.audio.dto.AudioUploadUrlRequest;
import com.callwrap.audio.dto.AudioUploadUrlResponse;
import com.callwrap.audio.service.AudioUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioUploadService audioUploadService;

    @PostMapping("/upload-url")
    public AudioUploadUrlResponse issueUploadUrl(@RequestBody(required = false) AudioUploadUrlRequest request) {
        String extension = request != null ? request.fileExtension() : null;
        return audioUploadService.issueUploadUrl(extension);
    }
}
