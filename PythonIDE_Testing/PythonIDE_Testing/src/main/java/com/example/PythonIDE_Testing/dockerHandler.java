package com.example.PythonIDE_Testing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class dockerHandler {

    public dockerHandler() {}

    public void saveFile(String pythonCode, String filePath) {

//            String strippedPythonCode = pythonCode;
            String strippedPythonCode = stripHtmlTags(pythonCode);

            String containerName = "pythonide_testing-app-1";


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
                    "docker", "exec", "-i", "pythonide_testing-app-1",
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


                if(line.contains("Issue: [")){

                    vulnerability += line;
                    for(int j = 0; j < 7; j++){
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
            if(vulnerabilities.toString().equals("[]")){
                System.out.println("No vulnerabilities found");
            }else{
                System.out.println(vulnerabilities.toString());

            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
            return vulnerabilities;

        }


    //method that runs the file and returns the result
    public String runFileAndReturnResult(String filePath) {
        StringBuilder result = new StringBuilder();
        try {
            System.out.println("Running file: " + filePath);
            ProcessBuilder runFileProcess = new ProcessBuilder(
                    "docker", "exec", "-i", "pythonide_testing-app-1",
                    "python3", filePath
            );
            Process runFile = runFileProcess.start();
            runFile.waitFor();
            System.out.println("File execution completed");

            BufferedReader reader = new BufferedReader(new InputStreamReader(runFile.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public Vulnerability parseVulnerability(String vulnString){
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


}
