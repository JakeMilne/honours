package com.example.PythonIDE_Testing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
//import javax.websocket.Session;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.socket.TextMessage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.LinkedList;

public class dockerHandler {

    private ConcurrentLinkedQueue<String> inputQueue = new ConcurrentLinkedQueue<>();
    private boolean waitingForInput = false;
    private String containerName = "pythonide_testing-app-1";
    private final Object webSocketLock = new Object();




    public dockerHandler() {
    }

    public void saveFile(String pythonCode, String filePath, boolean strip) {
        System.out.println("Saving Python code to the container at: " + filePath);

        String strippedPythonCode = "";
        if(strip){

            strippedPythonCode = stripHtmlTags(pythonCode);
//            System.out.println(strippedPythonCode);
        }else{
            strippedPythonCode = pythonCode;
//            System.out.println(strippedPythonCode);
        }
//        System.out.println("============= Stripped Python code end =============");

//        String containerName = "pythonide_testing-app-1";


        try {
            String[] command = {
                    "docker", "exec", "-i", containerName, "sh", "-c", "cat > " + filePath
            };

            Process process = Runtime.getRuntime().exec(command);

            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(strippedPythonCode.getBytes());

                outputStream.flush();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
//                        System.out.println("Output: " + line);
                }
                while ((line = errorReader.readLine()) != null) {
//                        System.err.println("Error: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Python code successfully saved to the container!");
            } else {
                System.err.println("Failed to save the Python code. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    //method that calls bandit on the file
    public ArrayList<Vulnerability> runBanditOnFile(String filepath) {
        ArrayList<Vulnerability> vulnerabilities = new ArrayList<Vulnerability>();

        try {
            ProcessBuilder banditProcess = new ProcessBuilder(
                    "docker", "exec", "-i", containerName,
//                    "bandit", "-r", "/tmp/script.py", "-v", "-f", "json"
                    "bandit", "-r", filepath, "-v"

            );
            Process bandit = banditProcess.start();
            bandit.waitFor();


            System.out.println("Bandit analysis completed successfully.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(bandit.getInputStream()));
            String line;
            String vulnerability = "";
            int i = 1;
            while ((line = reader.readLine()) != null) {


                if (line.contains("Issue: [")) {

                    vulnerability += line;
                    for (int j = 0; j < 7; j++) {
                        i++;
                        line = reader.readLine();
                        vulnerability += line;
                        vulnerability += "\n";
                    }

                    vulnerabilities.add(parseVulnerability(vulnerability));
                    vulnerability = "";

                }

                i++;
            }
            if (vulnerabilities.toString().equals("[]")) {
                System.out.println("No vulnerabilities found");
            } else {
                System.out.println(vulnerabilities.toString());

            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return vulnerabilities;

    }


    public Vulnerability parseVulnerability(String vulnString) {
//        System.out.println(vulnString);

//        Issue: [B608:hardcoded_sql_expressions] Possible SQL injection vector through string-based query construction.   Severity: Medium   Confidence: Low
//        CWE: CWE-89 (https://cwe.mitre.org/data/definitions/89.html)
//        More Info: https://bandit.readthedocs.io/en/1.8.2/plugins/b608_hardcoded_sql_expressions.html
//        Location: /tmp/script.py:10:15
//9           def format_query(self, username: str, password: str) -> str:
//10              return f"SELECT * FROM users WHERE username='{username}' AND password='{password}'"
//11
        String description = vulnString.substring(vulnString.indexOf("]") + 1, vulnString.indexOf("Severity:"));
        String severity = vulnString.substring(vulnString.indexOf("Severity:") + 9, vulnString.indexOf("Confidence:"));
        String CWE = extractCWE(vulnString);
        String moreInfo = vulnString.substring(vulnString.indexOf("More Info:") + 10, vulnString.indexOf("Location:"));
        String codeSnippet = vulnString.substring(vulnString.indexOf("Location:") + 10);

//        System.out.println("Description: " + description);
//        System.out.println("Severity: " + severity);
//        System.out.println("CWE: " + CWE);
//        System.out.println("More Info: " + moreInfo);
//        System.out.println("Code Snippet: " + codeSnippet);
        Vulnerability vulnerability = new Vulnerability(description, severity, Integer.parseInt(CWE), codeSnippet, moreInfo);
        return vulnerability;
    }

    //parsing CWEs
    public static String extractCWE(String input) {
        String regex = "CWE:\\s*CWE-(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }


    public static String stripHtmlTags(String input) {
        Document doc = Jsoup.parse(input, "", Parser.xmlParser());

        Element preTag = doc.selectFirst("pre");
        String text = preTag != null ? preTag.wholeText() : "";

        text = text.replace("<br>", "\n");

        return text;
    }

    public Queue<String> parseInputs(String code){
        Queue<String> inputs = new LinkedList<>();
        String regex = "input\\(\\s*\"(.*?)\"\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            inputs.add(matcher.group(1));
            System.out.println("Input: " + matcher.group(1));
        }
        return inputs;
    }


    public void runFile(String filePath, WebSocketSession session, String code) {
        new Thread(() -> {
            try {
                System.out.println("Running Python script: " + filePath);
                ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "python3", "-u", filePath);
                Process process = scriptBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                AtomicBoolean waitingForInput = new AtomicBoolean(false);
                Queue<String> inputs = parseInputs(code);


                Thread outputThread = new Thread(() -> {
                    try {
                        int c;
                        String nextInput = inputs.peek();
                        String oldInput = "";
                        boolean nextInputUsed = false;


                        System.out.println("Reading output");
                        StringBuilder lineBuffer = new StringBuilder();
                        while ((c = reader.read()) != -1) {
                            //using chars causes an issue with inputs. right now when the user responds to an input request, the server responds with the text from the input
                            //function + the first char of the users response, then it keeps sending responses building onto this with the next char of the users response and so on
                            char ch = (char) c;
                            lineBuffer.append(ch);
//                            System.out.println(lineBuffer.toString());


//                            sendMessageToUser(session, lineBuffer.toString());


                                if (nextInput != null && lineBuffer.toString().contains(nextInput)) {
                                    if(nextInput.length() == lineBuffer.toString().length()){
                                        sendMessageToUser(session, "Detected input request");
//                                        sendMessageToUser(session, "line 245" + lineBuffer.toString());
                                        sendMessageToUser(session, lineBuffer.toString());

                                        oldInput = inputs.poll();
                                        nextInputUsed = true;

                                        waitingForInput.set(true);
                                    }
//                                    sendMessageToUser(session, "Detected input request");
//                                    sendMessageToUser(session, "line 245" + lineBuffer.toString());
//                                    inputs.poll();
//                                    nextInputUsed = true;
//
//                                    waitingForInput.set(true);
//                                    if((oldInput.length() + nextInput.length()) == lineBuffer.toString().length()){
//                                        sendMessageToUser(session, "Detected user input");
//                                        sendMessageToUser(session, "line 245" + lineBuffer.toString().replace(nextInput, ""));
////                                        inputs.poll();
////                                        nextInputUsed = true;
//
////                                        waitingForInput.set(true);
//                                    }
                                }


                            if (ch == '\n') {
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");
                                System.out.println("NEW LINE");


                                String line = lineBuffer.toString().trim();
                                if(nextInput != null && line.contains(nextInput)){
                                    line = line.replace(nextInput, "");
                                    nextInputUsed = false;
                                    nextInput = inputs.peek();
                                }
//                                sendMessageToUser(session, "line 262 " + line);
                                sendMessageToUser(session, line);

                                System.out.println(line);


                                lineBuffer.setLength(0);
                            }
                        }

                        if (lineBuffer.length() > 0) {
                            sendMessageToUser(session, "line 271 " + lineBuffer.toString());
                        }

                    } catch (IOException e) {
                        sendMessageToUser(session, "Output error: " + e.getMessage());
                    }
                });

                Thread errorThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            sendMessageToUser(session, "Error: " + line);
                        }
                    } catch (IOException e) {
                        sendMessageToUser(session, "Error reading: " + e.getMessage());
                    }
                });

                outputThread.start();
                errorThread.start();

                while (process.isAlive()) {
                    if (waitingForInput.get() && !inputQueue.isEmpty()) {
                        try {
                            String input = inputQueue.poll();
                            if (input != null) {
                                sendMessageToUser(session, "Sending input: " + input);
                                writer.write(input + "\n");
                                writer.flush();
                                waitingForInput.set(false);
                            }
                        } catch (IOException e) {
                            sendMessageToUser(session, "Input error: " + e.getMessage());
                        }
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                int exitCode = process.waitFor();
                sendMessageToUser(session, "Process completed with code: " + exitCode);

            } catch (Exception e) {
                sendMessageToUser(session, "Process error: " + e.getMessage());
            }
        }).start();
    }




    public void addUserInput(String input) {
        System.out.println("adding to input queue: " + input);
        inputQueue.offer(input);
    }


    private void sendMessageToUser(WebSocketSession session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        System.out.println("sending message to user");
        synchronized (webSocketLock) {
            try {
                TextMessage textMessage = new TextMessage(message);
                session.sendMessage(textMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //for the userIDE endpoint
//    public void saveUserFile(String pythonCode, String filePath) {
//
//
//        String containerName = "pythonide_testing-app-1";
//
//
//        try {
//            String[] command = {
//                    "docker", "exec", "-i", containerName, "sh", "-c", "cat > " + filePath
//            };
//
//            Process process = Runtime.getRuntime().exec(command);
//
//            try (OutputStream outputStream = process.getOutputStream()) {
//                outputStream.write(pythonCode.getBytes());
//                outputStream.flush();
//            }
//
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
//
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    //send to websocket
//                    sendToSession(sessionId, line);
////                        System.out.println("Output: " + line);
//                }
//                while ((line = errorReader.readLine()) != null) {
////                        System.err.println("Error: " + line);
//                }
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                System.out.println("Python code successfully saved to the container!");
//            } else {
//                System.err.println("Failed to save the Python code. Exit code: " + exitCode);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }
}
