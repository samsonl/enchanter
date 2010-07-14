package org.twdata.enchanter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.python.util.PythonInterpreter;
import org.twdata.enchanter.impl.DefaultStreamConnection;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mojo which executes the enchanter.
 *
 * @aggregator true
 * @goal enchanter
 */
public class EnchanterMojo extends AbstractMojo
{
    private static final String PYTHON_EXTENSION = ".py";

   /**
    * Script file to execute.
    * The location is relative to the src root (where the pom.xml file located).
    *
    * @parameter
    * @required
    */
    private String scriptFile;

   /**
    * The config file which will be used to resolve variables in scriptFile.
    * The location is relative to the src root (where the pom.xml file located).
    *
    * @parameter
    */
    private String configFile;

   /**
    * The Maven Project Object
    *
    * @parameter expression="${project}"
    * @required
    * @readonly
    */
    private MavenProject project;

   /**
    * The Maven Session Object
    *
    * @parameter expression="${session}"
    * @required
    * @readonly
    */
    private MavenSession session;

    /**
     * Execute the Mojo. This runs the supplied scriptFile.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // the scriptFile parameter must be supplied.
        if (scriptFile == null || scriptFile.length() == 0)
        {
            throw new MojoExecutionException("scriptFile parameter is needed for executing the EnchanterMojo");
        }

        // get the absolute path.
        final String absoluteScriptPath = resolveAbsolutePath(scriptFile);

        // ensure that the script file exists.
        final File scriptFp = new File(absoluteScriptPath);
        if (!scriptFp.exists())
        {
            throw new MojoExecutionException(String.format("the specified scriptFile %s doesn't exist.", absoluteScriptPath));
        }

        // ensure that we have the right permission to read it.
        if (!scriptFp.canRead()) {
            throw new MojoExecutionException(String.format("cannot read the scriptFile %s due to permissions", absoluteScriptPath));
        }

        getLog().info("====================Executing enchanter====================");

        // delegate to the right executor.
        if (scriptFile.endsWith(PYTHON_EXTENSION))
        {
            // the map for keeping all variables in the config file.
            Map<String, String> variables;

            if (configFile == null || configFile.length() == 0)
            {
                getLog().info("No config file supplied. No variable resolution occurred in the scriptFile");
                variables  = Collections.emptyMap();
            }
            else
            {
                // load config file
                String absoluteConfigPath = resolveAbsolutePath(configFile);
                File configFp = new File(absoluteConfigPath);

                Properties props = new Properties();
                try
                {
                    props.load(new FileInputStream(configFp));
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(String.format("the specified configFile %s cannot be opened.", absoluteConfigPath));
                }

                // load all the properties to the variables map.
                variables = new HashMap<String, String>((Map)props);
            }

            executeInPython(scriptFp.getAbsolutePath(), variables);
        }
        else
        {
            throw new MojoExecutionException("the mojo currently only supports Python (*.py)!!!");
        }

        // signal the end of execution.
        getLog().info("====================Finished the execution====================");
    }

    /**
     * Resolve the given scriptFile to the absolute path.
     */
    private String resolveAbsolutePath(String filename)
    {
        return filename;
    }

    /**
     * Execute the python script.
     *
     * @param scriptLocation the script file location.
     * @param configs the user configurations.
     */
    private void executeInPython(String scriptLocation, Map<String, String> configs)
    {
        // The script interpreter
        final PythonInterpreter interp = new PythonInterpreter();
        final StreamConnection conn = new DefaultStreamConnection();

        // pass variables to the python interpreter.
        interp.set("ssh", conn);
        interp.set("conn", conn);
        interp.set("mavenProject", project);
        interp.set("mavenSession", session);

        // pass on all the user configs
        for(Map.Entry<String, String> pair:configs.entrySet())
        {
            getLog().info("variable " + pair.getKey()
                            + "(" + pair.getValue().getClass().toString() + ") ="
                            + pair.getValue());
            interp.set(pair.getKey(), pair.getValue());
        }

        // execute the script
        interp.execfile(scriptLocation);
    }
}
