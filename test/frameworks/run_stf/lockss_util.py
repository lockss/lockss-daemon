import sys, time

"""
Utility functions and classes used by testsuite and
lockss_daemon.
"""

## Log level enum
LOG_ERROR, LOG_WARN, LOG_INFO, LOG_DEBUG, LOG_DEBUG2 = range(5)  # Logger flags

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
    levelMap = {"error": LOG_ERROR,
                "warn": LOG_WARN,
                "info": LOG_INFO,
                "debug": LOG_DEBUG,
                "debug2": LOG_DEBUG2}
    
    def __init__(self):
        self.logLevel = self.__getLogLevel(config)

    def __getLogLevel(self, config):
        level = config.get("scriptLogLevel", "info")

        try:
            return self.levelMap[level.lower()]
        except:
            return LOG_INFO

    def error(self, msg):
        if (self.logLevel >= LOG_ERROR):
            self.__writeLog("ERROR: %s" % msg)

    def warn(self, msg):
        if (self.logLevel >= LOG_WARN):
            self.__writeLog("WARNING: %s" % msg)

    def info(self, msg):
        if (self.logLevel >= LOG_INFO):
            self.__writeLog("INFO: %s" % msg)

    def debug(self, msg):
        if (self.logLevel >= LOG_DEBUG):
            self.__writeLog("DEBUG: %s" % msg)

    def debug2(self, msg):
        if (self.logLevel >= LOG_DEBUG2):
            self.__writeLog("DEBUG2: %s" % msg)

    def __writeLog(self, msg):
        now = time.time()
        t = time.strftime("%H:%M:%S", time.localtime(time.time()))
        msec = int((now%1.0) * 1000)
        mmm = "%03d" % msec
        timestamp = t + '.' + mmm

        sys.stdout.write("%s: %s\n" % (timestamp, msg))
        sys.stdout.flush()

