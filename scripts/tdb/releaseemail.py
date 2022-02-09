#!/usr/bin/env python3

# Input:
# A tab-separated dump of the title database, with the columns being:
# publisher, title, name, status, plugin

import re
import sys

# Indices of the various columns in the tab-separated input
PUBLISHER = 0
TITLE = 1
NAME = 2
STATUS = 3
PLUGIN = 4

# Sets of statuses
RELEASING = set(['releasing'])
RELEASED = set(['released', 'down', 'superseded', 'zapped'])

# Set of non-Alliance plugins
NON_ALLIANCE = set([ \
                 "edu.columbia.plugin.JiwsPlugin", # humanities_project
                 "edu.cornell.library.epr.EPRPlugin2001", # humanities_project
                 "edu.cornell.library.epr.EPRPlugin2002On", # humanities_project
                 "edu.cornell.library.epr.EPRPlugin", # humanities_project
                 "edu.cornell.library.jbe.JBEPlugin", # humanities_project
                 "edu.fcla.plugin.arkivoc.ArkivocPlugin", # prod
                 "edu.fcla.plugin.arkivoc.Arkivoc2022Plugin", # prod
                 "edu.harvard.plugin.AppliedSemiotics.AppliedSemioticsPlugin", # humanities_project
                 "edu.harvard.plugin.AppliedSemiotics.AppliedSemiotics2022Plugin", # humanities_project
                 "edu.harvard.plugin.jrs.JRSPlugin", # humanities_project
                 "edu.harvard.plugin.jrs.JRS2022Plugin", # humanities_project
                 "edu.harvard.plugin.WorldHaikuReview.WorldHaikuReviewPlugin", # humanities_project
                 "edu.indiana.lib.plugin.jcjpc.JcjpcPlugin", # humanities_project
                 "edu.indiana.lib.plugin.mto.MTOPlugin", # humanities_project
                 "edu.indiana.lib.plugin.mto.MTO2022Plugin", # humanities_project
                 "edu.jhu.library.plugin.jrf.JournalOfReligionAndFilmPlugin", # humanities_project
                 "edu.jhu.library.plugin.MedievalForumPlugin", # humanities_project
                 "edu.nyu.plugin.bonefolder.BonefolderPlugin", # humanities_project
                 "edu.nyu.plugin.ejce.EJCEPlugin",
                 "edu.nyu.plugin.ejcjs.EJCJSPlugin",
                 "edu.nyu.plugin.heplwebzine.HEPLwebzine",
                 "edu.nyu.plugin.journalofglobalbuddhism.JournalOfGlobalBuddhismPlugin",
                 "edu.nyu.plugin.LeedsICSPlugin",
                 "edu.princeton.plugin.bmcr.BMCRPlugin",
                 "edu.princeton.plugin.ncaw.19thCenturyArtWorldwidePlugin",
                 "edu.stanford.plugin.exquisitecorpse.ExquisiteCorpsePlugin",
                 "edu.upenn.library.plugin.annualofurdustudies.AnnualOfUrduStudiesPlugin",
                 "edu.upenn.library.plugin.clcweb.CLCWebPlugin",
                 "edu.wisc.library.plugin.BigBridgePlugin",
                 "edu.wisc.library.plugin.BigBridgeVol1Plugin",
                 "edu.wisc.library.plugin.CortlandReviewPlugin",
                 "edu.wisc.library.plugin.CortlandReview00Plugin",
                 "edu.wisc.library.plugin.CortlandReview98Plugin",
                 "edu.wisc.library.plugin.CortlandReview99Plugin",
                 "edu.yale.library.lockss.plugin.intermarium.IntermariumPlugin",
                 "edu.yale.library.lockss.plugin.mitejmes.MITEJMESPlugin",
                 "gov.gpo.access.permanent.plugin.amberwaves.Amber_WavesPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.environmentalhealthperspectives.Environmental_Health_PerspectivesPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.fbilawenforcementbulletin.FBI_Law_Enforcement_BulletinPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.humanities.HumanitiesPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.monthlyenergyreview.MonthlyEnergyReviewPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.monthlylaborreview.MonthlyLaborReviewPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.nistjournalofresearch.NISTJournalOfResearchPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.socialsecuritybulletin.SocialSecurityBulletinPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.surveyofcurrentbusiness.SurveyOfCurrentBusinessPlugin", # gpo-pilot
                 "gov.gpo.access.permanent.plugin.treasurybulletin.TreasuryBulletinPlugin", # gpo-pilot
                 "gov.loc.plugin.CJPentecostalCharismaticResearchPlugin",
                 "gov.loc.plugin.TESLEJPlugin",
                 "nz.ac.otago.plugin.scholia.ScholiaPlugin",
                 "org.lockss.plugin.absinthe.AbsinthePlugin",
                 "org.lockss.plugin.bepress.BePressPlugin",
                 "org.lockss.plugin.bioone.BioOnePlugin",
                 "org.lockss.plugin.blackbird.BlackbirdPlugin",
                 "org.lockss.plugin.clogic.CulturalLogicPlugin",
                 "org.lockss.plugin.disputatio.DisputatioPlugin",
                 "org.lockss.plugin.emc.EarlyModernCulturePlugin",
                 "org.lockss.plugin.emls.EmlsPlugin",
                 "org.lockss.plugin.etd.fsuplugin.FSUETDPlugin", # aserl-etd
                 "org.lockss.plugin.etd.gatechplugin.GATechETDPlugin", # aserl-etd
                 "org.lockss.plugin.etd.ncstateplugin.NCStateETDLegacyPlugin", # aserl-etd
                 "org.lockss.plugin.etd.ukyplugin.UKYETDPlugin", # aserl-etd
                 "org.lockss.plugin.etd.vanderbiltetdplugin.VanderbiltETDPlugin", # aserl-etd
                 "org.lockss.plugin.etd.vtetdplugin.VTETDLegacyPlugin", # aserl-etd
                 "org.lockss.plugin.evergreenreview.EvergreenReviewPlugin",
                 "org.lockss.plugin.GendersPlugin",
                 "org.lockss.plugin.histcoop.HistoryCooperativePlugin",
                 "org.lockss.plugin.invisibleculture.InvisibleCulturePlugin",
                 "org.lockss.plugin.jackmagazine.JackMagazinePlugin",
                 "org.lockss.plugin.jscm.JSCMPlugin",
                 "org.lockss.plugin.lapetitezine.LaPetiteZinePlugin",
                 "org.lockss.plugin.locksscard.LockssCardPlugin",
                 "org.lockss.plugin.madhattersreview.MadHattersReviewPlugin",
                 "org.lockss.plugin.minerva.MinervaPlugin",
                 "org.lockss.plugin.minerva.Minerva2020Plugin",
                 "org.lockss.plugin.msr.MSRPlugin",
                 "org.lockss.plugin.ojs.OJSPlugin",
                 "org.lockss.plugin.othervoices.OtherVoicesPlugin",
                 "org.lockss.plugin.projmuse.ProjectMusePlugin",
                 "org.lockss.plugin.prok.ProkPlugin",
                 "org.molvis.plugin.MolVisPlugin",
                 "org.lockss.plugin.sfpoetrybroadside.SantaFePoetryBroadsidePlugin",
                 "org.nypl.plugin.failbetter.FailbetterPlugin",
                 "org.nypl.plugin.PoetryBayPlugin",
                 "org.nypl.plugin.shampoo.ShampooPlugin",
                 "org.nypl.plugin.WordsWithoutBordersPlugin",
                 "za.ac.nlsa.lockss.plugin.WaterSAPlugin"
])

