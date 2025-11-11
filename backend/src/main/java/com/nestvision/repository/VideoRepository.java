package com.nestvision.repository;

import com.nestvision.entity.Video;
import com.nestvision.entity.VideoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByCategoryOrderByRecordedAtDesc(VideoCategory category);
    List<Video> findAllByOrderByRecordedAtDesc();
}

