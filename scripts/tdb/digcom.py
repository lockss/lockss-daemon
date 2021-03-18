#!/usr/bin/env python2

# $Id$

#
# Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
#

__version__ = '0.2.2'

import optparse
import re
import sys
import urllib2

# Simple class to carry around option values from command line
class Options(object):
  def __init__(self):
    super(Options, self).__init__()
    # Initialize all fields that may be needed later here
    self.__output = None
    self.__tdb_title = None
  # Getters and setters
  def set_output(self, output): self.__output = output
  def get_output(self): return self.__output
  def set_tdb_title(self, tdb_title): self.__tdb_title = tdb_title
  def get_tdb_title(self): return self.__tdb_title

# An abstract class for output producers
class Output(object):
  # Output types
  LOCKSS = 'lockss'
  METAARCHIVE = 'metaarchive'
  def __init__(self): super(Output, self).__init__()
  # Produces output for a series that has the given headings
  def series_output(self, options, base_url, collection_type, collection, headings): raise NotImplementedError
  # Produces output for an event community with the given theme
  def event_community_output(self, options, base_url, collection, collection_theme): raise NotImplementedError

# A LOCKSS output producer
class LockssOutput(Output):

  def __init__(self):
    super(LockssOutput, self).__init__()

  def series_output(self, options, base_url, collection_type, collection, headings):
    if options.get_tdb_title(): self.tdb_series_output(options, base_url, collection_type, collection, headings)
    else: self.simple_series_output(options, base_url, collection_type, collection, headings)

  # Simply lists the headings in the series
  def simple_series_output(self, options, base_url, collection_type, collection, headings):
    for heading in headings: print heading

  # Produces a TDB fragment for the series
  def tdb_series_output(self, options, base_url, collection_type, collection, headings):
    title = options.get_tdb_title()
    print '''\
  {

    title <
      name = %s
    >

    plugin = org.lockss.plugin.bepress.DigitalCommonsRepositoryPlugin
    param[base_url] = %s
    param[collection_type] = %s
    param[collection] = %s
    implicit < status ; year ; name ; param[collection_heading] >
''' % (title, base_url, collection_type, collection)
    for heading in headings:
      print '''\
    au < manifest ; %s ; %s %s ; %s >''' % (heading, title, heading, heading)
    print '''\

  }
'''
  def event_community_output(self, options, base_url, collection, collection_theme):
    title = options.get_tdb_title()
    if title is None: error('for an event community, --tdb-title is required')
    print '''\
  {

    title <
      name = %s
    >

    plugin = org.lockss.plugin.bepress.DigitalCommonsRepositoryThemePlugin
    param[base_url] = %s
    param[collection] = %s
    implicit < status ; name ; param[collection_theme] >  

    au < manifest ; %s %s ; %s >

  }
''' % (title, base_url, collection, title, collection_theme, collection_theme)


# A MetaArchive output producer
class MetaArchiveOutput(Output):

  def __init__(self):
    super(MetaArchiveOutput, self).__init__()

  def series_output(self, options, base_url, collection_type, collection, headings):
    blank_line = False
    for heading in headings:
      if blank_line: print
      else: blank_line = True
      print '''\
plugin=org.lockss.plugin.bepress.DigitalCommonsRepositoryPlugin
base_url=%s
collection_type=%s
collection=%s
collection_heading=%s''' % (base_url, collection_type, collection, heading)

  def event_community_output(self, options, base_url, collection, collection_theme):
    print '''\
plugin=org.lockss.plugin.bepress.DigitalCommonsRepositoryThemePlugin
base_url=%s
collection=%s
collection_theme=%s''' % (base_url, collection, collection_theme)

def do_series(options, base_url, collection_type, collection):
  headings = []
  i = 1
  index_url = '%s%s/' % (base_url, collection)
  while index_url is not None:
    r = urllib2.urlopen(index_url)
    index_url = None
    i = i + 1
    for line in r.readlines():
      for y in re.findall(r'class="lockss_([^"]+)"', line):
        if y not in headings: headings.append(y)
      mat = re.search(r'href="((' + base_url + collection + r'/)?index\.' + str(i) + r'\.html)"', line)
      if mat is not None: index_url = mat.group(1)
  headings.sort()
  options.get_output().series_output(options, base_url, collection_type, collection, headings)

def do_event_community(options, base_url, collection, collection_theme):
  options.get_output().event_community_output(options, base_url, collection, collection_theme)

# Builds command line option parser
def make_parser():
  parser = optparse.OptionParser(version=__version__, usage='%prog {--lockss|--metaarchive} [--tdb-title=TITLE] MANIFPAGEURL')
  group = optparse.OptionGroup(parser, 'Output mode')
  group.add_option('--lockss', '-l', help='LOCKSS output mode', dest='output', action='store_const', const=Output.LOCKSS)
  group.add_option('--metaarchive', '-m', help='MetaArchive output mode', dest='output', action='store_const', const=Output.METAARCHIVE)
  parser.add_option_group(group)
  group = optparse.OptionGroup(parser, 'LOCKSS options')
  group.add_option('--tdb-title', metavar='TITLE', help='output TDB fragment for title TITLE', action='store')
  parser.add_option_group(group)
  return parser

# Interprets and validates results of parsing command line
def process_options(parser, opts, args):
  options = Options()
  if opts.output == Output.LOCKSS: options.set_output(LockssOutput())
  elif opts.output == Output.METAARCHIVE: options.set_output(MetaArchiveOutput())
  else: parser.error('output mode is required: --lockss/-l or --metaarchive/-m')
  if len(args) == 0: parser.error('no manifest page URL specified')
  if len(args) > 1: parser.error('expected one manifest page URL, got %d' % (len(args),))
  if opts.tdb_title: options.set_tdb_title(opts.tdb_title)
  return options

# Displays 'error: ' plus given message (or 'error' if None), then exits
def error(msg):
  if msg is None: sys.stderr.write('error\n')
  else: sys.stderr.write('error: %s\n' % (msg,))
  sys.exit(1)

# Main entry point
if __name__ == '__main__':
  parser = make_parser()
  (opts, args) = parser.parse_args()
  options = process_options(parser, opts, args)
  url = args[0]
  mat = re.match(r'(https?://[^/]+/)([^/]+)/lockss-(ir_(series|etd|gallery|book))-\2\.html', url)
  if mat is not None:
    do_series(options, mat.group(1), mat.group(3), mat.group(2))
    sys.exit()
  mat = re.match(r'(https?://[^/]+/)([^/]+)/lockss-theme-(.*)\.html', url)
  if mat is not None:
    do_event_community(options, mat.group(1), mat.group(2), mat.group(3))
    sys.exit()
  error('unknown URL type')


