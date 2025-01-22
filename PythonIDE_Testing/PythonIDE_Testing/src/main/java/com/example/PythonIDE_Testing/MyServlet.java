package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userprompt = request.getParameter("userprompt");
        String lname = request.getParameter("lname");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<h1>First Name: " + userprompt + "</h1>");
        writer.println("<h1>Last Name: " + lname + "</h1>");
        writer.println("</html>");
        writer.close();
        LLMHandler handler = new LLMHandler();
        handler.callLM(userprompt);
    }
}