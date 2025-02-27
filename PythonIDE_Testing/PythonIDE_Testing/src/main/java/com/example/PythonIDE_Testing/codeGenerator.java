package com.example.PythonIDE_Testing;



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
    private String[] outputNames;
    private String[] outputValues;
    public int iterationCap = 3;
    private int iterationCount = 0;
    private String[][] issues;
    private LLM llm;
    private String[] paramNames;
    private String[] paramValues;
//    private LLM llm = new LLM("http://localhost:1234/v1/chat/completions", "meta-llama-3.1-8b-instruct", false, "");
//    private LLM llm = new LLM("https://api.openai.com/v1/chat/completions", "gpt-4o-mini", true, System.getenv("OPENAI_API_KEY"));

    public codeGenerator() {}

    public codeGenerator(String prompt, String[] paramNames, String[] outputNames, String[] paramValues, String[] outputValues) {
        this.prompt = prompt;
        this.parameters = "";
        this.outputNames = outputNames;
        this.outputValues = outputValues;
        this.iterationCount = 0;
        this.paramNames = paramNames;
        this.paramValues = paramValues;
        for(String string : paramNames){
            System.out.println("Param Name: " + string);
        }
        for(String string : paramValues){
            System.out.println("Param Value: " + string);
        }
        this.llm = new LLM("https://api.openai.com/v1/chat/completions", "gpt-4o-mini", true, System.getenv("OPENAI_API_KEY"));


    }


    public String getPrompt() {
        return prompt;
    }

    public String[] getOutputNames() {
        return outputNames;
    }

    public String[] getOutputValues() {
        return outputValues;
    }

    public String[] getParamNames() {
        return paramNames;
    }

    public String[] getParamValues() {
        return paramValues;
    }

    //getting the initial code from the LLM
    public String callLM(String prompt) {

//        private LLM llm = new LLM("http://localhost:1234/v1/chat/completions", "meta-llama-3.1-8b-instruct", false, "");
//        LLM llm = new LLM("https://api.openai.com/v1/chat/completions", "gpt-4o-mini", true, System.getenv("OPENAI_API_KEY"));


        try {

//            Gson gson = new Gson();
//            String url = llm.getUrl();
//
//
//                Map<String, Object> requestBody = Map.of(
//                        "model", llm.getModel(),
//                        "messages", List.of(
//                                Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less. UNDER NO CIRCUMSTANCES WHATSOEVER SHOULD YOU GIVE ME MORE THAN 1 BLOCK OF CODE."),
//
//                                Map.of("role", "user", "content", prompt)
//                        )
//                );
//
//            String jsonBody = gson.toJson(requestBody);

            HttpRequest request = llm.buildCall(prompt, this);


            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                    .build();
//
//            if(llm.getNeedsKey()){
//                request = HttpRequest.newBuilder()
//                        .uri(URI.create(url))
//                        .header("Content-Type", "application/json")
//                        .header("Authorization", "Bearer " + llm.getKey())
//                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                        .build();
//            }

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

    //regenerating code when theres a vulnerability
    public String regenerateForVulnerability(String code, ArrayList<Vulnerability> vulnerabilities){
        try {



//                String vulnerability = "";
//                for (Vulnerability v : vulnerabilities) {
//                    vulnerability += v.toString() + "\n";
//                }
//                String url = "http://localhost:1234/v1/chat/completions";
//                String prompt = "Fix the following vulnerability(s): \n" + vulnerability + "\n found in this code: " + code;
//                Gson gson = new Gson();
//                Map<String, Object> requestBody = Map.of(
//                        "model", "meta-llama-3.1-8b-instruct",
//                        "messages", List.of(
//                                Map.of("role", "system", "content", "You are a coding assistant that creates python code. Only Create python 3.9 code, offer no explanation, do not include anything in your answer other than python code. Only return 1 piece of code. Tag all code with ```python. DO not create any code that is not specified by the user. All code generated must be in the same class and file. All code must be placed inside the same block. Only return python code, and do not provide any additional context or instructions to the user, unless they are in a comment inside the python code. your response must contain exactly 1 block of python code, no more or less"),
//
//                                Map.of("role", "user", "content", prompt)
//                        )
//                );
//                String jsonBody = gson.toJson(requestBody);

//                System.out.println("Request JSON Body: " + jsonBody);

//                HttpRequest request = HttpRequest.newBuilder()
//                        .uri(URI.create(url))
//                        .header("Content-Type", "application/json")
//                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                        .build();

            System.out.println("LLM instance: " + this.llm);


            HttpRequest request = this.llm.buildVulnCall(vulnerabilities, code);
            System.out.println("Request: " + request);
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
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

    //iteraton count is used to cap how many times code can be regenerated
    public void incrementIterationCount(){
        this.iterationCount++;
    }
    public int getIterationCount(){
        return this.iterationCount;
    }


}
