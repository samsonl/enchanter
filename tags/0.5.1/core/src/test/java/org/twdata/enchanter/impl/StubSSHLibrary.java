/**
 * 
 */
package org.twdata.enchanter.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.twdata.enchanter.SSHLibrary;

public class StubSSHLibrary implements SSHLibrary {

    InputStream inputStream;
    OutputStream outputStream = new ByteArrayOutputStream();
    
    public void connect(String host, String username) throws IOException {
    }

    public void connect(String host, int port, String username, String password) throws IOException {
    }

    public void disconnect() {
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    
    public String dumpOut() {
        String data = ((ByteArrayOutputStream)outputStream).toString();
        outputStream = new ByteArrayOutputStream();
        return data;
    }

    public void connect(String host, int port, String username, String password, String privateKeyPath) throws IOException {
        // TODO Auto-generated method stub
        
    }
}