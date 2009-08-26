#!/usr/bin/python

# $Id: tdbconst.py,v 1.9 2009-08-26 09:35:43 thib_gc Exp $
#
# Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

from tdb import *

TDB_OPTION_LEVEL                   = 'level'
TDB_LEVEL_CONTENT_TESTING          = 'contentTesting'
TDB_LEVEL_CONTENT_TESTING_STATUSES = [ AU.STATUS_EXISTS,
                                       AU.STATUS_MANIFEST,
                                       AU.STATUS_WANTED,
                                       AU.STATUS_TESTING,
                                       AU.STATUS_NOT_READY,
                                       AU.STATUS_READY,
                                       AU.STATUS_PRE_RELEASING,
                                       AU.STATUS_PRE_RELEASED,
                                       AU.STATUS_RELEASING,
                                       AU.STATUS_RELEASED,
                                       AU.STATUS_DOWN ]
TDB_LEVEL_EVERYTHING               = 'everything'
TDB_LEVEL_EVERYTHING_STATUSES      = AU.STATUSES[:]
TDB_LEVEL_PRODUCTION               = 'production'
TDB_LEVEL_PRODUCTION_STATUSES      = [ AU.STATUS_RELEASED,
                                       AU.STATUS_DOWN,
                                       AU.STATUS_SUPERSEDED ]
TDB_LEVEL_DEFAULT                  = [ TDB_LEVEL_PRODUCTION ]
TDB_LEVELS                         = AU.STATUSES[:] + [ TDB_LEVEL_CONTENT_TESTING,
                                                        TDB_LEVEL_EVERYTHING,
                                                        TDB_LEVEL_PRODUCTION ]

TDB_OPTION_STYLE      = 'style'
TDB_STYLE_NONE        = 'none'
TDB_STYLE_XML         = 'xml'
TDB_STYLE_XML_ENTRIES = 'xmlEntries'
TDB_STYLE_XML_LEGACY  = 'xmlLegacy'
TDB_STYLES            = [ TDB_STYLE_NONE,
                          TDB_STYLE_XML,
                          TDB_STYLE_XML_ENTRIES,
                          TDB_STYLE_XML_LEGACY ]
TDB_STYLE_DEFAULT     = TDB_STYLE_NONE
