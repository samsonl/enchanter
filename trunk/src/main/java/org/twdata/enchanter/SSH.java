package org.twdata.enchanter;

import java.io.IOException;

/**
 * Represents an SSH server connection implementation
 */
public interface SSH {
	
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
	public void connect(String host, int port, String username, String password) throws IOException;
	
	/**
	 * Sets the timeout for all wait calls
	 * 
	 * @param timeout The timeout in milliseconds
	 */
	public void setTimeout(int timeout);
	
	/**
	 * Sends the response whenever the prompt is encountered.  Only activated
	 * during a waitFor, waitForMux, or getLine call.  If the response is null,
	 * the respond trigger is removed.
	 * 
	 * @param prompt The text to match for the response
	 * @param response The response to send
	 */
	public void respond(String prompt, String response);
	
	/**
	 * Sets the text to the remote server
	 * 
	 * @param text The text to send
	 * @throws IOException
	 */
	public void send(String text) throws IOException;
	
	/**
	 * Sends the text to the remote server followed by an end of line marker
	 * 
	 * @param text The text to send
	 * @throws IOException
	 */
	public void sendLine(String text) throws IOException;
	
	/**
	 * Waits for multiple strings, returning the index of the first match
	 * 
	 * @param text An array of prompts
	 * @return The index of the prompt matched, -1 if the timeout was reached
	 * @throws IOException
	 */
	public int waitForMux(String... text) throws IOException;

	/**
	 * Waits for multiple strings, returning the index of the first match.  Can
	 * optionally read the whole line before returning.
	 * 
	 * @param text An array of prompts
	 * @param readLineOnMatch If true, the whole line containing the first match will
	 * 						  be read and available via {@link #lastLine()}
	 * @return The index of the prompt matched, -1 if the timeout was reached
	 * @throws IOException
	 */
	public int waitForMux(String[] text, boolean readLineOnMatch) throws IOException;
	
	/**
	 * Waits for a prompt and returns if it was matched. 
	 * 
	 * @param text The prompt
	 * @return True if matched, false if the timeout was reached
	 * @throws IOException
	 */
	public boolean waitFor(String text) throws IOException;
	
	/**
	 * Waits for a prompt and returns if it was matched. Can optionally read the whole 
	 * line before returning.
	 * 
	 * @param text The prompt
	 * @param readLineOnMatch If true, the whole line containing the first match will
	 * 						  be read and available via {@link #lastLine()}
	 * @return True if matched, false if the timeout was reached
	 * @throws IOException
	 */
	public boolean waitFor(String text, boolean readLineOnMatch) throws IOException;
	
	/**
	 * Gets the last line matched
	 * 
	 * @return The last line matched
	 */
	public String lastLine();
	
	/**
	 * Gets the next full line ending with the end of line marker
	 * 
	 * @return The next full line
	 * @throws IOException
	 */
	public String getLine() throws IOException;
	
	/**
	 * Sleeps for the specified number of milliseconds
	 * @param millis The sleep time in milliseconds
	 * @throws InterruptedException
	 */
	public void sleep(int millis) throws InterruptedException;
	
	/**
	 * Disconnects from the remote SSH server
	 */
	public void disconnect();

	/**
	 * Adds a stream listener to be notified of each byte read and written.
	 * 
	 * @param listener The StreamListener implementation
	 */
	public void addStreamListener(StreamListener listener);

	/**
	 * Sets whether to be in debugging mode or not.  Debugging mode usually
	 * means all the output will be copied to the console.
	 * 
	 * @param debug True for debugging mode
	 */
	public void setDebug(boolean debug);
}
