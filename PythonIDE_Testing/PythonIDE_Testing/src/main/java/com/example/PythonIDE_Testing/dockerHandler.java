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
//I've got the container name saved as an environment variable, but you can just hardcode it in if you want. on line 38
public class dockerHandler {

    //input queue is used to store the order of input requests in the python code, used in runFile on line 221
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

                inputQueue.clear(); //clearing queue, if the user wants to run code more than once, the queue would still have the old inputs
                AtomicBoolean waitingForInput = new AtomicBoolean(false);
                Queue<String> testCase = new LinkedList<>();

                //regex patterns for the unittest framework
                String testSuccess = ".----------------------------------------------------------------------Ran \\d+ test in \\d+\\.\\d+sOK";
//                String testFailure = "FAIL: ([^=]+).*?Traceback \\(most recent call last\\):.*?File \"([^\"]+)\", line (\\d+), in ([^A-Z]+).*?(AssertionError: .+?)------+Ran (\\d+) test.+?FAILED \\(failures=(\\d+)\\)";

//                String testFailure = "^FAIL: (.+?) \\((.+?)\\)\\n[-]+?\\nTraceback \\(most recent call last\\):\\n(?:.*?\\n)+?AssertionError: .+?";
                String testFailure = "F\\n=+\\nFAIL: (.+?) \\((.+?)\\)\\n-+\\nTraceback \\(most recent call last\\):\\n(?:.*?\\n)+?AssertionError: .+";


//                System.out.println("Running Python script: " + filePath);
                ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "python3", "-u", filePath);
                scriptBuilder.redirectErrorStream(true);
                Process process = scriptBuilder.start();

                //different readers + writers for the code output
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                BufferedReader codeReader = new BufferedReader(new StringReader(code));

                String line;
//                ArrayList<Function> functionCalls = new ArrayList<>();
                ArrayList<Inputs> inputList = new ArrayList<>();

//                System.out.println("Reading output");
                StringBuilder lineBuffer = new StringBuilder();

                //Im using the PythonChunk class to break the python code down into chunks (functions, loops etc) to determine if inputs are reuseable
                //root chunk is the entire code
                PythonChunk rootChunk = new PythonChunk(code, "root", null).parse(code);
                ArrayList<PythonChunk> chunks = new ArrayList<>();
                chunks.add(rootChunk);

                chunks = getAllChunks(chunks);

//                if (chunks.size() == 0) {
////                    System.out.println("Size is 0 for some reason");
////                    System.out.println(rootChunk.toString());
//                    chunks.add(rootChunk);
//                }


