package com.example.PythonIDE_Testing;

//used to represent a python input request, which is used to track which outputs are input requests
class Inputs {
    private String call;
    private Boolean single;


    public Inputs(String call, Boolean single) {
        this.call = call;
        this.single = single;
    }

    public String getCall() {
        return call;
    }

    public Boolean getSingle() {
        return single;
    }


}