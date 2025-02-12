package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.websocket.Session;

@WebServlet("/userIDE")
public class IDEServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/ide.html").forward(request, response);
    }




    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        String userCode = request.getParameter("usercode");

        String webSocketSessionId = request.getParameter("session_id");
        System.out.println(webSocketSessionId);
        System.out.println(webSocketSessionId);
        System.out.println(webSocketSessionId);
        System.out.println(webSocketSessionId);
        System.out.println(webSocketSessionId);



        if (webSocketSessionId == null) {
            response.getWriter().println("<h1>Error: WebSocket session ID is missing.</h1>");
            return;
        }

        dockerHandler docker = (dockerHandler) request.getSession().getAttribute("dockerHandler");

        String filePath = "/tmp/userscript.py";
        docker.saveUserFile(userCode, filePath);


        //This used to return null so keep an eye on it if stuff breaks again
        Session webSocketSession = WebSocket.getSessionById(webSocketSessionId);
        System.out.println("WebSocket session: " + webSocketSession);

        if (webSocketSession != null) {
            docker.runFile(filePath, webSocketSession);
        }

        //need to pass the users code somehow.
        response.sendRedirect("/ide.html");
    }

}


