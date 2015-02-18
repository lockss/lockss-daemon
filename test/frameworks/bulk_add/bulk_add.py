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
import lockss_util
import fix_auth_failure

__author__ = "Barry Hayes"
__maintainer__ = "Barry Hayes"
__version__ = "1.0.1"


def _parser():
    """Make a parser for the arguments."""
    #usage = "usage: %prog [options]"
    parser = optparse.OptionParser(
        usage='%prog [options] host port [filename ...]',
        description='Add AUs on a LOCKSS daemon. Each line in each file should '
                    'have an AUID. The filename \'-\' or an empty list will '
                    'read from stdin.'
        )
    parser.add_option('-u', '--user', default='lockss-u')
    parser.add_option('-p', '--password', default='lockss-p')
    parser.add_option('-v', '--verbose', dest='verbose', action='store_true',
                        default=False)
    parser.add_option('-q', '--quiet', dest='verbose', action='store_false')
    parser.add_option('-f', '--force', dest='verify', action='store_false',
                        help='ignore auids present on the daemon, '
                             'never prompt')
    parser.add_option('-i', '--verify', dest='verify', action='store_true',
                        default=False, help='prompt before each add')
    parser.add_option('-m', '--manual', dest='tdb', action='store_false',
                        help='use Manual Journal Configuration')
    parser.add_option('-t', '--tdb', dest='tdb', action='store_true',
                        default=False, help='use the TDB to find the AU')
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
            print "to be created"
            for au in missing:
                print au.auId
        if has:
            print "already has"
            for au in has:
                print au.auId

    for au in missing:
        try:
            if not options.verify or \
                    options.verify and \
                    raw_input('create %s [n]? ' % au.title).startswith('y'):
                if options.tdb:
                    client.addByAuid(au)
                else:
                    client.createAu(au)
        except lockss_util.LockssError:
            # Failed to create. Print the errors after all creation attempts.
            pass

    # Note: also prints out auids which were n'ed in verbose mode.
    final_auIds = client.getListOfAuids()
    failed = list()
    for au in missing:
        if au.auId not in final_auIds:
            failed.append(au)

    if failed:
        print >> sys.stderr, "failed to add"
        for au in failed:            
            print >> sys.stderr, au.auId
        exit(1)


if __name__ == '__main__':
    main()
