package com.example.PythonIDE_Testing;

public class LLM {

    private string url;
    private string model;
    private boolean needsKey;
    private string key;

    public LLM(string url, string model, boolean needsKey, string key){
        this.url = url;
        this.model = model;
        this.needsKey = needsKey;
        this.key = key;

    }



    public string getUrl(){
        return url;
    }

    public string getModel(){
        return model;
    }

    public boolean getNeedsKey(){
        return needsKey;
    }

    public string getKey(){
        return key;
    }

}