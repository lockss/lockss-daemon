#!/usr/bin/env python

# $Id: clean_cache.py,v 1.1 2011-02-15 20:23:52 barry409 Exp $

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

import argparse
import ConfigParser
import os
import sys
import urllib2
import fix_auth_failure
import lockss_daemon

__author__ = "Barry Hayes"
__maintainer__ = "Barry Hayes"
__version__ = "1.0.0"


class _SectionAdder(object):
    """Wrap a python configuration section around a file that doesn't
    have one."""
    def __init__(self, section, fp):
        self.section_done = False
        self.section = section
        self.fp = fp

    def readline(self):
        if not self.section_done:
            self.section_done = True
            return '[%s]' % self.section
        else:
            return self.fp.readline()


def _parser():
    """Make a parser for the arguments."""
    parser = argparse.ArgumentParser(
        description='Move cache directories on a LOCKSS daemon')
    parser.add_argument('-u', '--user', default='lockss-u')
    parser.add_argument('-p', '--password', default='lockss-p')
    parser.add_argument('-v', '--verbose', dest='verbose', action='store_true',
                        default=False)
    parser.add_argument('-q', '--quiet', dest='verbose', action='store_false')
    parser.add_argument('-f', '--force', dest='verify', action='store_false',
                        help='ignore auids not present on the daemon, '
                        'never prompt')
    parser.add_argument('-i', '--verify', dest='verify', action='store_true',
                        default=False, help='prompt before each move')
    parser.add_argument('--commands', action='store_true', default=False,
                        help='print mv commands, but do not move files')
    parser.add_argument('-d', '--directory', default='.',
                        help='the daemon directory where ./cache is '
                        '(default: \'.\')')
    parser.add_argument('--dest', default='deleted',
                        help='where under the daemon directory the cache '
                        'entries are moved to (default: \'deleted\')')
    return parser


def main():
    parser = _parser()
    arguments = parser.parse_args()

    src = arguments.directory
    local_txt = os.path.join(src, 'local.txt')
    if (not os.path.isdir(os.path.join(src, 'cache'))
        or not os.path.isfile(local_txt)):
        raise Exception('%s doesn\'t look like a daemon directory. '
                        'Try --directory.' % src)

    config = ConfigParser.ConfigParser()
    local_config = open(local_txt)
    config.readfp(_SectionAdder('foo', local_config))
    port = config.get('foo', 'org.lockss.ui.port')

    fix_auth_failure.fix_auth_failure()
    client = lockss_daemon.Client('127.0.0.1', port,
                                  arguments.user, arguments.password)
    repos = client._getStatusTable( 'RepositoryTable' )[ 1 ]
    deleted = [r for r in repos if r['status'] == 'Deleted']

    if arguments.verbose:
        if deleted:
            print 'These AUs have been deleted on the daemon:'
            for r in deleted:
                print r['plugin'], r['params']
        else:
            print 'No deleted AUs.'

    dst = os.path.join(arguments.directory, arguments.dest)
    for r in deleted:
        r = r['dir']
        if not arguments.verify or \
                arguments.verify and \
                raw_input('move %s [n]? ' % r).startswith('y'):
            src_r = os.path.join(src, r)
            dst_r = os.path.join(dst, r)
            if arguments.commands:
                print "mv %s %s" % (src_r, dst_r)
            else:
                os.renames(src_r, dst_r)


if __name__ == '__main__':
    main()
