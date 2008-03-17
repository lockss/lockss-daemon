#!/usr/bin/awk -f

#######
# BEGIN
#######

BEGIN {
 # Boolean
 false = 0;
 true = 1;

 # Format codes
 CODE_ATTRIBUTE      = "@"
 CODE_ESTIMATED_SIZE = "e"
 CODE_JOURNAL_TITLE  = "j"
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
 STYLE_XML         = "xml"
 STYLE_XML_ENTRIES = "xmlEntries"
 STYLE_XML_LEGACY  = "xmlLegacy"
 STYLE_DEFAULT     = STYLE_XML_ENTRIES

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
 # Disallow overrides
 formatString = ""
 namePrefix = ""

 # Defaults
 if (outputStyle == "") { outputStyle = STYLE_DEFAULT }
 if (outputLevel == "") { outputLevel = LEVEL_DEFAULT }

 # Parse
 parseOutputLevel()
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

 # For each format code...
 _numFields = split(formatString, _splitFormatString, ",")
 for (_field = 1 ; _field <= _numFields ; ++_field) {
  _code = _splitFormatString[_field]
  if (_code == CODE_SKIP_1 || _code == CODE_SKIP_2) { continue }
  else if (_code == CODE_ESTIMATED_SIZE || _code == CODE_JOURNAL_TITLE || _code == CODE_PLUGIN || _code == CODE_TITLE || _code == CODE_PUBLISHER || _code == CODE_STATUS || _code == CODE_RIGHTS) { indexOf[_code] = _field }
  else if (_code == CODE_ATTRIBUTE) { indexAdditionalAttributes[++numAdditionalAttributes] = _field }
  else if (_code == CODE_PARAMETER) { indexParameters[++numParameters] = _field }
  # FIXME: unknown code
 }
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
  for (_j in _splitStatuses) { outputLevels[_splitStatuses[_j]] = true; }
 }
}

#
# preambleXml
#
function preambleXml() {
 xmlInOrgLockssTitle = false;

 printf  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
 printf  "<!DOCTYPE lockss-config [\n"
 printf  "<!ELEMENT lockss-config (if|property)+>\n"
 printf  "<!ELEMENT property (property|list|value|if)*>\n"
 printf  "<!ELEMENT list (value)+>\n"
 printf  "<!ELEMENT value (#PCDATA)>\n"
 printf  "<!ELEMENT test EMPTY>\n"
 printf  "<!ELEMENT and (and|or|not|test)*>\n"
 printf  "<!ELEMENT or (and|or|not|test)*>\n"
 printf  "<!ELEMENT not (and|or|not|test)*>\n"
 printf  "<!ELEMENT if (and|or|not|then|else|test|property)*>\n"
 printf  "<!ELEMENT then (if|property)*>\n"
 printf  "<!ELEMENT else (if|property)*>\n"
 printf  "<!ATTLIST property name CDATA #REQUIRED>\n"
 printf  "<!ATTLIST property value CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test hostname CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test group CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test daemonVersionMin CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test daemonVersionMax CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test daemonVersion CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test platformVersionMin CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test platformVersionMax CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test platformVersion CDATA #IMPLIED>\n"
 printf  "<!ATTLIST test platformName CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if hostname CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if group CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if daemonVersionMin CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if daemonVersionMax CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if daemonVersion CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if platformVersionMin CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if platformVersionMax CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if platformVersion CDATA #IMPLIED>\n"
 printf  "<!ATTLIST if platformName CDATA #IMPLIED>\n"
 printf  "<!ATTLIST list append CDATA #IMPLIED>\n"
 printf  "]>\n"
 printf  "\n"
 printf  "<lockss-config>\n"
 printf  "\n"
}

#
# postambleXml
#
function postambleXml() {
 if (xmlInOrgLockssTitle) {
  xmlInOrgLockssTitle = false;
  printf " </property>\n"
  printf "\n"
 }
 printf  "</lockss-config>\n"
}

