package top.hiyorin.clashservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ClashServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClashServiceApplication.class, args);
    }

}
