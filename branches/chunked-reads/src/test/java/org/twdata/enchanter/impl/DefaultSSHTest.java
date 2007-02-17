package org.twdata.enchanter.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.twdata.enchanter.StreamListener;


import junit.framework.TestCase;

public class DefaultSSHTest extends TestCase {

    DefaultSSH ssh;
    StubSSHLibrary conn;
    
    public DefaultSSHTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        ssh = new DefaultSSH();
        conn = new StubSSHLibrary();
        ssh.setSSHConnection(conn);
        ssh.setTimeout(1000);
        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSend() throws IOException {
        ssh.connect(null, null);
        ssh.send("foo");
        assertEquals("foo", conn.dumpOut());
        ssh.disconnect();
        
        ssh.connect(null, null);
        ssh.send("foo^C");
        assertEquals("foo"+((char)3), conn.dumpOut());
        ssh.disconnect();
        
        ssh.connect(null, null);
        ssh.send("foo^M");
        assertEquals("foo\r\n", conn.dumpOut());
        ssh.disconnect();
    }

    public void testSendLine() throws IOException {
        conn.setInputStream(new ByteArrayInputStream("foo\r\n".getBytes()));
        ssh.connect(null, null);
        ssh.sendLine("foo");
        assertEquals("foo\r\n", conn.dumpOut());
        ssh.disconnect();
    }

    public void testSleep() throws InterruptedException {
        long start = System.currentTimeMillis();
        ssh.sleep(500);
        assertTrue(System.currentTimeMillis() >= start + 500);
    }

    public void testWaitForStringBoolean() throws IOException {
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar\r\njoo".getBytes()));
        ssh.connect(null, null);
        assertTrue(ssh.waitFor("bar", false));
        assertTrue(ssh.waitFor("jo", false));
        assertFalse(ssh.waitFor("asdf", false));
        ssh.disconnect();
        
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar\r\njoo".getBytes()));
        ssh.connect(null, null);
        assertTrue(ssh.waitFor("bar", true));
        assertTrue(ssh.waitFor("jo", true));
        assertFalse(ssh.waitFor("asdf", true));
        ssh.disconnect();
        
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbzrn\r\njoo".getBytes()));
        ssh.connect(null, null);
        assertTrue(ssh.waitFor("bz", false));
        assertTrue(ssh.waitFor("rn", false));
        assertFalse(ssh.waitFor("asdf", false));
        ssh.disconnect();
    }

    public void testWaitForMuxStringArrayBoolean() throws IOException {
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar ds\r\njoo dsf".getBytes()));
        ssh.connect(null, null);
        assertEquals(1, ssh.waitForMux("bsar", "bar"));
        assertEquals(0, ssh.waitForMux("jo", "fdo"));
        assertEquals(-1, ssh.waitForMux("asdf"));
        ssh.disconnect();
        
    }

    public void testLastLine() throws IOException {
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar ds\r\njoo dsf".getBytes()));
        ssh.connect(null, null);
        ssh.waitFor("bar");
        assertEquals("bar", ssh.lastLine());
        ssh.disconnect();
        
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar ds\r\njoo dsf".getBytes()));
        ssh.connect(null, null);
        ssh.waitFor("bar", true);
        assertEquals("bar ds", ssh.lastLine());
        ssh.disconnect();
    }

    public void testGetLine() throws IOException {
        conn.setInputStream(new ByteArrayInputStream("foo\r\nbar ds\r\njoo dsf".getBytes()));
        ssh.connect(null, null);
        assertEquals("foo", ssh.getLine());
        ssh.disconnect();
    }
    
}