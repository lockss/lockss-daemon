/*

Copyright (c) 2000-2016, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.*;
import org.lockss.tdb.AntlrUtil.SyntaxError;
import org.lockss.util.Constants;

/**
 * @author Thib Guicherd-Callin
 * @since 1.72
 */
public class TdbParse {

  /**
   * <p>
   * A version string for the TdbParse tool ({@value}).
   * </p>
   * 
   * @since 1.72
   */
  public static final String VERSION = "[TdbParse:0.1.0]";

  /**
   * @since 1.72
   */
  protected TdbBuilder tdbBuilder;
  
  /**
   * @since 1.72
   */
  private TdbParse() {
    this.tdbBuilder = new TdbBuilder();
  }
  
  /**
   * @param opts
   * @since 1.72
   */
  public void addOptions(Options opts) {
    HelpOption.addOptions(opts);
    VerboseOption.addOptions(opts);
    VersionOption.addOptions(opts);
    InputOption.addOptions(opts);
    OutputOption.addOptions(opts);
    KeepGoingOption.addOptions(opts);
  }
  
  /**
   * @param cmd
   * @return
   * @since 1.72
   */
  public Map<String, Object> processCommandLine(CommandLineAccessor cmd) {
    Map<String, Object> options = new HashMap<String, Object>();
    // HelpOption already processed
    VersionOption.processCommandLine(cmd, VERSION); // may exit
    InputOption.processCommandLine(options, cmd);
    OutputOption.processCommandLine(options, cmd);
    if (!OutputOption.isSingleOutput(options)) {
      AppUtil.error("--%s is required", OutputOption.KEY_OUTPUT);
    }
    KeepGoingOption.processCommandLine(options, cmd);
    return options;
  }
  
  public Tdb processFiles(Map<String, Object> options) {
    List<String> inputFiles = InputOption.getInput(options);
    for (String f : inputFiles) {
      try {
        if ("-".equals(f)) {
          f = "<stdin>";
          tdbBuilder.parse(f, System.in, Constants.ENCODING_UTF_8);
        }
        else {
          tdbBuilder.parse(f, Constants.ENCODING_UTF_8);
        }
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.warning(options, fnfe, "%s: file not found", f);
        KeepGoingOption.addError(options, fnfe);
      }
      catch (IOException ioe) {
        AppUtil.warning(options, ioe, "%s: I/O error", f);
        KeepGoingOption.addError(options, ioe);
      }
      catch (SyntaxError se) {
        AppUtil.warning(options, se, se.getMessage());
        KeepGoingOption.addError(options, se);
      }
    }
    
    List<Exception> errors = KeepGoingOption.getErrors(options);
    int errs = errors.size();
    if (KeepGoingOption.isKeepGoing(options) && errs > 0) {
      AppUtil.error(options, errors, "Encountered %d %s; exiting", errs, errs == 1 ? "error" : "errors");
    }
    return tdbBuilder.getTdb();
  }
  
  /**
   * @param mainArgs
   * @since 1.72
   */
  public void run(String[] mainArgs) throws ParseException {
    // Parse command line
    Options opts = new Options();
    addOptions(opts);
    CommandLineAccessor cmd = new CommandLineAdapter(new DefaultParser().parse(opts, mainArgs));
    HelpOption.processCommandLine(cmd, opts, getClass());
    Map<String, Object> options = processCommandLine(cmd);
    // Run
    Tdb tdb = processFiles(options);
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new GZIPOutputStream(OutputOption.getSingleOutput(options)));
      oos.writeObject(tdb);
    }
    catch (IOException ioe) {
      AppUtil.error(options, ioe, "I/O error");
    }
    finally {
      try {
        oos.close();
      }
      catch (IOException ioe) {
        // ...
      }
    }
  }
  
  /**
   * @param args
   * @since 1.72
   */
  public static void main(String[] args) throws ParseException {
    new TdbParse().run(args);
  }
  
}
