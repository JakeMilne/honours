
package com.example.PythonIDE_Testing;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Component
//public class MyWebSocketHandler extends TextWebSocketHandler {
//
//    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        sessions.put(session.getId(), session);
//        System.out.println("WebSocket Connected: " + session.getId());
//        session.sendMessage(new TextMessage("{\"type\":\"session_id\",\"data\":\"" + session.getId() + "\"}"));
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        sessions.remove(session.getId());
//        System.out.println("WebSocket Disconnected: " + session.getId());
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        System.out.println("Received: " + message.getPayload());
//        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
//    }
//
//    public static WebSocketSession getSessionById(String sessionId) {
//        return sessions.get(sessionId);
//    }
//
//    public static void sendToSession(String sessionId, String message) {
//        WebSocketSession session = sessions.get(sessionId);
//        if (session != null && session.isOpen()) {
//            try {
//                session.sendMessage(new TextMessage(message));
//            } catch (IOException e) {
//                System.err.println("Error sending message to session " + sessionId + ": " + e.getMessage());
//            }
//        } else {
//            System.err.println("Session not found or not open: " + sessionId);
//        }
//    }
//}


public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = Json.createReader(new StringReader(message.getPayload())).readObject();
        String type = jsonMessage.getString("type");

        if ("runCode".equals(type)) {
            String userCode = jsonMessage.getString("code");
            String result = runUserCode(userCode);
            sendMessageToUser(session, result);
        }
    }

    private String runUserCode(String code) {
        dockerHandler.saveUserFile(code, "/app/usercode.py");

        // need to call (+update) the runfile function in dockerHandler. maybe have another function to deal with interacting, call it the first time in runfile, then have a while in here that runs until the python is finished. would probably need to figure out how to identify the end of a python file, cant just go off of lines output
        return "Executed code: " + code;
    }

    private void sendMessageToUser(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}