#
# printOneXmlEntry
#
function printOneXmlEntry(        _status, _param, _attr) {
 # Skip based on output level
 _status = $indexOf[CODE_STATUS]
 if (!outputLevels[_status]) { return }

 # Start config subtree if needed
 if ((outputStyle == STYLE_XML || outputStyle == STYLE_XML_LEGACY) && !xmlInOrgLockssTitle) {
  xmlInOrgLockssTitle = true;
  printf " <property name=\"org.lockss.title\">\n"
  printf "\n"
 }

 # Begin
 printf  "  <property name=\"%s\">\n", getOpaqueName($indexOf[CODE_TITLE])

 # First the publisher if there is one
 if (indexOf[CODE_PUBLISHER] != "") { printOneXmlAttribute("publisher=" xml($indexOf[CODE_PUBLISHER])) }

 # Then the journal title, archival unit title and plugin
 printf  "   <property name=\"journalTitle\" value=\"%s\" />\n", xml($indexOf[CODE_JOURNAL_TITLE])
 printf  "   <property name=\"title\" value=\"%s%s\" />\n", xml($indexOf[CODE_TITLE]), (_status == STATUS_PRE_RELEASED ? " (Pre-release)" : "")
 printf  "   <property name=\"plugin\" value=\"%s\" />\n", $indexOf[CODE_PLUGIN]

 # Then the parameters
 for (_param = 1 ; _param <= numParameters ; ++_param) {
  _pair = $indexParameters[_param]
  if (_pair == "") { continue }
  printOneXmlParameter(_param, _pair)
 }
 # Down AUs are denoted by a parameter
 if (_status == STATUS_DOWN || _status == STATUS_SUPERSEDED) { printOneXmlParameter(99, "pub_down=true") }

 # Then any additional parameters
 for (_attr = 1 ; _attr <= numAdditionalAttributes ; ++_attr) {
  _pair = $indexAdditionalAttributes[_attr]
  if (_pair == "") { continue }
  printOneXmlAttribute(_pair)
 }

 # Then other tidbits
 if (indexOf[CODE_RIGHTS] != "" && $indexOf[CODE_RIGHTS] == "openaccess") { printOneXmlAttribute("rights=openaccess") }
 if (indexOf[CODE_ESTIMATED_SIZE] != "" && $indexOf[CODE_ESTIMATED_SIZE] != "") {
  printf "   <property name=\"estSize\" value=\"%s\" />\n", $indexOf[CODE_ESTIMATED_SIZE]
 }
 if (_status == STATUS_PRE_RELEASED) { printOneXmlAttribute("releaseStatus=pre-release") }

 # End
 printf  "  </property>\n"
 printf  "\n"
}

#
# printOneXmlParameter
#
function printOneXmlParameter(_num, _pair) {
 printf  "   <property name=\"param.%d\">\n", _num
 printf  "    <property name=\"key\" value=\"%s\" />\n", getKey(_pair)
 printf  "    <property name=\"value\" value=\"%s\" />\n", getValue(_pair)
 printf  "   </property>\n"
}

#
# printOneXmlAttribute
#
function printOneXmlAttribute(_pair) {
 printf  "   <property name=\"attributes.%s\" value=\"%s\" />\n", getKey(_pair), getValue(_pair)
}

#
# getOpaqueName
#
function getOpaqueName(_str) {
 gsub(/ Volume /, "", _str); # FIXME
 gsub(/&/, "and", _str);
 gsub(/[^a-zA-Z0-9]/, "", _str);
 return namePrefix _str
}

#
# getKey
#
function getKey(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, 1, RLENGTH - 1) : _str
}

#
# getValue
#
function getValue(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, RLENGTH + 1, length(_str) - RLENGTH) : _str
}

#
# xml
#
function xml(_str) {
 gsub(/&/, "\&amp;", _str);
 gsub(/</, "\&lt;", _str);
 gsub(/>/, "\&gt;", _str);
 return _str
}

##########
# PATTERNS
##########

#
# Blank line
#
/^\t*$/ {
 next
}

#
# Format line
#
/^# @formatString=/ {
 formatString = substr($1, 17, length($1) - 16)
 parseFormatString()
 next
}

#
# Name prefix line
#
/^# @namePrefix=/ {
 namePrefix = substr($1, 15, length($1) - 14)
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
 if (formatString == "") { exit EXIT_NO_FORMAT_STRING }
 if (outputStyle == STYLE_XML || outputStyle == STYLE_XML_ENTRIES || outputStyle == STYLE_XML_LEGACY) { printOneXmlEntry() }
 next
}

#####
# END
#####

END {
 # Postamble
 if (outputStyle == STYLE_XML || outputStyle == STYLE_XML_LEGACY) { postambleXml() }
}

