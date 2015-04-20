# Introduction #

Each executable jar supports a "learning mode" that observes an interactive SSH session and records prompts and responses.  This makes it easy to create simple scripts or generate the script skeleton for more complex ones.  The generated script is written in the favored scripting language of the jar.  For example, if the jar is `enchanter-python-VERSION.jar`, the script will be written in Python.

# Details #

Learning mode can be invoked from the command line by passing special arguments:

| **Short** | **Long** | **Required** | **Description** |
|:----------|:---------|:-------------|:----------------|
| l | learn | yes | Enables learning mode |
| h | host | no | The server host |
| p | port | no | The server port |
| u | username | no | The user name on the server |
| P | password | no | The password for the server account |
| _N/A_ | prompt-size | no | The number of characters to capture for prompts |

In learning mode, the script name, passed as the last argument, is interpreted as the name of the file to write the generated script.

The learning mode works by keeping track of the last 10 (configurable with the `prompt-size` argument) characters throughout the session.  When the user types characters that end in a newline, the last 10 characters (or as many as occurred after the last newline) are interpreted as the prompt and the user's characters as the response.  This means control codes like ^C won't be captured, or basically any input that doesn't end in a new line.

# Examples #

This connects to `myserver.com` as `myuser` and generates a script named `myscript.py`
```
java -jar enchanter-python-VERSION.jar -l -h myserver.com -u myuser myscript.py  
```