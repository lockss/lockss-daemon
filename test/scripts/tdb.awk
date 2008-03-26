#!/usr/bin/awk -f

#######
# BEGIN
#######

BEGIN {
 # Boolean
 false = 0;
 true = 1;

 # Directives
 DIRECTIVE_FIXED_VALUES        = "@fixedValues"
 DIRECTIVE_FORMAT_STRING       = "@formatString"
 DIRECTIVE_NAME_PREFIX         = "@namePrefix"
 DIRECTIVE_PUBLISHER_TITLE_SET = "@publisherTitleSet"
 DIRECTIVE_PUBLISHING_PLATFORM = "@publishingPlatform"

 # Format codes
 CODE_ATTRIBUTE      = "@"
 CODE_CODEN          = "c"
 CODE_ESTIMATED_SIZE = "e"
 CODE_ISSN           = "i"
 CODE_JOURNAL_TITLE  = "j"
 CODE_OCLC           = "o"
 CODE_PARAMETER      = "%"
 CODE_PLUGIN         = "p"
 CODE_PUBLISHER      = "P"
 CODE_RIGHTS         = "R"
 CODE_SKIP_1         = "x"
 CODE_SKIP_2         = "X"
 CODE_STATUS         = "S"
 CODE_TITLE          = "t"

 # Archival unit statuses
 STATUS_DOES_NOT_EXIST = "does_not_exist"
 STATUS_DO_NOT_PROCESS = "do_not_process"
 STATUS_EXISTS         = "exists"
 STATUS_MANIFEST       = "manifest"
 STATUS_TESTING        = "testing"
 STATUS_READY          = "ready"
 STATUS_PRE_RELEASED   = "pre_released"
 STATUS_RELEASED       = "released"
 STATUS_DOWN           = "down"
 STATUS_SUPERSEDED     = "superseded"
 STATUS_RETRACTED      = "retracted"

 # Output levels and corresponding archival unit statuses
 LEVEL_PRODUCTION              = "production"
 STATUSES_PRODUCTION           = STATUS_RETRACTED "," STATUS_SUPERSEDED "," STATUS_DOWN "," STATUS_RELEASED
 LEVEL_CONTENT_TESTING         = "contentTesting"
 STATUSES_CONTENT_TESTING      = STATUSES_PRODUCTION "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST "," STATUS_EXISTS
 LEVEL_DANGEROUS               = "dangerous"
 STATUSES_DANGEROUS            = STATUSES_CONTENT_TESTING "," STATUS_DO_NOT_PROCESS
 LEVEL_VERY_DANGEROUS          = "veryDangerous"
 STATUSES_VERY_DANGEROUS       = STATUSES_DANGEROUS "," STATUS_DOES_NOT_EXIST
 LEVEL_PRE_RELEASE             = "preRelease"
 STATUSES_PRE_RELEASE          = STATUS_PRE_RELEASED
 LEVEL_CONSERVATIVE_TESTING    = "conservativeTesting"
 STATUSES_CONSERVATIVE_TESTING = STATUS_RELEASED "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST
 LEVEL_DEFAULT                 = LEVEL_PRODUCTION

 # Output styles
 STYLE_MEDIAWIKI_PUBLISHER        = "mediawikiPublisher"
 STYLE_MEDIAWIKI_PUBLISHER_LIST   = "mediawikiPublisherList"
 STYLE_MEDIAWIKI_PUBLISHER_LIST_2 = "mediawikiPublisherList2"
 STYLE_XML                        = "xml"
 STYLE_XML_ENTRIES                = "xmlEntries"
 STYLE_XML_LEGACY                 = "xmlLegacy"
 STYLE_DEFAULT                    = STYLE_XML_ENTRIES

 # Publishing platforms
 PLATFORM_BIOONE         = "BioOne"
 PLATFORM_HIGHWIRE_PRESS = "HighWire Press"
 PLATFORM_PROJECT_MUSE   = "Project Muse"

 # Exit codes
 EXIT_NO_FORMAT_STRING = 11

 # Initial setup
 FS="\t"
 parseCommandLine()

 # Preamble
 if (outputStyle == STYLE_XML || outputStyle == STYLE_XML_LEGACY) { preambleXml() }
}

###########
# FUNCTIONS
###########

#
# parseCommandLine
#
function parseCommandLine() {
 # Defaults
 if (outputStyle == "") { outputStyle = STYLE_DEFAULT }
 if (outputLevel == "") { outputLevel = LEVEL_DEFAULT }

 # Parse
 parseOutputLevel()
}

