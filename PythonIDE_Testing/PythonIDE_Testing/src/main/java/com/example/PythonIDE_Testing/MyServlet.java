package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.http.HttpResponse;


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
//        String response = handler.callLM(userprompt);
        String callresponse = handler.callLM(userprompt);
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractContent(callresponse);
        System.out.println(content);
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