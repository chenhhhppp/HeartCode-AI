package com.chp.heartcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class HeartCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartCodeApplication.class, args);
    }

}
