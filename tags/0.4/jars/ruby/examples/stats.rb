server = "www.twdata.org"
user = "mrdon"
prompt = ":~>"

$ssh.connect(server, user);
$ssh.setTimeout(20000);
$ssh.waitFor(prompt);
$ssh.sendLine("top");
puts "==== Top ====\n"
puts $ssh.getLine()
puts $ssh.getLine()
puts $ssh.getLine()
puts $ssh.getLine()
$ssh.send("^C")

$ssh.waitFor(prompt)
puts "\n==== Disk Usage ====\n"
$ssh.sendLine("df")
mux = java.lang.reflect.Array.newInstance(java.lang.String, 2);
mux[0] = "\r\n"
mux[1] = prompt
while (true) 
	id = $ssh.waitForMux(mux)
	if id == 0 
		puts $ssh.lastLine()
	else
		retry
	end
end
$ssh.disconnect()
