//package com.example.PythonIDE_Testing;
//
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//import javax.websocket.OnClose;
//import javax.websocket.OnMessage;
//import javax.websocket.OnOpen;
//import javax.websocket.Session;
//import javax.websocket.server.ServerEndpoint;
//import java.util.concurrent.ConcurrentHashMap;
//import java.io.IOException;
//import jakarta.json.Json;
//import jakarta.json.JsonObject;
//import java.io.StringReader;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//
//@ServerEndpoint("/socket")
//public class WebSocket extends TextWebSocketHandler {
//
//    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
//
//    @OnOpen
//    public void onOpen(Session session) {
//        System.out.println("=== WebSocket Opened ===");
//        System.out.println("Session ID: " + session.getId());
//        sessions.put(session.getId(), session);
//
//        try {
//            session.getBasicRemote().sendText("{\"type\": \"session_id\", \"data\": \"" + session.getId() + "\"}");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @OnClose
//    public void onClose(Session session) {
//        sessions.remove(session.getId());
//        System.out.println("WebSocket closed: " + session.getId());
//    }
//
//    @OnMessage
//    public void onMessage(String message, Session session) {
//        JsonObject jsonMessage = Json.createReader(new StringReader(message)).readObject();
//
//        if ("session_id".equals(jsonMessage.getString("type"))) {
//            String sessionId = jsonMessage.getString("data");
//            sessions.put(sessionId, session);
//            System.out.println("WebSocket session ID received: " + sessionId);
//        } else if ("code".equals(jsonMessage.getString("type"))) {
//            String pythonCode = jsonMessage.getString("data");
//            String result = executePythonCodeInDocker(pythonCode);
//
//            try {
//                session.getBasicRemote().sendText("{\"type\": \"result\", \"data\": \"" + result + "\"}");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private String executePythonCodeInDocker(String pythonCode) {
//        try {
//            String filePath = "/tmp/script.py";
//            savePythonCodeToFileInDocker(filePath, pythonCode);
//
//            ProcessBuilder builder = new ProcessBuilder(
//                    "docker", "exec", "-i", "pythonide_testing-app-1", "python3", filePath
//            );
//            Process process = builder.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//            }
//            process.waitFor();
//            return output.toString();
//
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//            return "Error running the Python code.";
//        }
//    }
//
//    private void savePythonCodeToFileInDocker(String filePath, String pythonCode) {
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", "-i", "pythonide_testing-app-1", "sh", "-c", "cat > " + filePath);
//            Process process = processBuilder.start();
//
//            try (OutputStream outputStream = process.getOutputStream()) {
//                outputStream.write(pythonCode.getBytes());
//                outputStream.flush();
//            }
//
//            process.waitFor();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static Session getSessionById(String sessionId) {
//        return sessions.get(sessionId);
//    }
//}