//                while ((line = codeReader.readLine()) != null) {
//                    if (!(getFunctionCall(line).equals("false"))) {
//                        PythonChunk functionChunk = findChunk(line, rootChunk);
//                        functionCalls.add(new Function(getFunctionCall(line), functionChunk));
//                    }
//                }


                //getting inputs from all chunks
                for (PythonChunk chunk : chunks) {
//                    System.out.println(chunk.toString());
                    for (String input : chunk.getInputs()) {
                        System.out.println("Input: " + input);
                        if (chunk.getParent() == null) { //if the parent node is null its in the root chunk, so not reusable as it isnt inside a function or similar
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

                        StringBuilder lineBuffer2 = new StringBuilder();

                        ArrayList<Inputs> usedInputs = new ArrayList<>();
                        while ((c = reader.read()) != -1) {
//                            sendMessageToUser(session, "In output thread");
                            char ch = (char) c;
                            lineBuffer2.append(ch);
//                            System.out.println(lineBuffer2.toString());
//                            System.out.println(inputList.size());


                            //checking if a line contains an input request, if it does it gets added to usedInputs, and removed from the list IF its a single use input
                            Iterator<Inputs> iterator = inputList.iterator();
                            while (iterator.hasNext()) {
                                Inputs input = iterator.next();
//                                System.out.println("INPUT CALL: ");
//                                System.out.println(input.getCall());
                                if (lineBuffer2.toString().contains(input.getCall())) {

//                                    sendMessageToUser(session, "line 295 Detected input request");
                                    usedInputs.add(input);

                                    sendMessageToUser(session, lineBuffer2.toString());

                                    //without this, the user gets the reponse sent to them as it is being built. i.e H, then He, Hel, Hell, Hello
                                    if (lineBuffer2.indexOf(input.getCall()) != -1) {
                                        lineBuffer2.delete(lineBuffer2.indexOf(input.getCall()), lineBuffer2.indexOf(input.getCall()) + input.getCall().length());
                                    }
                                    if (input.getSingle()) {
                                        iterator.remove();
                                    }

                                    //flag to say that the code is waiting for input
                                    waitingForInput.set(true);
                                }
                            }


                            if (ch == '\n') {
                                String outputLine = lineBuffer2.toString().trim();
//                                System.out.println("308 " + inputList.size());
                                for (Inputs input : usedInputs) {
//                                    System.out.println("line 309 " + input.getCall());
                                    //when a response is sent, its appended to the input request, this removes the input from the line
                                    if (outputLine.contains(input.getCall())) {
                                        outputLine = outputLine.replace(input.getCall(), "");
                                    }
                                }
                                sendMessageToUser(session, outputLine);
                                System.out.println(outputLine);

                                //unittest failure message is ~ 15 lines long, so I'm tracking the last 15 lines.
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
                                sendMessageToUser(session, " Sending input: " + input);
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


    public void addUserInput(String input) {
        System.out.println("adding to input queue: " + input);
        inputQueue.offer(input);
    }


    //used to send output from the python code, or from bandit
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


    //used to find unittest outputs, pattern is the regex for either a success or failure, testCase is the output im checking it against
    private boolean checkPattern(String pattern, Queue<String> testCase) {
        StringBuilder concatenatedLines = new StringBuilder();
        for (String line : testCase) {
            concatenatedLines.append(line).append("\n");

        }

//        System.out.println("Concatenated lines: " + concatenatedLines.toString());
//        System.out.println("Pattern: " + pattern);
//        System.out.println();
//        System.out.println();
//        System.out.println("Pattern");
//        System.out.println(pattern);
//        System.out.println();
//        System.out.println("output");
//        System.out.println(concatenatedLines);
//        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);

        java.util.regex.Matcher matcher = regexPattern.matcher(concatenatedLines.toString().trim());

        if (matcher.find()) {
            System.out.println("Pattern found! " + pattern);
            return true;
        } else {
            System.out.println("Pattern not found! " + pattern);
//            System.out.println(concatenatedLines.toString());
        }
        return false;
    }

    //used to find child chunks for a chunk/set of chunks
    public static ArrayList<PythonChunk> getAllChunks(ArrayList<PythonChunk> chunks) {

        ArrayList<PythonChunk> allChunks = new ArrayList<>();
        for (PythonChunk chunk : chunks) {
            allChunks.add(chunk);
            allChunks.addAll(getAllChunks(chunk.getChildren()));
        }

        return allChunks;

    }


    //this is very similar to runFile, but instead of sending output to the user, this checks if any unittests fails, and returns the result. if a test fails, the code is regenerated.
    //this is used in the runTestsWithUnits method  in MyServlet.java
    public String runFileForRegenerating(String filePath, String code) {
        AtomicReference<String> result = new AtomicReference<>("");
        try {


//            System.out.println("INSIDE RUN FILE FOR REGENERATING");

            inputQueue.clear();
            AtomicBoolean waitingForInput = new AtomicBoolean(false);
            Queue<String> testCase = new LinkedList<>();
            String testSuccess = ".----------------------------------------------------------------------Ran \\d+ test in \\d+\\.\\d+sOK";
//            String testFailure = "^FAIL: (.+?) \\((.+?)\\)\\n[-]+?\\nTraceback \\(most recent call last\\):\\n(?:.*?\\n)+?AssertionError: .+?";
            String testFailure = "F\\n=+\\nFAIL: (.+?) \\((.+?)\\)\\n-+\\nTraceback \\(most recent call last\\):\\n(?:.*?\\n)+?AssertionError: .+";


            ProcessBuilder scriptBuilder = new ProcessBuilder("docker", "exec", "-i", containerName, "python3", "-u", filePath);
            scriptBuilder.redirectErrorStream(true);
            Process process = scriptBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            BufferedReader codeReader = new BufferedReader(new StringReader(code));
//            ArrayList<Function> functionCalls = new ArrayList<>();
            ArrayList<Inputs> inputList = new ArrayList<>();

            PythonChunk rootChunk = new PythonChunk(code, "root", null).parse(code);
            ArrayList<PythonChunk> chunks = new ArrayList<>();
            chunks.add(rootChunk);
            chunks = getAllChunks(chunks);

            if (chunks.isEmpty()) {
                chunks.add(rootChunk);
            }

            String line;
//            while ((line = codeReader.readLine()) != null) {
//                if (!getFunctionCall(line).equals("false")) {
//                    PythonChunk functionChunk = findChunk(line, rootChunk);
//                    functionCalls.add(new Function(getFunctionCall(line), functionChunk));
//                }
//            }

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
                            if (testCase.size() == 50) {
                                testCase.poll();
                            }
                            testCase.offer(lineBuffer.toString().trim());
//                            System.out.println(testCase.toString());


                            if (checkPattern(testFailure, testCase)) {
//                                System.out.println("Test failure");


                                if (result.get().contains("FAIL")) {
                                    result.set(result.get() + " SPLIT THE ERROR HERE " + testCase.toString());
                                } else {
                                    result.set("FAIL " + testCase.toString());
                                }
//
                            } else {
//                                System.out.println("Test success");
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

//            result.set("Success");
            outputThread.start();
            errorThread.start();

            try {
                outputThread.join();
                errorThread.join();

                if (!result.get().contains("FAIL")) {
                    result.set("Success");
                }
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
