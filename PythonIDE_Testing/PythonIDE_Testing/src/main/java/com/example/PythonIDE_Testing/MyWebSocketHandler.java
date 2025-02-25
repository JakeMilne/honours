
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




public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private dockerHandler docker = new dockerHandler();


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
            runUserCode(userCode, session);
//            sendMessageToUser(session, result);
        }
        if("userInput".equals(type)){
            String userInput = jsonMessage.getString("input");
            docker.addUserInput(userInput);
        }

        if("runBandit".equals(type)){
            String userCode = jsonMessage.getString("code");
            docker.saveFile(userCode, "/tmp/usercode.py", false);
            ArrayList<Vulnerability> vulnerabilities = docker.runBanditOnFile("/tmp/usercode.py");
            sendMessageToUser(session, "Bandit Results:");
            if(vulnerabilities.size() == 0){
                sendMessageToUser(session, "No vulnerabilities found");
            }else{
                for(Vulnerability vulnerability : vulnerabilities){

                    sendMessageToUser(session, vulnerability.toString());
                }
            }

        }
    }

    private void runUserCode(String code, WebSocketSession session) {
        System.out.println(code);
        docker.saveFile(code, "/tmp/usercode.py", false);
        docker.runFile("/tmp/usercode.py", session, code);

        //this might be completely crazy. But, I might need to tokenize the python myself and keep track of input messages to make it work..

        // need to call (+update) the runfile function in dockerHandler. maybe have another function to deal with interacting, call it the first time in runfile, then have a while in here that runs until the python is finished. would probably need to figure out how to identify the end of a python file, cant just go off of lines output
//        return "Executed code: " + code;
    }


    private void sendMessageToUser(WebSocketSession session, String message) {

        try{

                TextMessage textMessage = new TextMessage(message);
                session.sendMessage(textMessage);
        } catch (IOException e){
        e.printStackTrace();
    }




    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }


}

