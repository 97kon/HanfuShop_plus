package com.clothrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

@EnableScheduling
@SpringBootApplication
public class ClothRentApplication {

    private static  final Logger logger= LoggerFactory.getLogger(ClothRentApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ClothRentApplication.class, args);
        String serverPort = context.getEnvironment().getProperty("server.port");
        String path = context.getEnvironment().getProperty("server.servlet.context-path");
        if(StringUtils.isEmpty(path)){
            path="/";
        }
        logger.info("This project is started at http://localhost:" + serverPort + path);
    }
}


