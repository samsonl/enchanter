conn.connect("www.twdata.org", "mrdon");
conn.waitFor(":~>");
conn.sendLine("date");
print "Server date is "+conn.getLine();
conn.disconnect();
