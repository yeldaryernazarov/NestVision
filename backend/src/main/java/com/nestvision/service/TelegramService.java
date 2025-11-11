package com.nestvision.service;

import com.nestvision.entity.VideoCategory;
import com.nestvision.repository.VideoRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramService {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.channel.username}")
    private String channelUsername;
    
    @Value("${telegram.channel.id:}")
    private String channelId;
    
    @Value("${video.storage.path}")
    private String storagePath;
    
    private TelegramBot bot;
    
    /**
     * Инициализирует Telegram бота
     */
    public void initializeBot() {
        if (botToken == null || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            logger.warn("Telegram bot token не настроен. Пропускаем инициализацию Telegram.");
            return;
        }
        
        logger.info("Инициализация Telegram бота...");
        logger.info("  - Token (первые 20 символов): {}", botToken.length() > 20 ? botToken.substring(0, 20) + "..." : botToken);
        logger.info("  - Token length: {}", botToken.length());
        
        bot = new TelegramBot(botToken);
        
        // Проверяем, что бот работает
        try {
            GetMeResponse meResponse = bot.execute(new GetMe());
            if (meResponse.isOk() && meResponse.user() != null) {
                logger.info("Telegram бот инициализирован успешно:");
                logger.info("  - Bot ID: {}", meResponse.user().id());
                logger.info("  - Bot Username: @{}", meResponse.user().username());
                logger.info("  - Bot First Name: {}", meResponse.user().firstName());
                logger.info("  - Can Join Groups: {}", meResponse.user().canJoinGroups());
                logger.info("  - Can Read All Group Messages: {}", meResponse.user().canReadAllGroupMessages());
            } else {
                logger.error("Не удалось получить информацию о боте!");
                logger.error("  - Response OK: {}", meResponse.isOk());
                logger.error("  - Error Code: {}", meResponse.errorCode());
                logger.error("  - Description: {}", meResponse.description());
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке бота: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, инициализирован ли бот
     */
    public boolean isBotInitialized() {
        return bot != null;
    }
    
    /**
     * Получает информацию о боте
     */
    public Map<String, Object> getBotInfo() {
        Map<String, Object> info = new HashMap<>();
        
        if (bot == null) {
            info.put("error", "Бот не инициализирован");
            return info;
        }
        
        try {
            GetMeResponse response = bot.execute(new GetMe());
            User botUser = response.user();
            
            if (botUser != null) {
                info.put("id", botUser.id());
                info.put("username", botUser.username());
                info.put("firstName", botUser.firstName());
                info.put("lastName", botUser.lastName());
                info.put("isBot", botUser.isBot());
                info.put("canJoinGroups", botUser.canJoinGroups());
                info.put("canReadAllGroupMessages", botUser.canReadAllGroupMessages());
                info.put("supportsInlineQueries", botUser.supportsInlineQueries());
            } else {
                info.put("error", "Не удалось получить информацию о боте");
            }
        } catch (Exception e) {
            info.put("error", "Ошибка при получении информации о боте: " + e.getMessage());
            logger.error("Ошибка при получении информации о боте", e);
        }
        
        return info;
    }
    
    /**
     * Получает информацию об обновлениях
     */
    public Map<String, Object> getUpdatesInfo() {
        Map<String, Object> info = new HashMap<>();
        
        if (bot == null) {
            info.put("error", "Бот не инициализирован");
            return info;
        }
        
        try {
            GetUpdates getUpdates = new GetUpdates()
                    .limit(10)
                    .offset(0);
            
            GetUpdatesResponse response = bot.execute(getUpdates);
            List<Update> updates = response.updates();
            
            info.put("ok", response.isOk());
            info.put("totalUpdates", updates != null ? updates.size() : 0);
            
            if (updates != null && !updates.isEmpty()) {
                List<Map<String, Object>> updatesList = new ArrayList<>();
                for (Update update : updates) {
                    Map<String, Object> updateInfo = new HashMap<>();
                    updateInfo.put("updateId", update.updateId());
                    
                    Message channelPost = update.channelPost();
                    Message message = update.message();
                    
                    if (channelPost != null) {
                        Map<String, Object> chatInfo = new HashMap<>();
                        if (channelPost.chat() != null) {
                            chatInfo.put("id", channelPost.chat().id());
                            chatInfo.put("title", channelPost.chat().title());
                            chatInfo.put("username", channelPost.chat().username());
                            chatInfo.put("type", channelPost.chat().type());
                        }
                        updateInfo.put("channelPost", chatInfo);
                        updateInfo.put("hasVideo", channelPost.video() != null);
                    }
                    
                    if (message != null) {
                        Map<String, Object> chatInfo = new HashMap<>();
                        if (message.chat() != null) {
                            chatInfo.put("id", message.chat().id());
                            chatInfo.put("title", message.chat().title());
                            chatInfo.put("username", message.chat().username());
                            chatInfo.put("type", message.chat().type());
                        }
                        updateInfo.put("message", chatInfo);
                        updateInfo.put("hasVideo", message.video() != null);
                    }
                    
                    updatesList.add(updateInfo);
                }
                info.put("updates", updatesList);
            } else {
                info.put("updates", new ArrayList<>());
                info.put("message", "Нет обновлений. Убедитесь, что бот добавлен в канал как администратор.");
            }
        } catch (Exception e) {
            info.put("error", "Ошибка при получении обновлений: " + e.getMessage());
            logger.error("Ошибка при получении обновлений", e);
        }
        
        return info;
    }
    
    /**
     * Получает список каналов, к которым у бота есть доступ
     * Анализирует все обновления и извлекает информацию о каналах
     */
    public Map<String, Object> getChannelsList() {
        Map<String, Object> result = new HashMap<>();
        
        if (bot == null) {
            result.put("error", "Бот не инициализирован");
            return result;
        }
        
        try {
            // Получаем все доступные обновления (до 100 последних)
            GetUpdates getUpdates = new GetUpdates()
                    .limit(100)
                    .offset(0);
            
            GetUpdatesResponse response = bot.execute(getUpdates);
            List<Update> updates = response.updates();
            
            // Используем Map для хранения уникальных каналов (по ID)
            Map<Long, Map<String, Object>> channelsMap = new HashMap<>();
            
            if (updates != null && !updates.isEmpty()) {
                logger.info("Анализируем {} обновлений для поиска каналов", updates.size());
                
                for (Update update : updates) {
                    Message channelPost = update.channelPost();
                    Message message = update.message();
                    
                    // Обрабатываем channel_post (сообщения из каналов)
                    if (channelPost != null && channelPost.chat() != null) {
                        com.pengrad.telegrambot.model.Chat chat = channelPost.chat();
                        Long chatId = chat.id();
                        
                        if (!channelsMap.containsKey(chatId)) {
                            Map<String, Object> channelInfo = new HashMap<>();
                            com.pengrad.telegrambot.model.Chat.Type chatType = chat.type();
                            channelInfo.put("id", chatId);
                            channelInfo.put("title", chat.title());
                            channelInfo.put("username", chat.username());
                            channelInfo.put("type", chatType != null ? chatType.toString() : null);
                            channelInfo.put("isChannel", chatType == com.pengrad.telegrambot.model.Chat.Type.channel);
                            channelInfo.put("hasVideo", channelPost.video() != null);
                            channelInfo.put("lastUpdateId", update.updateId());
                            channelsMap.put(chatId, channelInfo);
                        } else {
                            // Обновляем информацию, если нашли видео
                            Map<String, Object> channelInfo = channelsMap.get(chatId);
                            if (channelPost.video() != null) {
                                channelInfo.put("hasVideo", true);
                            }
                        }
                    }
                    
                    // Обрабатываем message (может быть из групп/каналов)
                    if (message != null && message.chat() != null) {
                        com.pengrad.telegrambot.model.Chat chat = message.chat();
                        com.pengrad.telegrambot.model.Chat.Type chatType = chat.type();
                        
                        // Нас интересуют только каналы
                        if (chatType == com.pengrad.telegrambot.model.Chat.Type.channel) {
                            Long chatId = chat.id();
                            
                            if (!channelsMap.containsKey(chatId)) {
                                Map<String, Object> channelInfo = new HashMap<>();
                            channelInfo.put("id", chatId);
                            channelInfo.put("title", chat.title());
                            channelInfo.put("username", chat.username());
                            channelInfo.put("type", chatType != null ? chatType.toString() : null);
                            channelInfo.put("isChannel", true);
                                channelInfo.put("hasVideo", message.video() != null);
                                channelInfo.put("lastUpdateId", update.updateId());
                                channelsMap.put(chatId, channelInfo);
                            } else {
                                Map<String, Object> channelInfo = channelsMap.get(chatId);
                                if (message.video() != null) {
                                    channelInfo.put("hasVideo", true);
                                }
                            }
                        }
                    }
                }
            }
            
            // Преобразуем Map в List
            List<Map<String, Object>> channelsList = new ArrayList<>(channelsMap.values());
            
            result.put("ok", response.isOk());
            result.put("totalChannels", channelsList.size());
            result.put("channels", channelsList);
            
            if (channelsList.isEmpty()) {
                result.put("message", "Бот не получает обновления из каналов. " +
                        "Убедитесь, что бот добавлен в канал как администратор и имеет права на просмотр сообщений.");
            } else {
                logger.info("Найдено каналов: {}", channelsList.size());
            }
            
        } catch (Exception e) {
            result.put("error", "Ошибка при получении списка каналов: " + e.getMessage());
            logger.error("Ошибка при получении списка каналов", e);
        }
        
        return result;
    }
    
    /**
     * Загружает все видео из канала при старте приложения
     * @return количество загруженных видео
     */
    public int loadAllVideosFromChannel() {
        if (bot == null) {
            logger.warn("Telegram бот не инициализирован");
            return 0;
        }
        
        logger.info("=== НАЧАЛО ЗАГРУЗКИ ВИДЕО ИЗ КАНАЛА ===");
        logger.info("Канал: @{}", channelUsername);
        int loadedCount = 0;
        int offset = 0;
        int batchSize = 100;
        int totalUpdates = 0;
        int updatesWithMessages = 0;
        int updatesWithChannelPost = 0;
        int updatesWithVideo = 0;
        int alreadyExists = 0;
        int errors = 0;
        
        try {
            while (true) {
                GetUpdates getUpdates = new GetUpdates()
                        .limit(batchSize)
                        .offset(offset);
                
                logger.debug("Запрашиваем обновления с offset: {}", offset);
                GetUpdatesResponse updatesResponse = bot.execute(getUpdates);
                List<Update> updates = updatesResponse.updates();
                
                if (updates == null || updates.isEmpty()) {
                    logger.info("Больше обновлений нет. Всего обработано: {}", totalUpdates);
                    break;
                }
                
                totalUpdates += updates.size();
                logger.info("Получено обновлений в этом батче: {}", updates.size());
                
                for (Update update : updates) {
                    try {
                        // Для каналов сообщения приходят в channel_post, а не в message
                        Message message = update.channelPost() != null ? update.channelPost() : update.message();
                        
                        if (message == null) {
                            logger.debug("Обновление {}: нет сообщения (channel_post и message оба null)", update.updateId());
                            continue;
                        }
                        
                        if (update.channelPost() != null) {
                            updatesWithChannelPost++;
                            logger.debug("Обновление {}: найдено channel_post из чата ID: {}", 
                                    update.updateId(), message.chat() != null ? message.chat().id() : "null");
                        } else if (update.message() != null) {
                            updatesWithMessages++;
                            logger.debug("Обновление {}: найдено message из чата ID: {}", 
                                    update.updateId(), message.chat() != null ? message.chat().id() : "null");
                        }
                        
                        // Логируем информацию о чате
                        if (message.chat() != null) {
                            logger.debug("  - Название чата: {}", message.chat().title());
                            logger.debug("  - Username чата: {}", message.chat().username());
                            logger.debug("  - ID чата: {}", message.chat().id());
                            logger.debug("  - Тип чата: {}", message.chat().type());
                        }
                        
                        if (message.video() == null) {
                            logger.debug("Обновление {}: нет видео в сообщении", update.updateId());
                            continue;
                        }
                        
                        updatesWithVideo++;
                        logger.info("Обновление {}: НАЙДЕНО ВИДЕО! Название: {}, Размер: {} байт", 
                                update.updateId(), 
                                message.video().fileName() != null ? message.video().fileName() : "без имени",
                                message.video().fileSize());
                        
                        // УБРАНА ПРОВЕРКА КАНАЛА: обрабатываем видео из ЛЮБОГО канала
                        logger.info("  - Обрабатываем видео из канала: {}", message.chat() != null ? message.chat().title() : "N/A");
                        
                        boolean added = processVideoMessage(message);
                        if (added) {
                            loadedCount++;
                            logger.info("  - ✅ Видео успешно добавлено!");
                        } else {
                            alreadyExists++;
                            logger.info("  - ⚠️ Видео уже существует в базе");
                        }
                    } catch (Exception e) {
                        errors++;
                        logger.error("Ошибка при обработке обновления {}: {}", update.updateId(), e.getMessage(), e);
                    }
                }
                
                offset = updates.get(updates.size() - 1).updateId() + 1;
                
                // Небольшая задержка, чтобы не превысить лимиты API
                Thread.sleep(100);
            }
            
            logger.info("=== ИТОГИ ЗАГРУЗКИ ===");
            logger.info("Всего обновлений обработано: {}", totalUpdates);
            logger.info("  - С сообщениями (message): {}", updatesWithMessages);
            logger.info("  - С постами канала (channel_post): {}", updatesWithChannelPost);
            logger.info("  - С видео: {}", updatesWithVideo);
            logger.info("  - Уже существует в БД: {}", alreadyExists);
            logger.info("  - Ошибок: {}", errors);
            logger.info("  - ✅ Загружено новых видео: {}", loadedCount);
            logger.info("=== КОНЕЦ ЗАГРУЗКИ ===");
        } catch (Exception e) {
            logger.error("Критическая ошибка при загрузке видео из канала", e);
        }
        
        return loadedCount;
    }
    
    /**
     * Начинает слушать канал на предмет новых видео
     */
    public void startListeningChannel() {
        if (bot == null) {
            logger.warn("Telegram бот не инициализирован. Пропускаем запуск слушателя.");
            return;
        }
        
        logger.info("Запускаем слушатель канала: @{}", channelUsername);
        
        new Thread(() -> {
            int lastUpdateId = 0;
            
            while (true) {
                try {
                    GetUpdates getUpdates = new GetUpdates()
                            .limit(100)
                            .offset(lastUpdateId + 1)
                            .timeout(30);
                    
                    GetUpdatesResponse updatesResponse = bot.execute(getUpdates);
                    List<Update> updates = updatesResponse.updates();
                    
                    if (updates != null && !updates.isEmpty()) {
                        logger.info("Слушатель: получено {} новых обновлений", updates.size());
                        for (Update update : updates) {
                            logger.debug("Слушатель: обработка обновления {}", update.updateId());
                            
                            // Для каналов сообщения приходят в channel_post, а не в message
                            Message channelPost = update.channelPost();
                            Message message = update.message();
                            
                            logger.debug("  - channel_post: {}", channelPost != null ? "есть" : "нет");
                            logger.debug("  - message: {}", message != null ? "есть" : "нет");
                            
                            Message msg = channelPost != null ? channelPost : message;
                            
                            if (msg != null) {
                                if (msg.chat() != null) {
                                    logger.debug("  - Chat ID: {}", msg.chat().id());
                                    logger.debug("  - Chat title: {}", msg.chat().title());
                                    logger.debug("  - Chat type: {}", msg.chat().type());
                                }
                                logger.debug("  - Has video: {}", msg.video() != null);
                            }
                            
                            if (msg != null && msg.video() != null) {
                                logger.info("Слушатель: найдено новое видео в обновлении {}", update.updateId());
                                logger.info("Слушатель: обрабатываем видео из канала: {}", msg.chat() != null ? msg.chat().title() : "N/A");
                                boolean added = processVideoMessage(msg);
                                if (added) {
                                    logger.info("Слушатель: ✅ Новое видео добавлено!");
                                } else {
                                    logger.info("Слушатель: видео уже существует или ошибка при обработке");
                                }
                            } else {
                                if (msg == null) {
                                    logger.debug("Слушатель: обновление {} не содержит сообщения", update.updateId());
                                } else {
                                    logger.debug("Слушатель: обновление {} не содержит видео", update.updateId());
                                }
                            }
                            lastUpdateId = update.updateId();
                        }
                    }
                    
                    Thread.sleep(1000); // Проверяем каждую секунду
                } catch (InterruptedException e) {
                    logger.info("Слушатель канала остановлен");
                    break;
                } catch (Exception e) {
                    logger.error("Ошибка при прослушивании канала", e);
                    try {
                        Thread.sleep(5000); // При ошибке ждем 5 секунд
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }).start();
    }
    
    /**
     * Обрабатывает сообщение с видео
     * @return true если видео было добавлено, false если уже существует
     */
    private boolean processVideoMessage(Message message) {
        try {
            com.pengrad.telegrambot.model.Video telegramVideo = message.video();
            if (telegramVideo == null) {
                return false;
            }
            
            String fileId = telegramVideo.fileId();
            String fileName = telegramVideo.fileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "video_" + message.messageId() + ".mp4";
            }
            
            // Проверяем, есть ли уже это видео в базе
            // Проверяем по telegram_file_id (самый надежный способ)
            final String finalFileId = fileId;
            final Long messageId = (long) message.messageId();
            final String finalFileName = fileName;
            final Long fileSize = (long) telegramVideo.fileSize();
            boolean exists = videoRepository.findAll().stream()
                    .anyMatch(v -> {
                        // Проверяем по telegram_file_id (если есть)
                        if (v.getTelegramFileId() != null && v.getTelegramFileId().equals(finalFileId)) {
                            return true;
                        }
                        // Проверяем по telegram_message_id (если есть)
                        if (v.getTelegramMessageId() != null && v.getTelegramMessageId().equals(messageId)) {
                            return true;
                        }
                        // Проверяем по имени файла и размеру (для совместимости со старыми записями)
                        if (v.getFileName() != null && v.getFileName().equals(finalFileName) && 
                            v.getSize() != null && v.getSize().equals((long) fileSize)) {
                            return true;
                        }
                        return false;
                    });
            
            if (exists) {
                logger.debug("Видео уже существует в базе: {} (fileId: {})", fileName, fileId);
                return false;
            }
            
            // Определяем категорию из текста сообщения или названия канала
            VideoCategory category = detectCategoryFromMessage(message);
            
            // Скачиваем файл
            String filePath = downloadVideoFile(fileId, fileName, category);
            if (filePath == null) {
                logger.error("Не удалось скачать видео: {}", fileName);
                return false;
            }
            
            // Создаем запись в БД
            com.nestvision.entity.Video video = new com.nestvision.entity.Video();
            video.setFileName(fileName);
            video.setFilePath(filePath);
            video.setCategory(category);
            
            // Сохраняем Telegram метаданные для проверки дубликатов
            video.setTelegramFileId(fileId);
            video.setTelegramMessageId((long) message.messageId());
            
            // Определяем дату записи из сообщения
            LocalDateTime recordedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(message.date()),
                    ZoneId.systemDefault()
            );
            video.setRecordedAt(recordedAt);
            video.setUploadedAt(LocalDateTime.now());
            video.setSize((long) telegramVideo.fileSize());
            video.setDuration((long) telegramVideo.duration());
            
            videoRepository.save(video);
            logger.info("Видео добавлено: {} (категория: {})", fileName, category);
            
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при обработке видео сообщения", e);
            return false;
        }
    }
    
    /**
     * Обрабатывает видео по file_id (для вызова из Node.js скрипта)
     */
    public boolean processVideoFromFileId(String fileId, String fileName, Long messageId, String categoryStr, String recordedDateTimeStr) {
        try {
            logger.info("=== НАЧАЛО ОБРАБОТКИ ВИДЕО ИЗ NODE.JS ===");
            logger.info("  - File ID: {}", fileId);
            logger.info("  - File Name: {}", fileName);
            logger.info("  - Message ID: {}", messageId);
            logger.info("  - Category: {}", categoryStr);
            logger.info("  - Recorded DateTime: {}", recordedDateTimeStr);
            logger.info("  - Bot Token (первые 20 символов): {}", botToken != null && botToken.length() > 20 ? botToken.substring(0, 20) + "..." : botToken);
            logger.info("  - Bot initialized: {}", bot != null);
            
            // Проверяем, что бот инициализирован
            if (bot == null) {
                logger.error("Бот не инициализирован!");
                return false;
            }
            
            // Получаем информацию о файле
            logger.info("Отправляем запрос GetFile в Telegram API...");
            GetFile getFile = new GetFile(fileId);
            GetFileResponse fileResponse = bot.execute(getFile);
            
            logger.info("Получен ответ от Telegram API:");
            logger.info("  - isOk: {}", fileResponse.isOk());
            logger.info("  - Error Code: {}", fileResponse.errorCode());
            logger.info("  - Description: {}", fileResponse.description());
            
            // Детальное логирование ответа
            if (!fileResponse.isOk()) {
                logger.error("Ошибка при получении файла из Telegram API:");
                logger.error("  - File ID: {}", fileId);
                logger.error("  - Error Code: {}", fileResponse.errorCode());
                logger.error("  - Description: {}", fileResponse.description());
                logger.error("  - Возможные причины:");
                logger.error("    1. Бот не является администратором канала");
                logger.error("    2. Бот не имеет прав на доступ к файлам");
                logger.error("    3. file_id недействителен или истек");
                logger.error("    4. Файл был удален из Telegram");
                logger.error("    5. Неверный токен бота");
                return false;
            }
            
            File file = fileResponse.file();
            
            if (file == null) {
                logger.error("Файл null в ответе от Telegram API. File ID: {}", fileId);
                logger.error("  - Response OK: {}", fileResponse.isOk());
                logger.error("  - Error Code: {}", fileResponse.errorCode());
                logger.error("  - Description: {}", fileResponse.description());
                logger.error("  - Проверьте:");
                logger.error("     1. Бот является администратором канала");
                logger.error("     2. Бот имеет право 'Читать сообщения'");
                logger.error("     3. file_id корректен и не истек");
                return false;
            }
            
            logger.info("Файл получен успешно:");
            logger.info("  - File ID: {}", file.fileId());
            logger.info("  - File Path: {}", file.filePath());
            logger.info("  - File Size: {} bytes", file.fileSize());
            
            if (file.filePath() == null) {
                logger.error("filePath() == null для файла. File ID: {}", fileId);
                logger.error("  - File ID: {}", file.fileId());
                logger.error("  - File Size: {}", file.fileSize());
                logger.error("  - Это может означать, что файл недоступен для скачивания");
                return false;
            }
            
            // Получаем информацию о видео через getFile (если доступно)
            // Для получения полной информации о видео нужно использовать другой метод
            // Пока используем базовую информацию
            
            // Проверяем, есть ли уже это видео в базе
            final String finalFileId = fileId;
            final String finalFileName = fileName;
            boolean exists = videoRepository.findAll().stream()
                    .anyMatch(v -> {
                        if (v.getTelegramFileId() != null && v.getTelegramFileId().equals(finalFileId)) {
                            return true;
                        }
                        if (messageId != null && v.getTelegramMessageId() != null && 
                            v.getTelegramMessageId().equals(messageId)) {
                            return true;
                        }
                        return false;
                    });
            
            if (exists) {
                logger.debug("Видео уже существует в базе: {} (fileId: {})", fileName, fileId);
                return false;
            }
            
            // Определяем категорию
            VideoCategory category = VideoCategory.SUDDEN_EVENT; // По умолчанию
            if (categoryStr != null) {
                try {
                    category = VideoCategory.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("Неизвестная категория: {}, используем по умолчанию", categoryStr);
                }
            }
            
            // Парсим дату и время записи из формата: DD-MM-YYYY_HH-MM-SS
            LocalDateTime recordedAt = LocalDateTime.now(); // По умолчанию - текущее время
            if (recordedDateTimeStr != null && !recordedDateTimeStr.isEmpty()) {
                try {
                    // Формат: "07-07-2025_12-12-12"
                    java.time.format.DateTimeFormatter formatter = 
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
                    recordedAt = LocalDateTime.parse(recordedDateTimeStr, formatter);
                    logger.info("Дата записи из сообщения: {}", recordedAt);
                } catch (Exception e) {
                    logger.warn("Не удалось распарсить дату записи: {}, используем текущее время. Ошибка: {}", recordedDateTimeStr, e.getMessage());
                    recordedAt = LocalDateTime.now();
                }
            } else {
                logger.info("Дата записи не указана, используем текущее время: {}", recordedAt);
            }
            
            // Скачиваем файл
            logger.info("Начинаем скачивание файла...");
            String filePath = downloadVideoFile(fileId, fileName, category);
            if (filePath == null) {
                logger.error("Не удалось скачать видео: {}", fileName);
                logger.error("=== КОНЕЦ ОБРАБОТКИ ВИДЕО (ОШИБКА) ===");
                return false;
            }
            logger.info("Файл успешно скачан: {}", filePath);
            
            // Создаем запись в БД
            com.nestvision.entity.Video video = new com.nestvision.entity.Video();
            video.setFileName(fileName);
            video.setFilePath(filePath);
            video.setCategory(category);
            video.setTelegramFileId(fileId);
            if (messageId != null) {
                video.setTelegramMessageId(messageId);
            }
            video.setRecordedAt(recordedAt);
            video.setUploadedAt(LocalDateTime.now());
            if (file.fileSize() != null) {
                video.setSize((long) file.fileSize());
            }
            
            logger.info("Сохранение видео в базу данных...");
            videoRepository.save(video);
            logger.info("✅ Видео успешно добавлено из Node.js скрипта:");
            logger.info("  - File Name: {}", fileName);
            logger.info("  - Category: {}", category);
            logger.info("  - Recorded At: {}", recordedAt);
            logger.info("  - File Path: {}", filePath);
            logger.info("  - Telegram File ID: {}", fileId);
            logger.info("  - Telegram Message ID: {}", messageId);
            logger.info("=== КОНЕЦ ОБРАБОТКИ ВИДЕО (УСПЕХ) ===");
            
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при обработке видео из Node.js скрипта", e);
            return false;
        }
    }
    
    /**
     * Скачивает видео файл из Telegram
     */
    private String downloadVideoFile(String fileId, String fileName, VideoCategory category) {
        try {
            logger.info("downloadVideoFile: Начало скачивания");
            logger.info("  - File ID: {}", fileId);
            logger.info("  - File Name: {}", fileName);
            logger.info("  - Category: {}", category);
            
            // Получаем информацию о файле
            logger.info("  - Запрос GetFile в Telegram API...");
            GetFile getFile = new GetFile(fileId);
            GetFileResponse fileResponse = bot.execute(getFile);
            
            logger.info("  - Ответ получен: isOk={}, errorCode={}", fileResponse.isOk(), fileResponse.errorCode());
            
            if (!fileResponse.isOk()) {
                logger.error("  - Ошибка GetFile: {}", fileResponse.description());
                return null;
            }
            
            File file = fileResponse.file();
            
            if (file == null || file.filePath() == null) {
                logger.error("Не удалось получить путь к файлу: {}", fileId);
                logger.error("  - File is null: {}", file == null);
                if (file != null) {
                    logger.error("  - File Path is null: {}", file.filePath() == null);
                }
                return null;
            }
            
            logger.info("  - File Path получен: {}", file.filePath());
            logger.info("  - File Size: {} bytes", file.fileSize());
            
            // Формируем URL для скачивания
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.filePath();
            logger.info("  - Download URL: {}", fileUrl.replace(botToken, "TOKEN_HIDDEN"));
            
            // Определяем папку для сохранения
            String categoryFolder = getCategoryFolderName(category);
            Path categoryPath = Paths.get(storagePath, categoryFolder);
            logger.info("  - Category folder: {}", categoryFolder);
            logger.info("  - Storage path: {}", storagePath);
            logger.info("  - Full category path: {}", categoryPath.toAbsolutePath());
            
            Files.createDirectories(categoryPath);
            logger.info("  - Директория создана/проверена");
            
            // Скачиваем файл
            Path targetPath = categoryPath.resolve(fileName);
            logger.info("  - Target path: {}", targetPath.toAbsolutePath());
            logger.info("  - Начинаем скачивание файла...");
            
            long startTime = System.currentTimeMillis();
            try (InputStream in = new URL(fileUrl).openStream()) {
                long fileSize = Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                long duration = System.currentTimeMillis() - startTime;
                logger.info("  - Файл скачан успешно:");
                logger.info("     * Size: {} bytes ({} MB)", fileSize, String.format("%.2f", fileSize / 1024.0 / 1024.0));
                logger.info("     * Duration: {} ms", duration);
            }
            
            logger.info("✅ Видео скачано: {}", targetPath.toAbsolutePath());
            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.error("Ошибка при скачивании видео файла", e);
            logger.error("  - Exception type: {}", e.getClass().getName());
            logger.error("  - Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("  - Cause: {}", e.getCause().getMessage());
            }
            return null;
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при скачивании видео файла", e);
            return null;
        }
    }
    
    /**
     * Определяет категорию видео из сообщения
     */
    private VideoCategory detectCategoryFromMessage(Message message) {
        String text = message.caption();
        if (text == null) {
            text = "";
        }
        text = text.toLowerCase();
        
        // Проверяем ключевые слова в тексте
        if (text.contains("агрессия") && (text.contains("дети") || text.contains("ребенок"))) {
            return VideoCategory.AGGRESSION_BETWEEN_CHILDREN;
        }
        if (text.contains("агрессия") && (text.contains("воспитатель") || text.contains("учитель"))) {
            return VideoCategory.AGGRESSION_TEACHER;
        }
        if (text.contains("внезапное") || text.contains("событие")) {
            return VideoCategory.SUDDEN_EVENT;
        }
        if (text.contains("без присмотра") || text.contains("без присмотра")) {
            return VideoCategory.CHILDREN_UNATTENDED;
        }
        
        // Проверяем название канала или username
        if (message.chat() != null) {
            String chatTitle = message.chat().title();
            if (chatTitle != null) {
                String lowerTitle = chatTitle.toLowerCase();
                if (lowerTitle.contains("агрессия дети")) {
                    return VideoCategory.AGGRESSION_BETWEEN_CHILDREN;
                }
                if (lowerTitle.contains("агрессия воспитатель")) {
                    return VideoCategory.AGGRESSION_TEACHER;
                }
                if (lowerTitle.contains("внезапное событие")) {
                    return VideoCategory.SUDDEN_EVENT;
                }
                if (lowerTitle.contains("без присмотра")) {
                    return VideoCategory.CHILDREN_UNATTENDED;
                }
            }
        }
        
        // По умолчанию - внезапное событие
        return VideoCategory.SUDDEN_EVENT;
    }
    
    /**
     * УБРАНА ПРОВЕРКА КАНАЛА: теперь принимаем все видео из любых каналов
     * Оставлен для совместимости, но всегда возвращает true для каналов
     */
    private boolean isFromTargetChannel(Message message) {
        if (message == null) {
            logger.debug("isFromTargetChannel: message == null");
            return false;
        }
        
        com.pengrad.telegrambot.model.Chat chat = message.chat();
        
        if (chat == null) {
            logger.debug("isFromTargetChannel: chat == null");
            return false;
        }
        
        com.pengrad.telegrambot.model.Chat.Type chatType = chat.type();
        
        // Если это личный чат (private), значит сообщение переслано - принимаем
        if (chatType == com.pengrad.telegrambot.model.Chat.Type.Private) {
            logger.info("  - ✅ Это пересланное сообщение (private chat), принимаем");
            return true;
        }
        
        // Для каналов - принимаем ВСЕ
        if (chatType == com.pengrad.telegrambot.model.Chat.Type.channel) {
            logger.info("  - ✅ Принимаем видео из канала: {}", chat.title());
            return true;
        }
        
        // Для остальных типов чатов - не принимаем
        logger.debug("  - Пропускаем: это не канал и не private чат");
        return false;
    }
    
    /**
     * Возвращает имя папки для категории
     */
    private String getCategoryFolderName(VideoCategory category) {
        return switch (category) {
            case AGGRESSION_BETWEEN_CHILDREN -> "aggression_children";
            case AGGRESSION_TEACHER -> "aggression_teacher";
            case SUDDEN_EVENT -> "sudden_event";
            case CHILDREN_UNATTENDED -> "children_unattended";
        };
    }
}