#
# parseOutputLevel
#
function parseOutputLevel(        _splitOutputLevel, _i, _statuses, _splitStatuses, _j) {
 # Reset output level information
 delete outputLevels

 # For each output level...
 split(outputLevel, _splitOutputLevel, ",")
 for (_i in _splitOutputLevel) {
  # Synonyms
  if (_splitOutputLevel[_i] == LEVEL_PRODUCTION) { _statuses = STATUSES_PRODUCTION }
  else if (_splitOutputLevel[_i] == LEVEL_PRE_RELEASE) { _statuses = STATUSES_PRE_RELEASE }
  else if (_splitOutputLevel[_i] == LEVEL_CONTENT_TESTING) { _statuses = STATUSES_CONTENT_TESTING }
  else if (_splitOutputLevel[_i] == LEVEL_CONSERVATIVE_TESTING) { _statuses = STATUSES_CONSERVATIVE_TESTING }
  else if (_splitOutputLevel[_i] == LEVEL_DANGEROUS) { _statuses = STATUSES_DANGEROUS }
  else if (_splitOutputLevel[_i] == LEVEL_VERY_DANGEROUS) { _statuses = STATUSES_VERY_DANGEROUS }
  else { _statuses = _splitOutputLevel[_i] } # Not a synonym
  # For each status...
  split(_statuses, _splitStatuses, ",")
  for (_j in _splitStatuses) { outputLevels[_splitStatuses[_j]] = true }
 }
}

#
# parseFormatString
#
function parseFormatString(        _splitFormatString, _numFields, _field, _code) {
 # Reset format information
 delete indexOf
 numAdditionalParameters = 0
 delete indexAdditionalAttributes
 numParameters = 0
 delete indexParameters
 delete fixedValue

 # For each format code...
 _numFields = split(formatString, _splitFormatString, ",")
 for (_field = 1 ; _field <= _numFields ; ++_field) {
  _code = _splitFormatString[_field]
  if (_code == CODE_SKIP_1 || _code == CODE_SKIP_2) { continue }
  else if (_code == CODE_CODEN || _code == CODE_ESTIMATED_SIZE || _code == CODE_ISSN || _code == CODE_JOURNAL_TITLE || _code == CODE_OCLC || _code == CODE_PLUGIN || _code == CODE_PUBLISHER || _code == CODE_RIGHTS || _code == CODE_STATUS || _code == CODE_TITLE) { indexOf[_code] = _field }
  else if (_code == CODE_ATTRIBUTE) { indexAdditionalAttributes[++numAdditionalAttributes] = _field }
  else if (_code == CODE_PARAMETER) { indexParameters[++numParameters] = _field }
  else { warning(DIRECTIVE_FORMAT_STRING ": unknown code: " _code) }
 }
}

#
# parseFixedValues
#
function parseFixedValues(        _i, _code) {
 for (_i = 3 ; _i <= NF ; ++_i) {
  _code = getKey($_i)
  if (_code == "") { continue }
  if (_code == CODE_CODEN || _code == CODE_ISSN || _code == CODE_JOURNAL_TITLE || _code == CODE_OCLC || _code == CODE_PLUGIN || _code == CODE_PUBLISHER || _code == CODE_RIGHTS) {
   indexOf[_code] = -1
   fixedValue[_code] = getValue($_i)
  }
  else { warning(DIRECTIVE_FIXED_VALUES ": illegal code: " _code) }
 }
}

#
# preambleXml
#
function preambleXml() {
 xmlInOrgLockssTitle = false;

 printf   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
 printf   "<!DOCTYPE lockss-config [\n"
 printf   "<!ELEMENT lockss-config (if|property)+>\n"
 printf   "<!ELEMENT property (property|list|value|if)*>\n"
 printf   "<!ELEMENT list (value)+>\n"
 printf   "<!ELEMENT value (#PCDATA)>\n"
 printf   "<!ELEMENT test EMPTY>\n"
 printf   "<!ELEMENT and (and|or|not|test)*>\n"
 printf   "<!ELEMENT or (and|or|not|test)*>\n"
 printf   "<!ELEMENT not (and|or|not|test)*>\n"
 printf   "<!ELEMENT if (and|or|not|then|else|test|property)*>\n"
 printf   "<!ELEMENT then (if|property)*>\n"
 printf   "<!ELEMENT else (if|property)*>\n"
 printf   "<!ATTLIST property name CDATA #REQUIRED>\n"
 printf   "<!ATTLIST property value CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test hostname CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test group CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test daemonVersionMin CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test daemonVersionMax CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test daemonVersion CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test platformVersionMin CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test platformVersionMax CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test platformVersion CDATA #IMPLIED>\n"
 printf   "<!ATTLIST test platformName CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if hostname CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if group CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if daemonVersionMin CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if daemonVersionMax CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if daemonVersion CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if platformVersionMin CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if platformVersionMax CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if platformVersion CDATA #IMPLIED>\n"
 printf   "<!ATTLIST if platformName CDATA #IMPLIED>\n"
 printf   "<!ATTLIST list append CDATA #IMPLIED>\n"
 printf   "]>\n"
 printf   "\n"
 printf   "<lockss-config>\n"
 printf   "\n"
}