def nonRepeated(seq):
  '''Makes a new sequence in the same order as the input but with no
  items repeated consecutively.'''
  res = []
  x0 = None
  for x in seq:
    if x != x0:
      res.append(x)
      x0 = x
  return res

def isNotIn(seq, eliminating):
  '''Returns a sequence of elements of 'seq' that are not in
  'eliminating'.'''
  return [x for x in seq if x not in set(eliminating)]

def prettyInt(i):
  '''Given an integer between 0 and 999,999, returns a string version
  with a comma in the thousands position if needed.'''
  if i < 1000: return str(i)
  return str(i # 1000) + ',' + str(i % 1000).rjust(3, '0')

def wordInt(i):
  '''Given a positive integer, returns a string spelling out the
  number (e.g. 'twanty five') up to 99, else a string version of the
  number as returned by prettyInt() (e.g. '123', '1,234').'''
  if i >= 100: return prettyInt(i)
  s = ''
  if i >= 20:
    s = {2:'twenty', 3:'thirty', 4:'forty', 5:'fifty', 6:'sixty', 7:'seventy', 8:'eighty', 9:'ninety'}[i # 10]
    if i % 10 == 0: return s
    s = s + ' '
    i = i % 10
  s = s + {1:'one', 2:'two', 3:'three', 4:'four', 5:'five', 6:'six', 7:'seven', 8:'eight', 9:'nine',
           10:'ten', 11:'eleven', 12:'twelve', 13:'thirteen', 14:'fourteen', 15:'fifteen', 16:'sixteen', 17:'seventeen', 18:'eighteen', 19:'nineteen'}[i]
  return s

def plural(i, sing, plur):
  '''Picks between a singular phrase and a plural phrase, i.e.  if 'i'
  is 1 return 'sing', otherwise return 'plur'.'''
  if i == 1: return sing
  else: return plur

def enumerateCommaAnd(lst):
  '''Enumerates the items of 'lst' for a sentence using commas and 'and'
  between the last two. This is the non-Oxford way. Examples: ['foo'] ->
  'foo'; ['foo', 'bar'] -> 'foo and bar'; ['foo', 'bar', 'baz'] ->
  'foo, bar and baz'. The items of 'lst' are assumed to be strings.'''
  if len(lst) == 1: return lst[0]
  else: return ', '.join(lst[0:-1]) + ' and ' + lst[-1]

# The crux of this script is in the next two functions. They're likely
# likely to require more smarts as edge cases come up.

def guessVolume(st, title=None):
  '''Attempts to guess the variable "volume" part of an AU name, and
  returns it (as a string). If given an optional 'title', uses it to do
  a prefix match. Skips over suffixes like parenthesized years (e.g.
  ' (2001)') and bracketed suffixes (e.g. ' [superseded]').

  'Journal of Foo Volume Anything' -> 'Anything'
  'Journal of Foo Volume Anything (1234)' -> 'Anything'
  'Journal of Foo Volume Anything [something]' -> 'Anything'
  'Journal of Foo Volume Anything (1234) [superseded]' -> 'Anything'
  'Journal of Foo 1234' -> '1234'
  'Journal of Foo 1234 [something]' -> '1234'
'''
  mat = re.search(r'^(.*) Volume ([^ ]+)( \(\d{4}\))?( \[[^]]+\])?$', st)
  if mat: return mat.group(2)
  mat = re.search(r'^(.*) (\d{4})( \[[^]]+\])?$', st)
  if mat: return mat.group(2)
  # 
  return st

def buildIntervals(aus):
  '''Builds a list of tuples of publisher name and (list of tuples of
  title name and (list of tuple of contiguous lower volume number and
  upper volume number)) from the list of AUs. In other words, this is a
  tree structure encoded as nested lists.'''
  lst = []
  for curAu in reversed(aus):
    vol = guessVolume(curAu[NAME])
    if len(lst) == 0 or lst[0][0] != curAu[PUBLISHER]: lst.insert(0, (curAu[PUBLISHER], [(curAu[TITLE], [(vol, vol)])]))
    elif lst[0][1][0][0] != curAu[TITLE]: lst[0][1].insert(0, (curAu[TITLE], [(vol, vol)]))
    elif not (vol.isdigit() and lst[0][1][0][1][0][0].isdigit()): lst[0][1][0][1].insert(0, (vol, vol))
    else:
      x, y = int(vol), int(lst[0][1][0][1][0][0])
      if x + 1 == y: lst[0][1][0][1][0] = (vol, lst[0][1][0][1][0][1])
      else: lst[0][1][0][1].insert(0, (vol, vol))
  return lst

def breakdownByPublisher(s, lst, includeNewTitleTag=False):
  '''Appends to a string 's' how many AUs by each publisher there are,
  what these AUs are by title, if possible in ranges, optionally with a
  notation for new titles if 'includeNewTitleTag' is True, based on a
  structure 'lst' returned by 'buildIntervals'. Returns the longer
  string (the reference 's' is not modified).'''
  for p1, lot1 in lst:
    lp1 = len([x for x in ausReleasing if x[PUBLISHER] == p1])
    s = s + '%s archival %s from %s:\n\n' % (wordInt(lp1).capitalize(), plural(lp1, 'unit is', 'units are'), p1)
    for t1, lor1 in lot1:
      s = s + ' * %s (' % (t1,)
      if len(lor1) == 1 and lor1[0][0] == lor1[0][1]: s = s + 'Volume '
      else: s = s + 'Volumes '
      lohi = []
      for lo, hi in lor1:
        if lo == hi: lohi.append(lo)
        else: lohi.append('%s-%s' % (lo, hi))
      s = s + ', '.join(lohi) + ')'
      if includeNewTitleTag and t1 in titlesNew: s = s + ' [new title]'
      s = s + '\n'
    s = s + '\n'
  return s

def generateGenericEmail(lst):
  '''Generates the text of a generic release e-mail.'''
  s = '''\

Dear colleagues,

We are pleased to announce that %s additional archival %s now available for preservation in the network.

''' % (wordInt(len(ausReleasing)), plural(len(ausReleasing), 'unit is', 'units are'))

  s = breakdownByPublisher(s, lst)

  s = s + '''Please add these AUs to your LOCKSS box by logging in to the Web-based user interface and selecting Journal Configuration, then Add AUs.

If you encounter any problem in the process or have any question, do not reply to this message; instead please send a help request to lockss-support@lockss.org for assistance.

As always, thank you for working with us.

LOCKSS Team
Website: http://www.lockss.org/
Help: lockss-support (at) lockss (dot) org'''

  print(s)

def generateGlnEmail(lst):
  '''Generates the text of a typical GLN release e-mail.'''
  s = '''\

Dear LOCKSS Alliance and LOCKSS system participants,

We are pleased to announce that %s additional archival %s now available for preservation in the Global LOCKSS Network (GLN)'''\
    % (wordInt(len(ausReleasing)), plural(len(ausReleasing), 'unit is', 'units are'))

  if len(publishersNew) > 0 or len(titlesNew) > 0:
    s = s + ', including archival units from '
    if len(publishersNew) > 0:
      s = s + '%s' % (plural(len(publishersNew), 'one new publisher', '%d new publishers' % (len(publishersNew),)))
    if len(titlesNew) > 0:
      if len(publishersNew) > 0: s = s + ' and '
      s = s + '%s' % (plural(len(titlesNew), 'one new title', '%d new titles' % (len(titlesNew),)))
  
  s = s + ':\n\n'

  for p1 in publishersReleasing:
    lp1 = len([x for x in ausReleasing if x[PUBLISHER] == p1])
    s = s + ' * %s (%d archival %s)' % (p1, lp1, plural(lp1, 'unit', 'units'))
    if p1 in publishersNew: s = s + ' [new publisher]'
    s = s + '\n'

  s = s + '\n\n\n'

  s = breakdownByPublisher(s, lst, True)

  s = s + '\n\n'

  if len(publishersNonAlliance) > 0:
    s = s + 'The ' + enumerateCommaAnd(publishersNonAlliance) + ' archival units are available for preservation in all LOCKSS boxes.'
  if len(publishersAlliance) > 0:
    if len(publishersNonAlliance) > 0: s = s + ' '
    s = s + 'The ' + enumerateCommaAnd(publishersAlliance) + ' archival units are available for preservation by all LOCKSS Alliance members.'

  s = s + '''

See the instructions for how to select these archival units for preservation in your LOCKSS box below.

If you encounter any problem in the process or have any question, do not reply to this message; instead please send a help request to lockss-support@lockss.org for assistance.

As always, thank you for working with us.

LOCKSS Team
Website: http://www.lockss.org/
Help: lockss-support (at) lockss (dot) org



*** INSTRUCTIONS TO SELECT ARCHIVAL UNITS FOR PRESERVATION ***

STEP 1: ACCESS THE WEB USER INTERFACE

Access the Web user interface of your LOCKSS box, for instance http://lockssbox.university.edu:8081/ (replace lockssbox.university.edu with the host name of the LOCKSS box at your institution), then click "Journal Configuration", then "Add AUs".

STEP 2: SELECT GROUPS OF ARCHIVAL UNITS

Select the groups of archival units you want to see in step 3, such as "All Oxford University Press AUs", then click the "Select AUs" button.

STEP 3: SELECT ARCHIVAL UNITS

Select the archival units you wish to add to your LOCKSS box. (If your machine has several disks, we advise you to select all those with ample available space at the top of the screen so that AUs will be distributed across these disks when you click the "Select All" button.) When you are done, click the "Add Selected AUs" button.


'''

  print(s)

###
### Main
###

# Read AUs from stdin
ausAll = [l.rstrip().split('\t') for l in sys.stdin.readlines()]

# Compute sets
ausReleasing = [x for x in ausAll if x[STATUS] in RELEASING]
ausReleased = [x for x in ausAll if x[STATUS] in RELEASED]
titlesReleasing = nonRepeated([x[TITLE] for x in ausReleasing])
titlesReleased = nonRepeated([x[TITLE] for x in ausReleased])
titlesNew = isNotIn(titlesReleasing, titlesReleased)
publishersReleasing = nonRepeated([x[PUBLISHER] for x in ausReleasing])
publishersReleased = nonRepeated([x[PUBLISHER] for x in ausReleased])
publishersNew = isNotIn(publishersReleasing, publishersReleased)
publishersNonAlliance = nonRepeated([x[PUBLISHER] for x in [x for x in ausReleasing if x[PLUGIN] in NON_ALLIANCE]])
publishersAlliance = nonRepeated([x[PUBLISHER] for x in [x for x in ausReleasing if x[PLUGIN] not in NON_ALLIANCE]])

# Build range structure
lst = buildIntervals(ausReleasing)

# Parse command line switches and dispatch accordingly
if len(sys.argv) == 1:
  print('''\
Usage: %s [-g|--gln|-p|--pln|--by-name-only|--by-publisher-only]
Input: tab-separated: publisher,title,name,status,plugin''' % (sys.argv[0],))
elif sys.argv[1] in ['-g', '--gln']: generateGlnEmail(lst)
elif sys.argv[1] in ['-p', '--pln']: generateGenericEmail(lst)
elif sys.argv[1] in ['--by-name-only']: print('\n'.join([x for x in breakdownByPublisher('', lst).split('\n') if x.startswith(' * ')]))
elif sys.argv[1] in ['--by-publisher-only']: print(breakdownByPublisher('', lst))

