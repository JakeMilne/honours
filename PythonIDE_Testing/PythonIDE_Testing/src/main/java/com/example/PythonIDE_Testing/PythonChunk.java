package com.example.PythonIDE_Testing;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//this is used to break the code down into blocks, so I can track input requests and their order.
//I'm using block/chunk to describe anything that adds a layer of indentation in python kinda
//so in this code
//class NumberAnalyzer:
//    def __init__(self, numbers):
//        self.numbers = numbers
//        self.average = self.compute_average()
//
//    def compute_average(self):
//        if self.numbers:
//            return sum(self.numbers) / len(self.numbers)
//        else:
//            return 0
//
//    def analyze(self):
//        results = []
//        for number in self.numbers:
//            if number > self.average:
//                status = "above"
//            else:
//                status = "below or equal"
//            results.append((number, status))
//        return results
//
//class Runner:
//    def run(self):
//        raw_input = input("Enter numbers separated by spaces: ")
//        numbers = list(map(float, raw_input.strip().split()))
//
//        if not numbers:
//            print("No numbers provided.")
//            return
//
//        analyzer = NumberAnalyzer(numbers)
//        print(f"Average: {analyzer.average}")
//
//        results = analyzer.analyze()
//        for number, status in results:
//            print(f"{number} is {status} the average")
//
//if __name__ == "__main__":
//    runner = Runner()
//    runner.run()


//there is a root chunk, with 3 child chunks (NumberAnalyzer, Runner, and the if __name__ == "__main__": block)
//NumberAnalyzer has 3 child chunks (init, compute_average, and analyze), inside analyze there is 1 child chunk (for number)
// and inside for number there are 2 child chunks (if number > self.average and else)
//Runner has 1 child chunk (run) with 1 input request (raw_input), inside run there are 2 child chunks (if not numbers and for number, status in results)

//since the input request isnt in the root chunk it will be saved with repeatable = true, since the chunk its in could be called many times,

// "input request" refers to instances of the input() function


public class PythonChunk {
    private String chunk; //the code block
    private String definition; //definition line, i.e. "class NumberAnalyzer:"
    private ArrayList<PythonChunk> children; //any child chunks
    private ArrayList<String> inputs; //any input requests in the chunk
    private PythonChunk parent;// the parent chunk that this chunk belongs to (null if this is the root chunk)

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

    //finding child chunks for the root chunk,
    public PythonChunk parse(String pythonCode) {
        String[] lines = pythonCode.split("\n");
        PythonChunk rootChunk = new PythonChunk(pythonCode, "root", null);
        rootChunk.inputs = detectInputs(pythonCode);
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

    //finding input requests in the code
    private ArrayList<String> detectInputs(String pythonCode) {
        ArrayList<String> detectedInputs = new ArrayList<>();
        String[] lines = pythonCode.split("\n");

        Pattern pattern = Pattern.compile("input\\(\\s*\"(.*?)\"\\s*\\)");

        for (String line : lines) {
            if (getIndentation(line).isEmpty()) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    detectedInputs.add(matcher.group(1));
                }
            }
        }
        return detectedInputs;
    }


    //Object used to store when a chunk ends
    private class ChunkResult {
        PythonChunk chunk;
        int endIndex;

        public ChunkResult(PythonChunk chunk, int endIndex) {
            this.chunk = chunk;
            this.endIndex = endIndex;
        }
    }

    //this is the main method for parsing the code into chunks, finds any child chunks and input requests, then stops when the indentation level goes down
    private ChunkResult parseChunk(String[] lines, int startIndex, int indentation, PythonChunk parent) {

        //this stuff is checking if I've reached the end of the file or if theres a comment/ empty line
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

        //ready to start actually building the chunk
        StringBuilder chunkBuilder = new StringBuilder(currentLine).append("\n");
        int currentIndentation = currentIndent.length();
        ArrayList<PythonChunk> children = new ArrayList<>();
        ArrayList<String> detectedInputs = new ArrayList<>();

        int i = startIndex + 1;
        while (i < lines.length) {
            String line = lines[i];

            //checking for comments and empty lines
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                chunkBuilder.append(line).append("\n");
                i++;
                continue;
            }

            //checking if the indentation level has gone down, if it has the chunk is done
            String lineIndent = getIndentation(line);
            if (lineIndent.length() <= indentation) {
                break;
            }

            //checking if the line is a definition line, and starting a child chunk if it is + the indentation level is higher
            if (isDefinitionLine(line.trim()) && lineIndent.length() > currentIndentation) {
                ChunkResult childChunk = parseChunk(lines, i, lineIndent.length(), parent);
                if (childChunk != null) {
                    children.add(childChunk.chunk);
                    i = childChunk.endIndex;
                    continue;
                }
            }

            chunkBuilder.append(line).append("\n");

            //checking for input requests
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

    public String toString() {
        return "PythonChunk{" +
                "definition='" + definition + '\'' +
                ", children=" + children +
                ", inputs=" + inputs +
                '}';
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
