package org.twdata.enchanter.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.twdata.enchanter.StreamListener;

public class StreamReader implements Runnable {

    private BufferedInputStream in;
    private List<StreamListener> streamListeners;
    private boolean alive = true;
    
    public StreamReader(InputStream in, List<StreamListener> listeners) {
        this.in = new BufferedInputStream(in);
        this.streamListeners = listeners;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int len = 0;
        try {
            while (alive && (len = in.read(buffer)) >= 0) {
                for (StreamListener listener : streamListeners) {
                    listener.hasRead(buffer, 0, len);
                }
            }
            for (StreamListener listener : streamListeners) {
                listener.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            // ignore
        }
    }
    
    public void stop() {
        alive = false;
    }
}
