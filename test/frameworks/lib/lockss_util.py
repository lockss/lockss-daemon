"""Utility functions and classes used by testsuite and lockss_daemon."""

from __future__ import with_statement

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


class Config( dict ):
    """A special-purpose dictionary."""

    def getBoolean(self, prop, default=False):
        val = self.get( prop, default )
        if type( val ) is str:
            return val.lower() in ( 'true', 't', 'yes', 'y', '1' )
        return bool( val )
        
    def daemonItems(self):
        """Return only the daemon config properties. (anything
        with a key starting with 'org.lockss')"""
        return Config( ( key, value ) for key, value in self.iteritems() if key.startswith('org.lockss') )


def Logger():
    return logging.getLogger( 'LOCKSS' )


def loadConfig(f):
    """ Load a Config object from a file. """

    inMultiline = False

    with open( f ) as fd:
        for line in fd.readlines():

            # Ignore blank lines and comments.
            line = line.split( '#', 1 )[ 0 ].strip()
            if not line:
                continue
        
            if line.endswith('\\'):
                inMultiline = True
                
            if '=' in line:
                (key, val) = line.split('=',1)
                val = val.replace('\\', '').strip()
                if inMultiline:
                    continue

            elif inMultiline:
                # Last line of the multi-line?
                inMultiline = line.endswith('\\')
                val = val + line.replace('\\', '').strip()

            else:
                # Not a comment or a multiline or a proper key=val pair,
                # ignore
                continue

            key = key.strip()
        
            if key and val:
                config[ key ] = val
            
    logging.basicConfig( level = logging._levelNames.get( config.get( 'scriptLogLevel', '' ).upper(), logging.INFO ), format = '%(asctime)s.%(msecs)03d: %(levelname)s: %(message)s', datefmt = '%T' )


config = Config() # Globally accessible Config instance.

logging.addLevelName( DEBUG2, 'DEBUG2' )
logging.addLevelName( DEBUG3, 'DEBUG3' )
logging.setLoggerClass( LOCKSS_Logger )
