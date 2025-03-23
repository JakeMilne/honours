
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
import java.io.IOException;
import java.util.ArrayList;


//This is for the code editor, since the run can run the code I'm using this for communication between the user and the docker container
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private dockerHandler docker = new dockerHandler();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    //this handles messages from the user, sent from ide.html, via the runCode, sendInput, and runBandit js functions
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = Json.createReader(new StringReader(message.getPayload())).readObject();
        //type refers to which function the message came from
        String type = jsonMessage.getString("type");

        if ("runCode".equals(type)) {
            String userCode = jsonMessage.getString("code");
            runUserCode(userCode, session);
//            sendMessageToUser(session, result);
        }

        //used for when the user is responding to input requests from the python
        if ("userInput".equals(type)) {
            String userInput = jsonMessage.getString("input");
            docker.addUserInput(userInput);
        }

        if ("runBandit".equals(type)) {
            String userCode = jsonMessage.getString("code");
            docker.saveFile(userCode, "/tmp/usercode.py", false);
            ArrayList<Issue> vulnerabilities = docker.runBanditOnFile("/tmp/usercode.py");
            sendMessageToUser(session, "Bandit Results:");
            if (vulnerabilities.size() == 0) {
                sendMessageToUser(session, "No vulnerabilities found");
            } else {
                for (Issue vulnerability : vulnerabilities) {

                    sendMessageToUser(session, vulnerability.toString());
                }
            }

        }
    }

    private void runUserCode(String code, WebSocketSession session) {
        System.out.println(code);
        docker.saveFile(code, "/tmp/usercode.py", false);
        docker.runFile("/tmp/usercode.py", session, code);

    }


    //used for sending messages to the user
    private void sendMessageToUser(WebSocketSession session, String message) {

        try {

            TextMessage textMessage = new TextMessage(message);
            session.sendMessage(textMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }


}

