package com.example.PythonIDE_Testing;

import java.util.ArrayList;

public class Iteration {
    private String code;
    private ArrayList<Issue> issues;

    public Iteration(String code, ArrayList<Issue> issues) {
        this.code = code;
        this.issues = issues;
    }

    public String getCode() {
        return code;
    }

    public ArrayList<Issue> getIssues() {
        return issues;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setIssues(ArrayList<Issue> issues) {
        this.issues = issues;
    }


}
