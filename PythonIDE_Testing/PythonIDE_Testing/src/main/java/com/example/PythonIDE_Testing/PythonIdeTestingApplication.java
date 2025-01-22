package com.example.PythonIDE_Testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class PythonIdeTestingApplication {
	public static void main(String[] args) {
		SpringApplication.run(PythonIdeTestingApplication.class, args);
	}



}