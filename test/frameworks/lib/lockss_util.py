"""Utility functions and classes used by testsuite and lockss_daemon"""

from __future__ import with_statement

import cgi # urlparse for Python 2.6+
import logging
import mimetools
import mimetypes
import os
import sys
import time
import urllib
import urllib2

DEBUG2 = logging.DEBUG - 1
DEBUG3 = logging.DEBUG - 2


class LockssError(Exception):
    """Daemon exceptions"""
    def __init__(self, msg):
        Exception.__init__(self, msg)


class LOCKSS_Logger( logging.getLoggerClass() ):
    """Extension of the built-in class"""

    def debug2( self, msg, *args, **kwargs ):
        if self.manager.disable < DEBUG2 and DEBUG2 >= self.getEffectiveLevel():
            apply( self._log, ( DEBUG2, msg, args ), kwargs )

    def debug3( self, msg, *args, **kwargs ):
        if self.manager.disable < DEBUG3 and DEBUG3 >= self.getEffectiveLevel():
            apply( self._log, ( DEBUG3, msg, args ), kwargs )


class Configuration( dict ):
    """A special-purpose dictionary"""

    def daemonItems( self ):
        """Return only the daemon properties"""
        return Configuration( ( key, value ) for key, value in self.iteritems() if key.startswith( 'org.lockss.' ) )

    def getBoolean( self, key, default = False ):
        value = self.get( key, default )
        if type( value ) is str:
            return value.lower() in ( 't', 'true', 'y', 'yes', '1' )
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
            config[ key ] = value
        logging.basicConfig( format = '%(asctime)s.%(msecs)03d: %(levelname)s: %(message)s', datefmt = '%T' )
        logging.getLogger().setLevel( logging._levelNames.get( config.get( 'scriptLogLevel', '' ).upper(), logging.INFO ) )


class Multipart_Form_Request( urllib2.Request ):
    """Request with file-uploading support"""

    def __init__( self, url, data = None, file = None, headers = {}, origin_req_host = None, unverifiable = False ):
        urllib2.Request.__init__( self, url, data, headers, origin_req_host, unverifiable )
        self.file = file

    def add_file( self, file ):
        """Parameter file is ( key, path ) or None"""
        self.file = file

    def has_file( self ):
        return self.file is not None

    def get_file( self ):
        return self.file


class Multipart_Form_HTTP_Handler( urllib2.HTTPHandler ):
    """File-uploading HTTP request handler
    Based on:
        Recipe 146306: Http client to POST using multipart/form-data (http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/146306)
        Will Holcomb's MultipartPostHandler (http://odin.himinbi.org/MultipartPostHandler.py)"""

    def multipart_encode( self, data, file, boundary ):
        buffer = []
        for ( key, value ) in data:
            buffer.append( '--' + boundary )
            buffer.append( 'Content-Disposition: form-data; name="%s"' % key )
            buffer.append( '' )
            buffer.append( value )
        buffer.append( '--' + boundary )
        buffer.append( 'Content-Disposition: form-data; name="%s"; filename="%s"' % ( file[ 0 ], os.path.basename( file[ 1 ] ) ) )
        buffer.append( 'Content-Type: ' + ( mimetypes.guess_type( file[ 1 ] )[ 0 ] or 'application/octet-stream' ) )
        buffer.append( '' )
        buffer.append( open( file[ 1 ], 'rb' ).read() )
        buffer.append( '--' + boundary + '--' )
        buffer.append( '' )
        return '\r\n'.join( buffer )

    def http_request( self, request ):
        if request.has_file():
            boundary = mimetools.choose_boundary()
            request.add_unredirected_header( 'Content-Type', 'multipart/form-data; boundary=' + boundary )
            request.add_data( self.multipart_encode( cgi.parse_qsl( request.get_data() ) if request.has_data() else [], request.get_file(), boundary ) )
            request.add_file( None )
        return urllib2.HTTPHandler.http_request( self, request )


class HTTP_Request:
    """Wrapper for HTTP request management"""

    def __init__( self, URL_opener, url, data = None, file = None ):
        self.opener = URL_opener
        self.url = url
        self.data = data or {}
        self.file = file

    def add_data( self, key, value ):
        self.data[ key ] = value

    def add_file( self, key, filename ):
        self.file = ( key, filename )

    def execute( self ):
        """Send a GET or a POST with pre-initialized form data and an optional file and return the contents of the resource"""
        encoded_data = urllib.urlencode( self.data ) if self.data else None
        request = Multipart_Form_Request( self.url, encoded_data, self.file, {} if 'output' in self.data else { 'X-Lockss-Result': 'Please' } )
        log.debug2( "Sending HTTP request for %s: %s, %s" % ( request.get_full_url(), encoded_data, repr( self.file ) ) )
        resource = self.opener.open( request )
        if resource.info().getheader( 'X-Lockss-Result' ) == 'Fail':
            raise LockssError( 'HTML UI transaction failure' )
        return resource


logging.addLevelName( DEBUG2, 'DEBUG2' )
logging.addLevelName( DEBUG3, 'DEBUG3' )
logging.setLoggerClass( LOCKSS_Logger )
log = logging.getLogger( 'LOCKSS' )
config = Configuration()
