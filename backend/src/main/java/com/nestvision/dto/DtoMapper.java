package com.nestvision.dto;

import com.nestvision.entity.User;
import com.nestvision.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getKindergartenName()
        );
    }
    
    public VideoResponse toVideoResponse(Video video) {
        return new VideoResponse(
            video.getId(),
            video.getFileName(),
            video.getFilePath(),
            video.getCategory(),
            video.getRecordedAt(),
            video.getUploadedAt(),
            video.getDuration(),
            video.getSize()
        );
    }
}

