package com.example.PythonIDE_Testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.CSVWriter;

import java.io.FileWriter;

//https://github.com/tuhh-softsec/LLMSecEval/

public class Evaluation {
    //nD array containing prompt, how many vulnerabilities in the first iteration, how many vulnerabilities in the second iteration, third iteration
    private ArrayList<ArrayList<String>> evalData;

    private ArrayList<String> prompts;


    public static void eval() {
//        String inputFilePath = "C:\\Users\\jake\\output.csv";
        String inputFilePath = "C:\\Users\\jake\\Downloads\\LLMSecEval-Prompts_dataset.csv";
        String outputFilePath = "C:\\Users\\jake\\output_with_vulnerabilities.csv";

        try (CSVReader reader = new CSVReader(new FileReader(inputFilePath));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath))) {

            String[] header = {"Prompt", "Vulnerability Count 1", "Vulnerability Count 2", "Vulnerability Count 3"};
            writer.writeNext(header);

            reader.readNext();
            String[] nextLine;
//            int count = 0;

            while ((nextLine = reader.readNext()) != null) { // && count < 20
                if (nextLine.length > 0) {
                    String prompt = nextLine[2];
                    ArrayList<Iteration> iterations = getVulnerabilities(prompt);


                    String[] data = new String[4];
                    data[0] = prompt;

                    for (int i = 0; i < 3; i++) {
                        if (i < iterations.size()) {
                            int vulnerabilityCount = iterations.get(i).getIssues().size();
                            data[i + 1] = String.valueOf(vulnerabilityCount);
                        } else {
                            data[i + 1] = "0"; //
                        }
                    }

                    writer.writeNext(data);
                }
//                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<Iteration> getVulnerabilities(String prompt) {
        codeGenerator generator = new codeGenerator(prompt, new String[0], new String[0], new String[0], new String[0]);
        String callresponse = generator.callLM(prompt);

        // response handler object parses LLM responses
        ResponseHandler responseHandler = new ResponseHandler();
        String content = responseHandler.extractCode(callresponse);
        content = "<pre>" + content.replace("\n", "<br>") + "</pre>";


//        request.getSession().setAttribute("codeGenerator", generator);
//        request.getSession().setAttribute("responseHandler", responseHandler);


        // runTests method deals with the dockerHandler object and its methods
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
        ArrayList<Iteration> iterations = new ArrayList<Iteration>();
        //dockerHandler object deals with saving files and doing stuff such as calling bandit on files

        dockerHandler docker = new dockerHandler();

        //adding docker to session variables
//        request.getSession().setAttribute("dockerHandler", docker);


        String filePath = "/tmp/script.py";
        docker.saveFile(content, filePath, true);
        ArrayList<Issue> vulnerabilities = docker.runBanditOnFile(filePath);
        iterations.add(new Iteration(content, vulnerabilities));
        String newContent = "";

        while (!vulnerabilities.isEmpty() && (generator.getIterationCount() <= generator.iterationCap)) {
            generator.incrementIterationCount();
            newContent = generator.regenerateForVulnerability(content, vulnerabilities);
            String newCode = responseHandler.extractCode(newContent);
            filePath = "/tmp/script" + generator.getIterationCount() + ".py";
            docker.saveFile(newCode, filePath, true);
            vulnerabilities = docker.runBanditOnFile(filePath);
            newCode = "<pre>" + newCode.replace("\n", "<br>") + "</pre>";
            iterations.add(new Iteration(newCode, vulnerabilities));
        }

        return iterations;


    }


}