package com.example.PythonIDE_Testing;


class Function{
    private String call;
    private PythonChunk chunk;

    public Function(String call, PythonChunk chunk){
        this.call = call;
        this.chunk = chunk;
    }

    public String getCall(){
        return call;
    }

    public PythonChunk getChunk(){
        return chunk;
    }




}