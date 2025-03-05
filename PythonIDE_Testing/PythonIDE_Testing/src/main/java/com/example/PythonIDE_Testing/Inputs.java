package com.example.PythonIDE_Testing;


class Inputs{
    private String call;
    private Boolean single;


    public Inputs(String call, Boolean single){
        this.call = call;
        this.single = single;
    }

    public String getCall(){
        return call;
    }

    public Boolean getSingle(){
        return single;
    }


}