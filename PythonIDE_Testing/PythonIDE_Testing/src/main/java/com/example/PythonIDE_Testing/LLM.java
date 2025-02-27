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

    public HttpRequest buildCall(String prompt, codeGenerator generator){

        Gson gson = new Gson();
        Map<String, Object> requestBody = buildPrompt(prompt, generator);
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

    public Map<String, Object> buildPrompt(String prompt, codeGenerator generator){

        if((generator.getParamNames().length == 0) && (generator.getOutputNames().length == 0)){

            Map<String, Object> requestBody = Map.of(
                    "model", this.model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE. unit tests must use the unittest package."),

                            Map.of("role", "user", "content", prompt)
                    )
            );
            return requestBody;
        }else if(generator.getParamNames().length == 0){
            String[] outputNames = generator.getOutputNames();
            String[] outputValues = generator.getOutputValues();
            prompt = prompt + " you must also create and implement a unit test for the code for the following output(s): ";
            for(int i = 0; i < generator.getOutputNames().length; i++){
                prompt = prompt + " name: " + outputNames[i] + " value: " + outputValues[i];
            }
            Map<String, Object> requestBody = Map.of(
                    "model", this.model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE. unit tests must use the unittest package."),

                            Map.of("role", "user", "content", prompt)
                    )
            );
            return requestBody;
        }else{ //might need another for no outputs, but how can they even test that?????
            String[] paramNames = generator.getParamNames();
            String[] paramValues = generator.getParamValues();
            String[] outputNames = generator.getOutputNames();
            String[] outputValues = generator.getOutputValues();

            prompt = prompt + " you must also create and implement a unit test for the code for the following input(s): ";
            for(int i = 0; i < generator.getParamNames().length; i++){
                prompt = prompt + " name: " + paramNames[i] + " value: " + paramValues[i];
                System.out.println("name: " + paramNames[i] + " value: " + paramValues[i]);
            }

            prompt = prompt + " and output(s): ";
            for(int i = 0; i < generator.getOutputNames().length; i++){
                prompt = prompt + " name: " + outputNames[i] + " value: " + outputValues[i];
            }

            System.out.println(prompt);
            Map<String, Object> requestBody = Map.of(
                    "model", this.model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE. unit tests must use the unittest package."),

                            Map.of("role", "user", "content", prompt)
                    )
            );
            return requestBody;
        }



//        return requestBody;
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