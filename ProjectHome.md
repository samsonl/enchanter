
---

This project has moved to Bitbucket at https://bitbucket.org/mrdon/enchanter

---


Enchanter is a small library that helps you script SSH sessions in a manner similar to [Expect](http://expect.nist.gov/).  It comes in multiple flavors that support different scripting languages including [Python](http://www.jython.org), [Ruby](http://jruby.codehaus.org/) and [BeanShell](http://beanshell.org).  This tool requires Java 5 or greater.

Here is an example script, 'date.py', that connects to a remote SSH server and gets the output of the 'date' command:

```
ssh.connect('myserver', 'myusername');
ssh.waitFor(':~>');
ssh.sendLine('date');
print 'Server date is '+ssh.getLine();
ssh.disconnect();
```

To execute this script with the script and enchanter jar in the current directory, run
```
java -jar enchanter-python-VERSION.jar date.py
```

### Features ###
  * Different builds to support Python, Ruby, and BeanShell scripts
  * ['Learning Mode'](LearningMode.md) to automatically build scripts based on observing an interactive SSH session
  * Supports public key (RSA and DSA), password, and password-interactive authentication
  * API similar to the [ZOC](http://www.emtec.com/zoc/) telnet/SSH client

#### Latest release: 0.5.1 ####
  * Fixed missing RSA or DSA key not failing over properly to password-based authentication methods

### Documentation ###

Common commands for the 'ssh' variable include:

|**connect(server, username)**|Connects to the remote server|
|:----------------------------|:----------------------------|
|**disconnect()**|Disconnects from the remote server|
|**waitFor(prompt)**|Waits for the text and returns true if found|
|**waitForMux(promptList)**|Waits for multiple prompts and returns the index of the first match|
|**sleep(millis)**|Sleeps for the given number of milliseconds|
|**setTimeout(millis)**|Sets the timeout for all commands that wait for prompts|
|**send(text)**|Sends the text to the server|
|**sendLine(text)**|Sends the text to the server with the end of line markers|

For a full list of commands, see the [SSH interface](http://enchanter.googlecode.com/svn/tags/0.5.1/core/src/main/java/org/twdata/enchanter/SSH.java)

#### Examples ####

Each scripting language build has its own examples:
  * [Python](http://enchanter.googlecode.com/svn/trunk/jars/python/examples)
  * [Ruby](http://enchanter.googlecode.com/svn/trunk/jars/ruby/examples)
  * [BeanShell](http://enchanter.googlecode.com/svn/trunk/jars/beanshell/examples)

Comments or suggestions?  Any [feedback](mailto:mrdon@twdata.org) is appreciated.