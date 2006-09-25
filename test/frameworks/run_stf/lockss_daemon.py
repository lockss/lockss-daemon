import base64, glob, os, httplib, random, re, sha, shutil, signal, socket
import mimetools, mimetypes, sys, time, types, urllib, urllib2, urlparse
from os import path
from xml.dom import minidom
from lockss_util import *

# Constants

DEF_TIMEOUT = 60 * 30  # 30 minute default timeout for waits.
DEF_SLEEP = 10         # 10 second default sleep between loops.
FILE_TYPE_TEXT = 1
FILE_TYPE_HTML = 2
FILE_TYPE_PDF = 4
FILE_TYPE_JPEG = 8
FILE_TYPE_BIN = 16

# Statics
frameworkCount = 0

log = Logger()

# Classes
class Framework:
    """
    A framework is a set of LOCKSS daemons and associated test
    clients.  The framework daemons can be started or stopped as a
    group, and their status can be accessed by the clients the
    framework creates and returns at initialization time.

    To use a framework, it should be created by passing it two paths:
    A path to a LOCKSS build environment (where lockss.jar and
    lockss-dev.jar can be found), and a work directory in which the
    daemons will be run.  Optionally, host name and port numbers for
    each daemon can be passed in.  If not supplied, these default to
    'localhost' and ports 8041 through 8041+n, where 'n' is the number
    of daemons that have been created.

    When the framework is created, it returns a tuple of associated
    clients, one per daemon.  These clients are used to perform
    functional test interactions with the daemons.
    """
    def __init__(self, daemonCount = None, urlList = None, startPort = None):
        self.workDir = path.abspath(config.get('workDir', './'))
        self.frameworkDir = self.__makeTestDir()
        self.projectDir = config.get('projectDir')
        if self.projectDir == None:
            # will raise LockssError if not found.
            self.projectDir = self.__findProjectDir()
        self.localLibDir = path.join(self.workDir, 'lib')
        self.projectLibDir = path.join(self.projectDir, 'lib')
        if not daemonCount:
            self.daemonCount = int(config.get('daemonCount', 4))
        else:
            self.daemonCount = daemonCount
        if not startPort:
            self.startPort = int(config.get('startPort', 8041))
        else:
            self.startPort = startPort
        self.username = config.get('username', 'testuser')
        self.password = config.get('password', 'testpass')
        self.logLevel = config.get('daemonLogLevel', 'debug3')
        self.hostname = config.get('hostname', 'localhost')

        self.clientList = [] # ordered list of clients.
        self.daemonList = [] # ordered list of daemons.
        self.configCount = 0 # used when writing daemon properties

        self.isRunning = False

        # Assert that the project directory exists and that
        # the necessary libraries exist.
        if not self.__isProjectDirReady():
            raise LockssError("Project dir %s is not ready." % self.projectDir)

        # write the framework global config file.
        globalConfigFile = path.join(self.frameworkDir, 'lockss.txt')
        self.__writeLockssConfig(globalConfigFile, self.logLevel)

        # write the 'extra' config file.  This may be empty if there
        # are no LOCKSS daemon properties defined, but that's OK.
        extraConfigFile = path.join(self.frameworkDir, 'lockss.opt')
        self.__writeExtraConfig(extraConfigFile, config)

        # Copy the LOCKSS libs to a local working dir
        self.__setUpLibDir()

        # Set up a each daemon and create a work directory for it.
        for port in range(self.startPort, self.startPort + self.daemonCount):
            daemonDir = path.abspath(path.join(self.frameworkDir,
                                               'daemon-' + str(port)))
            # local config
            localConfigFile = path.join(daemonDir, 'local.txt')
            # Init the directory
            os.mkdir(daemonDir)
            # write the daemon-specific config file
            self.__writeLocalConfig(localConfigFile, daemonDir, port)
            # Create daemon
            if not urlList:
                daemonConfUrls = (globalConfigFile, localConfigFile, extraConfigFile)
            else:
                daemonConfUrls = urlList + (localConfigFile,)
            daemon = LockssDaemon(daemonDir, self.__makeClasspath(),
                                  daemonConfUrls)

            # create client for this daemon
            client = Client(daemon, self.hostname, port, self.username, self.password)

            self.clientList.append(client)
            self.daemonList.append(daemon)

    def start(self):
        " Start each daemon in the framework. "
        # if 'client' is none, start all daemons.
        for daemon in self.daemonList:
            daemon.start()

        self.isRunning = True

    def stop(self):
        " Stop each daemon in the framework."
        for daemon in self.daemonList:
            daemon.stop()

        self.isRunning = False

    def clean(self):
        " Delete the current framework working directory. "
        shutil.rmtree(self.frameworkDir)

    def appendLocalConfig(self, conf, client):
        """ Append the supplied configuration in the dictionary 'conf'
        (name / value pairs) to the local config file for the specified
        client daemon 'client' """
        localConf = path.join(client.daemonDir, 'local.txt')
        f = open(localConf, "a")
        for (key, value) in conf.items():
            f.write("%s=%s\n" % (key, value))
        f.close()

    def checkForDeadlock(self):
        """ Request that all the daemons in the framework dump their
        threads, and then look for deadlock messages.  Returns a list of
        log files to check, or an empty list if no deadlocks are found."""
        deadlockList = []
        for daemon in self.daemonList:
            logfile = daemon.logfile
            daemon.requestThreadDump()
            if self.__grepDeadlock(logfile):
                deadlockList.append(logfile)
        return deadlockList

    def __grepDeadlock(self, f):
        """ Very naive implementation. """
        return (open(f, 'r').read()).find('FOUND A JAVA LEVEL DEADLOCK') > -1

    def __setUpLibDir(self):
        """ Create the directory 'lib' if it does not already exist, then copy
        lockss.jar, lockss-test.jar, and lockss-plugin.jar from self.projectDir/lib
        to 'lib'. """
        if not path.isdir(self.localLibDir):
            os.mkdir(self.localLibDir)

        lockssJar = path.join(self.projectLibDir, 'lockss.jar')
        lockssTestJar = path.join(self.projectLibDir, 'lockss-test.jar')
        lockssPluginJar = path.join(self.projectLibDir, 'lockss-plugins.jar')

        # Copy from buildLib to localLib

        shutil.copy(lockssJar, self.localLibDir)
        shutil.copy(lockssTestJar, self.localLibDir)
        shutil.copy(lockssPluginJar, self.localLibDir)

    def __makeClasspath(self):
        """ Return a list of all *.jar and *.zip files under self.projectDir/lib,
        plus all *.jar and *.zip files under self.localLibDir. """

        fd = open(path.join(self.projectDir, 'test', 'test-classpath'), 'r')
        line = fd.readline()
        fd.close()

        return (":".join(glob.glob(path.join(self.localLibDir, "*.jar"))) +
                 ":" + line)

    def makeClasspath(self):
        return self.__makeClasspath()

    def __encPasswd(self, passwd):
        """ Return a SHA1 encoded version of the specified password """
        digest = sha.new()
        digest.update(passwd)
        return digest.hexdigest()

    def __findProjectDir(self):
        """ Walk up the tree until 'build.xml' is found.  Assume this
        is the root of the project. """
        ## Shortcut for cwd.
        cwd = os.getcwd()
        if (path.isfile(path.join(cwd, 'build.xml'))):
            return cwd

        path_elements = os.getcwd().split(os.sep)
        while len(path_elements) > 1:
            p = os.sep.join(path_elements)
            if path.isfile(path.join(p, 'build.xml')):
                return p
            else:
                path_elements = path_elements[:len(path_elements) - 1]

        # Wasn't found, raise LockssError
        raise LockssError("No LOCKSS project directory found.")

    def __isProjectDirReady(self):
        if not path.isdir(self.projectDir):
            return False
        if not (path.isfile(path.join(self.projectDir, 'lib', 'lockss.jar')) and
                path.isfile(path.join(self.projectDir, 'lib', 'lockss-test.jar')) and
                path.isfile(path.join(self.projectDir, 'lib', 'lockss-plugins.jar'))):
            return False

        return True

    def __makeTestDir(self):
        """ Construct the name of a directory under the top-level work
        directory in which to create the framework.  This allows each
        functional test to work in its own directory, and for all the test
        results to be left for examination if deleteAfter is false. """
        global frameworkCount
        frameworkCount += 1
        fwDir = path.join(self.workDir, 'testcase-' + str(frameworkCount))
        if path.isdir(fwDir):
            log.info("Caution:  Old directory exists.  Renaming to %s " % (fwDir + "-" + time.time().__str__()))
            os.rename(fwDir, (fwDir + "-" + time.time().__str__()))
        os.mkdir(fwDir)
        return fwDir

    def __writeLockssConfig(self, file, logLevel):
        f = open(file, 'w')
        # defined at the end of this file
        f.write(globalConfigTemplate % {"logLevel": logLevel})
        f.close()

    def __writeLocalConfig(self, file, dir, uiPort):
        """ Write local config file for a daemon.  Daemon num is
        assumed to start at 0 and go up to n, for n-1 daemons """
        baseUnicastPort = 9000
        baseIcpPort = 3031
        f = open(file, 'w')
        tr = str(self.configCount + 1)
        # Kluge for proper handling of multiple daemons.  If this is
        # the first dameon, enable the proxy.
        if self.configCount == 0:
            proxyStart = 'yes'
        else:
            proxyStart = 'no'
        ipAddr = tr + '.' + tr + '.' + tr + '.' + tr
        unicastPort = str(baseUnicastPort + self.configCount)
        icpPort = str(baseIcpPort + self.configCount)
        unicastSendToPort = str(baseUnicastPort)
        f.write(localConfigTemplate % {"username": self.username,
                                       "password": "SHA1:%s" % self.__encPasswd(self.password),
                                       "dir": dir, "proxyStart": proxyStart,
                                       "uiPort": uiPort, "ipAddr": ipAddr,
                                       "unicastPort": unicastPort,
                                       "unicastSendToPort": unicastSendToPort,
                                       "icpPort": icpPort})
        f.close()
        self.configCount += 1 # no ++ in python, oops.

    def __writeExtraConfig(self, file, conf):
        """ Write out any LOCKSS daemon properties. """
        f = open(file, 'w')
        for (key, val) in conf.daemonItems():
            f.write('%s=%s\n' % (key, val))
        f.close()

