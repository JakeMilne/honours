
package com.example.PythonIDE_Testing;

import org.glassfish.tyrus.server.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@ServletComponentScan
public class PythonIdeTestingApplication {

	public static void main(String[] args) {
//		SpringApplication.run(PythonIdeTestingApplication.class, args);
		Evaluation.eval();
	}

//	private static void startWebSocketServer() {
//		Map<String, Object> serverConfig = new HashMap<>();
//
//		Server server = new Server("localhost", 8082, "/socket", serverConfig, WebSocket.class);
//
//		try {
//			server.start();
//			System.out.println("WebSocket server started at ws://localhost:8082/socket");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}


//String uri = "ws://localhost:8082/socket";  // WebSocket URI
//Server server = new Server("localhost", 8082, "/socket", WebSocket.class);
//
//            try {
//					// Start the server
//					server.start();
//                System.out.println("WebSocket server started at ws://localhost:8082/socket");
//                Thread.sleep(60000);  // Keep server running for 60 seconds
//            } catch (Exception e) {
//		e.printStackTrace();
//            } finally {
//					server.stop();
//            }