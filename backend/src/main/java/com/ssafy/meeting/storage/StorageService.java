package com.ssafy.meeting.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String save(MultipartFile file, String category);
    Resource load(String filePath);
    void delete(String filePath);
}
