/**
 * 
 */
package org.twdata.enchanter.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
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
public class DefaultSSH implements SSH, StreamListener {
    
    private PrintWriter out;
    
    private StringBuilder backBuffer;
    
    private Map<String, Response> respondWith = new HashMap<String, Response>();
    private List<Prompt> waitFor = new ArrayList<Prompt>();
    List<StreamListener> streamListeners = new ArrayList<StreamListener>();

    private boolean readTillEndOfLine = false;
    private char lastChar;
    private int lastMatch = -1;
    private boolean alive = true;
    private StringBuilder lastLine = new StringBuilder();
    private Thread timeoutThread;
    private int timeout = 0;
    private SSHLibrary sshConnection;
    private Thread streamReaderThread;
    private StreamReader streamReader;

    public DefaultSSH() {
        this.sshConnection = new GanymedSSH();
        this.streamListeners.add(this);
        this.backBuffer = new StringBuilder(5 * 1024);
    }
    
    public synchronized void connect(String host, String username) throws IOException {
        sshConnection.connect(host, username);
        init();
    }

    public synchronized void connect(String host, int port, String username,
            final String password) throws IOException {
        sshConnection.connect(host, port, username, password);
        init();
    }
    
    protected void init() {
        
        this.out = new PrintWriter(sshConnection.getOutputStream());
        this.streamReader = new StreamReader(sshConnection.getInputStream(), streamListeners);
        streamReaderThread = new Thread(streamReader);
        streamReaderThread.start();
    }
    
    public void setSSHConnection(SSHLibrary conn) {
        this.sshConnection = conn;
    }
    
    public synchronized void disconnect() {
        if (timeoutThread != null) {
            timeoutThread.interrupt();
        }
        alive = false;
        streamReader.stop();
        sshConnection.disconnect();
    }
    
    public synchronized void addStreamListener(StreamListener listener) {
        streamListeners.add(listener);
    }
    
    public synchronized void setDebug(boolean debug) {
        if (debug) {
            addStreamListener(new StreamListener() {
                public void hasRead(byte[] b, int pos, int len) {
                    System.out.print(new String(b, pos, len));
                }

                public void hasWritten(byte[] b) {
                    // Not usually necessary
                    // System.out.print(new String(b));
                }
                public void close() {}
            });
        }
    }
    
    public synchronized void send(String text) throws IOException {
        print(text, false);

    }

    public synchronized void sendLine(String text) throws IOException {
        print(text, true);
    }

    public synchronized void sleep(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }
    
    private void print(String text, boolean eol) throws IOException {
        text = text.replace("^C", String.valueOf((char) 3));
        text = text.replace("^M", "\r\n");
        if (eol) {
            out.print(text+"\r\n");
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

    public synchronized void respond(String prompt, String response) {
        if (response == null) {
            respondWith.remove(prompt);
        } else {
            respondWith.put(prompt, new Response(prompt, response));
        }
    }
    
    public synchronized boolean waitFor(String waitFor) throws IOException {
        return waitFor(waitFor, false);
    }

    public synchronized boolean waitFor(String waitFor,
            boolean readLineOnMatch) throws IOException {
        prepare(new String[] { waitFor }, readLineOnMatch);
        try {
            String data = backBuffer.toString();
            backBuffer.setLength(0);
            readFromStream(data, false);
            if (this.waitFor.size() > 0) {
                wait(timeout);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            this.waitFor.clear();
        }
        return (lastMatch == 0);
    }
    static int counter = 0;

    public synchronized int waitForMux(String[] waitFor) throws IOException {
        return waitForMux(waitFor, false);
    }
    
    public synchronized int waitForMux(String[] waitFor,
            boolean readLineOnMatch) throws IOException {
        prepare(waitFor, readLineOnMatch);
        try {
            String data = backBuffer.toString();
            backBuffer.setLength(0);
            readFromStream(data, false);
            if (this.waitFor.size() > 0) {
                wait(timeout);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            this.waitFor.clear();
        }
        return lastMatch;
    }
    
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    protected void prepare(String[] text, boolean readLineOnMatch) {
        this.alive = true;
        for (String val : text) {
            waitFor.add(new Prompt(val, readLineOnMatch));
        }
        this.lastMatch = -1;
        this.readTillEndOfLine = false;
        this.lastLine.setLength(0);
    }

    public synchronized String lastLine() {
        return this.lastLine.toString();
    }
    
    public synchronized String getLine() throws IOException {
        if (waitFor("\r\n", false)) {
            return lastLine();
        }
        return null;
    }

    synchronized void readFromStream(CharSequence data, boolean notifyOnMatch) throws IOException {
        if (waitFor.size() == 0) {
            backBuffer.append(data);
        } else {
            for (int x = 0; x < data.length(); x++) {
                char s = data.charAt(x);
                
                if (waitFor.size() == 0) {
                    backBuffer.append(data, x, data.length() - 1);
                    break;
                } else {
                    if (readTillEndOfLine && (s == '\r' || s == '\n')) {
                        x--;
                        waitFor.clear();
                        notifyAll();
                        continue;
                    }
                    
                    if (s != '\r' && s != '\n')
                        lastLine.append(s);
                    for (int m = 0; alive && m < waitFor.size(); m++) {
                        Prompt prompt = (Prompt) waitFor.get(m);
                        if (prompt.matchChar(s)) {
                            // the whole thing matched so, return the match answer
                            if (prompt.match()) {
                                lastMatch = m;
                                if (prompt.readLineOnMatch() && (s != '\r' && s != '\n')) {
                                    readTillEndOfLine = true;
                                } else {
                                    //System.out.println("found match");
                                    waitFor.clear();
                                    notifyAll();
                                    continue;
                                }
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
                    lookForResponse(s);
                    lastChar = s;
                }
            }
        }
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

    static class Prompt {
        private String prompt;
        private boolean readLineOnMatch;

        private int pos;

        public Prompt(String prompt) {
            this(prompt, false);
        }
        
        public Prompt(String prompt, boolean readLineOnMatch) {
            this.prompt = prompt;
            this.pos = 0;
            this.readLineOnMatch = readLineOnMatch;
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
        
        public boolean readLineOnMatch() {
            return readLineOnMatch;
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

    public void hasRead(byte[] b, int pos, int len) {
        try {
            readFromStream(new String(b, pos, len), true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void hasWritten(byte[] b) {
        // TODO Auto-generated method stub
        
    }
    
    public synchronized void close() {
        waitFor.clear();
        try {
            notifyAll();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}