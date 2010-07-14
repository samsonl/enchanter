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
    private static final String CLASSPATH_RESOURCE_PREFIX = "classpath:";

   /**
    * Script file to execute.
    * The location is relative to the src root (where the pom.xml file located).
    *
    * @parameter
    * @required
    */
    protected String scriptFile;

   /**
    * The config file which will be used to resolve variables in scriptFile.
    * The location is relative to the src root (where the pom.xml file located).
    *
    * @parameter
    */
    protected String configFile;

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

    public MavenProject getProject() {
        return project;
    }

    public MavenSession getSession() {
        return session;
    }

    /**
     * Extension point for hardcoding scriptFile for specific use.
     */
    protected String getScriptFile()
    {
        return scriptFile;
    }

    /**
     * Extension point for hardcoding configFile for specific use.
     */
    protected String getConfigFile()
    {
        return configFile;
    }

    /**
     * Read all the content from the reader and return as a single string.
     */
    private String slurp(final BufferedReader reader) throws MojoExecutionException {

        // this will store the content
        final StringBuilder content = new StringBuilder();
        String line;

        try
        {
            while((line=reader.readLine()) != null)
            {
                content.append(line);
                content.append(System.getProperty("line.separator"));
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error reading file", e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error closing file", e);
            }
        }

        return content.toString();
    }

    /**
     * Get reader from the given path which could be 1)resource in classpath 2) actual file on file system.
     * The caller is responsible for closing the returned reader.
     */
    private BufferedReader getReader(String path) throws MojoExecutionException {

        // if the path is a classpath resource
        if (path.startsWith(CLASSPATH_RESOURCE_PREFIX))
        {
            final String resourcePath = path.replaceFirst(CLASSPATH_RESOURCE_PREFIX, "");
            getLog().info("reading resource from classpath:" + resourcePath);
            return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath)));
        }

        // the path is for an actual file on filesystem.
        else
        {
            // then we read from the actual file
            try
            {
                getLog().info("reading resource from filesystem:" + path);
                return new BufferedReader(new FileReader(path));
            }
            catch (FileNotFoundException e)
            {
                // bad things can happen such as the file doesn't exist or no permission to read.
                throw new MojoExecutionException("Error opening file", e);
            }
        }
    }

    /**
     * Execute the Mojo. This runs the supplied scriptFile.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("====================Executing====================");

        // the path is null or empty.
        if (getScriptFile() == null || getScriptFile().length() == 0)
        {
            throw new MojoExecutionException("missing the required scriptFile parameter");
        }

        // read the content of the scriptFile.
        final String scriptContent = slurp(getReader(getScriptFile()));

        // delegate to the right executor.
        if (getScriptFile().endsWith(PYTHON_EXTENSION))
        {
            // the map for keeping all variables in the config file.
            final Map<String, String> variables;

            if (getConfigFile() == null || getConfigFile().length() == 0)
            {
                getLog().info("No config file supplied. No variable resolution occurred in the scriptFile");
                variables  = Collections.emptyMap();
            }
            else
            {
                // load the content of properties file.
                final Properties props = new Properties();
                Reader configReader = null;
                try
                {
                    configReader = getReader(getConfigFile());
                    // this load operation doesn't close the reader when done
                    props.load(configReader);
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(String.format("Error while reading the configFile:%s", getConfigFile()), e);
                }
                finally
                {
                    if (configReader != null)
                    {
                        try
                        {
                            configReader.close();
                        }
                        catch (IOException e)
                        {
                            throw new MojoExecutionException("Error while finishing the reading of the configFile", e);
                        }
                    }
                }

                // load all the properties to the variables map.
                variables = new HashMap<String, String>((Map)props);
            }

            executeInPython(scriptContent, variables);
        }
        else
        {
            throw new MojoExecutionException("the mojo currently only supports Python (*.py)!!!");
        }

        // signal the end of execution.
        getLog().info("====================Finished the execution====================");
    }

    /**
     * Execute the python script.
     *
     * @param scriptContent the script to be executed (content, not file location).
     * @param configs the user configurations.
     */
    private void executeInPython(String scriptContent, Map<String, String> configs)
    {
        // The script interpreter
        final PythonInterpreter interp = new PythonInterpreter();
        final StreamConnection conn = new DefaultStreamConnection();

        // info for users
        if (getProject() != null)
        {
            getLog().info("variable mavenProject available");
        }
        if (getSession() != null)
        {
            getLog().info("variable mavenSession available");
        }

        // pass variables to the python interpreter.
        interp.set("ssh", conn);
        interp.set("conn", conn);
        interp.set("mavenProject", getProject());
        interp.set("mavenSession", getSession());

        // pass on all the user configs
        for(Map.Entry<String, String> pair:configs.entrySet())
        {
            // print out the parameter being passed
            getLog().info("variable " + pair.getKey()
                            + "(" + pair.getValue().getClass().toString() + ") ="
                            + pair.getValue());
            // make the parameter known by the interpreter
            interp.set(pair.getKey(), pair.getValue());
        }

        // execute the script
        interp.exec(scriptContent);
    }
}
