package com.assetbox.common.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // TODO: seed SUPER_ADMIN and SYSTEM users after User domain is implemented.
    }
}
