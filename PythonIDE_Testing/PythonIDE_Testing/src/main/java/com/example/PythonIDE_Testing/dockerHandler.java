package com.example.PythonIDE_Testing;


import java.io.*;



public class dockerHandler {

    public dockerHandler() {}

    public String saveFile(String pythonCode) {
        try {
            String strippedPythonCode = stripHtmlTags(pythonCode);
            String containerName = "pythonide_testing-app-1";
            String filePath = "/tmp/script.py";


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
                        System.out.println("Output: " + line);
                    }
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("Error: " + line);
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

//            ProcessBuilder banditProcess = new ProcessBuilder(
//                    "docker", "exec", "-i", "pythonide_testing-app-1",
//                    "bandit", "-r", "/tmp/script.py"
//            );
            ProcessBuilder banditProcess = new ProcessBuilder(
                    "docker", "exec", "-i", "pythonide_testing-app-1",
//                    "bandit", "-r", "/tmp/script.py", "-v", "-f", "json"
                    "bandit", "-r", "/tmp/script.py", "-v"

            );
            Process bandit = banditProcess.start();
            bandit.waitFor();





//            int banditExitCode = bandit.waitFor();
//            if (banditExitCode != 0) {
//                System.err.println("Error: Bandit analysis failed. Exit code: " + banditExitCode);
//                BufferedReader errorReader = new BufferedReader(new InputStreamReader(bandit.getErrorStream()));
//                String errorLine;
//                while ((errorLine = errorReader.readLine()) != null) {
//                    System.err.println(errorLine);
//                }
//                return;
//            }
            System.out.println("Bandit analysis completed successfully.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(bandit.getInputStream()));
            String line;
            String vulnerability = "";
            int i = 1;
            while ((line = reader.readLine()) != null) {
                System.out.println(("Line " + i));
                System.out.println(line);

                if(line.contains("Issue: [")){

                    vulnerability += line;
                        for(int j = 0; j < 7; j++){
                            i++;
                            line = reader.readLine();
                            System.out.println(("Line " + i));
                            System.out.println(line);
                            vulnerability += line;
                            vulnerability += "\n";
                            parseVulnerability(vulnerability);
                        }

                }

                i++;
            }
            System.out.println(vulnerability);
            return vulnerability;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    //method that calls bandit on the file
    public void runBanditOnFile(String filePath) {
        try {
            System.out.println("Running Bandit on file: " + filePath);
            ProcessBuilder banditProcess = new ProcessBuilder(
                    "docker", "exec", "-i", "pythonide_testing-app-1",
                    "bandit", "-r", filePath, "-v", "-f", "json"
            );
//            banditProcess.redirectErrorStream(true);
            Process bandit = banditProcess.start();
            bandit.waitFor();
            System.out.println("Bandit analysis completed");

            BufferedReader reader = new BufferedReader(new InputStreamReader(bandit.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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

    public void parseVulnerability(String vulnString){
        System.out.println(vulnString);





//        Vulnerability vulnerability = new Vulnerability();
    }
    public static String stripHtmlTags(String input) {
        return input.replaceAll("<[^>]+>", "").trim();
    }


}
