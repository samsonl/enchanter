package org.twdata.enchanter;

import java.io.IOException;

import org.python.util.PythonInterpreter;
import org.twdata.enchanter.impl.DefaultStreamConnection;

/**
 * Executes the passed script using Python
 */
public class Main {

    public static void main(String[] args) throws IOException {
        ScriptRecorder rec = new PythonScriptRecorder();
        
        args = rec.processForLearningMode(args);

        String filePath = args[0];

        PythonInterpreter interp = new PythonInterpreter();
        StreamConnection conn = new DefaultStreamConnection();
        
        // deprecated
        interp.set("ssh", conn);
        interp.set("conn", conn);
        interp.set("args", args);
        interp.execfile(filePath);
        

    }

}
