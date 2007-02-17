package org.twdata.enchanter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SSHLibrary {
    /**
     * Connect to the remote SSH server using public key authentication
     * 
     * @param host The remote SSH server
     * @param username The user name on the server
     * @throws IOException If a connection cannot be made
     */
    public void connect(String host, String username) throws IOException;

    /**
     * Connect to the remote SSH server using password authentication
     * 
     * @param host The remote SSH server
     * @param username The user name on the server
     * @throws IOException If a connection cannot be made
     */
    public void connect(String host, int port, String username, String password)
            throws IOException;
    
    /**
     * Gets the inputstream of the current connection
     */
    public InputStream getInputStream();

    /**
     * Gets the output stream of the current connection
     */
    public OutputStream getOutputStream();
    
    /**
     * Disconnects from the remote SSH server
     */
    public void disconnect();
}
