package com.example.PythonIDE_Testing;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonChunk {
    String chunk;
    String definition;
    PythonChunk[] children;
    ArrayList<String> inputs;

    public PythonChunk(String chunk, String definition) {
        this.chunk = chunk;
        this.definition = definition;
        this.children = new PythonChunk[0];
        this.inputs = new ArrayList<>();
    }

    public static PythonChunk parse(String pythonCode) {
        String[] lines = pythonCode.split("\n");
        StringBuilder fullCode = new StringBuilder();
        for (String line : lines) {
            fullCode.append(line).append("\n");
        }

        PythonChunk rootChunk = new PythonChunk(fullCode.toString(), "root");
        ArrayList<PythonChunk> topLevelChunks = new ArrayList<>();

        int i = 0;
        while (i < lines.length) {
            ChunkResult result = parseChunk(lines, i, 0);
            if (result != null) {
                topLevelChunks.add(result.chunk);
                i = result.endIndex;
            } else {
                i++;
            }
        }

        rootChunk.children = topLevelChunks.toArray(new PythonChunk[0]);
        return rootChunk;
    }

    private static class ChunkResult {
        PythonChunk chunk;
        int endIndex;

        public ChunkResult(PythonChunk chunk, int endIndex) {
            this.chunk = chunk;
            this.endIndex = endIndex;
        }
    }

    private static ChunkResult parseChunk(String[] lines, int startIndex, int indentation) {
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
            chunkBuilder.append(line).append("\n");

            Matcher inputMatcher = Pattern.compile("input\\(\\s*\"(.*?)\"\\s*\\)").matcher(line);
            while (inputMatcher.find()) {
                detectedInputs.add(inputMatcher.group(1));
            }

            i++;
        }

        String chunkStr = chunkBuilder.toString();
        PythonChunk pythonChunk = new PythonChunk(chunkStr, currentLine.trim());
        pythonChunk.inputs = detectedInputs;

        pythonChunk.children = children.toArray(new PythonChunk[0]);
        return new ChunkResult(pythonChunk, i);
    }

    private static String getIndentation(String line) {
        Matcher matcher = Pattern.compile("^(\\s*)").matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static boolean isDefinitionLine(String line) {
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
        System.out.println(indent + "Children: " + children.length);
        for (PythonChunk child : children) {
            child.print(level + 1);
        }
        for (String input : inputs) {
            System.out.println(indent + "Input: " + input);
        }
    }

    public static void test(String pythonCode) {
        PythonChunk rootChunk = PythonChunk.parse(pythonCode);
        System.out.println("Parsed Python Chunks:");
        rootChunk.print(0);
    }
}
