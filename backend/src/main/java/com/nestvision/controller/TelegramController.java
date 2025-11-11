package com.nestvision.controller;

import com.nestvision.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
@CrossOrigin(origins = "http://localhost:3000")
public class TelegramController {
    
    @Autowired
    private TelegramService telegramService;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("botInitialized", telegramService.isBotInitialized());
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/bot-info")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        return ResponseEntity.ok(telegramService.getBotInfo());
    }
    
    @GetMapping("/updates")
    public ResponseEntity<Map<String, Object>> getUpdates() {
        return ResponseEntity.ok(telegramService.getUpdatesInfo());
    }
    
    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> getChannels() {
        return ResponseEntity.ok(telegramService.getChannelsList());
    }
    
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadVideos() {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!telegramService.isBotInitialized()) {
                response.put("success", false);
                response.put("message", "Telegram бот не инициализирован");
                return ResponseEntity.ok(response);
            }
            
            int loadedCount = telegramService.loadAllVideosFromChannel();
            response.put("success", true);
            response.put("loadedCount", loadedCount);
            response.put("message", "Загружено новых видео: " + loadedCount);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Endpoint для обработки видео из Node.js скрипта
     * Принимает file_id из Telegram и обрабатывает его
     */
    @PostMapping("/process-video")
    public ResponseEntity<Map<String, Object>> processVideoFromNode(
            @RequestParam("fileId") String fileId,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "messageId", required = false) Long messageId,
            @RequestParam(value = "category", required = false) String categoryStr,
            @RequestParam(value = "recordedDateTime", required = false) String recordedDateTimeStr) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!telegramService.isBotInitialized()) {
                response.put("success", false);
                response.put("message", "Telegram бот не инициализирован");
                return ResponseEntity.ok(response);
            }
            
            boolean added = telegramService.processVideoFromFileId(fileId, fileName, messageId, categoryStr, recordedDateTimeStr);
            if (added) {
                response.put("success", true);
                response.put("message", "Видео успешно обработано и добавлено в базу данных");
            } else {
                response.put("success", false);
                response.put("message", "Видео уже существует или ошибка при обработке");
            }
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
