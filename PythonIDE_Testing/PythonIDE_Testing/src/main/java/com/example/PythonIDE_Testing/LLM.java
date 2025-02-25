package com.example.PythonIDE_Testing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.List;
import com.google.gson.Gson;
import java.util.ArrayList;


public class LLM {

    private String url;
    private String model;
    private boolean needsKey;
    private String key;

    public LLM(String url, String model, boolean needsKey, String key){
        this.url = url;
        this.model = model;
        this.needsKey = needsKey;
        this.key = key;

    }



    public String getUrl(){
        return url;
    }

    public String getModel(){
        return model;
    }

    public boolean getNeedsKey(){
        return needsKey;
    }

    public String getKey(){
        return key;
    }

    public HttpRequest buildCall(String prompt){

        Gson gson = new Gson();
        Map<String, Object> requestBody = buildPrompt(prompt);
        String jsonBody = gson.toJson(requestBody);
        HttpRequest request = buildRequest(jsonBody);
        return request;
    }

    public HttpRequest buildVulnCall(ArrayList<Vulnerability> vulnerabilities, String code){

        Gson gson = new Gson();
        Map<String, Object> requestBody = buildVulnPrompt(vulnerabilities, code);
        String jsonBody = gson.toJson(requestBody);
        HttpRequest request = buildRequest(jsonBody);



        return request;
    }

    public Map<String, Object> buildPrompt(String prompt){
        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE."),

                        Map.of("role", "user", "content", prompt)
                )
        );



        return requestBody;
    }

    public Map<String, Object> buildVulnPrompt(ArrayList<Vulnerability> vulnerabilities, String code){
        String vulnerability = "";
        for (Vulnerability v : vulnerabilities) {
            vulnerability += v.toString() + "\n";
        }

        String prompt = "Fix the following vulnerability(s): \n" + vulnerability + "\n found in this code: " + code;

        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE."),

                        Map.of("role", "user", "content", prompt)
                )
        );

        return requestBody;
    }

    public HttpRequest buildRequest(String jsonBody){
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        if(this.needsKey){
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.key)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
        }

        return request;
    }
}