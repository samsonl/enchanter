package org.twdata.enchanter.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.twdata.enchanter.SSHConnection;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

/**
 * An implementation of an ssh library using Ganymed
 */
public class GanymedSSH implements SSHConnection {

    private Session sess;
    
    public void connect(String host, String username) throws IOException {
        connect(host, 22, username, "");
    }

    public void connect(String host, int port, String username,
            final String password) throws IOException {
        /* Create a connection instance */

        final Connection conn = new Connection(host);

        /* Now connect */

        conn.connect();

        /*
         * Authenticate. If you get an IOException saying something like
         * "Authentication method password not supported by the server at this
         * stage." then please check the FAQ.
         */

        File home = new File(System.getProperty("user.home"));

        boolean isAuthenticated = conn.authenticateWithPublicKey(username,
                new File(home, ".ssh/id_dsa"), password);

        // if (isAuthenticated == false)
        // w
        // throw new IOException("Authentication failed.");

        /* Create a session */

        sess = conn.openSession();

        sess.requestDumbPTY();

        sess.startShell();

    }
    
    public OutputStream getOutputStream() {
        return sess.getStdin();
    }
    
    public InputStream getInputStream() {
        return sess.getStdout();
    }

    public void disconnect() {
        sess.close();
        sess = null;
    }
}
