package com.example.PythonIDE_Testing;

import com.example.PythonIDE_Testing.Vulnerability;


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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;


@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {

    private ArrayList<Iteration> iterations = new ArrayList<Iteration>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // storing user input
        String userprompt = request.getParameter("userprompt");
//        String parameter = request.getParameter("parameters");
//        String exampleOutputs = request.getParameter("exampleOutputs");

//        if (paramNames != null) {
//            //different prompt perhaps?
//        }
        String[] paramNames = request.getParameterValues("paramName[]");
        String[] paramValues = request.getParameterValues("paramValue[]");

        String[] outputNames = request.getParameterValues("outputName[]");
        String[] outputValues = request.getParameterValues("outputValue[]");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
//        writer.println("<html>");

        writer.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        writer.println("<title>Local Host</title>");
        writer.println("<link rel=\"stylesheet\" href=\"styles.css\">");
        writer.println("<link rel=\"stylesheet\"\n" +
                "        href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\">");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>");

        writer.println("</head>");
        String allParamNames = "";
        for (String string : paramNames) {
            allParamNames += string + ", ";
        }

        String allParamValues = "";
        for (String string : paramValues) {
            allParamValues += string + ", ";
        }


        // showing the users input, was/is used for testing
//        writer.println("<h1>Prompt: " + userprompt + "</h1>");

        writer.println("<div class=\"prompt-section\">");
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("</div>");


//        writer.println("<h1>parameters: " + allParamNames + "</h1>");
//        writer.println("<h1>parameter values: " + allParamValues + "</h1>");
        String valuesCat = "";
        String outputsCat = "";
        for (int i = 0; i < paramValues.length; i++) {
            if (paramValues[i].equals("")) {
                continue;
            }
            valuesCat += "" + paramNames[i] + ": " + paramValues[i] + ", ";
        }
        if (!valuesCat.equals("")) {
            writer.println("<h1>example value(s): " + valuesCat + "</h1>");
        }
        for (int i = 0; i < outputNames.length; i++) {
            if (outputValues[i].equals("")) {
                continue;
            }
            outputsCat += "" + outputNames[i] + ": " + outputValues[i] + ", ";
        }
        if (!outputsCat.equals("")) {
            writer.println("<h1>example output(s): " + outputsCat + "</h1>");
        }

        // code generator object handles LLM calls
        codeGenerator generator = new codeGenerator(userprompt, paramNames, outputNames, paramValues, outputValues);
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

        if ((outputValues.length > 0 && outputValues[0] != "") || (paramValues.length > 0 && paramValues[0] != "")) {

            System.out.println("Running tests with units");
            runTestsWithUnits(generator, responseHandler, content, response, writer, request);
        } else {
            System.out.println("Running tests");
            runTests(generator, responseHandler, content, response, writer, request);
        }
        // some stuff to display the different iterations of the code
        int index = iterations.size() - 1;
        System.out.println("Iterations size: " + iterations.size());


        if (iterations.get(index).getIssues().size() > 0) {
            Boolean hasVulnerabilities = false;
            //for loop to check if issues contains any vulnerabilities, then i can start showing them
            for (Issue issue : iterations.get(index).getIssues()) {
                if (issue instanceof Vulnerability) {
                    hasVulnerabilities = true;
                }
            }
//            if (hasVulnerabilities) {
//
//                writer.println("<h1>Vulnerabilities Detected:</h1>");
//                writer.println("<ul>");
//                for (Issue issue : iterations.get(index).getIssues()) {
//                    if (issue instanceof Vulnerability) {
//                        writer.println("<li>" + ((Vulnerability) issue).toStringWithHLink() + "</li>");
//
//                    }
//                }
//                writer.println("</ul>");
//            } else {
//                writer.println("<h1>No Vulnerabilities Detected</h1>");
//
//            }

            if (hasVulnerabilities) {
                writer.println("<div class=\"vulnerability-section\">");
                writer.println("<h1>Vulnerabilities Detected:</h1>");
                writer.println("<ul>");
                for (Issue issue : iterations.get(index).getIssues()) {
                    if (issue instanceof Vulnerability) {
                        writer.println("<li>" + ((Vulnerability) issue).toStringWithHLink() + "</li>");
                    }
                }
                writer.println("</ul>");
                writer.println("</div>");
            } else {
                writer.println("<div class=\"no-vulnerability-section\">");
                writer.println("<h1>No Vulnerabilities Detected</h1>");
                writer.println("</div>");
            }


        }

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

        generateForm(code, writer, iterations.get(index).getIssues());

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


        writer.println("<h2>Iteration " + (index + 1) + "</h2>");

//        if (index > 0) {
//            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous iteration</a> ");
//        }
//        if (index < iterations.size() - 1) {
//            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next iteration</a>");
//        }


        writer.println("<div class=\"nav-links\">");
        if (index > 0) {
            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous iteration</a>");
        }
        if (index < iterations.size() - 1) {
            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next iteration</a>");
        }
        writer.println("</div>");

        writer.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String indexParam = request.getParameter("index");
        int index = (indexParam != null) ? Integer.parseInt(indexParam) : iterations.size() - 1;

        String userprompt = request.getParameter("userprompt");
//        String parameter = request.getParameter("parameters");
//        String exampleOutputs = request.getParameter("exampleOutputs");

        //showing stuff to the user
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        writer.println("<title>Local Host</title>");
        writer.println("<link rel=\"stylesheet\" href=\"styles.css\">");
        writer.println("<link rel=\"stylesheet\"\n" +
                "        href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\">");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>");

        writer.println("</head>");


//        writer.println("<h1>Prompt: " + userprompt + "</h1>");

        writer.println("<div class=\"prompt-section\">");
        writer.println("<h1>Prompt: " + userprompt + "</h1>");
        writer.println("</div>");


//        writer.println("<h1>Parameters: " + parameter + "</h1>");
//        writer.println("<h1>Example output(s): " + exampleOutputs + "</h1>");
        if (iterations.get(index).getIssues().size() > 0) {
            Boolean hasVulnerabilities = false;
            //for loop to check if issues contains any vulnerabilities, then i can start showing them
            for (Issue issue : iterations.get(index).getIssues()) {
                if (issue instanceof Vulnerability) {
                    hasVulnerabilities = true;
                }
            }
//            if (hasVulnerabilities) {
//
//                writer.println("<h1>Vulnerabilities Detected:</h1>");
//                writer.println("<ul>");
//                for (Issue issue : iterations.get(index).getIssues()) {
//                    if (issue instanceof Vulnerability) {
//                        writer.println("<li>" + ((Vulnerability) issue).toStringWithHLink() + "</li>");
//
//                    }
//                }
//                writer.println("</ul>");
//            } else {
//                writer.println("<h1>No Vulnerabilities Detected</h1>");
//
//            }

            if (hasVulnerabilities) {
                writer.println("<div class=\"vulnerability-section\">");
                writer.println("<h1>Vulnerabilities Detected:</h1>");
                writer.println("<ul>");
                for (Issue issue : iterations.get(index).getIssues()) {
                    if (issue instanceof Vulnerability) {
                        writer.println("<li>" + ((Vulnerability) issue).toStringWithHLink() + "</li>");
                    }
                }
                writer.println("</ul>");
                writer.println("</div>");
            } else {
                writer.println("<div class=\"no-vulnerability-section\">");
                writer.println("<h1>No Vulnerabilities Detected</h1>");
                writer.println("</div>");
            }


        }


        writer.println("<h2>Iteration " + (index + 1) + "</h2>");
        String code = iterations.get(index).getCode().replace("<br>", "\n").replace("<pre>", "").replace("</pre>", "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");


        generateForm(code, writer, iterations.get(index).getIssues());
//        writer.println(iterations.get(index).getCode());

//        if (index > 0) {
//            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous</a> ");
//        }
//        if (index < iterations.size() - 1) {
//            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next</a>");
//        }


        writer.println("<div class=\"nav-links\">");
        if (index > 0) {
            writer.println("<a href='/MyServlet?index=" + (index - 1) + "'>Previous iteration</a>");
        }
        if (index < iterations.size() - 1) {
            writer.println("<a href='/MyServlet?index=" + (index + 1) + "'>Next iteration</a>");
        }
        writer.println("</div>");

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
        ArrayList<Issue> vulnerabilities = docker.runBanditOnFile(filePath);
        iterations.add(new Iteration(content, vulnerabilities));
        String newContent = "";
        System.out.println(generator.getIterationCount());
        System.out.println(generator.iterationCap);
        while (!vulnerabilities.isEmpty() && (generator.getIterationCount() < generator.iterationCap)) {


            generator.incrementIterationCount();

            ArrayList<Issue> allVulnerabilities = new ArrayList<>();
            for (int i = 0; i < iterations.size(); i++) {
                allVulnerabilities.addAll(iterations.get(i).getIssues());
            }

            System.out.println("All vulnerabilities: ");
            for (Issue issue : allVulnerabilities) {
                System.out.println(issue.toString());
            }
            newContent = generator.regenerateForVulnerability(content, allVulnerabilities, (generator.getIterationCount() < 3));
            String newCode = responseHandler.extractCode(newContent);
            filePath = "/tmp/script" + generator.getIterationCount() + ".py";
            System.out.println("New code: " + newCode);
            System.out.println("ABOUT TO SAVE FILE");
            docker.saveFile(newCode, filePath, false);
            vulnerabilities = docker.runBanditOnFile(filePath);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
            iterations.add(new Iteration(newCode, vulnerabilities));
        }


        if (generator.getIterationCount() == generator.iterationCap) {
            writer.println("<h1>Iteration cap reached</h1>");
        }


    }

    public void runTestsWithUnits(codeGenerator generator, ResponseHandler responseHandler, String content,
                                  HttpServletResponse response, PrintWriter writer, HttpServletRequest request) {

        //dockerHandler object deals with saving files and doing stuff such as calling bandit on files

        dockerHandler docker = new dockerHandler();

        //adding docker to session variables
        request.getSession().setAttribute("dockerHandler", docker);


        String filePath = "/tmp/script.py";
        docker.saveFile(content, filePath, true);
        ArrayList<Issue> vulnerabilities = docker.runBanditOnFile(filePath);
        iterations.add(new Iteration(content, vulnerabilities));
        String newContent = "";

        while (!vulnerabilities.isEmpty() && (generator.getIterationCount() < generator.iterationCap)) {
            generator.incrementIterationCount();


            ArrayList<Issue> allVulnerabilities = new ArrayList<>();
            for (int i = 0; i < iterations.size(); i++) {
                allVulnerabilities.addAll(iterations.get(i).getIssues());
            }

            System.out.println("All vulnerabilities: ");

            for (Issue issue : allVulnerabilities) {
                System.out.println(issue.toString());
            }
            newContent = generator.regenerateForVulnerability(content, allVulnerabilities, (generator.getIterationCount() < 3));
//            newContent = generator.regenerateForVulnerability(content, vulnerabilities, (generator.getIterationCount() < 3));
            String newCode = responseHandler.extractCode(newContent);
            filePath = "/tmp/script" + generator.getIterationCount() + ".py";
            docker.saveFile(newCode, filePath, false);
            vulnerabilities = docker.runBanditOnFile(filePath);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
            iterations.add(new Iteration(newCode, vulnerabilities));
        }

        String result = docker.runFileForRegenerating(filePath, content);
        while (result.contains("FAIL") && (generator.getIterationCount() <= generator.iterationCap)) {
            System.out.println("Regenerating for errors");
            generator.incrementIterationCount();
            newContent = generator.regenerateForErrors(content, result, (generator.getIterationCount() < 3));
            String newCode = responseHandler.extractCode(newContent);

            filePath = "/tmp/script" + generator.getIterationCount() + ".py";
            docker.saveFile(newCode, filePath, true);
            result = docker.runFileForRegenerating(filePath, newCode);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";

            ArrayList<Issue> errors = parseErrors(result);
            iterations.add(new Iteration(newCode, errors));
        }

        if (generator.getIterationCount() > generator.iterationCap) {
            writer.println("<h1>Iteration cap reached</h1>");
        }


    }

    public void generateForm(String code, PrintWriter writer, ArrayList<Issue> vulnerabilities) {
        Set<Integer> highlightedLines = new HashSet<>();
        for (Issue vulnerability : vulnerabilities) {
            if (vulnerability instanceof Vulnerability) {
                List<Integer> lines = lineNumbers(((Vulnerability) vulnerability).getCodeSnippet());
                if (lines != null) {
                    highlightedLines.addAll(lines);
                    System.out.println(highlightedLines.toString());
                }
            }
        }

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
        writer.println("    overflow: hidden;");
        writer.println("  }");
        writer.println("  .highlight {");
        writer.println("    background-color: yellow;");
        writer.println("    color: black;");
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

        writer.println("function adjustHeight(textarea) {");
        writer.println("  textarea.style.height = 'auto';");
        writer.println("  textarea.style.height = textarea.scrollHeight + 'px';");
        writer.println("}");

        writer.println("document.addEventListener('DOMContentLoaded', function() {");
        writer.println("  let textarea = document.getElementById('usercode');");
        writer.println("  if (textarea) { adjustHeight(textarea); }");
        writer.println("});");
        writer.println("</script>");

        writer.println("<form action=\"/userIDE\" method=\"POST\">");
        writer.println("<h1>Code:</h1>");
        writer.println("<div class=\"editor-container\">");
        writer.println("  <pre class=\"line-numbers\" id=\"lineNumbers\"></pre>");
        writer.println("  <textarea id=\"usercode\" name=\"usercode\" class=\"editor\" oninput=\"updateLines(); adjustHeight(this);\">" + code + "</textarea>");
        writer.println("</div>");
        writer.println("<button type=\"submit\">Check</button>");
        writer.println("<button type=\"button\" onclick=\"downloadFile()\">Download</button>");
        writer.println("</form>");

        writer.println("<script>");
        writer.println("  function updateLines() {");
        writer.println("    let textarea = document.getElementById('usercode');");
        writer.println("    let lines = textarea.value.split(\"\\n\").length;");
        writer.println("    let lineNumberArea = document.getElementById(\"lineNumbers\");");
        writer.println("    lineNumberArea.innerHTML = ''; ");
        writer.println("    for (let i = 0; i < lines; i++) {");
        writer.println("      let lineDiv = document.createElement('div');");
        writer.println("      lineDiv.id = 'line-' + (i + 1);");
        writer.println("      lineDiv.textContent = i + 1;");
        writer.println("      lineNumberArea.appendChild(lineDiv);");
        writer.println("    }");
        writer.println("    highlightedLines.forEach(line => {");
        writer.println("      let lineElement = document.getElementById('line-' + line);");
        writer.println("      if (lineElement) {");
        writer.println("        lineElement.classList.add('highlight');");
        writer.println("      }");
        writer.println("    });");
        writer.println("  }");
        writer.println("  const highlightedLines = " + highlightedLines.toString() + ";");
        writer.println("  updateLines();");
        writer.println("</script>");
    }


    public List<Integer> lineNumbers(String snippet) {
        Pattern pattern = Pattern.compile("(?m)^\\d+");
        Matcher matcher = pattern.matcher(snippet);
        List<Integer> lineNumbers = new ArrayList<>();

        while (matcher.find()) {
            lineNumbers.add(Integer.parseInt(matcher.group()));
        }

        System.out.println(lineNumbers);
        return lineNumbers;
    }

    public ArrayList<Issue> parseErrors(String result) {
        ArrayList<Issue> issues = new ArrayList<>();

        // split the string on " SPLIT THE ERROR HERE "

        String[] errorStrings = result.split(" SPLIT THE ERROR HERE ");
        for (String errorString : errorStrings) {
            issues.add(new Error(errorString));
        }
        return issues;
    }

}

