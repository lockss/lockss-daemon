/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
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
import java.nio.charset.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.lockss.tdb.AntlrUtil.SyntaxError;

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
  public static final String VERSION = "[TdbParse:0.2.0]";

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
  
  public Tdb processFiles(Map<String, Object> options) {
    List<String> inputFiles = InputOption.getInput(options);
    for (String f : inputFiles) {
      try {
        if ("-".equals(f)) {
          f = "<stdin>";
          tdbBuilder.parse(f, System.in, StandardCharsets.UTF_8);
        }
        else {
          tdbBuilder.parse(f, StandardCharsets.UTF_8);
        }
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.warning(options, fnfe, "%s: file not found", f);
        KeepGoing.addError(options, fnfe);
      }
      catch (MalformedInputException mie) {
        AppUtil.warning(options, mie, "%s: %s", f, mie.getMessage());
        KeepGoing.addError(options, mie);
      }
      catch (IOException ioe) {
        AppUtil.warning(options, ioe, "%s: I/O error", f);
        KeepGoing.addError(options, ioe);
      }
      catch (SyntaxError se) {
        AppUtil.warning(options, se, se.getMessage());
        KeepGoing.addError(options, se);
      }
    }
    
    List<Exception> errors = KeepGoing.getErrors(options);
    int errs = errors.size();
    if (KeepGoing.isKeepGoing(options) && errs > 0) {
      AppUtil.error(options, errors, "Encountered %d %s; exiting", errs, errs == 1 ? "error" : "errors");
    }
    return tdbBuilder.getTdb();
  }
  
  /**
   * @param opts
   * @since 1.72
   */
  public void addOptions(Options opts) {
    opts.addOption(Help.option()); // --help
    InputOption.addOptions(opts); // --input
    opts.addOption(KeepGoing.option()); // --keep-going
    opts.addOption(OutputData.option()); // --output-data
    opts.addOption(Verbose.option()); // --verbose
    opts.addOption(Version.option()); // --version
  }
  
  /**
   * @param cmd
   * @return
   * @since 1.72
   */
  public Map<String, Object> processCommandLine(CommandLineAccessor cmd) {
    Map<String, Object> options = new HashMap<String, Object>();
    // Help already processed
    Version.parse(cmd, VERSION, TdbBuilder.VERSION); // may exit
    InputOption.processCommandLine(options, cmd);
    OutputData.parse(options, cmd);
    if (OutputData.get(options) == null) {
      AppUtil.error("--%s is required", OutputData.KEY);
    }
    KeepGoing.parse(options, cmd);
    return options;
  }
  
  public void run(Map<String, Object> options) {
    Tdb tdb = processFiles(options);
    OutputStream os = null;
    try {
      String f = OutputData.get(options);
      os = (f == null || "-".equals(f)) ? System.out : new FileOutputStream(f);
      writeTdb(tdb, os);
    }
    catch (IOException ioe) {
      AppUtil.error(options, ioe, "Output error");
    }
    finally {
      try {
        os.close();
      }
      catch (IOException ioe) {
        // ignore
      }
    }
  }
  
  /**
   * @param mainArgs
   * @since 1.72
   */
  public void run(String[] mainArgs) throws ParseException {
    Options opts = new Options();
    addOptions(opts);
    CommandLineAccessor cmd = new CommandLineAdapter(new DefaultParser().parse(opts, mainArgs));
    Help.parse(cmd, opts, getClass());
    Map<String, Object> options = processCommandLine(cmd);
    run(options);
  }

  /**
   * @param tdb
   * @param outputStream
   * @throws IOException
   * @since 1.72
   */
  public static void writeTdb(Tdb tdb, OutputStream outputStream) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(outputStream);
    oos.writeObject(tdb);
  }

  /**
   * @param inputStream
   * @return
   * @throws IOException
   * @since 1.72
   */
  public static Tdb readTdb(InputStream inputStream) throws IOException {
    ObjectInputStream ois = new ObjectInputStream(inputStream);
    try {
      return (Tdb)ois.readObject();
    }
    catch (ClassNotFoundException cnfe) {
      throw new IOException(cnfe);
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
