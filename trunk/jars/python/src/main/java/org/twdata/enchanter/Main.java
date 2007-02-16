package org.twdata.enchanter;

import java.io.File;
import java.util.Collections;

import org.python.util.PythonInterpreter;
import org.twdata.enchanter.impl.DefaultSSH;

/**
 * Executes the passed script using Ruby
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar enchanter.jar SCRIPT_PATH");
            System.exit(1);
        }

        String filePath = args[0];

        PythonInterpreter interp = new PythonInterpreter();
        interp.set("ssh", new DefaultSSH());
        interp.execfile(filePath);
        

    }

}
