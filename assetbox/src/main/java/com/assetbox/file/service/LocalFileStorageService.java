package com.assetbox.file.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Override
    public String store(MultipartFile file) {
        return "TODO";
    }

    @Override
    public Resource load(String storageKey) {
        return null;
    }

    @Override
    public void delete(String storageKey) {
    }
}
