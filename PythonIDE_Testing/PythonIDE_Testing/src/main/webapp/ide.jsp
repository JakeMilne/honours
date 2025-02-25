<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Local Host</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        #conversation {
            width: 80%;
            height: 300px;
            margin-bottom: 10px;
            padding: 10px;
            border: 1px solid #ccc;
            font-family: monospace;
            white-space: pre-wrap;
            overflow-y: auto;
        }
        #usercode {
            width: 80%;
            height: 100px;
        }
        #userinput {
            width: 80%;
            height: 50px;
        }
        #runButton {
            padding: 10px;
        }
    </style>
    <script>
        const socket = new WebSocket('ws://localhost:8083/ws');

        socket.onopen = function() {
            console.log('WebSocket connection established');
        };

        socket.onmessage = function(event) {
            const result = event.data;

            const conversation = document.getElementById('conversation');
            conversation.value += 'Server: ' + result + '\n';

            conversation.scrollTop = conversation.scrollHeight;
        };

        function runCode() {
            const userCode = document.getElementById('usercode').value;

            const conversation = document.getElementById('conversation');
            conversation.value += 'User (code): ' + userCode + '\n';

            conversation.scrollTop = conversation.scrollHeight;

            socket.send(JSON.stringify({
                type: 'runCode',
                code: userCode
            }));
        }

        function sendInput() {
            const userInput = document.getElementById('userinput').value;

            const conversation = document.getElementById('conversation');
            conversation.value += 'User (input): ' + userInput + '\n';

            conversation.scrollTop = conversation.scrollHeight;

            socket.send(JSON.stringify({
                type: 'userInput',
                input: userInput
            }));

            document.getElementById('userinput').value = '';
        }

        // Set the usercode from the servlet
        window.onload = function() {
            const usercode = "${usercode != null ? usercode : ''}";
            document.getElementById('usercode').value = usercode;
        };
    </script>
</head>
<body>
<h1>Code Editor</h1>
<textarea id="usercode" placeholder="Enter your Python code here..." rows="5" cols="80"></textarea><br>

<button id="runButton" onclick="runCode()">Run Code</button>

<h2>Conversation</h2>
<textarea id="conversation" readonly></textarea><br>

<h2>User Input (for interacting with the running code)</h2>
<textarea id="userinput" placeholder="Enter input to interact with the code..." rows="3" cols="80"></textarea><br>

<button id="sendInputButton" onclick="sendInput()">Send Input</button>

</body>
</html>
