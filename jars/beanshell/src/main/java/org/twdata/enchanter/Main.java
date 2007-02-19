package org.twdata.enchanter;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.twdata.enchanter.impl.DefaultSSH;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Executes the passed script using Beanshell
 */
public class Main {

    /**
     * @param args
     * @throws EvalError 
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws BSFException
     */
    public static void main(String[] args) throws EvalError, FileNotFoundException, IOException {
        ScriptRecorder rec = new BeanShellScriptRecorder();
        
        args = rec.processForLearningMode(args);

        String filePath = args[0];

        SSH ssh = new DefaultSSH();
        
        Interpreter i = new Interpreter();
        i.set("ssh", ssh);
        i.set("args", args);
        i.source(filePath);
    }

}
