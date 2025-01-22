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

public class LLMHandler {

    public LLMHandler() {}

    public static String callLM(String prompt) {
        try {
            String url = "http://localhost:1234/v1/chat/completions";

            Gson gson = new Gson();
            Map<String, Object> requestBody = Map.of(
                    "model", "meta-llama-3.1-8b-instruct",
                    "messages", List.of(
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

            System.out.println("Response Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

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
}
