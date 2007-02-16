ssh.connect("www.twdata.org", "mrdon");
ssh.waitFor(":~>");
ssh.sendLine("date");
print "Server date is "+ssh.getLine();
ssh.disconnect();
