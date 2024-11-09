package net.codejava.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
        name = "AnnotatedServlet",
        description = "A sample annotated servlet",
        urlPatterns = {"/QuickServlet"}
)
public class Servlet extends HttpServlet {

//    public void doGet(HttpServletRequest request, HttpServletResponse response)
//            throws IOException {
//
//        PrintWriter writer = response.getWriter();
//        writer.println("<html>Hello, I am a Java servlet!</html>");
//        writer.flush();
//    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String paramWidth = request.getParameter("width");
        int width = Integer.parseInt(paramWidth);

        String paramHeight = request.getParameter("height");
        int height = Integer.parseInt(paramHeight);

        long area = width * height;

        PrintWriter writer = response.getWriter();
        writer.println("<html>Area of the rectangle is: " + area + "</html>");
        writer.flush();

    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("index.html");
    }
}