package com.nestvision.dto;

import com.nestvision.entity.VideoCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private Long id;
    private String fileName;
    private String filePath;
    private VideoCategory category;
    private LocalDateTime recordedAt;
    private LocalDateTime uploadedAt;
    private Long duration;
    private Long size;
}

