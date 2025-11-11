package com.nestvision.config;

import com.nestvision.service.TelegramService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Инициализирует Telegram бота при старте приложения
 * Загружает все видео из канала и запускает слушатель новых сообщений
 */
@Component
public class TelegramBotInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotInitializer.class);
    
    @Autowired
    private TelegramService telegramService;
    
    @PostConstruct
    public void initialize() {
        logger.info("=== Инициализация Telegram бота ===");
        
        try {
            // Инициализируем бота
            telegramService.initializeBot();
            
            // Проверяем, что бот инициализирован
            if (telegramService.isBotInitialized()) {
                // Загружаем все существующие видео из канала
                logger.info("Загружаем все видео из канала...");
                int loadedCount = telegramService.loadAllVideosFromChannel();
                logger.info("Загружено видео при старте: {}", loadedCount);
                
                // Запускаем слушатель новых сообщений
                // ВРЕМЕННО ОТКЛЮЧЕНО - используется Node.js скрипт для пересылки сообщений
                // logger.info("Запускаем слушатель канала...");
                // telegramService.startListeningChannel();
                logger.info("Слушатель канала отключен (используется Node.js скрипт для автоматической пересылки)");
                
                logger.info("=== Telegram бот успешно инициализирован ===");
            } else {
                logger.warn("Telegram бот не инициализирован. Проверьте настройки в application.properties");
            }
        } catch (Exception e) {
            logger.error("Ошибка при инициализации Telegram бота", e);
        }
    }
}

