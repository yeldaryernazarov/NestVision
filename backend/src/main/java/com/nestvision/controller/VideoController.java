package com.nestvision.controller;

import com.nestvision.dto.VideoResponse;
import com.nestvision.entity.VideoCategory;
import com.nestvision.service.VideoService;
import com.nestvision.service.VideoFolderScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:3000")
public class VideoController {
    
    @Autowired
    private VideoService videoService;
    
    @Autowired
    private VideoFolderScanner videoFolderScanner;
    
    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        List<VideoResponse> videos = videoService.getAllVideos();
        return ResponseEntity.ok(videos);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<VideoResponse>> getVideosByCategory(@PathVariable VideoCategory category) {
        List<VideoResponse> videos = videoService.getVideosByCategory(category);
        return ResponseEntity.ok(videos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideoById(@PathVariable Long id) {
        VideoResponse video = videoService.getVideoById(id);
        return ResponseEntity.ok(video);
    }
    
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) {
        Resource resource = videoService.getVideoResource(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @PostMapping("/upload")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") VideoCategory category,
            @RequestParam(value = "recordedAt", required = false) String recordedAtStr) {
        
        LocalDateTime recordedAt = null;
        if (recordedAtStr != null && !recordedAtStr.isEmpty()) {
            try {
                recordedAt = LocalDateTime.parse(recordedAtStr);
            } catch (Exception e) {
                // Если не удалось распарсить, используем текущее время
                recordedAt = LocalDateTime.now();
            }
        }
        
        VideoResponse video = videoService.uploadVideo(file, category, recordedAt);
        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }
    
    /**
     * Сканирует папку с видео и добавляет новые файлы в базу данных
     * Полезно, если вы добавили видео напрямую в папку
     * Также можно использовать для тестирования перед автоматическим запуском в 00:00
     */
    @PostMapping("/scan-folder")
    public ResponseEntity<Map<String, Object>> scanFolder() {
        int addedCount = videoFolderScanner.scanAndAddVideos();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Сканирование завершено");
        response.put("addedCount", addedCount);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
