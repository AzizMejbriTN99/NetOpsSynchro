package com.mejbri.pfe.netopssynchro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NetOpsSynchroApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetOpsSynchroApplication.class, args);
    }

}
