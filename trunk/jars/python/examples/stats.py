server = "www.twdata.org"
user = "mrdon"
prompt = ":~>"

ssh.connect(server, user);
ssh.setTimeout(20000);
ssh.waitFor(prompt);
ssh.sendLine("top");
print "==== Top ====\n"
print ssh.getLine()
print ssh.getLine()
print ssh.getLine()
print ssh.getLine()
ssh.send("^C")

ssh.waitFor(prompt)
print "\n==== Disk Usage ====\n"
ssh.sendLine("df")
mux = ["\r\n", prompt]
while 1:
    id = ssh.waitForMux(mux)
    if id == 0 :
        print ssh.lastLine()
    else:
        break
ssh.disconnect()
