/**
 * 
 */
package org.twdata.enchanter.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.twdata.enchanter.SSH;
import org.twdata.enchanter.SSHLibrary;
import org.twdata.enchanter.StreamListener;

/**
 * Default implementation of SSH connection and parsing methods
 */
public class DefaultSSH implements SSH {
    
    private BufferedInputStream in;
    private PrintWriter out;
    
    private Map<String, Response> respondWith = new HashMap<String, Response>();
    private List<Prompt> waitFor = new ArrayList<Prompt>();
    List<StreamListener> streamListeners = new ArrayList<StreamListener>();

    private char lastChar;
    private boolean alive = true;
    private StringBuilder lastLine = new StringBuilder();
    private Thread timeoutThread;
    private int timeout = 0;
    private SSHLibrary sshConnection;

    public DefaultSSH() {
        this.sshConnection = new GanymedSSHLibrary();
    }
    
    public void connect(String host, String username) throws IOException {
        sshConnection.connect(host, username);
        setupStreams();
    }

    private void setupStreams() {
        this.in = new BufferedInputStream(sshConnection.getInputStream());
        this.out = new PrintWriter(sshConnection.getOutputStream());
        for (StreamListener listener : streamListeners) {
            listener.init(this.out);
        }
    }

    public void connect(String host, int port, String username,
            final String password) throws IOException {
        sshConnection.connect(host, port, username, password);
        setupStreams();
    }
    
    public void setSSHConnection(SSHLibrary conn) {
        this.sshConnection = conn;
    }
    
    public void disconnect() {
        if (timeoutThread != null) {
            timeoutThread.interrupt();
        }
        alive = false;
        sshConnection.disconnect();
    }
    
    public void addStreamListener(StreamListener listener) {
        streamListeners.add(listener);
    }
    
    public void setDebug(boolean debug) {
        if (debug) {
            addStreamListener(new StreamListener() {
                public void hasRead(byte b) {
                    if (b != '\r')
                        System.out.print((char) b);
                }

                public void hasWritten(byte[] b) {
                    // Not usually necessary
                    // System.out.print(new String(b));
                }

                public void init(PrintWriter writer) {
                }
            });
        }
    }
    
    public void send(String text) throws IOException {
        print(text, false);

    }

    public void sendLine(String text) throws IOException {
        print(text, true);
    }

    public void sleep(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }
    
    private void print(String text, boolean eol) throws IOException {
        text = text.replace("^C", String.valueOf((char) 3));
        text = text.replace("^M", "\r\n");
        if (eol) {
            out.print(text+"\n");
            out.flush();
            getLine();
        } else {
            out.print(text);
            out.flush();
        }
        byte[] bytes = text.getBytes();
        for (StreamListener listener : streamListeners) {
            listener.hasWritten(bytes);
        }

    }

    public void respond(String prompt, String response) {
        if (response == null) {
            respondWith.remove(prompt);
        } else {
            respondWith.put(prompt, new Response(prompt, response));
        }
    }
    
    public boolean waitFor(String waitFor) throws IOException {
        return waitFor(waitFor, false);
    }

    public boolean waitFor(String waitFor,
            boolean readLineOnMatch) throws IOException {
        prepare(new String[] { waitFor });
        return (readFromStream(readLineOnMatch) == 0);
    }

    public int waitForMux(String... waitFor) throws IOException {
        return waitForMux(waitFor, false);
    }
    
    public int waitForMux(String[] waitFor,
            boolean readLineOnMatch) throws IOException {
        prepare(waitFor);
        return readFromStream(readLineOnMatch);
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    protected void prepare(String[] text) {
        this.alive = true;
        for (String val : text) {
            waitFor.add(new Prompt(val));
        }
        this.lastLine.setLength(0);
    }

    public String lastLine() {
        return this.lastLine.toString();
    }
    
    public String getLine() throws IOException {
        if (waitFor("\r\n", false)) {
            return lastLine();
        }
        return null;
    }

    public int readFromStream(boolean readLineOnMatch) throws IOException {
        int result = -1;
        int data;
        boolean readTillEndOfLine = false;
        if (timeout > 0) {
            timeoutThread = new Thread() {
                public void run() {
                    try {
                        sleep(timeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                    alive = false;
                }
            };
            timeoutThread.start();
        }
        while (alive && (data = in.read()) >= 0) {
            for (StreamListener listener : streamListeners) {
                listener.hasRead((byte) data);
            }
            char c = (char) data;
            if (readTillEndOfLine && (c == '\r' || c == '\n'))
                break;

            int match = lookForMatch(c);
            if (match != -1) {
                result = match;
                if (readLineOnMatch && (c != '\r' && c != '\n')) {
                    readTillEndOfLine = true;
                } else {
                    break;
                }
            } else {
                lookForResponse((char) data);
                lastChar = (char) data;
            }
        }
        reset();
        return result;
    }

    int lookForMatch(char s) {
        if (s != '\r' && s != '\n')
            lastLine.append(s);
        for (int m = 0; alive && m < waitFor.size(); m++) {
            Prompt prompt = (Prompt) waitFor.get(m);
            if (prompt.matchChar(s)) {
                // the whole thing matched so, return the match answer
                if (prompt.match()) {
                    return m;
                } else {
                    prompt.nextPos();
                }

            } else {
                // if the current character did not match reset
                prompt.resetPos();
                if (s == '\n' && lastChar == '\r') {
                    lastLine.setLength(0);
                }
            }
        }
        return -1;
    }

    void lookForResponse(char s) throws IOException {
        for (Response response : respondWith.values()) {
            if (response.matchChar(s)) {
                if (response.match()) {
                    print(response.getResponse(), false);
                    response.resetPos();
                } else {
                    response.nextPos();
                }
            } else {
                response.resetPos();
            }
        }
    }

    void reset() {
        waitFor.clear();
        if (timeout > 0) {
            timeoutThread.interrupt();
        }
        alive = true;
    }
    
    static class Prompt {
        private String prompt;

        private int pos;

        public Prompt(String prompt) {
            this.prompt = prompt;
            this.pos = 0;
        }

        public boolean matchChar(char c) {
            return (prompt.charAt(pos) == c);
        }

        public boolean match() {
            return pos + 1 == prompt.length();
        }

        public String getPrompt() {
            return prompt;
        }

        public void nextPos() {
            this.pos++;
        }

        public void resetPos() {
            this.pos = 0;
        }

    }

    static class Response extends Prompt {
        private String response;

        public Response(String prompt, String response) {
            super(prompt);
            this.response = response;
        }

        public String getResponse() {
            return response;
        }
    }

}