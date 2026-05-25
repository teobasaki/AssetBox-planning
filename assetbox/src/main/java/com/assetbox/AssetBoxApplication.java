package com.assetbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AssetBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetBoxApplication.class, args);
    }
}
