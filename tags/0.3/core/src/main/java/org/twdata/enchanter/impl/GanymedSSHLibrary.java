package org.twdata.enchanter.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.twdata.enchanter.SSHLibrary;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.Session;

/**
 * An implementation of an ssh library using Ganymed
 */
public class GanymedSSHLibrary implements SSHLibrary {

    private Session sess;
    
    public void connect(String host, String username) throws IOException {
        connect(host, 22, username, "");
    }

    public void connect(String host, int port, String username,
            final String password) throws IOException {
        /* Create a connection instance */

        final Connection conn = new Connection(host, port);

        /* Now connect */

        conn.connect();

        /*
         * Authenticate. If you get an IOException saying something like
         * "Authentication method password not supported by the server at this
         * stage." then please check the FAQ.
         */
        
        /*
        * Authenticate. If you get an IOException saying something like
        * "Authentication method password not supported by the server at this
        * stage." then please check the FAQ.
        */

        File home = new File(System.getProperty("user.home"));

        boolean isAuthenticated = conn.authenticateWithPublicKey(username,
               new File(home, ".ssh/id_dsa"), password);

        if (!isAuthenticated) {
            isAuthenticated = conn.authenticateWithKeyboardInteractive(username, new InteractiveCallback() {
    
                public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
                    String[] responses = new String[numPrompts];
                    for (int x=0; x < numPrompts; x++) {
                        responses[x] = password;
                    }
                    return responses;
                }
            });
        }

        if (!isAuthenticated) {
            throw new IOException("Authentication failed.");
        }

        /* Create a session */

        openSession(conn);

    }

    private void openSession(final Connection conn) throws IOException {
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