#
# postambleXml
#
function postambleXml() {
 if (xmlInOrgLockssTitle) {
  xmlInOrgLockssTitle = false;
  printf  " </property>\n"
  printf  "\n"
 }
 printf   "</lockss-config>\n"
}

#
# printOneXmlEntry
#
function printOneXmlEntry(        _status, _i) {
 # Skip based on output level
 _status = get(CODE_STATUS)
 if (!outputLevels[_status]) { return }

 # Start config subtree if needed
 if ((outputStyle == STYLE_XML || outputStyle == STYLE_XML_LEGACY) && !xmlInOrgLockssTitle) {
  xmlInOrgLockssTitle = true;
  printf  " <property name=\"org.lockss.title\">\n"
  printf  "\n"
 }

 # Begin
 printf   "  <property name=\"%s\">\n", getOpaqueName(get(CODE_TITLE))

 # First the publisher
 printOneXmlAttribute("publisher=" xml(get(CODE_PUBLISHER)))

 # Then the ISSN, CODEN, journal title, archival unit title and plugin
 printOneXmlProperty("issn=" get(CODE_ISSN))
 printOneXmlProperty("journalTitle=" xml(get(CODE_JOURNAL_TITLE)))
 printOneXmlProperty("title=" xml(get(CODE_TITLE)) (_status == STATUS_PRE_RELEASED ? " (pre-release)" : "") (_status == STATUS_SUPERSEDED ? " (superseded)" : ""))
 printOneXmlProperty("plugin=" get(CODE_PLUGIN))

 # Then the parameters
 for (_i = 1 ; _i <= numParameters ; ++_i) { printOneXmlParameter(_i, $indexParameters[_i]) }
 # Down AUs are denoted by a parameter
 if (_status == STATUS_DOWN || _status == STATUS_SUPERSEDED) { printOneXmlParameter(99, "pub_down=true") }

 # Then any additional attributes
 for (_i = 1 ; _i <= numAdditionalAttributes ; ++_i) { printOneXmlAttribute($indexAdditionalAttributes[_i]) }

 # Then other tidbits
 if (get(CODE_RIGHTS) == "openaccess") { printOneXmlAttribute("rights=openaccess") }
 printOneXmlProperty("estSize=" get(CODE_ESTIMATED_SIZE))
 if (_status == STATUS_PRE_RELEASED) { printOneXmlAttribute("releaseStatus=pre-release") }

 # End
 printf   "  </property>\n"
 printf   "\n"
}

#
# printOneXmlPublisherTitleSet
#
function printOneXmlPublisherTitleSet(        _pub) {
 if (getKey($3) == "publisher") {
  if (xmlInOrgLockssTitle) {
   xmlInOrgLockssTitle = false;
   printf " </property>\n"
   printf "\n"
  }
  _pub = xml(getValue($3))
  printf  " <property name=\"org.lockss.titleSet\">\n"
  printf  "\n"
  printf  "  <property name=\"%s\">\n", _pub
  printOneXmlProperty("name=All " _pub " Titles")
  printOneXmlProperty("class=xpath")
  printOneXmlProperty("xpath=[attributes/publisher='" _pub "']")
  printf  "  </property>\n"
  printf  "\n"
  printf  " </property>\n"
  printf  "\n"
 }
 else { error(DIRECTIVE_PUBLISHER_TITLE_SET ": illegal key: " getKey($3)) }
}

#
# printOneXmlParameter
#
function printOneXmlParameter(_num, _pair) {
 if (_pair == "") { return }
 printf   "   <property name=\"param.%d\">\n", _num
 printOneXmlProperty("key=" getKey(_pair), " ")
 printOneXmlProperty("value=" getValue(_pair), " ")
 printf   "   </property>\n"
}

#
# printOneXmlAttribute
#
function printOneXmlAttribute(_pair) {
 printOneXmlProperty("attributes." _pair)
}

#
# printOneXmlProperty
#
function printOneXmlProperty(_pair, _pad) {
 if (_pair == "" || getValue(_pair) == "") { return }
 if (_pad != "") { printf "%s", _pad }
 printf   "   <property name=\"%s\" value=\"%s\" />\n", getKey(_pair), getValue(_pair)
}

