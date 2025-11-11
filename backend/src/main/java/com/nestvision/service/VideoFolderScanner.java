package com.nestvision.service;

import com.nestvision.entity.Video;
import com.nestvision.entity.VideoCategory;
import com.nestvision.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoFolderScanner {
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Value("${video.storage.path}")
    private String storagePath;
    
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
        ".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm"
    );
    
    /**
     * Сканирует папку с видео и добавляет новые файлы в базу данных
     * Ожидает структуру папок:
     *   videos/
     *     ├── aggression_children/
     *     ├── aggression_teacher/
     *     ├── sudden_event/
     *     └── children_unattended/
     * 
     * @return количество добавленных видео
     */
    public int scanAndAddVideos() {
        int addedCount = 0;
        try {
            Path folderPath = Paths.get(storagePath);
            if (!Files.exists(folderPath)) {
                System.out.println("Папка videos не существует: " + storagePath);
                createCategoryFolders(folderPath);
                return 0;
            }
            
            // Создаем папки категорий, если их нет
            createCategoryFolders(folderPath);
            
            // Сканируем все подпапки категорий
            File folder = folderPath.toFile();
            File[] categoryFolders = folder.listFiles(File::isDirectory);
            
            if (categoryFolders == null) {
                return 0;
            }
            
            for (File categoryFolder : categoryFolders) {
                VideoCategory category = detectCategoryFromFolderName(categoryFolder.getName());
                if (category == null) {
                    // Пропускаем папки, которые не соответствуют категориям
                    continue;
                }
                
                File[] files = categoryFolder.listFiles();
                if (files == null) {
                    continue;
                }
                
                for (File file : files) {
                    if (file.isFile() && isVideoFile(file.getName())) {
                        // Проверяем, есть ли уже это видео в базе
                        String fileName = file.getName();
                        boolean exists = videoRepository.findAll().stream()
                            .anyMatch(v -> v.getFileName().equals(fileName) && 
                                          v.getFilePath().equals(file.getAbsolutePath()));
                        
                        if (!exists) {
                            Video video = createVideoFromFile(file, category);
                            videoRepository.save(video);
                            addedCount++;
                            System.out.println("Добавлено видео: " + categoryFolder.getName() + "/" + fileName + 
                                             " (категория: " + category + ")");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при сканировании папки: " + e.getMessage());
            e.printStackTrace();
        }
        
        return addedCount;
    }
    
    /**
     * Создает папки для всех категорий, если их нет
     */
    private void createCategoryFolders(Path basePath) {
        try {
            String[] categoryFolders = {
                "aggression_children",
                "aggression_teacher",
                "sudden_event",
                "children_unattended"
            };
            
            for (String folderName : categoryFolders) {
                Path categoryPath = basePath.resolve(folderName);
                if (!Files.exists(categoryPath)) {
                    Files.createDirectories(categoryPath);
                    System.out.println("Создана папка категории: " + folderName);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при создании папок категорий: " + e.getMessage());
        }
    }
    
    private boolean isVideoFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return VIDEO_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }
    
    private Video createVideoFromFile(File file, VideoCategory category) {
        Video video = new Video();
        video.setFileName(file.getName());
        video.setFilePath(file.getAbsolutePath());
        video.setCategory(category);
        
        // Пытаемся извлечь дату и время из имени файла
        LocalDateTime recordedAt = parseDateTimeFromFileName(file.getName());
        
        // Если не удалось распарсить из имени, используем дату создания файла
        if (recordedAt == null) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(
                    file.toPath(), 
                    BasicFileAttributes.class
                );
                recordedAt = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(),
                    ZoneId.systemDefault()
                );
            } catch (Exception e) {
                recordedAt = LocalDateTime.now();
            }
        }
        
        video.setRecordedAt(recordedAt);
        video.setUploadedAt(LocalDateTime.now());
        video.setSize(file.length());
        
        return video;
    }
    
    /**
     * Парсит дату и время из имени файла
     * Поддерживаемые форматы:
     * - YYYY-MM-DD_HH-MM-SS.mp4
     * - YYYYMMDD_HHMMSS.mp4
     * - YYYY-MM-DD HH-MM-SS.mp4
     * - YYYYMMDDHHMMSS.mp4
     * - и другие варианты
     * 
     * @param fileName имя файла
     * @return LocalDateTime или null, если не удалось распарсить
     */
    private LocalDateTime parseDateTimeFromFileName(String fileName) {
        // Убираем расширение файла
        String nameWithoutExt = fileName;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = fileName.substring(0, lastDot);
        }
        
        // Различные паттерны для парсинга даты и времени
        DateTimeFormatter[] formatters = {
            // YYYY-MM-DD_HH-MM-SS
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"),
            // YYYYMMDD_HHMMSS
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"),
            // YYYY-MM-DD HH-MM-SS
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"),
            // YYYYMMDDHHMMSS
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            // YYYY-MM-DD_HHMMSS
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"),
            // YYYYMMDD_HH-MM-SS
            DateTimeFormatter.ofPattern("yyyyMMdd_HH-mm-ss"),
            // YYYY-MM-DD_HH:MM:SS
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"),
            // YYYY-MM-DD HH:MM:SS
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            // YYYYMMDD_HH:MM:SS
            DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss"),
        };
        
        // Пробуем каждый формат
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(nameWithoutExt, formatter);
            } catch (DateTimeParseException e) {
                // Продолжаем пробовать другие форматы
            }
        }
        
        // Если точные форматы не подошли, пытаемся найти дату/время через regex
        // Паттерн: YYYY-MM-DD или YYYYMMDD, затем время HH-MM-SS или HHMMSS
        Pattern pattern = Pattern.compile(
            "(\\d{4})[-_]?(\\d{2})[-_]?(\\d{2})[-_\\s]?(\\d{2})[-:]?(\\d{2})[-:]?(\\d{2})?"
        );
        Matcher matcher = pattern.matcher(nameWithoutExt);
        
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                int minute = Integer.parseInt(matcher.group(5));
                int second = matcher.group(6) != null ? Integer.parseInt(matcher.group(6)) : 0;
                
                return LocalDateTime.of(year, month, day, hour, minute, second);
            } catch (Exception e) {
                // Если не удалось распарсить, возвращаем null
            }
        }
        
        return null;
    }
    
    /**
     * Определяет категорию по названию папки
     * @param folderName название папки
     * @return VideoCategory или null, если папка не соответствует ни одной категории
     */
    private VideoCategory detectCategoryFromFolderName(String folderName) {
        String lowerName = folderName.toLowerCase().trim();
        
        // Проверяем точные совпадения с названиями папок
        if (lowerName.equals("aggression_children") || 
            lowerName.equals("aggression-children") ||
            lowerName.equals("агрессия_дети") ||
            lowerName.equals("агрессия-дети")) {
            return VideoCategory.AGGRESSION_BETWEEN_CHILDREN;
        }
        
        if (lowerName.equals("aggression_teacher") || 
            lowerName.equals("aggression-teacher") ||
            lowerName.equals("агрессия_воспитатель") ||
            lowerName.equals("агрессия-воспитатель")) {
            return VideoCategory.AGGRESSION_TEACHER;
        }
        
        if (lowerName.equals("sudden_event") || 
            lowerName.equals("sudden-event") ||
            lowerName.equals("внезапное_событие") ||
            lowerName.equals("внезапное-событие")) {
            return VideoCategory.SUDDEN_EVENT;
        }
        
        if (lowerName.equals("children_unattended") || 
            lowerName.equals("children-unattended") ||
            lowerName.equals("дети_без_присмотра") ||
            lowerName.equals("дети-без-присмотра")) {
            return VideoCategory.CHILDREN_UNATTENDED;
        }
        
        // Если не найдено точное совпадение, возвращаем null
        return null;
    }
}

