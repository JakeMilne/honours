
package com.example.PythonIDE_Testing;

import org.glassfish.tyrus.server.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@ServletComponentScan
public class PythonIdeTestingApplication {

    //starting point, either use SpringApplication.run to use it as a website, or Evaluation.eval() to run testing
    public static void main(String[] args) {
        SpringApplication.run(PythonIdeTestingApplication.class, args);

//        Evaluation.eval();


    }


}


