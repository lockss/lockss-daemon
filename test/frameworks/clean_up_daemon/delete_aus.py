#!/usr/bin/env python

# $Id$

# Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.

import optparse
import os
import sys
import urllib2
import pprint
import lockss_daemon
import fix_auth_failure

__author__ = "Barry Hayes"
__maintainer__ = "Barry Hayes"
__version__ = "1.0.2"


def _parser():
    """Make a parser for the arguments."""
    #usage = "usage: %prog [options]"
    parser = optparse.OptionParser(
        usage='%prog [options] host port [filename ...]',
        description='Delete AUs on a LOCKSS daemon. Each line in each file should have an'
                    'AUID. The filename \'-\' or an empty list will read from stdin.'
        )
    parser.add_option('-u', '--user', default='lockss-u')
    parser.add_option('-p', '--password', default='lockss-p')
    parser.add_option('-v', '--verbose', dest='verbose', action='store_true',
                        default=False)
    parser.add_option('-q', '--quiet', dest='verbose', action='store_false')
    parser.add_option('-f', '--force', dest='verify', action='store_false',
                        help='ignore auids not present on the daemon, \
                              never prompt')
    parser.add_option('-i', '--verify', dest='verify', action='store_true',
                        default=False, help='prompt before each delete')
    return parser


def _process_args():
    parser = _parser()
    (options, arguments) = parser.parse_args()
    try:
        host = arguments[0]
        port = arguments[1]
        auid_files = arguments[2:]
        if auid_files == []:
            auid_files.append('-')
    except IndexError:
        parser.error('host and port are required. Try --help')
    return (options, host, port, auid_files)


def _aus(auid_files):
    """Read each line of each file and make a list of lockss_daemon.AUs"""
    aus = list()
    for auid_file in auid_files:
        if auid_file == '-':
            f = sys.stdin
        else:
            f = open(auid_file)
        for auid in f.readlines():
            aus.append(lockss_daemon.AU(auid))
    return aus


def main():
    (options, host, port, auid_files) = _process_args()
    fix_auth_failure.fix_auth_failure()
    client = lockss_daemon.Client(host, port,
                                  options.user, options.password)
    aus = _aus(auid_files)
    has = list()
    missing = list()
    initial_auIds = client.getListOfAuids()
    for au in aus:
        if au.auId in initial_auIds:
            has.append(au)
        else:
            missing.append(au)
    
    if options.verbose:
        print "Daemon %s" % client
        if missing:
            print "does not have"
            for au in missing:
                print au.auId
        if has:
            print "to be deleted"
            for au in has:
                print au.auId

    for au in has:
        if not options.verify or \
                options.verify and \
                raw_input('delete %s [n]? ' % au.title).startswith('y'):
            client.deleteAu(au)
            if options.verbose:
                print 'Deleted: %s' % (au.auId,)


if __name__ == '__main__':
    main()
