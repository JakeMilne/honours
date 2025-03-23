package com.example.PythonIDE_Testing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;


//used to handle the response from the LLMs, raw response looks like:

//{"id":"chatcmpl-BEEo6i9luOS9HkG7NkXkg7ZPHsZk9","object":"chat.completion","created":1742733210,"model":"gpt-4o-mini-2024-07-18","choices":[{"index":0,"message":{"role":"assistant","content":"```python\nclass HelloWorld:\n    def greet(self):\n        print(\"Hello, World!\")\n\nif __name__ == \"__main__\":\n    hw = HelloWorld()\n    hw.greet()\n```","refusal":null,"annotations":[]},"logprobs":null,"finish_reason":"stop"}],"usage":{"prompt_tokens":169,"completion_tokens":40,"total_tokens":209,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}},"service_tier":"default","system_fingerprint":"fp_b8bc95a0ac"}


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

        // '''python <some code> ''' is how the APIs format their responses/ identify what is code and what is plaintext
        String code = response.substring(response.indexOf("```python") + 10, response.lastIndexOf("```"));
        System.out.println("Extracted code:");
        System.out.println(code);

        return code;
    }


}
