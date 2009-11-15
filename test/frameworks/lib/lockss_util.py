"""Utility functions and classes used by testsuite and lockss_daemon"""

import cgi # urlparse for Python 2.6+
import logging
import mimetools
import mimetypes
import os
import socket
import threading
import urllib
import urllib2

DEBUG2 = logging.DEBUG - 1
DEBUG3 = logging.DEBUG - 2


class LockssError( Exception ):
    """Daemon exceptions"""

    def __init__( self, msg ):
        Exception.__init__( self, msg )


class LOCKSS_Logger( logging.getLoggerClass() ):
    """Extension of the built-in class"""

    def debug2( self, message, *arguments, **keyword_arguments ):
        if self.manager.disable < DEBUG2 and DEBUG2 >= self.getEffectiveLevel():
            self._log( DEBUG2, message, arguments, keyword_arguments )

    def debug3( self, message, *arguments, **keyword_arguments ):
        if self.manager.disable < DEBUG3 and DEBUG3 >= self.getEffectiveLevel():
            self._log( DEBUG3, message, arguments, keyword_arguments )


class Configuration( dict ):
    """A special-purpose dictionary"""

    def daemonItems( self ):
        """Return only the daemon properties"""
        return Configuration( ( key, value ) for key, value in self.iteritems() if key.startswith( 'org.lockss.' ) )

    def getBoolean( self, key, default = False ):
        value = self.get( key, default )
        if type( value ) is str:
            return value.lower() in ( '1', 'on', 't', 'true', 'y', 'yes' )
        return bool( value )
        
    def load( self, filename ):
        """Populate the configuration from a file"""

        def error( line_number ):
            raise LockssError( 'Invalid expression on line %i of %s' % ( line_number + 1, filename ) )

        for line_number, line in enumerate( open( filename ) ):
            line = line.split( '#', 1 )[ 0 ].strip()
            if not line:
                continue
            try:
                key, value = ( item for item in ( substring.strip() for substring in line.split( '=' ) ) if item )
            except ValueError:
                error( line_number )
            if value.endswith( '\\' ):
                error( line_number )    # Unsupported
            self[ key ] = value
            if key == 'scriptLogLevel':
                log.setLevel( logging._levelNames[ value.upper() ] )


class Multipart_Form_Request( urllib2.Request ):
    """Request with file-uploading support"""

    def __init__( self, url, data = None, files = None, headers = None, origin_request_host = None, unverifiable = False ):
        urllib2.Request.__init__( self, url, data, {} if headers is None else headers, origin_request_host, unverifiable )
        self.files = files

    def add_files( self, files ):
        """File dictionary must be of the form { key: path } or None"""
        self.files = files

    def has_files( self ):
        return bool( self.files )

    def get_files( self ):
        return self.files


class Multipart_Form_HTTP_Handler( urllib2.HTTPHandler ):
    """File-uploading HTTP request handler
   http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/146306
   http://odin.himinbi.org/MultipartPostHandler.py"""

    def http_request( self, request ):

        def multipart_encode( data, files, boundary ):
            form = []
            if data:
                for key, value in cgi.parse_qsl( data ):
                    form.append( '--' + boundary )
                    form.append( 'Content-Disposition: form-data; name="%s"' % key )
                    form.append( '' )
                    form.append( value )
            for key, filename in files.iteritems():
                form.append( '--' + boundary )
                form.append( 'Content-Disposition: form-data; name="%s"; filename="%s"' % ( key, os.path.basename( filename ) ) )
                form.append( 'Content-Type: ' + ( mimetypes.guess_type( filename )[ 0 ] or 'application/octet-stream' ) )
                form.append( '' )
                form.append( open( filename, 'rb' ).read() )
            form.append( '--' + boundary + '--' )
            form.append( '' )
            return '\r\n'.join( form )

        if isinstance( request, Multipart_Form_Request ) and request.has_files():
            boundary = mimetools.choose_boundary()
            request.add_unredirected_header( 'Content-Type', 'multipart/form-data; boundary=' + boundary )
            request.add_data( multipart_encode( request.get_data(), request.get_files(), boundary ) )
            request.add_files( None )
        return urllib2.HTTPHandler.http_request( self, request )


def write_to_file( data, filename, overwrite = True ):
    data_file = open( filename, 'w' if overwrite else 'a' )
    data_file.write( data )
    data_file.close()


def unused_port():
    test_socket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
    try:
        test_socket.bind( ( 'localhost', 0 ) )
        return test_socket.getsockname()[ 1 ]
    finally:
        test_socket.close()


def server_is_listening( hostname, port ):
    try:
        test_socket = socket.socket()
        test_socket.connect( ( hostname, port ) )
        return True
    except socket.error, exception:
        log.debug2( "Connect error: %s" % exception )
        return False
    finally:
        test_socket.close()


def HTTP_request( opener, url, data = None, files = None ):
    """Send a GET or a POST with pre-initialized form data and optional files and return the contents of the resource"""
    encoded_data = urllib.urlencode( data ) if data else None
    request = Multipart_Form_Request( url, encoded_data, files, None if data and 'output' in data else { 'X-Lockss-Result': 'Please' } )
    session_lock = session_locks.setdefault( request.get_host(), threading.Lock() )
    session_lock.acquire()  # Server will dispense multiple initial session cookies if addressed concurrently
    try:
        log.debug2( "Sending HTTP request for %s: %s, %s" % ( request.get_full_url(), encoded_data, files ) )
        resource = opener.open( request )
        log.debug2( "Received HTTP reply  for %s: %s, %s" % ( request.get_full_url(), encoded_data, files ) )
    finally:
        session_lock.release()
    if resource.info().getheader( 'X-Lockss-Result' ) == 'Fail':
        raise LockssError( 'HTML UI transaction failure' )
    return resource


logging.addLevelName( DEBUG2, 'DEBUG2' )
logging.addLevelName( DEBUG3, 'DEBUG3' )
logging.setLoggerClass( LOCKSS_Logger )
logging.basicConfig( format = '%(asctime)s.%(msecs)03d: %(levelname)s: %(message)s', datefmt = '%T', level = logging.INFO )
log = logging.getLogger( 'LOCKSS' )
config = Configuration()
session_locks = {}
