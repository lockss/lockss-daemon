import sys, time

"""
Utility functions and classes used by testsuite and
lockss_daemon.
"""

loglevel = {"error": 0, "warn": 1, "info": 2, "debug": 3, "debug2": 4, "debug3": 5}

def loadConfig(f):
    """ Load a Config object from a file. """
    fd = open(f)
    inMultiline = False

    global config
    while True:
        line = fd.readline()
        if not line:
            break
        
        line = line.replace('\r', '').replace('\n', '')
        line = line.strip()
        
        # Ignore comments and blank lines.
        if line.startswith('#') or line == '':
            continue
    
        if line.endswith('\\'):
            inMultiline = True
            
        if line.find('=') > -1:
            (key, val) = line.split('=')
            # Allow comments on the same line, then strip and
            # clean the value.
            val = val.split('#')[0].replace('\\', '').strip()
            if inMultiline:
                continue
        elif inMultiline:
            # Last line of the multi-line?
            if not line.endswith('\\'):
                inMultiline = False
        
            line = line.replace('\\', '').strip()
            val = val + line
        else:
            # Not a comment or a multiline or a proper key=val pair,
            # ignore
            continue

        key = key.strip()
    
        if key and val:
            config.put(key, val)
            
    fd.close()
    
class Config:
    """ A safe wrapper around a dictionary.  Handles KeyErrors by
    returning None values. """
    def __init__(self):
        self.dict = {}
    
    def put(self, prop, val):
        self.dict[prop] = val

    def get(self, prop, default=None):
        try:
            return self.dict[prop]
        except KeyError:
            return default

    def getBoolean(self, prop, default=False):
        try:
            val = self.dict[prop]
            return val.lower() == 'true' or \
                   val.lower() == 't' or \
                   val.lower() == 'yes' or \
                   val.lower() == 'y' or \
                   val.lower() == '1'
        except KeyError:
            return default
        
    ##
    ## Set views
    ##
    def items(self):
        """ Return the entire set of properties. """
        return dict.items()

    def testItems(self):
        """
        Return only the testsuite properties. (anything
        with a key not starting with 'org.lockss')
        """
        retDict = {}
        for (key, val) in self.dict.items():
            if not key.startswith('org.lockss'):
                retDict[key] = val
        return retDict.items()

    def daemonItems(self):
        """
        Return only the daemon config properties. (anything
        with a key starting with 'org.lockss')
        """
        retDict = {}
        for (key, val) in self.dict.items():
            if key.startswith('org.lockss'):
                retDict[key] = val
        return retDict.items()

##
## Globally accessible config instance.
##
config = Config()

class LockssError(Exception):
    " Daemon exceptions. "
    def __init__(self, msg):
        Exception.__init__(self, msg)

class Logger:
    " Simple stdout logger. "
    
    def __init__(self):
        self.logLevel = self.__getLogLevel(config)

    def __getLogLevel(self, conf):
        level = conf.get("scriptLogLevel", "info")

        try:
            return loglevel[level.lower()]
        except:
            return loglevel["info"]

    def error(self, msg):
        if (self.logLevel >= loglevel["error"]):
            self.__writeLog("ERROR: %s" % msg)

    def warn(self, msg):
        if (self.logLevel >= loglevel["warn"]):
            self.__writeLog("WARNING: %s" % msg)

    def info(self, msg):
        if (self.logLevel >= loglevel["info"]):
            self.__writeLog("INFO: %s" % msg)

    def debug(self, msg):
        if (self.logLevel >= loglevel["debug"]):
            self.__writeLog("DEBUG: %s" % msg)

    def debug2(self, msg):
        if (self.logLevel >= loglevel["debug2"]):
            self.__writeLog("DEBUG2: %s" % msg)

    def debug3(self, msg):
        if (self.logLevel >= loglevel["debug3"]):
            self.__writeLog("DEBUG3: %s" % msg)

    def __writeLog(self, msg):
        now = time.time()
        t = time.strftime("%H:%M:%S", time.localtime(time.time()))
        msec = int((now % 1.0) * 1000)
        mmm = "%03d" % msec
        timestamp = t + '.' + mmm

        sys.stdout.write("%s: %s\n" % (timestamp, msg))
        sys.stdout.flush()

