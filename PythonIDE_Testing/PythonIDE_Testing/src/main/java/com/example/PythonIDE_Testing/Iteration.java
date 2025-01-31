package com.example.PythonIDE_Testing;

import java.util.ArrayList;


public class Iteration {
    private String code;
    private ArrayList<Vulnerability> vulnerabilities;

    public Iteration(String code, ArrayList<Vulnerability> vulnerabilities) {
        this.code = code;
        this.vulnerabilities = vulnerabilities;
    }

    public String getCode() {
        return code;
    }

    public ArrayList<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setVulnerabilities(ArrayList<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
}