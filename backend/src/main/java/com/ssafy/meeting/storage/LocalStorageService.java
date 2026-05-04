package com.ssafy.meeting.storage;

import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${storage.base-path:./data}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String save(MultipartFile file, String category) {
        String extension = extension(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        String relativePath = category + "/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + extension;
        Path destination = basePath.resolve(relativePath).normalize();
        if (!destination.startsWith(basePath)) {
            throw new ValidationException("Invalid storage path");
        }
        try {
            Files.createDirectories(destination.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination);
            }
            log.info("File stored category={} path={} size={}", category, relativePath, file.getSize());
            return relativePath.replace('\\', '/');
        } catch (IOException exception) {
            log.error("File storage failed category={} name={}", category, file.getOriginalFilename(), exception);
            throw new ValidationException("Could not store file");
        }
    }

    @Override
    public Resource load(String filePath) {
        try {
            Path target = basePath.resolve(filePath).normalize();
            if (!target.startsWith(basePath) || !Files.exists(target)) {
                throw new ResourceNotFoundException("File not found");
            }
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not readable");
            }
            return resource;
        } catch (IOException exception) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path target = basePath.resolve(filePath).normalize();
            if (target.startsWith(basePath)) {
                Files.deleteIfExists(target);
                log.info("File deleted path={}", filePath);
            }
        } catch (IOException exception) {
            log.warn("File delete failed path={} reason={}", filePath, exception.getMessage());
        }
    }

    private String extension(String fileName) {
        String cleanName = StringUtils.cleanPath(fileName == null ? "upload" : fileName);
        int dot = cleanName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return cleanName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
