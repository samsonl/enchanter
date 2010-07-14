# retrieve data from mavenSession
print "All the servers in maven settings:"
for server in mavenSession.getSettings().getServers():
    print "server: %s" % server.getId()

# retrieve data from mavenProject
print "Maven project info:"
print "GroupId: %s" % mavenProject.getModel().getGroupId()
print "ArtifactId: %s" % mavenProject.getModel().getArtifactId()
print "Version: %s" % mavenProject.getModel().getVersion()

# connect to server
ssh.connect(target_server, None)
ssh.waitFor(unix_prompt);
ssh.sendLine('date');
print 'Enchanter:server date is '+ssh.getLine();
ssh.disconnect();