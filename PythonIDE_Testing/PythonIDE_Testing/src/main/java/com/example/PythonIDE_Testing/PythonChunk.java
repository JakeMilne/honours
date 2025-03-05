package com.example.PythonIDE_Testing;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonChunk {
    private String chunk;
    private String definition;
    private ArrayList<PythonChunk> children;
    private ArrayList<String> inputs;
    private PythonChunk parent;

    public PythonChunk(String chunk, String definition, PythonChunk parent) {
        this.chunk = chunk;
        this.definition = definition;
        this.children = new ArrayList<>();
        this.inputs = new ArrayList<>();
        this.parent = parent;
    }

    public PythonChunk getParent() {
        return parent;
    }

    public String getDefinition() {
        return definition;
    }

    public PythonChunk parse(String pythonCode) {
        String[] lines = pythonCode.split("\n");
        PythonChunk rootChunk = new PythonChunk(pythonCode, "root", null);
        int i = 0;

        while (i < lines.length) {
            ChunkResult result = parseChunk(lines, i, 0, rootChunk);
            if (result != null) {
                rootChunk.children.add(result.chunk);
                i = result.endIndex;
            } else {
                i++;
            }
        }
        return rootChunk;
    }

    private class ChunkResult {
        PythonChunk chunk;
        int endIndex;

        public ChunkResult(PythonChunk chunk, int endIndex) {
            this.chunk = chunk;
            this.endIndex = endIndex;
        }
    }

    private ChunkResult parseChunk(String[] lines, int startIndex, int indentation, PythonChunk parent) {
        if (startIndex >= lines.length) {
            return null;
        }

        while (startIndex < lines.length && (lines[startIndex].trim().isEmpty() || lines[startIndex].trim().startsWith("#"))) {
            startIndex++;
        }
        if (startIndex >= lines.length) {
            return null;
        }

        String currentLine = lines[startIndex];
        String currentIndent = getIndentation(currentLine);
        if (currentIndent.length() < indentation) {
            return null;
        }
        if (!isDefinitionLine(currentLine.trim())) {
            return null;
        }

        StringBuilder chunkBuilder = new StringBuilder(currentLine).append("\n");
        int currentIndentation = currentIndent.length();
        ArrayList<PythonChunk> children = new ArrayList<>();
        ArrayList<String> detectedInputs = new ArrayList<>();

        int i = startIndex + 1;
        while (i < lines.length) {
            String line = lines[i];

            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                chunkBuilder.append(line).append("\n");
                i++;
                continue;
            }

            String lineIndent = getIndentation(line);
            if (lineIndent.length() <= indentation) {
                break;
            }

            if (isDefinitionLine(line.trim()) && lineIndent.length() > currentIndentation) {
                ChunkResult childChunk = parseChunk(lines, i, lineIndent.length(), parent);
                if (childChunk != null) {
                    children.add(childChunk.chunk);
                    i = childChunk.endIndex;
                    continue;
                }
            }

            chunkBuilder.append(line).append("\n");

            Matcher inputMatcher = Pattern.compile("input\\(\\s*\"(.*?)\"\\s*\\)").matcher(line);
            while (inputMatcher.find()) {
                detectedInputs.add(inputMatcher.group(1));
            }

            i++;
        }

        String chunkStr = chunkBuilder.toString();
        PythonChunk pythonChunk = new PythonChunk(chunkStr, currentLine.trim(), parent);
        pythonChunk.inputs = detectedInputs;
        pythonChunk.children = children;

        return new ChunkResult(pythonChunk, i);
    }

    private String getIndentation(String line) {
        Matcher matcher = Pattern.compile("^(\\s*)").matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    public ArrayList<PythonChunk> getChildren() {
        return children;
    }

    public ArrayList<String> getInputs() {
        return inputs;
    }

    private boolean isDefinitionLine(String line) {
        if ((line.startsWith("class ") || line.startsWith("def ")) && line.contains(":")) {
            return true;
        }
        String[] blockStarters = {"if ", "elif ", "else:", "for ", "while ", "try:", "except ", "finally:", "with "};
        for (String starter : blockStarters) {
            if (line.startsWith(starter) && line.contains(":")) {
                return true;
            }
        }
        return false;
    }

    public void print(int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + "Definition: " + definition);
        System.out.println(indent + "Children: " + children.size());
        for (PythonChunk child : children) {
            child.print(level + 1);
        }
        for (String input : inputs) {
            System.out.println(indent + "Input: " + input);
        }
    }

    public void test(String pythonCode) {
        PythonChunk rootChunk = parse(pythonCode);
        System.out.println("Parsed Python Chunks:");
        rootChunk.print(0);
    }
}