#
# postambleMediawikiList
#
function postambleMediawikiList(        _sorted, _i, _n, _letter) {
 _n = librarianSort(setOfPublishers, _sorted)
 for (_i = 1 ; _i <= _n ; ++_i) {
  _letter = toupper(substr(_sorted[_i], 1, 1))
  if (outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST) {
   if (_letter != toupper(substr(_sorted[_i - 1], 1, 1))) { printf "{{Anchor|%s}}{{Big|%s}}\n", _letter, _letter }
   printf "* [[%s]]\n", _sorted[_i]
  }
  else if (outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST_2) {
   if (_letter != toupper(substr(_sorted[_i - 1], 1, 1))) { printf "{{Anchor|%s}}\n", _letter }
   printf "{{IncludePublisherPage|%s}}\n", _sorted[_i]
  }
  else { error("wrong output style: " outputStyle) }
 }
}

#
# registerOnePublisher{{Anchor|A}}{{Big|A}}
#
function registerOnePublisher() {
 setOfPublishers[get(CODE_PUBLISHER)] = true;
}

#
# librarianSort
#
function librarianSort(_unsorted, _sorted,        _s1, _s2, _i, _j, _size) {
 _size = 0
 for (_i in _unsorted) {
  _j = _size++
  while (_j > 0 && librarianNormalize(_sorted[_j]) > librarianNormalize(_i)) {
   _sorted[_j + 1] = _sorted[_j]
   --_j
  }
  _sorted[_j + 1] = _i
 }
 return _size
}

#
# librarianNormalize
#
function librarianNormalize(_str) {
 _str = tolower(_str)
 gsub(/[^A-Za-z]/, "", _str)
 gsub(/^the /, "", _str)
 gsub(/^an? /, "", _str)
 return _str
}


#
# get
#
function get(_code) {
 if (indexOf[_code] < 0) { return fixedValue[_code] }
 if (indexOf[_code] > 0) { return $indexOf[_code] }
 return "";
}

#
# getOpaqueName
#
function getOpaqueName(_str) {
 _str = (publishingPlatform != "" ? publishingPlatform : namePrefix) _str
 gsub(/ Volume /, "", _str); # FIXME
 gsub(/&/, "and", _str);
 gsub(/[^a-zA-Z0-9]/, "", _str);
 return _str
}

#
# getKey
#
# Returns the key in a key=value pair.
#
function getKey(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, 1, RLENGTH - 1) : _str
}

#
# getValue
#
# Returns the value in a key=value pair.
#
function getValue(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, RLENGTH + 1, length(_str) - RLENGTH) : _str
}

#
# xml
#
# Encodes a string's ampersands and angle brackets for XML output.
# Assumes UTF-8 input; produces UTF-8 output.
#
function xml(_str) {
 gsub(/&/, "\&amp;", _str);
 gsub(/</, "\&lt;", _str);
 gsub(/>/, "\&gt;", _str);
 return _str
}

#
# Displays a warning to stderr
#
function warning(_str) {
 printf "Warning: %s line %d: %s\n", _str, FILENAME, FNR > "/dev/stderr"
}

#
# Displays an error message to stderr
#
function error(_str) {
 printf "Error: %s line %d: %s\n", _str, FILENAME, FNR > "/dev/stderr"
}

##########
# PATTERNS
##########

#
# Beginning of file
# unconditional
#
FNR == 1 {
 formatString = ""
 namePrefix = ""
 publishingPlatform = ""
}

#
# Blank line
#
/^\t*$/ {
 next
}

#
# Fixed values line
#
$1 == "#" && $2 == DIRECTIVE_FIXED_VALUES {
 parseFixedValues()
 next
}

#
# Format line
#
$1 == "#" && $2 == DIRECTIVE_FORMAT_STRING {
 formatString = $3
 parseFormatString()
 next
}

#
# Name prefix line
#
$1 == "#" && $2 == DIRECTIVE_NAME_PREFIX {
 namePrefix = $3
 publishingPlatform = ""
 next
}

#
# Publisher title set line
#
$1 == "#" && $2 == DIRECTIVE_PUBLISHER_TITLE_SET {
 if (outputStyle == STYLE_XML) { printOneXmlPublisherTitleSet() }
 next
}

$1 == "#" && $2 == DIRECTIVE_PUBLISHING_PLATFORM {
 publishingPlatform = $3
 namePrefix = ""
 next
}

#
# Comment line
#
/^#/ {
 next
}

# Normal line
# unconditional
{
 if (formatString == "") {
  error("fatal: no current " DIRECTIVE_FORMAT_STRING " directive")
  exit EXIT_NO_FORMAT_STRING
 }
 if (outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST || outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST_2) { registerOnePublisher() }
 else if (outputStyle == STYLE_XML || outputStyle == STYLE_XML_ENTRIES || outputStyle == STYLE_XML_LEGACY) { printOneXmlEntry() }
 next
}

#####
# END
#####

END {
 # Postamble
 if (outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST || outputStyle == STYLE_MEDIAWIKI_PUBLISHER_LIST_2) { postambleMediawikiList() }
 else if (outputStyle == STYLE_XML || outputStyle == STYLE_XML_LEGACY) { postambleXml() }
}

