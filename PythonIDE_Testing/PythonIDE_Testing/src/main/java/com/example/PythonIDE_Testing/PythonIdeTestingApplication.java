
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

    public static void main(String[] args) {
        SpringApplication.run(PythonIdeTestingApplication.class, args);
//        Evaluation.eval();
//		String code = "import unittest\n" +
//				"\n" +
//				"class Calculator:\n" +
//				"    def add_and_square(self, a, b):\n" +
//				"        return (a + b) ** 2\n" +
//				"    def add_and_notsquare(self, a, b):\n" +
//				"		 input(\"What is your age? \")" +
//				"        return a + b\n" +
//				"\n" +
//				"class TestCalculator(unittest.TestCase):\n" +
//				"    def test_add_and_square(self):\n" +
//				"        calc = Calculator()\n" +
//				"        result = calc.add_and_square(18, 7)\n" +
//				"        self.assertEqual(result, 625)\n" +
//				"\n" +
//				"if __name__ == '__main__':\n" +
//				"    name = input(\"What is your name? \")\n" +
//				"    age = input(\"What is your age? \")\n" +
//				"    print(f'Hello {name}, you are {age} years old.')\n" +
//				"    unittest.main()\n";
//
//		PythonChunk rootChunk = new PythonChunk(code, "root").parse(code);
//		rootChunk.print(0);
//		ArrayList<PythonChunk> chunks = rootChunk.getChildren();
//		for (PythonChunk chunk : chunks) {
//			chunk.print(0);
//		}


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