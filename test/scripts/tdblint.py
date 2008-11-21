#!/usr/bin/python

# $Id: tdblint.py,v 1.1 2008-11-21 01:39:34 thib_gc Exp $
#
# Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

from tdbconst import *

def tdb_lint(tdb, options):
    valid = True
    # Check that each AU has a name and a recognized status
    for au in tdb.aus():
        if au.name() is None or au.name() == '':
            print 'AU with no name'
        if au.status() is None or au.status() == '':
            valid = False
            print 'AU with no status: %s' % ( au.name(), )
        elif au.status() not in TDB_STATUSES:
            valid = False
            print 'AU with unrecognized status: %s [%s]' % ( au.name(), au.status() )
    if not valid:
        if options.lintforgive: return
        import sys
        sys.exit('The input is invalid')
