package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
//import javax.websocket.Session;
import org.springframework.web.socket.WebSocketSession;

@WebServlet("/userIDE")
public class IDEServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Forward to the ide.html page where the WebSocket will be established
        request.getRequestDispatcher("/ide.html").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Handle code submission if necessary (or just pass to WebSocket handling)
        String userCode = request.getParameter("usercode");
        // Optional: Store user code in session or database if needed.
        // If you're using WebSocket handling, you may not need this directly.

        // Redirect to /userIDE after form submission
        response.sendRedirect("/userIDE");
    }

}


