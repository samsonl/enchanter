package org.twdata.enchanter;

import java.io.IOException;

public interface SSH {
	
	public void connect(String host, int port, String username, String password) throws IOException;
	
	public void setTimeout(int timeout);
	
	public void respond(String prompt, String response);
	
	public void send(String text);
	
	public void sendLine(String text);
	
	public int waitForMux(String... text) throws IOException;
	
	public String lastLine();
	
	public String getLine() throws IOException;
	
	public boolean waitFor(String text) throws IOException;
	
	public void sleep(int millis) throws InterruptedException;
	
	public void disconnect();
	
	public void addStreamListener(StreamListener listener);

	public void setDebug(boolean debug);

	public void connect(String host, String username) throws IOException;
	
}
