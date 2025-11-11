package com.nestvision.service;

import com.nestvision.dto.DtoMapper;
import com.nestvision.dto.VideoResponse;
import com.nestvision.entity.Video;
import com.nestvision.entity.VideoCategory;
import com.nestvision.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Autowired
    private DtoMapper dtoMapper;
    
    @Value("${video.storage.path}")
    private String storagePath;
    
    private Path getStoragePath() {
        Path path = Paths.get(storagePath);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
        return path;
    }
    
    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByRecordedAtDesc()
                .stream()
                .map(dtoMapper::toVideoResponse)
                .collect(Collectors.toList());
    }
    
    public List<VideoResponse> getVideosByCategory(VideoCategory category) {
        return videoRepository.findByCategoryOrderByRecordedAtDesc(category)
                .stream()
                .map(dtoMapper::toVideoResponse)
                .collect(Collectors.toList());
    }
    
    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return dtoMapper.toVideoResponse(video);
    }
    
    public Resource getVideoResource(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        
        try {
            Path filePath = Paths.get(video.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Video file not found or not readable");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading video file", e);
        }
    }
    
    public VideoResponse uploadVideo(MultipartFile file, VideoCategory category, LocalDateTime recordedAt) {
        try {
            Path storagePath = getStoragePath();
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "video_" + System.currentTimeMillis() + ".mp4";
            }
            
            Path targetPath = storagePath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            Video video = new Video();
            video.setFileName(fileName);
            video.setFilePath(targetPath.toString());
            video.setCategory(category);
            video.setRecordedAt(recordedAt != null ? recordedAt : LocalDateTime.now());
            video.setUploadedAt(LocalDateTime.now());
            video.setSize(file.getSize());
            
            video = videoRepository.save(video);
            return dtoMapper.toVideoResponse(video);
        } catch (IOException e) {
            throw new RuntimeException("Error uploading video", e);
        }
    }
}

