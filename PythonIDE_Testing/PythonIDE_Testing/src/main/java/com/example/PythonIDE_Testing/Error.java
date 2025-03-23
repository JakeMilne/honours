package com.example.PythonIDE_Testing;

//used for unittest failures
public class Error extends Issue {

    private String error;


    public Error(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
