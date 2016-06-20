Thanks for taking the time to contribute!

# How to contribute

#### **If you find a bug?**
* **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/lockss/lockss-daemon/issues).

* If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/lockss/lockss-daemon/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.

#### **Did you write a patch that fixes a bug?**

Please follow the guidelines for submitting changes below.

#### **Do you intend to add a new feature or change an existing one?**

* Suggest your change by sending mail to info@lockss.org and start writing code.

* Do not open an issue on GitHub until you have collected positive feedback about the change. GitHub issues are primarily intended for bug reports and fixes.


## Submitting changes

Please send a [GitHub Pull Request to lockss](https://github.com/lockss/lockss-daemon/pull/new/master) with a clear list of what you've done (read more about [pull  requests](http://help.github.com/pull-requests/)). When you send a pull request, Please include tests and an explanation of changes. Please follow our coding conventions (below) and make sure all of your commits are atomic (one feature per commit).

Always write a clear log message for your commits. One-line messages are fine for small changes, but bigger changes should look like this:

    $ git commit -m "A brief summary of the commit
    > 
    > A paragraph describing what changed and its impact."

## Coding conventions

Start reading our code and you'll get the hang of it.This is open source software. Consider the people who will read your code, and make it look nice for them.  We generally follow the coding conventions found at[oracle java codinging conventions](http://www.oracle.com/technetwork/java/codeconventions-150003.pdf)

  * We indent using two spaces (no tabs)
  * No whitespace between a method name and its argument list.
  * No whitespace after casts.
  * Don't uppercase acronyms embedded in names: 'getHttpStream' not 'getHTTPStream'
  * Avoid single letter variables, 'ix' not 'x'
  * Javadoc comments should be used for interfaces and public methods and fields (and protected methods and fields in frameworks)
  * Use '//' rather than surrounding the entire block with /* ... */

## Testing

We rely on unit tests for most changes and functional tests for more extensive changes.  The testing frameworks should use run_one_daemon or run_multiple_daemons can be used for experimentation and testing of a running daemon.

Thanks for contributing,

The LOCKSS Team
