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

@WebServlet("/userIDE")
public class IDEServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<Iteration>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        PrintWriter writer = response.getWriter();



        String userCode = request.getParameter("usercode");
        codeGenerator generator = (codeGenerator) request.getSession().getAttribute("codeGenerator");
        dockerHandler docker = (dockerHandler) request.getSession().getAttribute("dockerHandler");
        ResponseHandler responseHandler = (ResponseHandler) request.getSession().getAttribute("responseHandler");



        String filePath = "/tmp/userscript.py";
        docker.saveUserFile(userCode, filePath);
        ArrayList<Vulnerability> vulnerabilities = docker.runBanditOnFile(filePath);
        iterations.add(new Iteration(userCode, vulnerabilities));
//        String newContent = "";

        if (!vulnerabilities.isEmpty()) {
            writer.println("<h1>Vulnerabilities Detected!</h1>");
            writer.println("<ul>");
            for (Vulnerability vuln : vulnerabilities) {
                writer.println("<li>" + vuln.getDescription() + "</li>");
            }
            writer.println("</ul>");



            writer.println("<form action=\"/userIDE\" method=\"POST\">");
            writer.println("<label for=\"usercode\">code:</label><br>");
            writer.println("<textarea id=\"usercode\" name=\"usercode\" rows=\"20\" cols=\"80\">" + userCode + "</textarea><br>");
            writer.println("<input type='submit' name='regenerate' value='Regenerate Code'>");

            writer.println("</form>");


        }else{
            writer.println("<h1>No Vulnerabilities Detected!</h1>");
        }


        if (request.getParameter("regenerate") != null) {
            int iterationCount = generator.getIterationCount();
            String newContent = generator.regenerateForVulnerability(userCode, vulnerabilities);
            String newCode = responseHandler.extractCode(newContent);
            filePath = "/tmp/userscript" + iterationCount + ".py";
            docker.saveFile(newCode, filePath);
            vulnerabilities = docker.runBanditOnFile(filePath);

            iterations.add(new Iteration(newCode, vulnerabilities));

            writer.println("<h2>Regenerated Code:</h2>");
            writer.println("<pre>" + newCode.replace("<", "&lt;").replace(">", "&gt;") + "</pre>");

            if (!vulnerabilities.isEmpty()) {
                writer.println("<h1>Vulnerabilities Detected!</h1>");
                writer.println("<ul>");
                for (Vulnerability vuln : vulnerabilities) {
                    writer.println("<li>" + vuln.getDescription() + "</li>");
                }
                writer.println("</ul>");
            }
        }









        String code = request.getParameter("usercode");
        System.out.println("Reformatted code: " + code);

//        writer.println("<h2>Submitted Code:</h2>");
//        writer.println("<pre>" + code.replace("<", "&lt;").replace(">", "&gt;") + "</pre>");

        writer.println("<a href=\"/userIDE\">Go Back</a>");
        writer.close();
    }
}
