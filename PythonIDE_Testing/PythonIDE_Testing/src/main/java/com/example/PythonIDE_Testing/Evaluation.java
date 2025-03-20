package com.example.PythonIDE_Testing;

import java.io.IOException;
import java.util.ArrayList;

import com.opencsv.CSVReader;

import java.io.FileReader;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.CSVWriter;

import java.io.FileWriter;

public class Evaluation {
    private ArrayList<ArrayList<String>> evalData;
    private ArrayList<String> prompts;

    public static void eval() {
        String inputFilePath = "C:\\Users\\jake\\Downloads\\LLMSecEval-Prompts_dataset.csv";

        for (int run = 1; run <= 3; run++) {
            String outputFilePath = "C:\\Users\\jake\\output_with_vulnerabilities_run" + run + ".csv";
            try (CSVReader reader = new CSVReader(new FileReader(inputFilePath));
                 CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath))) {
                String[] header = {
                        "Prompt",
                        "Vulnerability Count 1", "Vulnerability Count 2", "Vulnerability Count 3",
                        "Vulnerability Count 4", "Vulnerability Count 5", "Lowest Vulnerability Count",
                        "CWE 1", "CWE 2", "CWE 3", "CWE 4", "CWE 5", "Lowest CWE"
                };
                writer.writeNext(header);
                reader.readNext();
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    if (nextLine.length > 0) {
                        String prompt = nextLine[2];
                        ArrayList<Iteration> iterations = getVulnerabilities(prompt);
                        String[] data = new String[13];
                        data[0] = prompt;

                        int lowest = Integer.MAX_VALUE;
                        StringBuilder lowestCWE = new StringBuilder();

                        for (int i = 0; i < 5; i++) {
                            if (i < iterations.size()) {
                                Iteration iteration = iterations.get(i);
                                int issueCount = iteration.getIssues().size();
                                data[i + 1] = String.valueOf(issueCount);

                                StringBuilder cwes = new StringBuilder();
                                for (Issue issue : iteration.getIssues()) {
                                    if (issue instanceof Vulnerability) {
                                        if (cwes.length() > 0) {
                                            cwes.append(", ");
                                        }
                                        cwes.append(((Vulnerability) issue).getCWE());
                                    }
                                }
                                data[i + 7] = cwes.toString();

                                if (issueCount < lowest) {
                                    lowest = issueCount;
                                    lowestCWE.setLength(0);
                                    lowestCWE.append(cwes);
                                }
                            } else {
                                data[i + 1] = "0";
                                data[i + 7] = "";
                            }
                        }

                        if (lowest == Integer.MAX_VALUE) {
                            lowest = 0;
                            lowestCWE.setLength(0);
                        }

                        data[6] = String.valueOf(lowest);
                        data[12] = lowestCWE.toString();
                        writer.writeNext(data);
                    }
                }
            } catch (IOException | CsvValidationException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<Iteration> getVulnerabilities(String prompt) {
        codeGenerator generator = new codeGenerator(prompt, new String[0], new String[0], new String[0], new String[0]);
        String callresponse = generator.callLM(prompt);
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";
        ArrayList<Iteration> iterations = runTests(generator, responseHandler, content);
        for (Iteration iteration : iterations) {
            System.out.println(iteration.getIssues().size());
            for (Issue issue : iteration.getIssues()) {
                System.out.println(issue.toString());
            }
        }
        return iterations;
    }

    public static ArrayList<Iteration> runTests(codeGenerator generator, ResponseHandler responseHandler, String content) {
        ArrayList<Iteration> iterations = new ArrayList<>();
        dockerHandler docker = new dockerHandler();
        String filePath = "/tmp/script.py";
        docker.saveFile(content, filePath, true);
        ArrayList<Issue> vulnerabilities = docker.runBanditOnFile(filePath);
        iterations.add(new Iteration(content, vulnerabilities));
        String newContent = "";
        while (!vulnerabilities.isEmpty() && (generator.getIterationCount() < generator.iterationCap)) {
            generator.incrementIterationCount();
            newContent = generator.regenerateForVulnerability(content, vulnerabilities, (generator.getIterationCount() == 1));
            String newCode = responseHandler.extractCode(newContent);
            filePath = "/tmp/script" + generator.getIterationCount() + ".py";
            docker.saveFile(newCode, filePath, false);
            vulnerabilities = docker.runBanditOnFile(filePath);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
            iterations.add(new Iteration(newCode, vulnerabilities));
        }
        return iterations;
    }
}