class Client:
    " Client interface to a test framework. "
    def __init__(self, daemon, hostname, port, username, password):
        self.hostname = hostname
        self.port = port
        self.url = 'http://' + self.hostname + ':' + str(port) + '/'
        self.daemon = daemon
        self.daemonDir = daemon.daemonDir
        self.username = username
        self.password = password

    def createAu(self, au):
        """ Create a simulated AU.  This will block until the AU appears in
        the daemon's status table. """
        post = self.__makeAuPost(au, 'Create')
        post.execute()
        if not self.waitForCreateAu(au):
            raise LockssError("Timed out while waiting for AU %s to appear." % au)

    def deleteAu(self, au):
        """ Delete a simulated AU.  This will block until the AU no longer
        appears in the daemon's status table. """
        post = self.__makeAuPost(au, 'Confirm Delete')
        post.execute()
        if not self.waitForDeleteAu(au):
            raise LockssError("Timed out while waiting for "\
                              "AU %s to be deleted." % au)

    def setPublisherDown(self, au):
        """ Modify the configuration of the specified AU to set it's 'pub_down'
        parameter """
        post = self.__makeAuPost(au, 'Update')
        post.add('lfp.pub_down', 'true') # Override setting in AU
        post.execute()
        if not self.waitForPublisherDown(au):
            raise LockssError("Timed out waiting for AU %s to be marked "\
                              "'Publisher Down'." % au)

    def requestTreeWalk(self, au):
        """
        Possibly a poorly named method.  This will merely deactivate
        and then reactivate the specified AU, which may or may not
        trigger a tree walk.  Worst case, it actually pushes the
        schedule back and the tree walk occurs later than it would
        have.  Best case, a tree walk happens in about 10 seconds.
        """
        self.deactivateAu(au, True)
        self.reactivateAu(au, True)

    def reactivateAu(self, au, doWait=True):
        """
        Re-activate a simulated AU.  If doWait is set, wait for the AU
        to disappear from the daemon status table before returning.
        """

        if self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'DoReactivate')
        post.execute()
        if doWait:
            if not self.waitForReactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be reactivated." % au)

    def deactivateAu(self, au, doWait=True):
        """
        Deactivate a simulated AU.  If doWait is set, wait for the AU
        to disappear from the daemon status table before returning.
        """
        if not self.isActiveAu(au):
            return

        post = self.__makeAuPost(au, 'Confirm Deactivate')
        post.execute()

        if doWait:
            if not self.waitForDeactivateAu(au):
                raise LockssError("Timed out while waiting for "\
                                  "AU %s to be deactivated." % au)

    ##
    ## Back up the configuration
    ##

    def backupConfiguration(self):
        """ Very quick and dirty way to download the config backup. """

        request = Get(self.url + "BatchAuConfig?lockssAction=Backup",
                      self.username, self.password)
        backupData = request.execute().read()

        # Write the data into a file
        f = file("configbackup.zip", "w")
        f.write(backupData)
        f.close()


    def restoreConfiguration(self, au):

        post = MultipartPost(self.url + "BatchAuConfig",
                             self.username, self.password)
        post.add("lockssAction", "SelectRestoreTitles")
        post.add("Verb", "5")
        post.addFile("AuConfigBackupContents", "configbackup.zip")

        (result, cookie) = post.execute()

        log.debug3("Got result from Batch AU Config servlet\n%s" % result)

        # Expect to see the strings 'Simulated Content: foo' and
        # 'Restore Selected AUs' in the response.  FRAGILE, obviously
        # this will break if the servlet UI is changed.

        p = re.compile("%s.*Restore Selected AUs" % au.title, re.MULTILINE | re.DOTALL)

        if not p.search(result):
            raise LockssError("Unexpected response from BatchAuConfig servlet")

        # Now confirm the restoration.

        post = Post(self.url + "BatchAuConfig",
                    self.username, self.password, cookie)

        post.add("lockssAction", "DoAddAus")
        post.add("Verb", "5")
        post.add("auid", au.auId)

        result = post.execute()

        # If this was successful, delete the configbackup.zip file
        os.unlink("configbackup.zip")

    ##
    ## General status accessors
    ##

    def getCrawlStatus(self, au=None):
        """
        Return the current crawl status of this cache.
        """
        key = None
        if not au == None:
            key = au.auId

        (summary, table) = self.__getStatusTable('crawl_status_table', key)
        return table

    def getAuRepository(self, au):
        """ RepositoryStatus table does not accept key, so loop through it until
        the corresponding au is found. """
        auid = au.auId
        (summary, table) = self.__getStatusTable('RepositoryTable')
        for row in table:
            auRef = row['au']
            if auRef['key'] == auid:
                d = row['dir']
                repo = path.abspath(path.join(self.daemonDir, d))
                return repo

        ## Wasn't found.
        return None

    def getAuRoot(self, au):
        """ Return the full path to the AU's root.  This is used by methods
        that damage, create, or delete repository files behind the daemon's
        back.  """
        repository = self.getAuRepository(au)
        return path.join(repository, au.dirStruct)

    def hasAu(self, au):
        """ Return true iff the status table lists the given au. """
        auid = au.auId
        (summary, tab) = self.__getStatusTable('AuIds')
        for row in tab:
            if row["AuId"] == auid:
                return True
        # au wasn't found
        return False

    def isActiveAu(self, au):
        """ Return true iff the au is activated."""
        (summary, tab) = self.__getStatusTable('RepositoryTable')
        for row in tab:
            status = row["status"]
            auRef = row["au"]
            # Repository status table may report with two types,
            # a <reference> for 'au', or a string for 'au'.  We want to
            # ignore the string versions.
            if isinstance(auRef, types.DictType):
                if au.auId == auRef['key']:
                    return (status == "Active")
            else:
                continue

        # au wasn't found
        return False

    def isAuDamaged(self, au, node=None):
        (summary, table) = self.__getStatusTable('ArchivalUnitTable', au.auId)
        if node:
            url = node.url
        else:
            url = 'lockssau'
        for row in table:
            rowUrl = row['NodeName']
            if not row.has_key('NodeStatus'):
                continue
            if url == rowUrl:
                return row['NodeStatus'] == "Damaged"
        # au wasn't found.
        return False

    def isPublisherDown(self, au):
        """ Return true if the AU is marked 'publisher down' (i.e., if
        the ArchivalUnitTable lists 'Available From Publisher' as 'No'
        for this AU """
        (summary, table) = self.__getStatusTable('ArchivalUnitTable', au.auId)
        return (summary.has_key('Available From Publisher') and
                summary['Available From Publisher'] == "No")


    def isAuOK(self, au):
        """ Return true if the top level of the AU has been repaired. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if (row.has_key('Range') or not row['PollType'] == 'C'):
                continue
            if (row['URL'] == 'lockssau:'):
                return row['Status'] == 'Won'
        # Poll wasn't found.
        return False

    def isContentRepaired(self, au, node):
        """ Return true if the AU has been repaired by a SNCUSS poll """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row.has_key('Range'):
                continue
            if not (row['PollType'] == 'C' and row['Range'] == 'single node'):
                continue
            if row['URL'] == node.url:
                return row['Status'] == 'Repaired'
        # Poll wasn't found
        return False

    def isCompleteV3Repaired(self, au):
        """ Return true if the given AU has had all its nodes repaired by V3.
            Used in testing complete loss recovery via V3 """
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title and row['status'] == "Complete":
                # Found the right entry
                pollKey = row['pollId']['key']
                (summary, table) = self.getV3PollerDetail(pollKey)
                allUrls = int(summary['Total URLs In Vote'])
                agreeUrls = int(summary['Agreeing URLs'])
                repairs = int(summary['Completed Repairs']['value'])
                return ((repairs == allUrls) and (agreeUrls == allUrls))
        return False
                
    def hasWonV3Poll(self, au):
        """ Return true if a poll has been called, and no repairs have been made. """
        return self.isV3Repaired(au, [])
    
    def isV3Repaired(self, au, nodeList=[]):
        """ Return true if the given content node has been repaired via V3 """
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title and row['status'] == "Complete":
                # Found the right entry
                pollKey = row['pollId']['key']
                (summary, table) = self.getV3PollerDetail(pollKey)
                allUrls = int(summary['Total URLs In Vote'])
                agreeUrls = int(summary['Agreeing URLs']['value'])
                repairs = int(summary['Completed Repairs']['value'])
                return (len(nodeList) == repairs and (agreeUrls == allUrls))
                # TODO: This will really need to be improved when the status
                # tables are better!  Need a way to determine whether this particular NODE was
                # repaired.
        return False
    
    def isNodeRepairedFromPeerByV3(self, au, node):
        """ Return true if the given content node has been repaired 
        from a V3 peer """
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title and row['status'] == "Complete":
                # Found the right entry.
                pollKey = row['pollId']['key']
                (summary,repairTable) = self.getV3CompletedRepairsTable(pollKey)
                for repairRow in repairTable:
                    if repairRow['url'] == node.url: 
                        return re.match('^TCP:\[.*\]:.*', repairRow['repairFrom'])
        # Didn't find it.
        return False

    def isNodeRepairedFromPublisherByV3(self, au, node):
        """ Return true if the given content node has been repaired
        from the publisher """
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title and row['status'] == "Complete":
                # Found the right entry.
                pollKey = row['pollId']['key']
                log.debug("Found the right row in the V3 Pollers table.  Key: %s" % pollKey)
                (summary,repairTable) = self.getV3CompletedRepairsTable(pollKey)
                log.debug("Got a repairTable with %d rows." % len(repairTable))
                for repairRow in repairTable:
                    log.debug("repairRow['url'] == %s ; repairRow['repairFrom'] == %s" % (repairRow['url'], repairRow['repairFrom']))
                    if repairRow['url'] == node.url: 
                        return repairRow['repairFrom'] == 'Publisher'
        # Didn't find it.
        return False

    def isV3NoQuorum(self, au):
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title:
                return row['status'] == "No Quorum"
        return False

    def isContentRepairedFromCache(self, au, node=None):
        """ Return true if the given content node has been repaired from
        another cache via the proxy. """
        if node:
            url = node.url
        else:
            url = au.startUrl
        tab = self.getCrawlStatus(au)
        for row in tab:
            if not row["start_urls"] == url or not row['crawl_type'] == "Repair":
                continue
            return not row['sources'] == "Publisher"
        return False

    def isContentRepairedFromPublisher(self, au, node):
        """ Return true iff the given content node has been repaired by
        crawling the publisher. """
        tab = self.getCrawlStatus(au)
        for row in tab:
            if not row["start_urls"] == node.url or not row['crawl_type'] == "Repair":
                continue
            return not row['sources'] == "Publisher"
        return False

    def isNameRepaired(self, au, node=None):
        """ Return true if the AU has been repaired by non-ranged name poll """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            rowUrl = row['URL']
            rowStatus = row['Status']
            if not node:
                if rowUrl == au.baseUrl:
                    return rowStatus == 'Repaired'
            else:
                if rowUrl == node.url:
                    return rowStatus == 'Repaired'

        # Poll wasn't found
        return False

    def isRangedNameRepaired(self, au, node=None):
        """ Return true if the AU has been repaired by a ranged name poll """
        tab = self.getAuV1Polls(au)
        for row in tab:
            rowPollType = row['PollType']
            # some rows do not have a range.
            if row.has_key('Range') or not rowPollType == 'N':
                continue
            rowUrl = row['URL']
            rowStatus = row['Status']
            if not node:
                if rowUrl == au.baseUrl:
                    return rowStatus == 'Repaired'
            else:
                if rowUrl == node.url:
                    return rowStatus == 'Repaired'

        # Poll wasn't found
        return False

    def getAuV1Polls(self, au, activeOnly=False):
        """ Return the full poll status of an AU """
        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
        if activeOnly:
            key = key + '&Status~Active'
        (summary, table) = self.__getStatusTable('PollManagerTable', key)
        return table

    def getAuV3Pollers(self, au):
# Mea culpa:  I broke accessing V3 Pollers by key when I was enabling
# status references from the AU table to the V3 Poller table.  It should
# be OK just to comment this out permanently. [sethm]
#        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
#        (summary, table) = self.__getStatusTable('V3PollerTable', key)
        (summary, table) = self.__getStatusTable('V3PollerTable')
        return table

    def getAuV3Voters(self, au):
        key = 'AU~%s' % urllib.quote(au.auId) # requires a pre-quoted key
        (summary, table) = self.__getStatusTable('V3VoterTable', key)
        return table

    def getV3PollerDetail(self, key):
        """ Returns both the summary and table """
        return self.__getStatusTable('V3PollerDetailTable', key)
    
    def getV3CompletedRepairsTable(self, key):
        """ Returns the V3 completed repairs status table """
        return self.__getStatusTable('V3CompletedRepairsTable', key)

    def getAuNodesWithContent(self, au):
        """ Return a list of all nodes that have content. """
        # Hack!  Pass the table a very large number for numrows, and
        # pray we don't have more than that.
        (summary, tab) = self.__getStatusTable('ArchivalUnitTable',
                                               key=au.auId,
                                               numrows=100000)
        nodes = []
        for row in tab:
            url = row['NodeName']
            if row.has_key('NodeContentSize') \
                   and not row['NodeContentSize'] == '-':
                nodes.append(self.getAuNode(au, url))
        return nodes

    def getAuNodesWithChildren(self, au):
        """ Return a list of all nodes that have children. """
        (summary, tab) = self.__getStatusTable('ArchivalUnitTable',
                                               key=au.auId,
                                               numrows=100000)
        nodes = []
        for row in tab:
            url = row['NodeName']
            if row.has_key('NodeChildCount') \
                   and not row['NodeChildCount'] == '-':
                nodes.append(self.getAuNode(au, url))
        return nodes

    def isDaemonReady(self):
        """ Simply try to get the AU status.  If it responds, return
        true.  If it raises any kind of error, return False.  Not the
        best implementation in the world, but such is life."""
        try:
            self.__getStatusTable('ArchivalUnitStatusTable')
            return True
        except LockssError, e:
            ## If a Lockss error is raised, pass it on.
            raise e
        except Exception, e:
            ## On any other error, just return false.
            log.debug("Got exception: %s" % e)
            return False

    def getAdminUi(self):
        """ Fetch the contents of the top-level admin UI.  Useful for
        testing the Tiny UI.  May throw urllib2.URLError or
        urllib2.HTTPError """
        get = Get(self.url, self.username, self.password)
        return get.execute()

    def hasV3Poller(self, au):
        """ Return true if the client has an active V3 Poller """
        tab = self.getAuV3Pollers(au)
        for row in tab:
            if row['auId'] == au.title:
                return True
        # Poll wasn't found.
        return False

    def hasV3Voter(self, au):
        """ Return true if the client has an active V3 Poller """
        tab = self.getAuV3Voters(au)
        for row in tab:
            if row['auId'] == au.title:
                return True
        # Poll wasn't found.
        return False

    def hasTopLevelContentPoll(self, au):
        """ Return true if the client has an active or won top level
        content poll """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                ## return (row['Status'] == 'Active' or row['Status'] == 'Won')
                return row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be called. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                ## return (row['Status'] == 'Active' or row['Status'] == 'Won')
                return row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasSNCUSSPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False

    def hasLostTopLevelContentPoll(self, au):
        """ Return true if a top level content poll has been lost. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelContentPoll(self, au):
        """ Return true if a top level content poll has been won. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'C':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be lost. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonTopLevelNamePoll(self, au):
        """ Wait for a top level name poll to be won. """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == 'lockssau:':
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasNamePoll(self, au, node):
        """ Wait for a name poll to run on a given node (won or active) """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not row['PollType'] == 'N':
                continue
            if row['URL'] == node.url:
                return (row['Status'] == 'Active' or row['Status'] == 'Won')
        # Poll wasn't found
        return False


    def hasLostNamePoll(self, au, node):
        """ Wait for a name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonNamePoll(self, au, node):
        """ Wait for a name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if row.has_key('Range') or not row['PollType'] == 'N':
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll on the given node. """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won' or row['Status'] == 'Active'
        # Poll wasn't found
        return False

    def hasLostRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll to be lost on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonRangedNamePoll(self, au, node):
        """ Wait for a ranged name poll to be won on the given node """
        if not node:
            url = au.baseUrl
        else:
            url = node.url
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'N' and row.has_key('Range')):
                continue
            if row['URL'] == url:
                return row['Status'] == 'Won'
        # Poll wasn't found
        return False

    def hasLostSNCUSSPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node
        to be lost """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       row['Status'] == 'Lost'
        # Poll wasn't found
        return False

    def hasWonSNCUSSContentPoll(self, au, node):
        """ Wait for a Single-Node CUSS poll for the given AU and node
        to be won """
        tab = self.getAuV1Polls(au)
        for row in tab:
            if not (row['PollType'] == 'C' and row.has_key('Range')):
                continue
            if row['URL'] == node.url:
                return row['Range'] == 'single node' and \
                       row['Status'] == 'Won'
        # Poll wasn't found
        return False

    ###
    ### Methods that block while waiting for various events
    ###

    def waitForCanConnectToHost(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.__canConnectToHost()
        return self.wait(waitFunc, timeout, sleep)

    def waitForCreateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to appear, or for the timeout to expire."""
        def waitFunc():
            return self.hasAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDeleteAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be deleted, or for the timeout to
        expire. """
        def waitFunc():
            return not self.hasAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForPublisherDown(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the specified AU to be marked 'publisher down' """
        def waitFunc():
            return self.isPublisherDown(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForReactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be activated, or for the timeout to
        expire. """
        def waitFunc():
            return self.isActiveAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDeactivateAu(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for the au to be deactivated, or for the timeout to
        expire."""
        def waitFunc():
            return not self.isActiveAu(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForSuccessfulCrawl(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until the specified au has had at least one
        successful new content crawl."""
        def waitFunc():
            tbl = self.getCrawlStatus(au)
            return tbl and tbl[0]['crawl_status'] == 'Successful'
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3Poller(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasV3Poller(au)
        return self.wait(waitFunc, timeout, sleep)
        
    def waitForWonV3Poll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasWonV3Poll(au)
        return self.wait(waitFunc, timeout, sleep)
        
    def waitForCompleteV3Repair(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.isCompleteV3Repaired(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3Voter(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasV3Voter(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasSNCUSSPoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """ Block until a top level content poll is lost. """
        def waitFunc():
            return self.hasLostTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a top level name poll is lost. """
        def waitFunc():
            return self.hasLostTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a node name poll is lost. """
        def waitFunc():
            return self.hasLostNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)


    def waitForLostSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """ Block until a top level name poll is lost. """
        def waitFunc():
            return self.hasLostSNCUSSPoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelContentPoll(self, au, timeout=DEF_TIMEOUT,
                                       sleep=DEF_SLEEP):
        """ Block until a top level content poll is won. """
        def waitFunc():
            return self.hasWonTopLevelContentPoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonTopLevelNamePoll(self, au, timeout=DEF_TIMEOUT,
                                    sleep=DEF_SLEEP):
        """ Block until a top level name poll is won. """
        def waitFunc():
            return self.hasWonTopLevelNamePoll(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                        sleep=DEF_SLEEP):
        """ Block until a node level name poll is run (active or won) """
        def waitFunc():
            return self.hasNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                           sleep=DEF_SLEEP):
        """ Block until a node level name poll is won. """
        def waitFunc():
            return self.hasWonNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonSNCUSSContentPoll(self, au, node, timeout=DEF_TIMEOUT,
                                     sleep=DEF_SLEEP):
        """ Block until a SNCUSS content poll is won. """
        def waitFunc():
            return self.hasWonSNCUSSContentPoll(au,  node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """ Block until a ranged name poll has occured (active or won)"""
        def waitFunc():
            return self.hasRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForWonRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                 sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasWonRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForLostRangedNamePoll(self, au, node, timeout=DEF_TIMEOUT,
                                  sleep=DEF_SLEEP):
        def waitFunc():
            return self.hasLostRangedNamePoll(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelDamage(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until an au is marked 'Damaged' at the top level
        (lockssau:). """
        def waitFunc():
            return self.isAuDamaged(au, None)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDamage(self, au, node, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until a node is marked 'repairing', or until the
        timeout expires. """
        def waitFunc():
            return self.isAuDamaged(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForTopLevelRepair(self, au, timeout=DEF_TIMEOUT,
                              sleep=DEF_SLEEP):
        """ Block until the top-level of the AU is marked repaired. """
        def waitFunc():
            return self.isAuOK(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForContentRepair(self, au, node, timeout=DEF_TIMEOUT,
                             sleep=DEF_SLEEP):
        """ Block until a node has been successfully repaired ,or until
        the timeout expires."""
        def waitFunc():
            return self.isContentRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForV3Repair(self, au, nodeList=[], timeout=DEF_TIMEOUT,
                        sleep=DEF_SLEEP):
        """ Wait for a successful repair of the specified node by a V3 Poll """
        def waitFunc():
            return self.isV3Repaired(au, nodeList)
        return self.wait(waitFunc, timeout, sleep)
        
    def waitForV3NoQuorum(self, au, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Wait for a V3 poll to be marked No Quorum """
        def waitFunc():
            return self.isV3NoQuorum(au)
        return self.wait(waitFunc, timeout, sleep)

    def waitForNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                          sleep=DEF_SLEEP):
        """ Block until a node has been successfully repaired, or until
        the timeout expires.  If 'node' is None, this will just wait until
        the base URL has been marked repaired."""
        def waitFunc():
            return self.isNameRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForRangedNameRepair(self, au, node=None, timeout=DEF_TIMEOUT,
                                sleep=DEF_SLEEP):
        def waitFunc():
            return self.isRangedNameRepaired(au, node)
        return self.wait(waitFunc, timeout, sleep)

    def waitForDaemonReady(self, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Block until the framework is ready for client communication """
        def waitFunc():
            return self.isDaemonReady()
        res = self.wait(waitFunc, timeout, sleep)
        # Kludge.  Wait an additional 4 seconds after the daemon is ready.
        # This seems to be enough time to wait for it to accept new AU
        # creations.
        time.sleep(4)
        return res

    def wait(self, condFunc, timeout=DEF_TIMEOUT, sleep=DEF_SLEEP):
        """ Given a function to evaluate, loop until the function evals to
        true, or until the timeout has expired. """
        start = time.time()
        while(time.time() - start) < timeout:
            if condFunc():
                return True
            else:
                time.sleep(sleep)
        # fall out, condition wasn't met.
        return False

    ###
    ### Methods for causing damage
    ###

    def randomDamageSingleNode(self, au):
        """ Randomly select a node with content, and cause damage in it. """
        node = self.__getRandomContentNode(au)
        self.damageNode(node)
        return node

    def randomDamageRandomNodes(self, au, minCount=1, maxCount=5):
        """ Damage a random selection of between minCount and maxCount
        nodes with content for the given au.  Returns the list of
        damaged nodes. """
        nodeList = self.__getRandomContentNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.damageNode(node)
        nodeList.sort()
        return nodeList

    def randomDelete(self, au):
        """ Randomly select a node with content, and delete it. """
        node = self.__getRandomContentNode(au)
        self.deleteNode(node)
        return node

    def randomDeleteRandomNodes(self, au, minCount=1, maxCount=5):
        """ Delete a random selection of between minCount and maxCount
        nodes with content for the given au.  Returns the list of
        damaged nodes as a tuple. """
        nodeList = self.__getRandomContentNodeList(au, minCount, maxCount)
        for node in nodeList:
            self.deleteNode(node)
        nodeList.sort()
        return nodeList

    def damageNode(self, node):
        """ Damage a specific node. """
        # Only want to damage the file contents
        fullPath = path.join(node.file, '#content', 'current')
        if path.isfile(fullPath):
            f = open(fullPath, 'a')
            f.write('*** DAMAGE ***')
            f.close()
        else:
            raise LockssError("File does not exist: %s" % f)

    def deleteNode(self, node):
        """ Delete a specific node. """
        f = node.file
        if path.isfile(f):
            os.unlink(f)
        elif path.isdir(f):
            shutil.rmtree(f)
        else:
            raise LockssError("File does not exist: %s" % f)

    def simulateDiskFailure(self):
        """ Delete the entire contents of this client's cache.  Obviously,
        very destructive. """
        cacheDir = path.join(self.daemonDir, "cache")
        configDir = path.join(self.daemonDir, "config")
        pluginsDir = path.join(self.daemonDir, "plugins")
        if path.isdir(cacheDir) and path.isdir(configDir)\
               and path.isdir(pluginsDir):
            shutil.rmtree(cacheDir)
            shutil.rmtree(configDir)
            shutil.rmtree(pluginsDir)
        else:
            raise LockssError("Cache not in an expected state!")

    def createFile(self, au, filespec):
        """ Create an extra file in the repository under the given AU and
        filespec.  The filespec should be relative to the AU's root, for example
        'foo/bar' would attempt to create the file
        /path/to/au/repository/www.example.com/http/foo/bar.  Returns
        the new file. """
        root = self.getAuRoot(au)
        output = path.join(root, filespec)
        if path.isfile(output):
            raise LockssError("File already exists: %s" % filespec)
        f = open(output, 'w')
        f.write('Garbage File')
        f.close()
        return output

    # NOTE!  These two methods are extremely dependent on the
    # current implementation of the repository.  If that changes,
    # thesee must change.
    def createNode(self, au, filespec, node=None):
        """ Given a branch node, create a new child under it. """
        
        if not node:
            root = self.getAuRoot(au)
        else:
            root = node.file

        now = time.strftime("%s000") # No ms precision
        nowComment = time.strftime("#%a %b %d %H:%M:%S %Z %Y")
        
        auRoot = self.getAuRoot(au)
        nodeRoot = path.join(root, filespec)
        url = au.baseUrl + nodeRoot[len(auRoot):]
        
        os.mkdir(nodeRoot)
        os.mkdir(path.join(nodeRoot, '#content'))
        
        contentFile = path.join(nodeRoot, '#content', 'current')
        f = open(contentFile, 'w')
        f.write('Garbage File')
        f.close()
        
        propsFile = path.join(nodeRoot, '#content', 'current.props')
        f = open(propsFile, 'w')
        f.write("#HTTP headers for %s\n" % url)
        f.write("%s\n" % nowComment)
        f.write("last-modified=0\n")
        f.write("org.lockss.version.number=1\n")
        f.write("x-lockss-content-type=text/plain\n")
        f.write("x-lockss-node-url=%s\n" % url)
        f.write("x-lockss-orig-url=%s\n" % url)
        f.write("x_lockss-server-date=%s\n" % now)
        f.close()
        
        nodeProps = path.join(nodeRoot, '#node_props')
        f = open(nodeProps, 'w')
        f.write("#Node properties\n")
        f.write("%s\n" % nowComment)
        f.write("node.child.count=0\n")
        f.write("node.tree.size=12\n")
        f.close()

        return Node(url, nodeRoot)

    def randomCreateRandomNodes(self, au, minCount=1, maxCount=5):
        """ Create a random number of between minCount and maxCount
        nodes on the given au.  Return the list of new nodes. """
        nodeList = []
        random.seed(time.time())
        numNodes = random.randint(minCount, maxCount)
        for nodeNum in range(minCount, maxCount+1):
            newNode = self.createNode(au, '%sextrafile.txt' % nodeNum)
            nodeList.append(newNode)
        nodeList.sort()
        return nodeList


    def getAuNode(self, au, url, checkForContent=False):
        """ Construct a node from a url on an AU """
        root = self.getAuRoot(au)
        ### Kludge for getting "lockssau" node.
        if url == 'lockssau':
            f = root
        else:
            f = path.join(root, url[(len(au.baseUrl) + 1):])

        node = Node(url, f)

        if checkForContent:
            if path.isfile(path.join(f, '#content', 'current')):
                return node
            else:
                raise LockssError("Node has no content: %s" % node.url)
        else:
            if path.isdir(f) or path.isfile(f):
                return node
            else:
                raise LockssError("Node does not exist: %s" % node.url)


    ###
    ### Internal methods
    ###

    def __getStatusTable(self, statusTable, key=None, numrows=None):
        """ Given an XML string, parse it as a status table and return
        a list of dictionaries representing the data.  Each item in
        the list is a dictionary of column names to values, stored as
        Unicode strings. """
        post = self.__makePost('DaemonStatus')
        post.add('table', statusTable)
        if not key == None:
            post.add('key', key)
        if not numrows == None:
            post.add('numrows', numrows)
        post.add('output', 'xml')

        xml = post.execute().read()
        log.debug2("Received XML response: \n" + xml)
        doc = minidom.parseString(xml)
        doc.normalize() # required for python 2.2

        summaryList = doc.getElementsByTagName('st:summaryinfo')
        summaryDict = {}
        summaryTitle = None
        summaryValue = None
        for summary in summaryList:
            if summary.getElementsByTagName('st:title')[0] and \
               summary.getElementsByTagName('st:title')[0].firstChild:
                summaryTitle = summary.getElementsByTagName('st:title')[0].firstChild.data
            if summary.getElementsByTagName('st:value')[0]:
                summaryValue = summary.getElementsByTagName('st:value')[0]
            # See if this is a reference, or CDATA
            refList = summary.getElementsByTagName('st:reference')
            if summaryTitle and refList:
                ref = refList[0] # should be only one!
                name = ref.getElementsByTagName('st:name')[0].firstChild.data
                key = ref.getElementsByTagName('st:key')[0].firstChild.data
                value = ref.getElementsByTagName('st:value')[0].firstChild.data
                summaryDict[summaryTitle] = {'name': name,
                                             'key': key,
                                             'value': value}
            elif summaryTitle and summaryValue:
                summaryDict[summaryTitle] = summaryValue.firstChild.data

        rowList = doc.getElementsByTagName('st:row')
        data = []
        for row in rowList:
            rowDict = {}
            for cell in row.getElementsByTagName('st:cell'):
                try:
                    colName = cell.getElementsByTagName('st:columnname')[0].firstChild.data
                    colVal = cell.getElementsByTagName('st:value')[0]

                    # See if this is a reference, or CDATA
                    refList = colVal.getElementsByTagName('st:reference')
                    if refList:
                        ref = refList[0] # should only be one!
                        name = ref.getElementsByTagName('st:name')[0].firstChild.data
                        key = ref.getElementsByTagName('st:key')[0].firstChild.data
                        value = ref.getElementsByTagName('st:value')[0].firstChild.data
                        rowDict[colName] = {'name': name,
                                            'key': key,
                                            'value': value}
                    else:
                        rowDict[colName] = colVal.firstChild.data
                except (IndexError, AttributeError):
                    # Unlikely to happen, but just in case...
                    continue
            data.append(rowDict)
        return (summaryDict, data)

    def __canConnectToHost(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((self.hostname, self.port))
            s.close()
            return True
        except Exception, e:
            log.debug2("Connect error: %s" % (e))
            return False

    def __makePost(self, page, lockssAction=None):
        postUrl = self.url + page
        post = Post(postUrl, self.username, self.password)
        if lockssAction:
            post.add('lockssAction', lockssAction)
        return post

    def __makeAuPost(self, au, lockssAction):
        post = self.__makePost('AuConfig', lockssAction)
        post.add('PluginId', au.pluginId)
        post.add('lfp.root', au.root)

        if (not au.depth == -1):
            post.add('lfp.depth', au.depth)
        if (not au.branch == -1):
            post.add('lfp.branch', au.branch)
        if (not au.numFiles == -1):
            post.add('lfp.numFiles', au.numFiles)
        if (not au.binFileSize == -1):
            post.add('lfp.binFileSize', au.binFileSize)
        if (not au.maxFileName == -1):
            post.add('lfp.maxFileName', au.maxFileName)
        if (not au.fileTypes == -1):
            post.add('lfp.fileTypes', au.fileTypes)
        if (not au.oddBranchContent == -1):
            post.add('lfp.odd_branch_content', au.oddBranchContent)
        if (not au.badFileLoc == None):
            post.add('lfp.badFileLoc', au.badFileLoc)
        if (not au.badFileNum == -1):
            post.add('lfp.badFileNum', au.badFileNum)
        if (au.publisherDown):
            post.add('lfp.pub_down', 'true')
        if (au.protocolVersion > 0):
            post.add('lfp.protocol_version', au.protocolVersion)
        post.add('auid', au.auId)

        return post

    def __getRandomContentNode(self, au):
        ### Raise an error if the AU hasn't been created or
        ### crawled.
        repository = self.getAuRepository(au)
        if repository == None:
            raise LockssError("No repository for au: %s" % au)

        ### Randomly select a node to damage
        random.seed(time.time())
        nodeList = self.getAuNodesWithContent(au)
        randomNode = nodeList[random.randint(0, len(nodeList) - 1)]

        return randomNode

    def __getRandomContentNodeList(self, au, minCount, maxCount):
        """ Return a randomly sized list of nodes with content,
        between min and max in length. If there are fewer than 'max'
        nodes, this will return a list between 'min' and
        'len(allnodes)' """
        nodes = self.getAuNodesWithContent(au)
        maxCount = min(maxCount, len(nodes))

        random.seed(time.time())
        returnListLength = random.randint(minCount, maxCount)

        # Build a list of nodes.
        ix = 0
        returnList = []
        while ix < returnListLength:
            random.seed(time.time())
            node = nodes[random.randint(0, len(nodes) - 1)]
            if node in returnList:
                continue
            returnList.append(node)
            ix += 1

        return returnList

    def __getRandomBranchNodeList(self, au, minCount, maxCount):
        """ Return a randomly sized list of nodes with children,
        between min and max in length. If there are fewer than 'max'
        nodes, this will return a list between 'min' and
        'len(allnodes)' """
        nodes = self.getAuNodesWithChildren(au)
        maxCount = min(maxCount, len(nodes))
        if (maxCount == 0):
            log.warn("getAuNodesWithChildren returned no nodes!")
            return []

        random.seed(time.time())
        returnListLength = random.randint(minCount, maxCount)

        # Build a list of nodes.
        ix = 0
        returnList = []
        while ix < returnListLength:
            random.seed(time.time())
            node = nodes[random.randint(0, len(nodes) - 1)]
            if node in returnList:
                continue
            returnList.append(node)
            ix += 1

        return returnList


    def __str__(self):
        return "%s" % self.url

class SimulatedAu:
    """ A Python abstraction of a SimulatedPlugin ArchivalUnit for use
    in the functional test framework. """
    def __init__(self, root, depth=-1, branch=-1, numFiles=-1,
                 binFileSize=-1, maxFileName=-1, fileTypes=-1,
                 oddBranchContent=-1, badFileLoc=None, badFileNum=-1,
                 publisherDown=False, protocolVersion=-1):
        self.root = root
        self.depth = depth
        self.branch = branch
        self.numFiles = numFiles
        self.binFileSize = binFileSize
        self.maxFileName = maxFileName
        self.fileTypes = fileTypes
        self.oddBranchContent = oddBranchContent
        self.badFileLoc = badFileLoc
        self.badFileNum = badFileNum
        self.publisherDown = publisherDown
        self.protocolVersion = protocolVersion
        self.baseUrl = 'http://www.example.com'
        self.dirStruct = path.join('www.example.com', 'http')
        self.pluginId = 'org.lockss.plugin.simulated.SimulatedPlugin'
        self.auId = "%s&root~%s" % (self.pluginId.replace('.', '|'), self.root)
        self.title = "Simulated Content: " + root
        self.startUrl = self.baseUrl + '/index.html'

    def __str__(self):
        return self.title

    def getAuId(self):
        auIdKey = self.pluginId.replace('.', '|')
        return "%s&root~%s" % (auIdKey, self.root)

class Node:
    """ In keeping with python written in a Java accent, here's a
    simple object that could just as easily have been a tuple or a
    dict. """
    def __init__(self, url, file):
        self.url = url
        self.file = file

    def __cmp__(self, other):
        return cmp(self.url, other.url)

    def __str__(self):
        return "%s" % self.url

class LockssDaemon:
    """ Wrapper around a daemon instance.  Controls starting and stopping
    a LOCKSS Java daemon. """
    def __init__(self, dir, cp, configList):
        self.daemonDir = dir
        self.cp = cp
        self.configList = configList
        if not os.environ.has_key('JAVA_HOME'):
            raise LockssError("JAVA_HOME must be set.")
        javaHome = os.environ['JAVA_HOME']
        self.javaBin = path.join(javaHome, 'bin', 'java')
        self.logfile = path.join(self.daemonDir, 'test.out')
        self.isRunning = False

    def start(self):
        if not self.isRunning:
            cmd = '%s -cp %s -Dorg.lockss.defaultLogLevel=debug '\
                  'org.lockss.app.LockssDaemon %s > %s 2>&1 & '\
                  'echo $! > %s/dpid'\
                  % (self.javaBin, self.cp, ' '.join(self.configList),
                     self.logfile, self.daemonDir)
            os.system(cmd)
            self.pid = int(open(path.join(self.daemonDir, 'dpid'), 'r').readline())
            self.isRunning = True

    def stop(self):
        if self.isRunning:
            os.kill(self.pid, signal.SIGTERM)
            self.isRunning = False

    def requestThreadDump(self):
        if self.isRunning:
            os.kill(self.pid, signal.SIGQUIT)
            # Horrible kludge.  Pause for one second so that the VM has
            # time to comply and flush the daemon log file.
            time.sleep(1)

class Post:
    """ A simple wrapper for HTTP post management. """
    def __init__(self, url='', username=None, password=None, cookie=None):
        self.request = urllib2.Request(url)
        if cookie:
            self.request.add_header('Cookie', cookie)
        if not username == None and not password == None:
            # Attempt to set authentication
            encoded = base64.encodestring('%s:%s' % (username, password))[:-1]
            authheader = "Basic %s" % encoded
            self.request.add_header("Authorization", authheader)
        self.postData = []

    def add(self, key, val):
        self.postData.append((key, val))

    def execute(self):
        """ Send a POST with the previously constructed form data,
        and return the contents of the file. """
        args = urllib.urlencode(self.postData)
        opener = urllib2.build_opener()
        log.debug2("Sending POST: %s?%s" % (self.request.get_full_url(), args))
        return opener.open(self.request, args)
 
class MultipartPost:
    def __init__(self, url='', username=None, password=None, cookie=None):
        urlparts = urlparse.urlsplit(url)
        self.host = urlparts[1]
        self.selector = urlparts[2]
        self.cookie = cookie
        self.postData = []
        self.fileData = []
        self.authHeader = "Basic %s" % base64.encodestring('%s:%s' % (username, password))[:-1]

    def add(self, key, val):
        self.postData.append((key, val))

    def addFile(self, key, filename):
        f = open(filename, "r")
        self.fileData.append((key, filename, f.read()))

    def execute(self):
        """ Returns a tuple of (response body, cookie).  cookie is 'None' if no
        SetCookie header was received. """

        (contentType, body) = self.encodeMultipartFormdata(self.postData, self.fileData)
        length = str(len(body))
        h = httplib.HTTP(self.host)
        h.putrequest('POST', self.selector)
        h.putheader("Authorization", self.authHeader)
        h.putheader("Content-Type", contentType)
        h.putheader("Content-Length", length)
        if self.cookie:
            h.putheader("Cookie", self.cookie)
        h.endheaders()
        h.send(body)
        errcode, errmsg, headers = h.getreply()
        responseCookie = headers.getheader('Set-Cookie')
        if responseCookie:
            responseCookie = responseCookie.split(';')[0]
        return (h.file.read(), responseCookie)


    def encodeMultipartFormdata(self, fields, files):
        """ fields is a sequence of (name, value) elements for regular
        form fields.  files is a sequence of (name, filename, value)
        elements for data to be uploaded as files Return (content_type,
        body) ready for httplib.HTTP instance """

        boundary = mimetools.choose_boundary()

        content_type = 'multipart/form-data; boundary=%s' % boundary

        data = ''
        for (key, value) in fields:
            data += '--%s\r\n' % boundary
            data += 'Content-Disposition: form-data; name="%s"\r\n' % key
            data += '\r\n'
            data += "%s\r\n" % value
        for (key, filename, value) in files:
            data += '--%s\r\n' % boundary
            data += 'Content-Disposition: form-data; name="%s"; filename="%s"\r\n' % \
                     (key, filename)
            data += 'Content-Type: %s\r\n' % self.getContentType(filename)
            data += '\r\n'
            data += '%s\r\n' % value
        data += '--%s--\r\n' % boundary
        data += '\r\n'

        return (content_type, data)

    def getContentType(self, filename):
        return mimetypes.guess_type(filename)[0] or 'application/octet-stream'

class Get:
    def __init__(self, url='', username=None, password=None):
        self.request = urllib2.Request(url)
        if not username == None and not password == None:
            # Attempt to set authentication
            encoded = base64.encodestring('%s:%s' % (username, password))[:-1]
            authheader = "Basic %s" % encoded
            self.request.add_header("Authorization", authheader)

    def execute(self):
        opener = urllib2.build_opener()
        log.debug2("Sending GET: %s" % self.request.get_full_url())
        return opener.open(self.request)

###########################################################################
##
## Config file strings, used when creating default config files.
##
###########################################################################

globalConfigTemplate = """\
# LOCKSS & LCAP tuning parameters
org.lockss.log.default.level=%(logLevel)s

org.lockss.config.reloadInterval=60m

#comm settings
org.lockss.ui.start=yes
org.lockss.proxy.start=no
org.lockss.comm.multicast.group=239.4.5.6
#org.lockss.comm.multicast.port=3456
org.lockss.comm.multicast.port=localIp
org.lockss.comm.unicast.port=1025
org.lockss.comm.multicast.verify=no

# lcap protocol settings
org.lockss.protocol.ttl=2
org.lockss.protocol.hashAlgorithm=SHA-1

# poll settings
org.lockss.poll.maxpolls=20
org.lockss.poll.quorum=3
org.lockss.poll.agreeVerify=10
org.lockss.poll.disagreeVerify=50
org.lockss.poll.voteMargin=51
org.lockss.poll.trustedWeight=350
org.lockss.poll.namepoll.deadline=5m

org.lockss.poll.contentpoll.min=4m
org.lockss.poll.contentpoll.max=6m

org.lockss.treewalk.initial.estimate=20s
org.lockss.treewalk.interval.min=3m
org.lockss.treewalk.interval.max=5m
org.lockss.treewalk.start.delay=20s
org.lockss.comm.router.beacon.interval=1m
org.lockss.baseau.toplevel.poll.interval.min=4m
org.lockss.baseau.toplevel.poll.interval.max=6m
org.lockss.baseau.toplevel.poll.prob.initial=100

# Set the v3 poll state dir to /tmp
org.lockss.poll.v3.messageDir=/tmp

org.lockss.metrics.slowest.hashrate=250
org.lockss.state.recall.delay=5m

# Turn down logging on areas that we normally
# don't care much about during polling tests.
org.lockss.log.LockssServlet.level=info
org.lockss.log.GenericHasher.level=info
org.lockss.log.GoslingHtmlParser.level=info
org.lockss.log.StringFilter.level=info
org.lockss.log.PluginMgr.level=info
org.lockss.log.DaemonStatus.level=info
"""

localConfigTemplate = """\
org.lockss.platform.ui.username=%(username)s
org.lockss.platform.ui.password=%(password)s
org.lockss.platform.diskSpacePaths=%(dir)s
org.lockss.cache.location=%(dir)s
org.lockss.proxy.start=%(proxyStart)s
org.lockss.ui.port=%(uiPort)s
org.lockss.localIPAddress=%(ipAddr)s
org.lockss.comm.unicast.port=%(unicastPort)s
org.lockss.comm.unicast.sendToPort=%(unicastSendToPort)s
org.lockss.comm.unicast.sendToAddr=127.0.0.1
org.lockss.proxy.icp.port=%(icpPort)s
"""
