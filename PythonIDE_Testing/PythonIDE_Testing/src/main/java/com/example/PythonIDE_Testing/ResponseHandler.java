package com.example.PythonIDE_Testing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class ResponseHandler {
    public ResponseHandler() {
    }

    public static String extractContent(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);

            JsonArray choices = responseObject.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message"); //message is made of "role" and "content", we want content
                return message.get("content").getAsString();
            }

            return "No content found in the response";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }

    public static String extractCode(String jsonResponse) {
        String response = extractContent(jsonResponse);
        String code = response.substring(response.indexOf("```python") + 10, response.lastIndexOf("```"));
        System.out.println("Extracted code:");
        System.out.println(code);

        return code;
    }


}
