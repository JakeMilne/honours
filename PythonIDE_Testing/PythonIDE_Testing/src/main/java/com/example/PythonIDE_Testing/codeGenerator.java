package com.example.PythonIDE_Testing;

//this class is used to call the LLMs via the LLM class

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
    //    private String[] drafts;
    private String parameters;
    private String[] outputNames;
    private String[] outputValues;
    public int iterationCap = 5;
    private int iterationCount = 1;
    private String[][] issues;
    private LLM llm;
    private LLM deepseekLlm;
    private String[] paramNames;
    private String[] paramValues;

    public codeGenerator() {
    }

    public codeGenerator(String prompt, String[] paramNames, String[] outputNames, String[] paramValues, String[] outputValues) {
        this.prompt = prompt;
        this.parameters = "";
        this.outputNames = outputNames;
        this.outputValues = outputValues;
//        this.iterationCount = 1;
        this.paramNames = paramNames;
        this.paramValues = paramValues;
        for (String string : paramNames) {
            System.out.println("Param Name: " + string);
        }
        for (String string : paramValues) {
            System.out.println("Param Value: " + string);
        }

        //originaly "deepseekLlm" was actually a deepseek LLM, but I found it often took too long to respond, which ended in errors while I was testing. so instead im using another OpenAI model, deepseekLlm could be replaced with any other model as long as it works with the prompt formatting, I used a local LLM via LM studio for a while, and it worked fine but was slow to respond
        this.llm = new LLM("https://api.openai.com/v1/chat/completions", "gpt-4o-mini", true, System.getenv("OPENAI_API_KEY"));
//        this.deepseekLlm = new LLM("https://api.deepseek.com/chat/completions", "deepseek-chat", true, System.getenv("DEEPSEEK_API_KEY"));

        this.deepseekLlm = new LLM("https://api.openai.com/v1/chat/completions", "gpt-4o", true, System.getenv("OPENAI_API_KEY"));
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

    //getting the initial code from the LLM,  building the API call via the LLM class then calling it
    public String callLM(String prompt) {

        try {

            HttpRequest request = llm.buildCall(prompt, this);
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

    //regenerating code when theres a vulnerability, similar to callLM but the prompt is different
    public String regenerateForVulnerability(String code, ArrayList<Issue> vulnerabilities, boolean OpenAI) {
        try {


            HttpRequest request = this.llm.buildVulnCall(vulnerabilities, code);
            if (!OpenAI) {

                System.out.println("Deepseek LLM instance: " + this.deepseekLlm);
                request = this.deepseekLlm.buildVulnCall(vulnerabilities, code);


            }
            System.out.println("Request: " + request);
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response);

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


    //pretty much the same as regenerateForVulnerability but the prompt is slghtly different
    public String regenerateForErrors(String code, String errors, boolean OpenAI) {
        try {

            HttpRequest request = this.llm.buildErrorCall(errors, code);
            if (!OpenAI) {
                request = this.deepseekLlm.buildErrorCall(errors, code);
            }
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
    public void incrementIterationCount() {
        this.iterationCount++;
    }

    public int getIterationCount() {
        return this.iterationCount;
    }


}
