package com.anping.music;

import com.anping.music.service.TokenRefresh;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.anping.music.*")
public class MusicApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(MusicApplication.class, args);
        TokenRefresh tokenRefresh = applicationContext.getBean(TokenRefresh.class);
        tokenRefresh.startService();
    }

}
