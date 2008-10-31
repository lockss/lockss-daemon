"""Utility functions and classes used by testsuite and lockss_daemon."""

import logging
import sys
import time


DEBUG2 = logging.DEBUG - 1
DEBUG3 = logging.DEBUG - 2


class LockssError(Exception):
    """Daemon exceptions."""
    def __init__(self, msg):
        Exception.__init__(self, msg)


class LOCKSS_Logger( logging.getLoggerClass() ):

    def debug2(self, msg, *args, **kwargs):
        if self.manager.disable < DEBUG2 and DEBUG2 >= self.getEffectiveLevel():
            apply(self._log, (DEBUG2, msg, args), kwargs)

    def debug3(self, msg, *args, **kwargs):
        if self.manager.disable < DEBUG3 and DEBUG3 >= self.getEffectiveLevel():
            apply(self._log, (DEBUG3, msg, args), kwargs)


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
        """Return only the daemon config properties. (anything
        with a key starting with 'org.lockss')"""
        retDict = {}
        for (key, val) in self.dict.items():
            if key.startswith('org.lockss'):
                retDict[key] = val
        return retDict.items()


def Logger():
    return logging.getLogger( 'LOCKSS' )


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

    logging.basicConfig( level = logging._levelNames.get( config.get( 'scriptLogLevel', '' ).upper(), logging.INFO ), format = '%(asctime)s.%(msecs)03d: %(levelname)s: %(message)s', datefmt = '%T' )


config = Config() # Globally accessible Config instance.

logging.addLevelName( DEBUG2, 'DEBUG2' )
logging.addLevelName( DEBUG3, 'DEBUG3' )
logging.setLoggerClass( LOCKSS_Logger )
