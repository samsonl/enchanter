package org.twdata.enchanter.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

public class DefaultSSHPerformanceTest extends TestCase {

    DefaultSSH ssh;
    StubSSHConnection conn;
    byte[] bibleBytes;
    
    public DefaultSSHPerformanceTest(String arg0) throws IOException {
        super(arg0);
        InputStream in = getClass().getResourceAsStream("/kjv10.txt");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int len = 0;
        while ((len = in.read(buffer)) > -1) {
            bout.write(buffer, 0, len);
        }
        bibleBytes = bout.toByteArray();
        
    }

    protected void setUp() throws Exception {
        super.setUp();
        ssh = new DefaultSSH();
        conn = new StubSSHConnection();
        conn.setInputStream(new ByteArrayInputStream(bibleBytes));
        ssh.setSSHConnection(conn);
        ssh.connect("host", "username");
    }
    
    public void testLotsOfWaitFor() throws Exception {
        long start = System.currentTimeMillis();
        int count = 0;
        while (ssh.waitFor("God")) {
            count++;
        }
        long now = System.currentTimeMillis();
        System.out.println("Found "+count+" in "+(now-start)+" ms");
    }

}
