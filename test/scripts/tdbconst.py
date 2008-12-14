#!/usr/bin/python

# $Id: tdbconst.py,v 1.4 2008-12-14 06:46:46 thib_gc Exp $
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

TDB_OPTION_LINT          = 'lint'
TDB_OPTION_LINT_FORGIVE  = 'lintforgive'
TDB_LINT_DEFAULT         = False
TDB_LINT_FORGIVE_DEFAULT = False

TDB_STATUS_DOES_NOT_EXIST = 'does_not_exist'
TDB_STATUS_DO_NOT_PROCESS = 'do_not_process'
TDB_STATUS_EXISTS         = 'exists'
TDB_STATUS_MANIFEST       = 'manifest'
TDB_STATUS_TESTING        = 'testing'
TDB_STATUS_NOT_READY      = 'not_ready'
TDB_STATUS_READY          = 'ready'
TDB_STATUS_PRE_RELEASING  = 'pre_releasing'
TDB_STATUS_PRE_RELEASED   = 'pre_released'
TDB_STATUS_RELEASING      = 'releasing'
TDB_STATUS_RELEASED       = 'released'
TDB_STATUS_DOWN           = 'down'
TDB_STATUS_SUPERSEDED     = 'superseded'
TDB_STATUS_RETRACTED      = 'retracted'
TDB_STATUSES              = [TDB_STATUS_DOES_NOT_EXIST,
                             TDB_STATUS_DO_NOT_PROCESS,
                             TDB_STATUS_EXISTS,
                             TDB_STATUS_MANIFEST,
                             TDB_STATUS_TESTING,
                             TDB_STATUS_NOT_READY,
                             TDB_STATUS_READY,
                             TDB_STATUS_PRE_RELEASING,
                             TDB_STATUS_PRE_RELEASED,
                             TDB_STATUS_RELEASING,
                             TDB_STATUS_RELEASED,
                             TDB_STATUS_DOWN,
                             TDB_STATUS_SUPERSEDED,
                             TDB_STATUS_RETRACTED]

TDB_OPTION_LEVEL                   = 'level'
TDB_LEVEL_CONTENT_TESTING          = 'content_testing'
TDB_LEVEL_CONTENT_TESTING_STATUSES = [TDB_STATUS_EXISTS,
                                      TDB_STATUS_MANIFEST,
                                      TDB_STATUS_TESTING,
                                      TDB_STATUS_NOT_READY,
                                      TDB_STATUS_READY,
                                      TDB_STATUS_PRE_RELEASING,
                                      TDB_STATUS_PRE_RELEASED,
                                      TDB_STATUS_RELEASING,
                                      TDB_STATUS_RELEASED,
                                      TDB_STATUS_DOWN]
TDB_LEVEL_EVERYTHING               = 'everything'
TDB_LEVEL_EVERYTHING_STATUSES      = TDB_STATUSES[:]
TDB_LEVEL_PRODUCTION               = 'production'
TDB_LEVEL_PRODUCTION_STATUSES      = [TDB_STATUS_RELEASED,
                                      TDB_STATUS_DOWN,
                                      TDB_STATUS_SUPERSEDED]
TDB_LEVEL_DEFAULT                  = [TDB_LEVEL_PRODUCTION]
TDB_LEVELS                         = TDB_STATUSES[:].extend([TDB_LEVEL_CONTENT_TESTING,
                                                             TDB_LEVEL_EVERYTHING,
                                                             TDB_LEVEL_PRODUCTION])

TDB_OPTION_STYLE      = 'style'
TDB_STYLE_NONE        = 'none'
TDB_STYLE_XML         = 'xml'
TDB_STYLE_XML_ENTRIES = 'xml_entries'
TDB_STYLE_XML_LEGACY  = 'xml_legacy'
TDB_STYLES            = [TDB_STYLE_NONE,
                         TDB_STYLE_XML,
                         TDB_STYLE_XML_ENTRIES,
                         TDB_STYLE_XML_LEGACY]
TDB_STYLE_DEFAULT     = TDB_STYLE_NONE
