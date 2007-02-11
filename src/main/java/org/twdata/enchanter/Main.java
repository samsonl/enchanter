package org.twdata.enchanter;

import java.io.FileReader;
import java.io.IOException;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.IOUtils;

/**
 * Executes the passed script using the Bean Scripting Framework
 */
public class Main {

	/**
	 * @param args
	 * @throws BSFException 
	 */
	public static void main(String[] args) throws BSFException {
		if (args.length == 0) {
			System.err.println("Usage: java -jar enchanter.jar SCRIPT_PATH");
			System.exit(1);
		}
		
		String filePath = args[0];
		
		BSFManager bsfManager = new BSFManager();
		SSH ssh = new GanymedSSH();
		bsfManager.declareBean("ssh", ssh, SSH.class);
		
		String fileContents = null;
		FileReader reader = null;
        try {
            reader = new FileReader(filePath);
            fileContents = IOUtils.getStringFromReader(reader);
        } catch (IOException ex) {
        	System.err.println("Unable to load script: "+filePath);
			System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        
        String language = BSFManager.getLangFromFilename(filePath);
        
        bsfManager.exec(language, filePath, 0, 0, fileContents);

	}

}
