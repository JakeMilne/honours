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
import org.springframework.web.socket.WebSocketSession;



@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<Iteration>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // storing user input
        String userprompt = request.getParameter("userprompt");
        String parameter = request.getParameter("parameters");
        String exampleOutputs = request.getParameter("exampleOutputs");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
//        writer.println("<html>");

        writer.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        writer.println("<title>Local Host</title>");
        writer.println("<link rel=\"stylesheet\"\n" +
                "        href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\">");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>");

        writer.println("</head>");


        // showing the users input, was/is used for testing
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("<h1>parameters: " + parameter + "</h1>");
        writer.println("<h1>example output(s): " + exampleOutputs + "</h1>");

        // code generator object handles LLM calls
        codeGenerator generator = new codeGenerator(userprompt, parameter, exampleOutputs);
        String callresponse = generator.callLM(userprompt);

        // response handler object parses LLM responses
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";

        // setting session variables
        request.getSession().setAttribute("codeGenerator", generator);
        request.getSession().setAttribute("responseHandler", responseHandler);

        // might need to clear iterations here. at the moment if the user goes back to the main page and tries another prompt it keeps adding to the same arraylist
        iterations.clear();

        // runTests method deals with the dockerHandler object and its methods
        runTests(generator, responseHandler, content, response, writer, request);

        // some stuff to display the different iterations of the code
        int index = iterations.size() - 1;
        System.out.println("Iterations size: " + iterations.size());

        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
        // writer.println(iterations.get(index).getCode());
        // System.out.println("Iteration code: " + iterations.get(index).getCode());

        // getting rid of pesky html tags
        String code = iterations.get(index).getCode().replace("<br>", "\n").replace("<pre>", "").replace("</pre>", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        System.out.println("reformatted code: " + code);
//        String sessionId = request.getParameter("session_id");
//        if (sessionId == null) {
//            writer.println("<h1>Error: Session ID is missing.</h1>");
//            return;
//        } else {
//            writer.println("<h1>Session ID: " + sessionId + "</h1>");
//        }
//
//        WebSocketSession webSocketSession = MyWebSocketHandler.getSessionById(sessionId);
//        if (webSocketSession == null) {
//            writer.println("<h1>Error: WebSocket session not found.</h1>");
//            return;
//        }

        generateForm(code, writer);

//        writer.println("<form action=\"/MyServlet\" method=\"POST\" onsubmit=\"redirectToIDE()\">");
//        writer.println("<textarea id=\"usercode\" name=\"usercode\" rows=\"20\" cols=\"80\">" + code + "</textarea><br>");
//        writer.println("<button type="submit">Submit</button>");
//        writer.println("</form>");
//        writer.println("<script>\n" +
//                "        function redirectToIDE() {\n" +
//                "            // Redirect to /userIDE after form submission\n" +
//                "            window.location.href = \"/userIDE\";\n" +
//                "        }\n" +
//                "    </script>");


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
        writer.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        writer.println("<title>Local Host</title>");
        writer.println("<link rel=\"stylesheet\"\n" +
                "        href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\">");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>");

        writer.println("</head>");


        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("<h1>Parameters: " + parameter + "</h1>");
        writer.println("<h1>Example output(s): " + exampleOutputs + "</h1>");
        if(iterations.get(index).getVulnerabilities().size() > 0) {
            writer.println("<h1>Vulnerabilities Detected:</h1>");
            writer.println("<ul>");
            for (Vulnerability vuln : iterations.get(index).getVulnerabilities()) {
                writer.println("<li>" + vuln.toStringWithHLink() + "</li>");
            }
            writer.println("</ul>");
        } else {
            writer.println("<h1>No Vulnerabilities Detected</h1>");
        }


        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
        String code = iterations.get(index).getCode().replace("<br>", "\n").replace("<pre>", "").replace("</pre>", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");


        generateForm(code, writer);
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
            docker.saveFile(content, filePath, true);
            ArrayList<Vulnerability> vulnerabilities = docker.runBanditOnFile(filePath);
            iterations.add(new Iteration(content, vulnerabilities));
            String newContent = "";

            while(!vulnerabilities.isEmpty() && (generator.getIterationCount() <= generator.iterationCap)) {
                generator.incrementIterationCount();
                newContent = generator.regenerateForVulnerability(content, vulnerabilities);
                String newCode = responseHandler.extractCode(newContent);
                filePath = "/tmp/script" + generator.getIterationCount() + ".py";
                docker.saveFile(newCode, filePath, true);
                vulnerabilities = docker.runBanditOnFile(filePath);
                newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
                iterations.add(new Iteration(newCode, vulnerabilities));
            }

            if(generator.getIterationCount() > generator.iterationCap) {
                writer.println("<h1>Iteration cap reached</h1>");
            }


    }
    public void generateForm(String code, PrintWriter writer) {
        writer.println("<style>");
        writer.println("  .editor-container { display: flex; font-family: monospace; }");
        writer.println("  .line-numbers { ");
        writer.println("    text-align: right; ");
        writer.println("    padding-right: 10px; ");
        writer.println("    user-select: none; ");
        writer.println("    font-size: 14px;");
        writer.println("    line-height: 1.2em;");
        writer.println("    margin: 0;");
        writer.println("    padding-top: 6px;");
        writer.println("  }");
        writer.println("  .editor { ");
        writer.println("    font-size: 14px;");
        writer.println("    line-height: 1.2em;");
        writer.println("    padding: 6px;");
        writer.println("    margin: 0;");
        writer.println("    border: 1px solid #ccc;");
        writer.println("    resize: none;");
        writer.println("  }");
        writer.println("</style>");
        //download file function

        writer.println("<script>");
        writer.println("function downloadFile() {");
        writer.println("const userCode = document.getElementById('usercode').value;");
        writer.println("const blob = new Blob([userCode], { type: 'text/plain' });");
        writer.println("const link = document.createElement('a');");
        writer.println("link.href = window.URL.createObjectURL(blob);");
        writer.println("link.download = 'usercode.py';");
        writer.println("link.click();");
        writer.println("}");
        writer.println("</script>");



        writer.println("<form action=\"/userIDE\" method=\"POST\">");
        writer.println("<div class=\"editor-container\">");
        writer.println("  <pre class=\"line-numbers\" id=\"lineNumbers\"></pre>");
        writer.println("  <textarea id=\"usercode\" name=\"usercode\" rows=\"20\" cols=\"80\" class=\"editor\" oninput=\"updateLines()\">" + code + "</textarea>");
        writer.println("</div>");
        writer.println("<input type=\"submit\" value=\"Check\">");
        //button that lets user download file
        writer.println("<button type=\"button\" onclick=\"downloadFile()\">Download</button>");
        writer.println("</form>");

        writer.println("<script>");
        writer.println("  function updateLines() {");
        writer.println("    let textarea = document.getElementById(\"usercode\");");
        writer.println("    let lines = textarea.value.split(\"\\n\").length;");
        writer.println("    let lineNumberArea = document.getElementById(\"lineNumbers\");");
        writer.println("    lineNumberArea.innerHTML = Array.from({length: lines}, (_, i) => (i + 1)).join(\"\\n\");");
        writer.println("  }");
        writer.println("  updateLines();");
        writer.println("</script>");


    }

}

