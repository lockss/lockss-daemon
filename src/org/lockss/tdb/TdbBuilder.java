package org.lockss.tdb;

import java.io.*;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;

import org.lockss.tdb.TdbParser.*;

public class TdbBuilder extends TdbParserBaseListener {

  protected Tdb tdb;
  
  protected Publisher currentPublisher;
  
  protected Title currentTitle;
  
  protected Stack<Au> stack;
  
  public TdbBuilder() {
    this.tdb = new Tdb();
    this.stack = new Stack<Au>();
    this.stack.push(new Au());
  }
  
  public Tdb getTdb() {
    return tdb;
  }
  
  protected Au stackPush() {
    Au newAu = new Au(stack.peek());
    stack.push(newAu);
    return newAu;
  }
  
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
    
  @Override
  public void exitPublisherContainer(@NotNull PublisherContainerContext pctx) {
    stack.pop();
  }

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
    
  @Override
  public void exitTitleContainer(@NotNull TitleContainerContext tctx) {
    stack.pop();
  }

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
    
  @Override
  public void exitAuContainer(@NotNull AuContainerContext actx) {
    stack.pop();
  }

  @Override
  public void enterPublisher(@NotNull PublisherContext pctx) {
    Map<String, String> map = new HashMap<String, String>();
    for (SimpleAssignmentContext sactx : pctx.listOfSimpleAssignments().simpleAssignment()) {
      map.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
    }
    currentPublisher = new Publisher(map);
    tdb.addPublisher(currentPublisher);
  }

  @Override
  public void enterTitle(@NotNull TitleContext tctx) {
    Map<String, String> map = new HashMap<String, String>();
    for (SimpleAssignmentContext sactx : tctx.listOfSimpleAssignments().simpleAssignment()) {
      map.put(sactx.IDENTIFIER().getText(), sactx.STRING().getText());
    }
    currentTitle = new Title(currentPublisher, map);
    tdb.addTitle(currentTitle);
  }
  
  @Override
  public void enterImplicit(@NotNull ImplicitContext ictx) {
    List<String> implicit = new ArrayList<String>();
    for (TerminalNode identifier : ictx.listOfIdentifiers().IDENTIFIER()) {
      implicit.add(identifier.getText());
    }
    stack.peek().setImplicit(implicit);
  }
  
  @Override
  public void enterAu(@NotNull AuContext actx) {
    Au au = new Au(currentTitle, stack.peek());
    List<String> currentImplicit = au.getImplicit();
    int sizeImplicit = currentImplicit.size();
    int sizeListOfStrings = actx.listOfStrings().STRING().size();
    if (sizeImplicit != sizeListOfStrings) {
      AntlrUtil.error(actx.getStart(), "Expected %d implicit assignments but got %d", sizeImplicit, sizeListOfStrings);
    }
    for (int i = 0 ; i < sizeListOfStrings ; ++i) {
      au.put(currentImplicit.get(i), actx.listOfStrings().STRING(i).getText());
    }
    tdb.addAu(au);
  }

  public void parse(String fileName) throws IOException {
    parse(new ANTLRFileStream(fileName));
  }

  public void parse(String fileName, String encoding) throws IOException {
    parse(new ANTLRFileStream(fileName, encoding));
  }

  public void parse(InputStream inputStream) throws IOException {
    if (inputStream == System.in) {
      parse(AntlrUtil.makeNamedAntlrInputStream("<stdin>", inputStream));
    }
    else {
      parse(new ANTLRInputStream(inputStream));
    }
  }
  
//  public void parse(Reader reader) throws IOException {
//    parse(new ANTLRInputStream(reader));
//  }
  
  protected void parse(CharStream charStream) {
    TdbLexer lexer = new TdbLexer(charStream);
    AntlrUtil.setEmacsErrorListener(lexer);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TdbParser parser = new TdbParser(tokens);
    AntlrUtil.setEmacsErrorListener(parser);
    ParserRuleContext tree = parser.tdb();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
  }
  
}
