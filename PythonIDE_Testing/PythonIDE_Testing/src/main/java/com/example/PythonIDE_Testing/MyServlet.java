package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.http.HttpResponse;
import java.util.ArrayList;


@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userprompt = request.getParameter("userprompt");
        String parameter = request.getParameter("parameters");
        String exampleOutputs = request.getParameter("exampleOutputs");


        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("<h1>parameters: " + parameter + "</h1>");
        writer.println("<h1>example output(s): " + exampleOutputs + "</h1>");
        codeGenerator handler = new codeGenerator(userprompt, parameter, exampleOutputs);
//        String response = handler.callLM(userprompt);
        String callresponse = handler.callLM(userprompt);
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";
//        String content = responseHandler.extractContent(callresponse);
        dockerHandler docker = new dockerHandler();
        docker.saveFile(content);
        ArrayList<Vulnerability> vulnerabilities = docker.runBanditOnFile();
//        System.out.println(content);
        String newContent = "";
        writer.println("<h1>Initial Code: " + content + "</h1>");
        writer.println("</html>");
        if(!vulnerabilities.isEmpty()) {
            newContent = handler.regenerateForVulnerability(content, vulnerabilities);
            String newCode = responseHandler.extractCode(newContent);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
            writer.println("<h1>fixed code: " + newCode + "</h1>");
            writer.println("</html>");
        }

        writer.close();
    }





}

//python response looks like

//```python
//# This is a comment - anything after the "#" symbol is ignored by the interpreter
//
//# The print function is used to output text to the screen
//print("Hello, World!")
//```
// can parse using the ```python part