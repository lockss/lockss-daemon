/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.tdb;

import java.io.*;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;

import org.lockss.tdb.AntlrUtil.NamedAntlrInputStream;
import org.lockss.tdb.AntlrUtil.SyntaxError;
import org.lockss.tdb.TdbParser.*;

/**
 * <p>
 * A module to parse TDB files, based on the ANTLR-generated
 * <code>TdbLexer.g4</code> and <code>TdbParser.g4</code> grammars. 
 * </p>
 * <p>
 * <i>This class does not depend on class files found in the daemon's JAR
 * libraries or on source files found in the daemon's code base, but on source
 * files generated into <code>generated/src/</code> by the ANTLR tool. Run
 * <code>ant generate-parsers</code> to cause the necessary source files to be
 * generated.</i>
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class TdbBuilder extends TdbParserBaseListener {

  /**
   * <p>
   * A version string for the TdbBuilder module ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  public static final String VERSION = "[TdbBuilder:0.2.3]";
  
  /**
   * <p>
   * The {@link Tdb} structure being built by this instance.
   * </p>
   * 
   * @since 1.67
   */
  protected Tdb tdb;
  
  /**
   * <p>
   * The {@link Publisher} currently being processed.
   * </p>
   * 
   * @since 1.67
   */
  protected Publisher currentPublisher;
  
  /**
   * <p>
   * The {@link Title} currently being processed.
   * </p>
   * 
   * @since 1.67
   */
  protected Title currentTitle;
  
  /**
   * <p>
   * The {@link Au} stack.
   * </p>
   * 
   * @since 1.67
   */
  protected Stack<Au> stack;
  
  /**
   * <p>
   * Makes a new TDB builder.
   * </p>
   * 
   * @since 1.67
   */
  public TdbBuilder() {
    this.tdb = new Tdb();
    this.stack = new Stack<Au>();
    this.stack.push(new Au());
  }
  
  /**
   * <p>
   * Retrieves the {@link Tdb} structure built by this instance.
   * </p>
   * 
   * @return The {@link Tdb} structure built by this instance.
   * @since 1.67
   */
  public Tdb getTdb() {
    return tdb;
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type publisherContainer, pushes a
   * new {@link Au} frame onto the stack and populates it with all the simple
   * assignments present.
   * </p>
   * 
   * @param pcctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterPublisherContainer(@NotNull PublisherContainerContext pcctx) {
    Au au = stackPush();
    for (AssignmentContext actx : pcctx.assignment()) {
      SimpleAssignmentContext sactx = actx.simpleAssignment();
      if (sactx != null) {
        au.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
      }
    }
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type publisherContainer, pops the
   * current {@link Au} frame.
   * </p>
   * 
   * @param pcctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitPublisherContainer(@NotNull PublisherContainerContext pcctx) {
    stack.pop();
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type titleContainer, pushes a new
   * {@link Au} frame onto the stack and populates it with all the simple
   * assignments present.
   * </p>
   * 
   * @param tcctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterTitleContainer(@NotNull TitleContainerContext tcctx) {
    Au au = stackPush();
    for (AssignmentContext actx : tcctx.assignment()) {
      SimpleAssignmentContext sactx = actx.simpleAssignment();
      if (sactx != null) {
        au.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
      }
    }
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type titleContainer, pops the
   * current {@link Au} frame.
   * </p>
   * 
   * @param tcctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitTitleContainer(@NotNull TitleContainerContext tcctx) {
    stack.pop();
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type auContainer, pushes a new
   * {@link Au} frame onto the stack and populates it with all the simple
   * assignments present.
   * </p>
   * 
   * @param acctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterAuContainer(@NotNull AuContainerContext acctx) {
    Au au = stackPush();
    for (AssignmentContext actx : acctx.assignment()) {
      SimpleAssignmentContext sactx = actx.simpleAssignment();
      if (sactx != null) {
        au.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
      }
    }
  }
    
  /**
   * <p>
   * If processing an abstract syntax tree of type auContainer, pops the current
   * {@link Au} frame.
   * </p>
   * 
   * @param acctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void exitAuContainer(@NotNull AuContainerContext acctx) {
    stack.pop();
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type publisher, makes a new
   * current {@link Publisher} and populates it with its list of simple
   * assignments.
   * </p>
   * 
   * @param pctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterPublisher(@NotNull PublisherContext pctx) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    for (SimpleAssignmentContext sactx : pctx.listOfSimpleAssignments().simpleAssignment()) {
      map.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
    }
    currentPublisher = new Publisher(map);
    tdb.addPublisher(currentPublisher);
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type title, makes a new current
   * {@link Title} and populates it with its list of simple assignments.
   * </p>
   * 
   * @param tctx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterTitle(@NotNull TitleContext tctx) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    for (SimpleAssignmentContext sactx : tctx.listOfSimpleAssignments().simpleAssignment()) {
      map.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
    }
    currentTitle = new Title(currentPublisher, map);
    tdb.addTitle(currentTitle);
  }
  
  /**
   * <p>
   * If processing an abstract syntax tree of type au, makes a new {@link Au}
   * and populates it by matching the current implicit spec with the AU's list
   * of strings.
   * </p>
   * 
   * @param actx
   *          Context supplied by ANTLR.
   * @throws SyntaxError
   *           if the sizes of the implicit spec and list of strings are
   *           mismatched.
   * @since 1.67
   */
  @Override
  public void enterAu(@NotNull AuContext actx) throws SyntaxError {
    Au au = new Au(currentTitle, stack.peek());
    List<String> currentImplicit = au.getImplicit();
    if (currentImplicit == null) {
      AntlrUtil.syntaxError(actx.getStart(), "no implicit statement in scope");
    }
    int sizeImplicit = currentImplicit.size();
    List<TerminalNode> listOfStrings = actx.listOfStrings().STRING();
    int sizeListOfStrings = listOfStrings.size();
    if (sizeImplicit != sizeListOfStrings) {
      AntlrUtil.syntaxError(actx.getStart(), "expected %d implicit assignments but got %d", sizeImplicit, sizeListOfStrings);
    }
    for (int i = 0 ; i < sizeListOfStrings ; ++i) {
      au.put(currentImplicit.get(i), listOfStrings.get(i).getText());
    }
    tdb.addAu(au);
  }

  /**
   * <p>
   * If processing an abstract syntax tree of type implicit, makes a new current
   * implicit spec and populates it with its list of identifiers.
   * </p>
   * 
   * @param ictx
   *          Context supplied by ANTLR.
   * @since 1.67
   */
  @Override
  public void enterImplicit(@NotNull ImplicitContext ictx) {
    List<TerminalNode> identifiers = ictx.listOfIdentifiers().IDENTIFIER();
    List<String> implicit = new ArrayList<String>(identifiers.size());
    for (TerminalNode identifier : identifiers) {
      implicit.add(identifier.getText());
    }
    stack.peek().setImplicit(implicit);
  }
  
  /**
   * <p>
   * Parses a TDB file (by name) and appends its contents to the {@link Tdb}
   * structure.
   * </p>
   * 
   * @param fileName
   *          A file name.
   * @param encoding
   *          The encoding of the file.
   * @throws IOException
   *           if an I/O exception occurs.
   * @throws SyntaxError
   *           if there is a syntax error.
   * @since 1.68
   */
  public void parse(String fileName,
                    String encoding)
      throws IOException, SyntaxError {
    parse(new ANTLRFileStream(fileName, encoding));
  }

  /**
   * <p>
   * Parses a TDB file (by naming an input stream) and appends its contents to
   * the {@link Tdb} structure.
   * </p>
   * 
   * @param fileName
   *          A source name.
   * @param inputStream
   *          An input stream.
   * @param inputStream
   *          The encoding of the input stream.
   * @throws UnsupportedEncodingException
   *           if the given encoding is invalid.
   * @throws IOException
   *           if some other I/O exception occurs.
   * @throws SyntaxError
   *           if there is a syntax error.
   * @since 1.68
   */
  public void parse(String name,
                    InputStream inputStream,
                    String encoding)
      throws UnsupportedEncodingException, IOException, SyntaxError {
    parse(new NamedAntlrInputStream(name, inputStream, encoding));
  }

  /**
   * <p>
   * Parses a TDB {@link CharStream} and appends its contents to the {@link Tdb}
   * structure.
   * </p>
   * 
   * @param charStream
   *          A character stream.
   * @throws SyntaxError
   *           if there is a syntax error.
   * @since 1.67
   */
  protected void parse(CharStream charStream) throws SyntaxError {
    TdbLexer lexer = new TdbLexer(charStream);
    AntlrUtil.setEmacsErrorListener(lexer);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TdbParser parser = new TdbParser(tokens);
    AntlrUtil.setEmacsErrorListener(parser);
    ParserRuleContext tree = parser.tdb();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
  }
  
  /**
   * <p>
   * Convenience method that pushes a new {@link Au} based on the {@link Au}
   * currently on top of the stack onto the stack and returns it.
   * </p>
   * 
   * @return An {@link Au} currently on top of the stack based on the {@link Au}
   *         previously on top of the stack.
   * @since 1.67
   */
  protected Au stackPush() {
    Au newAu = new Au(stack.peek());
    stack.push(newAu);
    return newAu;
  }
  
}
