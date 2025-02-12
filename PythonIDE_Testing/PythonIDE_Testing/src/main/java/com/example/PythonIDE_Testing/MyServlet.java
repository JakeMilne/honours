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

        //storing user input
        String userprompt = request.getParameter("userprompt");
        String parameter = request.getParameter("parameters");
        String exampleOutputs = request.getParameter("exampleOutputs");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        //showing the users input, was/is used for testing
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("<h1>parameters: " + parameter + "</h1>");
        writer.println("<h1>example output(s): " + exampleOutputs + "</h1>");

        //code generator object handles LLM calls
        codeGenerator generator = new codeGenerator(userprompt, parameter, exampleOutputs);
        String callresponse = generator.callLM(userprompt);

        //response handler object parses LLM responses
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";

        //setting session variables
        request.getSession().setAttribute("codeGenerator", generator);
        request.getSession().setAttribute("responseHandler", responseHandler);


        // might need to clear iterations here.





        //runTests method deals with the dockerHandler object and its methods
        runTests(generator, responseHandler, content, response, writer, request);

        //some stuff to display the different iterations of the code
        int index = iterations.size() - 1;
        System.out.println("Iterations size: " + iterations.size());

//        writer.println("<h1>HELLO</h1>");

        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
//        writer.println(iterations.get(index).getCode());
//        System.out.println("Iteration code: " + iterations.get(index).getCode());


        //getting rid of pesky html tags
        String code = iterations.get(index).getCode().replace("<br>", "\n").replace("<pre>", "").replace("</pre>", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");



        System.out.println("reformatted code: " + code);
        String sessionId = request.getSession().getId(); // Get the session ID
        writer.println("<form action=\"/userIDE\" method=\"POST\">");
        writer.println("<input type=\"hidden\" name=\"session_id\" value=\"" + sessionId + "\">");
        writer.println("<label for=\"usercode\">code:</label><br>");
        writer.println("<textarea id=\"usercode\" name=\"usercode\" rows=\"20\" cols=\"80\">" + code + "</textarea><br>");
        writer.println("<input type=\"submit\" value=\"Check\">");
        writer.println("</form>");

//        Session webSocketSession = WebSocket.getSessionById(webSocketSessionId);

        // I think i need to pass the session id here when the users goes to a different iteration
        if (index > 0) {
            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous iteration</a> ");
        }
        if (index < iterations.size() - 1) {
            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next iteration</a>");
        }


        writer.close();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String indexParam = request.getParameter("index");
        int index = (indexParam != null) ? Integer.parseInt(indexParam) : iterations.size() - 1;

        String userprompt = request.getParameter("userprompt");
        String parameter = request.getParameter("parameters");
        String exampleOutputs = request.getParameter("exampleOutputs");

        //showing stuff to the user
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("<h1>Parameters: " + parameter + "</h1>");
        writer.println("<h1>Example output(s): " + exampleOutputs + "</h1>");
        if(iterations.get(index).getVulnerabilities().size() > 0) {
            writer.println("<h1>Vulnerabilities Detected:</h1>");
            writer.println("<ul>");
            for (Vulnerability vuln : iterations.get(index).getVulnerabilities()) {
                writer.println("<li>" + vuln.getDescription() + "</li>");
            }
            writer.println("</ul>");
        } else {
            writer.println("<h1>No Vulnerabilities Detected</h1>");
        }


        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
        String code = iterations.get(index).getCode().replace("<br>", "\n").replace("<pre>", "").replace("</pre>", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");


        writer.println("<form action=\"/userIDE\" method=\"POST\">");
        writer.println("<label for=\"usercode\">code:</label><br>");
        writer.println("<textarea id=\"usercode\" name=\"usercode\" rows=\"20\" cols=\"80\">" + code + "</textarea><br>");
        writer.println("<input type=\"submit\" value=\"Check\">");
        writer.println("</form>");
//        writer.println(iterations.get(index).getCode());

        if (index > 0) {
            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous</a> ");
        }
        if (index < iterations.size() - 1) {
            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next</a>");
        }

        writer.close();
    }


    //running bandit on the code + saving different iterations
    public void runTests(codeGenerator generator, ResponseHandler responseHandler, String content,
                         HttpServletResponse response, PrintWriter writer, HttpServletRequest request) {

            //dockerHandler object deals with saving files and doing stuff such as calling bandit on files

            dockerHandler docker = new dockerHandler();

            //adding docker to session variables
            request.getSession().setAttribute("dockerHandler", docker);


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

