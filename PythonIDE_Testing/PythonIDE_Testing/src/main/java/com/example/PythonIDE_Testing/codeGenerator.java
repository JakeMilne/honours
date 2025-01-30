package com.example.PythonIDE_Testing;
//
//could be renamed to codeGenerator, codeGenerator instance can then be passed between components
//have a field for initial prompt
//array of drafts
//array of parameters
//array of example outputs
//value which caps iterations
//could track amount of iterations + issues at each iteration
//maybe an ID of some sort to store multiple files



import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.List;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;

public class codeGenerator {

    private String prompt;
    private String[] drafts;
//    private String[] parameters;
    private String parameters;
//    private String[] exampleOutputs;
    private String exampleOutputs;
    private int iterationCap = 3;
    private int iterationCount = 0;
    private String[][] issues;



    public codeGenerator() {}

    public codeGenerator(String prompt, String parameters, String exampleOutputs) {
        this.prompt = prompt;
        this.parameters = parameters;
        this.exampleOutputs = exampleOutputs;
        this.iterationCount = 0;
    }

    public static String callLM(String prompt) {
        try {
            String url = "http://localhost:1234/v1/chat/completions";

            Gson gson = new Gson();
            Map<String, Object> requestBody = Map.of(
                    "model", "meta-llama-3.1-8b-instruct",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user"),
                            Map.of("role", "user", "content", prompt)
                    )
            );
            String jsonBody = gson.toJson(requestBody);

            System.out.println("Request JSON Body: " + jsonBody);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//            System.out.println("Response Status Code: " + response.statusCode());
//            System.out.println("Response Body: " + response.body());

            if (response.statusCode() != 200) {
                return String.format("{\"error\": \"API responded with status code %d: %s\"}",
                        response.statusCode(), response.body());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to communicate with LLM service: " + e.getMessage() + "\"}";
        }
    }


    public String regenerateForVulnerability(String code, ArrayList<Vulnerability> vulnerabilities){
        try {
            String vulnerability = "";
            for(Vulnerability v : vulnerabilities){
                vulnerability += v.toString() + "\n";
            }
            String url = "http://localhost:1234/v1/chat/completions";
            String prompt = "Fix the following vulnerability(s): \n" + vulnerability + "\n found in this code: " + code;
            Gson gson = new Gson();
            Map<String, Object> requestBody = Map.of(
                    "model", "meta-llama-3.1-8b-instruct",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user"),
                            Map.of("role", "user", "content", prompt)
                    )
            );
            String jsonBody = gson.toJson(requestBody);

            System.out.println("Request JSON Body: " + jsonBody);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() != 200) {
                return String.format("{\"error\": \"API responded with status code %d: %s\"}",
                        response.statusCode(), response.body());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to communicate with LLM service: " + e.getMessage() + "\"}";
        }
    }
    public void incrementIterationCount(){
        this.iterationCount++;
    }
    public int getIterationCount(){
        return this.iterationCount;
    }
}
