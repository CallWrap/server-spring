package com.callwrap;

import com.callwrap.audio.config.AudioStorageProperties;
import com.callwrap.counselor.config.CounselorHealthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({AudioStorageProperties.class, CounselorHealthProperties.class})
@SpringBootApplication
public class CallWrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallWrapApplication.class, args);
    }
}
