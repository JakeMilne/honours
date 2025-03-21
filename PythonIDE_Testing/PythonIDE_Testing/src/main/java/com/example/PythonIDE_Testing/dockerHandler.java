package com.example.PythonIDE_Testing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.socket.TextMessage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;


//this is the class used to store files, run bandit, and run python scripts in a docker container.
//The docker container needs python3 and bandit installed, along with any python libraries you might need.
//I've got the container name saved as an environment variable, but you can just hardcode it in if you want. on line 37
public class dockerHandler {

    private ConcurrentLinkedQueue<String> inputQueue = new ConcurrentLinkedQueue<>();
    private boolean waitingForInput = false;
    //    private String containerName = "pythonide_testing-app-1";
    private String containerName = System.getenv("CONTAINER_NAME");
    private final Object webSocketLock = new Object();


    public dockerHandler() {
    }

    //right now, files are saved in /tmp/ with the names script.py, then script2.py, script3.py, etc. with the IDE file being usercode.py, its probably best to make unique file names in the future
    public void saveFile(String pythonCode, String filePath, boolean strip) {


        //this is just for debugging, the code will sometimes contain html tags, so this will strip them out
        System.out.println("Saving Python code to the container at: " + filePath);
        System.out.println("============= Python code start =============");
        System.out.println(pythonCode);
        System.out.println("============= Python code end =============");
        System.out.println(strip);

        String strippedPythonCode = "";
        if (strip) {
            System.out.println("Stripping HTML tags");
            strippedPythonCode = stripHtmlTags(pythonCode);
//            System.out.printl n(strippedPythonCode);
        } else {
            System.out.println("Not stripping HTML tags");
            strippedPythonCode = pythonCode;
//            System.out.println(strippedPythonCode);
        }
//        System.out.println("============= Stripped Python code end =============");


        System.out.println("Stripped Python code: " + strippedPythonCode);
        try {
            String[] command = {
                    "docker", "exec", "-i", containerName, "sh", "-c", "cat > " + filePath
            };

            Process process = Runtime.getRuntime().exec(command);

            try (OutputStream outputStream = process.getOutputStream()) {
                //adding the code to the file
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
    public ArrayList<Issue> runBanditOnFile(String filepath) {
        ArrayList<Issue> vulnerabilities = new ArrayList<Issue>();
        System.out.println("Running Bandit on file: " + filepath);
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

            //parsing bandit output to check for vulnerabilities
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

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

        //example bandit output:
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


    //to remove html tags from LLM responses
    public static String stripHtmlTags(String input) {
        Document doc = Jsoup.parse(input, "", Parser.xmlParser());

        Element preTag = doc.selectFirst("pre");
        String text = preTag != null ? preTag.wholeText() : "";

        text = text.replace("<br>", "\n");

        return text;
    }


    //method for running python code
    public void runFile(String filePath, WebSocketSession session, String code) {
        new Thread(() -> {
            try {
                
                inputQueue.clear();
                AtomicBoolean waitingForInput = new AtomicBoolean(false);
                Queue<String> testCase = new LinkedList<>();
                String testSuccess = ".----------------------------------------------------------------------Ran \\d+ test in \\d+\\.\\d+sOK";
                String testFailure = "FAIL: ([^=]+).*?Traceback \\(most recent call last\\):.*?File \"([^\"]+)\", line (\\d+), in ([^A-Z]+).*?(AssertionError: .+?)------+Ran (\\d+) test.+?FAILED \\(failures=(\\d+)\\)";

                System.out.println("Running Python script: " + filePath);
                ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "python3", "-u", filePath);
                scriptBuilder.redirectErrorStream(true);
                Process process = scriptBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                BufferedReader codeReader = new BufferedReader(new StringReader(code));

                String line;
                ArrayList<Function> functionCalls = new ArrayList<>();
                ArrayList<Inputs> inputList = new ArrayList<>();

                System.out.println("Reading output");
                StringBuilder lineBuffer = new StringBuilder();

                PythonChunk rootChunk = new PythonChunk(code, "root", null).parse(code);
                ArrayList<PythonChunk> chunks = new ArrayList<>();
                chunks.add(rootChunk);

                chunks = getAllChunks(chunks);

                if (chunks.size() == 0) {
                    System.out.println("Size is 0 for some reason");
                    System.out.println(rootChunk.toString());
                    chunks.add(rootChunk);
                }
                while ((line = codeReader.readLine()) != null) {
                    if (!(getFunctionCall(line).equals("false"))) {
                        PythonChunk functionChunk = findChunk(line, rootChunk);
                        functionCalls.add(new Function(getFunctionCall(line), functionChunk));
                    }
                }

                for (PythonChunk chunk : chunks) {
                    System.out.println(chunk.toString());
                    for (String input : chunk.getInputs()) {
                        System.out.println("Input: " + input);
                        if (chunk.getParent() == null) {
                            inputList.add(new Inputs(input, true));
                        } else {
                            inputList.add(new Inputs(input, false));
                        }
                    }
                }

                Thread outputThread = new Thread(() -> {
                    try {
//                        System.out.println("In output thread");
                        int c;
                        String newLine;
                        System.out.println("HELLO");

                        System.out.println("hi");
                        StringBuilder lineBuffer2 = new StringBuilder();
                        System.out.println("2");
                        System.out.println("3");

                        ArrayList<Inputs> usedInputs = new ArrayList<>();
                        while ((c = reader.read()) != -1) {
//                            sendMessageToUser(session, "In output thread");
                            char ch = (char) c;
                            lineBuffer2.append(ch);
                            System.out.println(lineBuffer2.toString());
                            System.out.println(inputList.size());

                            Iterator<Inputs> iterator = inputList.iterator();
                            while (iterator.hasNext()) {
                                Inputs input = iterator.next();
                                System.out.println("INPUT CALL: ");
                                System.out.println(input.getCall());
                                if (lineBuffer2.toString().contains(input.getCall())) {

                                    sendMessageToUser(session, "line 295 Detected input request");
                                    usedInputs.add(input);

                                    sendMessageToUser(session, "line 298 " + lineBuffer2.toString());
                                    if (lineBuffer2.indexOf(input.getCall()) != -1) {
                                        lineBuffer2.delete(lineBuffer2.indexOf(input.getCall()), lineBuffer2.indexOf(input.getCall()) + input.getCall().length());
                                    }
                                    if (input.getSingle()) {
                                        iterator.remove();
                                    }
                                    waitingForInput.set(true);
                                }
                            }

                            if (ch == '\n') {
                                String outputLine = lineBuffer2.toString().trim();
                                System.out.println("308 " + inputList.size());
                                for (Inputs input : usedInputs) {
                                    System.out.println("line 309 " + input.getCall());
                                    if (outputLine.contains(input.getCall())) {
                                        outputLine = outputLine.replace(input.getCall(), "");
                                    }
                                }
                                sendMessageToUser(session, "line 315 " + outputLine);
                                System.out.println(outputLine);

                                if (testCase.size() == 15) {
                                    testCase.poll();
                                }
                                testCase.offer(outputLine);

                                if (checkPattern(testSuccess, testCase)) {
                                    sendMessageToUser(session, "Test success");
                                }
                                if (checkPattern(testFailure, testCase)) {
                                    sendMessageToUser(session, "Test failure");
                                }

                                lineBuffer2.setLength(0);
                            }
                        }
                    } catch (Exception e) {
                        sendMessageToUser(session, "Output error: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });


                Thread errorThread = new Thread(() -> {
                    try {
                        String errorline;
                        while ((errorline = errorReader.readLine()) != null) {
                            if (!errorline.trim().isEmpty()) {
                                sendMessageToUser(session, "Error: " + errorline);
                            }
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
                                sendMessageToUser(session, " line 376 Sending input: " + input);
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
            } catch (Exception e) {
                sendMessageToUser(session, "Output error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).start();
    }


    public static String readFileFromContainer(String containerName, String filePath) throws IOException, InterruptedException {
        ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "cat", filePath);
        Process process = scriptBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error executing Docker command");
        }

        return content.toString();
    }


    public static ArrayList<String> getDefinitions(String code) {
        //goes over code and finds all lines containing python definitions, i.e functions, loops, classes, etc
        ArrayList<String> definitions = new ArrayList<>();
        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.contains("def") || line.contains("class") || line.contains("for") || line.contains("while") || line.contains("if") || line.contains("elif") || line.contains("else")) {
                definitions.add(line);
            }
        }
        return definitions;
    }

    public static String getFunctionCall(String line) {
        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) + "(" : "false";
    }

    public static PythonChunk findChunk(String line, PythonChunk rootChunk) {
        ArrayList<PythonChunk> chunks = rootChunk.getChildren();
        for (PythonChunk chunk : chunks) {
            if (chunk.getDefinition().contains(line)) {
                return chunk;
            }
        }
        return null;
    }

    public String checkInput(String line) {
        String regex = "input\\(\\s*\"(.*?)\"\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            return (matcher.group(1));
        }
        return null;
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

    private boolean checkPattern(String pattern, Queue<String> testCase) {
        StringBuilder concatenatedLines = new StringBuilder();
        for (String line : testCase) {
            concatenatedLines.append(line).append("\n");

        }
        System.out.println("Concatenated lines: " + concatenatedLines.toString());
        System.out.println("Pattern: " + pattern);
        System.out.println();
        System.out.println();
        System.out.println("Pattern");
        System.out.println(pattern);
        System.out.println();
        System.out.println("output");
        System.out.println(concatenatedLines);
        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regexPattern.matcher(concatenatedLines.toString());

        if (matcher.find()) {
            System.out.println("Pattern found! " + pattern);
            return true;
        } else {
            System.out.println("Pattern not found! " + pattern);
        }
        return false;
    }

    public static ArrayList<PythonChunk> getAllChunks(ArrayList<PythonChunk> chunks) {

        ArrayList<PythonChunk> allChunks = new ArrayList<>();
        for (PythonChunk chunk : chunks) {
            allChunks.add(chunk);
            allChunks.addAll(getAllChunks(chunk.getChildren()));
        }

        return allChunks;

    }


    //need to change this so: if theres input requests it returns, if there arent it runs
    public String runFileForRegenerating(String filePath, String code) {
        AtomicReference<String> result = new AtomicReference<>("");
        try {


            System.out.println("INSIDE RUN FILE FOR REGENERATING");

            inputQueue.clear();
            AtomicBoolean waitingForInput = new AtomicBoolean(false);
            Queue<String> testCase = new LinkedList<>();
            String testSuccess = ".----------------------------------------------------------------------Ran \\d+ test in \\d+\\.\\d+sOK";
//            String testFailure = "FAIL: ([^=]+).*?Traceback \\(most recent call last\\):.*?File \"([^\"]+)\", line (\\d+), in ([^A-Z]+).*?(AssertionError: .+?)------+Ran (\\d+) test.+?FAILED \\(failures=(\\d+)\\)";

//            String testFailure = "FAIL: ([^=]+).*?Traceback \\(most recent call last\\):.*?File \\\"([^\"]+)\\\", line (\\d+), in ([^\\s]+).*?(AssertionError: .+?)\\-+\\nRan (\\d+) test.+?FAILED \\(failures=(\\d+)\\)";
//            String testFailure = "FAIL: ([^=]+).*?Traceback \\(most recent call last\\):.*?File \\\"([^\"]+)\\\", line (\\d+), in ([^\\s]+).*?(AssertionError: .+?)-{5,}\\nRan (\\d+) test.+?FAILED \\(failures=(\\d+)\\)";
            String testFailure = "F\\s*======================================================================\\s*" +
                    "FAIL: (\\w+) \\(([\\w.]+)\\)\\s*" +
                    "----------------------------------------------------------------------\\s*" +
                    "Traceback \\(most recent call last\\):\\s*" +
                    "  File \"([^\"]+)\", line (\\d+), in (\\w+)\\s*" +
                    "    (.*?)\\s*" +
                    "AssertionError: (\\d+) != (\\d+)\\s*" +
                    "----------------------------------------------------------------------\\s*" +
                    "Ran (\\d+) test in ([\\d.]+)s\\s*" +
                    "FAILED \\(failures=(\\d+)\\)";

            ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "python3", "-u", filePath);
            scriptBuilder.redirectErrorStream(true);
            Process process = scriptBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            BufferedReader codeReader = new BufferedReader(new StringReader(code));
            ArrayList<Function> functionCalls = new ArrayList<>();
            ArrayList<Inputs> inputList = new ArrayList<>();

            PythonChunk rootChunk = new PythonChunk(code, "root", null).parse(code);
            ArrayList<PythonChunk> chunks = new ArrayList<>();
            chunks.add(rootChunk);
            chunks = getAllChunks(chunks);

            if (chunks.isEmpty()) {
                chunks.add(rootChunk);
            }

            String line;
            while ((line = codeReader.readLine()) != null) {
                if (!getFunctionCall(line).equals("false")) {
                    PythonChunk functionChunk = findChunk(line, rootChunk);
                    functionCalls.add(new Function(getFunctionCall(line), functionChunk));
                }
            }

            for (PythonChunk chunk : chunks) {
                for (String input : chunk.getInputs()) {
                    if (chunk.getParent() == null) {
                        inputList.add(new Inputs(input, true));
                    } else {
                        inputList.add(new Inputs(input, false));
                    }
                }
            }

            if (!inputList.isEmpty()) {
                result.set("unable to run tests, input requests detected");
                return result.get();
            }

            Thread outputThread = new Thread(() -> {
                try {
                    System.out.println("In output thread");
                    int c;
                    StringBuilder lineBuffer = new StringBuilder();
                    while ((c = reader.read()) != -1) {
                        char ch = (char) c;
                        lineBuffer.append(ch);

                        if (ch == '\n') {
                            if (testCase.size() == 15) {
                                testCase.poll();
                            }
                            testCase.offer(lineBuffer.toString());
                            System.out.println(testCase.toString());

                            if (checkPattern(testFailure, testCase)) {
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                System.out.println("Test failure");
                                if (result.get().contains("FAIL")) {
                                    result.set(result.get() + " SPLIT THE ERROR HERE " + testCase.toString());
                                } else {
                                    result.set("FAIL " + testCase.toString());
                                }
//
                            } else {
                                System.out.println("Test success");
                            }

                            lineBuffer.setLength(0);
                        }
                    }
                } catch (Exception e) {
                }
            });

            Thread errorThread = new Thread(() -> {
                try {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        if (!errorLine.trim().isEmpty()) {
                        }
                    }
                } catch (IOException e) {
                }
            });

            result.set("Success");
            outputThread.start();
            errorThread.start();

            try {
                outputThread.join();
                errorThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Error: Thread was interrupted";
            }

            while (process.isAlive()) {
                if (waitingForInput.get() && !inputQueue.isEmpty()) {
                    try {
                        String input = inputQueue.poll();
                        if (input != null) {
                            writer.write(input + "\n");
                            writer.flush();
                            waitingForInput.set(false);
                        }
                    } catch (IOException e) {
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
        System.out.println("Result: " + result.get());
        return result.get();
    }

}
