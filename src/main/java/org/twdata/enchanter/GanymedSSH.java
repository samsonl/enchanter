package org.twdata.enchanter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

/**
 * An implementation of an ssh library using Ganymed
 */
public class GanymedSSH implements SSH {

	private Session sess;
	private PrintWriter out;
	private InputStreamDumper dumper;
	private Thread dumperThread;
	private List<StreamListener> streamListeners = new ArrayList<StreamListener>();
	
	int timeout = 0;
	
	public void setDebug(boolean debug) {
		if (debug) {
			addStreamListener(new StreamListener() {
				public void hasRead(byte b) {
					if (b != '\r')
						System.out.print((char)b);
				}
				public void hasWritten(byte[] b) {
					// Not usually necessary
					// System.out.print(new String(b));
				}
			});
		}
	}

	public void addStreamListener(StreamListener listener) {
		streamListeners.add(listener);
	}

	public void connect(String host, String username) throws IOException {
		connect(host, 22, username, "");
	}
	
	public void connect(String host, int port, String username, final String password) throws IOException {
		/* Create a connection instance */

		final Connection conn = new Connection(host);

		/* Now connect */

		conn.connect();

		/* Authenticate.
		 * If you get an IOException saying something like
		 * "Authentication method password not supported by the server at this stage."
		 * then please check the FAQ.
		 */

		File home = new File(System.getProperty("user.home"));
		
		boolean isAuthenticated = conn.authenticateWithPublicKey(username, new File(home, ".ssh/id_dsa"), password);

		//if (isAuthenticated == false)
		//w
		//throw new IOException("Authentication failed.");

		/* Create a session */

		sess = conn.openSession();
		
		sess.requestDumbPTY();
		
		out = new PrintWriter(sess.getStdin());
		dumper = new InputStreamDumper(sess.getStdout());
		sess.startShell();
		
	}

	public void send(String text) throws IOException {
		print(text, false);

	}

	public void sendLine(String text) throws IOException {
		print(text, true);
	}
	
	private void print(String text, boolean eol) throws IOException {
		text = text.replace("^C", String.valueOf((char) 3));
		text = text.replace("^M", "\r\n");
		if (eol) {
			out.println(text);
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

	public void sleep(int millis) throws InterruptedException {
		Thread.currentThread().sleep(millis);
	}

	public boolean waitFor(String text) throws IOException {
		return dumper.waitFor(text, false);
	}

	public int waitForMux(String... text) throws IOException {
		return dumper.waitForMux(text, false);
	}
	
	public boolean waitFor(String text, boolean readLineOnMatch) throws IOException {
		return dumper.waitFor(text, readLineOnMatch);
	}

	public int waitForMux(String[] text, boolean readLineOnMatch) throws IOException {
		return dumper.waitForMux(text, readLineOnMatch);
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void respond(String prompt, String response) {
		dumper.respond(prompt, response);
	}
	
	public String lastLine() {
		return dumper.getLastLine();
	}
	
	public String getLine() throws IOException {
		if (dumper.waitFor("\r\n", false)) {
			return dumper.getLastLine();
		}
		return null;
	}
	
	public class InputStreamDumper {
		private BufferedInputStream in;
		
		Map<String, Response> respondWith = new HashMap<String, Response>();
		List<Prompt> waitFor = new ArrayList<Prompt>();
		
		char lastChar;
		boolean alive = true;
		StringBuilder lastLine = new StringBuilder();
		Thread timeoutThread;
		
		public InputStreamDumper(InputStream in) {
			this.in = new BufferedInputStream(in, 2048);
		}
		
		public synchronized void respond(String prompt, String response) {
			if (response == null) {
				respondWith.remove(prompt);
			} else {
				respondWith.put(prompt, new Response(prompt, response));
			}
		}
		
		public synchronized boolean waitFor(String waitFor, boolean readLineOnMatch) throws IOException {
			prepare(new String[]{waitFor});
			return (readFromStream(readLineOnMatch) == 0);
		}
		
		public synchronized int waitForMux(String[] waitFor, boolean readLineOnMatch) throws IOException {
			prepare(waitFor);
			return readFromStream(readLineOnMatch);
		}
		
		protected void prepare(String[] text) {
			this.alive = true;
			for (String val : text) {
				waitFor.add(new Prompt(val));
			}
			this.lastLine.setLength(0);
		}
		
		public synchronized String getLastLine() {
			return this.lastLine.toString();
		}
		
		public void stop() {
			if (timeoutThread != null) {
				timeoutThread.interrupt();
			}
			alive = false;
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
						} catch(InterruptedException e) {
							return;
						}
						alive = false;
					}
				};
				timeoutThread.start();
			}
			while (alive && (data = in.read()) >= 0) {
				for (StreamListener listener : streamListeners) {
					listener.hasRead((byte)data);
				}
				char c = (char)data;
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
					lookForResponse((char)data);
					lastChar = (char)data;
				}
			}
			reset();
			return result;
		}
		
		synchronized int lookForMatch(char s) {
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
		
		synchronized void lookForResponse(char s) throws IOException {
			for (Response response : respondWith.values()) {
				if (response.matchChar(s)) {
					if (response.match()) {
						send(response.getResponse());
						response.resetPos();
					} else {
						response.nextPos();
					}
				} else {
					response.resetPos();
				}
			}
		}
		
		synchronized void reset() {
			waitFor.clear();
			if (timeout > 0) {
				timeoutThread.interrupt();
			}
			alive = true;
		}

	}

	public void disconnect() {
		dumper.stop();
		sess.close();
		
		sess = null;
		dumper = null;
		dumperThread = null;
		out = null;
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
