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

    private ArrayList<Iteration> iterations = new ArrayList<Iteration>();

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

        codeGenerator generator = new codeGenerator(userprompt, parameter, exampleOutputs);
        String callresponse = generator.callLM(userprompt);
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";

        runTests(generator, responseHandler, content, response, writer);

        int index = iterations.size() - 1;
        System.out.println("Iterations size: " + iterations.size());

        writer.println("<h1>HELLO</h1>");

        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
        writer.println(iterations.get(index).getCode());
        System.out.println("Iteration code: " + iterations.get(index).getCode());

                if (index > 0) {
            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous</a> ");
        }
        if (index < iterations.size() - 1) {
            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next</a>");
        }


        writer.close();
    }

//        if (index > 0) {
//            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous</a> ");
//        }
//        if (index < iterations.size() - 1) {
//            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next</a>");
//        }

    public void runTests(codeGenerator generator, ResponseHandler responseHandler, String content,
                         HttpServletResponse response, PrintWriter writer) {
        dockerHandler docker = new dockerHandler();

            String filePath = "/tmp/script.py";
            docker.saveFile(content, filePath);
            ArrayList<Vulnerability> vulnerabilities = docker.runBanditOnFile(filePath);
            iterations.add(new Iteration(content, vulnerabilities));
            String newContent = "";

            while(!vulnerabilities.isEmpty() && (generator.getIterationCount() <= generator.iterationCap)) {
                generator.incrementIterationCount();
                newContent = generator.regenerateForVulnerability(content, vulnerabilities);
                String newCode = responseHandler.extractCode(newContent);
                filePath = "/tmp/script" + generator.getIterationCount() + ".py";
                docker.saveFile(newCode, filePath);
                vulnerabilities = docker.runBanditOnFile(filePath);
                newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
                iterations.add(new Iteration(newCode, vulnerabilities));
            }

            if(generator.getIterationCount() > generator.iterationCap) {
                writer.println("<h1>Iteration cap reached</h1>");
            }


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