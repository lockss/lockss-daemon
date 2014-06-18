/**
Web service that provides information about the current state of the LOCKSS
daemon.
<p />In many ways, it provides the same data that is shown in the Daemon Status
pages of the LOCKSS Daemon Administration web interface.
<a name="SQL-Like_Query"><!-- --></a><h3>SQL-Like Query</h3>
There are several operations that require as an argument a text string
containing a SQL-like query used to determine the results to be returned.
<p />The result of executing such a query is always a List of objects. The kind
of objects populating the result List is specific to the web service operation
being performed.
<br />For example, the Daemon Status web service operation {@link
org.lockss.ws.status.DaemonStatusService#queryPlugins(java.lang.String)
queryPlugins} returns a List of {@link org.lockss.ws.entities.PluginWsResult}
objects.
<p />The query may have up to three clauses, each of them allowing the user to
control a different aspect of the results. Each clause is separated from the
other by at least one space.
<p />The three clauses are, in order of appearance in the query:<p />
<ul>
  <li>A required <b>select</b> clause, which specifies which properties in the
  returned objects should be properly populated, while the rest are populated
  with null values.
  <br>The text of the <b>select</b> clause begins with the word
  <code>SELECT</code> followed by a space and either a comma-separated list of
  property names or an asterisk (<code>*</code>):
  <ul>
    <li>If a list of property names is included, the names in the list must
    correspond to properties of the objects being returned. The property names
    may be obtained from the get* accessor method names by removing the leading
    "get" and changing the capitalization of the resulting first letter to
    lower case.
    <li>The asterisk (<code>*</code>) is a shorthand way of specifying all the
    property names.
  </ul>
</ul><p />
<ul>
  <li>An optional <b>where</b> clause, which specifies which objects have to be
  included in the results, while the rest are not.<br>
  The text of the <b>where</b> clause begins with the word <code>WHERE</code>
  followed by a space and a space-separated list of conditions using
  <code>OR</code> and <code>AND</code> keywords to specify the filtering logic.
  Parentheses may be used in the typical grouping manner.
  <br>Each condition is a comparison between properties of the objects being
  returned and other properties or between properties and constants using
  comparators like <code>&lt;</code>, <code>=</code>, etc. Text constants must
  be surrounded by single quote (<code>'</code>) characters.
</ul><p />
<ul >
  <li>An optional <b>order by</b> clause, which specifies the order of the
  objects in the List that contains the results.<br>
  The text of the <b>order by</b> clause begins with the words
  <code>ORDER BY</code> followed by a space and a comma-separated list of
  property names, each of them optionally followed by the keyword
  <code>ASC</code> (default) or <code>DESC</code>.
</ul><p />
Examples
<ol>
  <li><code>select *</code>: It is the simplest (but costly) query and it
  results in a List of all the objects with each and every property properly
  populated.
  <li><code>select name</code>: Results in a List of all the objects with just
  the <code>name</code> property properly populated and the rest of properties
  populated with null values.
  <li><code>select name where id &gt; 10</code>: Results in a List of the
  objects with the numeric <code>id</code> property greater than <b>10</b> only
  and properly populated with just the <code>name</code> property and the rest
  of properties populated with null values.
  <li><code>select id where name = 'abc'</code>: Results in a List of the
  objects with the text <code>name</code> property equal to <b>abc</b> only and
  properly populated with just the <code>id</code> property and the rest of
  properties populated with null values.
  <li><code>select id, name where value &lt; 100 and color like 'red%' order by
  id</code>: Results in a List of the objects with both the numeric
  <code>value</code> property lower than <b>100</b> and the text
  <code>color</code> property starting with <b>red</b> only and properly
  populated with just the <code>id</code> and <code>name</code> properties and
  the rest of properties populated with null values. The objects in the list are
  ordered from the smallest <code>id</code> property to the largest one.
  <li><code>select id, longValue where toDate(longValue) &lt;
  toDate('02/Apr/2014')</code>: Results in a List of the objects with the long
  numeric <code>longValue</code> property representing a date before
  <b>April 2, 2014</b> and properly populated with just the <code>id</code> and
  <code>longValue</code> properties and the rest of properties populated with
  null values.
</ol>
*/
package org.lockss.ws.status;

