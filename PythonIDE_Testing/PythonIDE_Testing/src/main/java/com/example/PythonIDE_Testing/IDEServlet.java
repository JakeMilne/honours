package com.example.PythonIDE_Testing;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.websocket.Session;
import java.net.URLEncoder;
import org.springframework.web.socket.WebSocketSession;

@WebServlet("/userIDE")
public class IDEServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<>();


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/ide.html").forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userCode = request.getParameter("usercode");
        response.sendRedirect("/userIDE?userCode=" + URLEncoder.encode(userCode, "UTF-8"));
//        response.sendRedirect("/userIDE");
    }


}


