package com.suiyin.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SuiyinApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuiyinApiApplication.class, args);
	}

}
