package com.sd_editions.collatex.Collate;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

import com.sd_editions.collatex.Block.BlockStructure;
import com.sd_editions.collatex.Block.BlockStructureCascadeException;
import com.sd_editions.collatex.InputPlugin.StringInputPlugin;

public class TupleToTableTest extends TestCase {
  public void testSentence() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(2, 2), new Tuple(3, 3) };
    Table table = wordAlignmentTable("a black cat", "a black cat", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("identical: black", table.get(1, 4).toString());
    assertEquals("identical: cat", table.get(1, 6).toString());
  }

  public void testReplacement() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(3, 3) };
    Table table = wordAlignmentTable("a white cat", "a black cat", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("replacement: white / black", table.get(1, 4).toString());
    assertEquals("identical: cat", table.get(1, 6).toString());
  }

  public void testOmission_InTheMiddle() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(3, 2) };
    Table table = wordAlignmentTable("a white horse", "a horse", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("omission: white", table.get(1, 4).toString());
    assertEquals("identical: horse", table.get(1, 6).toString());
  }

  public void testAddition_InTheMiddle() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(2, 3) };
    Table table = wordAlignmentTable("a cat", "a calico cat", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("addition: calico", table.get(1, 3).toString());
    assertEquals("identical: cat", table.get(1, 4).toString());
  }

//  public void testJoin() throws FileNotFoundException, IOException, BlockStructureCascadeException {
//    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(3, 4) };
//    Table table = wordAlignmentTable("a full blood cat", "a fullblood cat", tuples);
//    assertEquals("identical: a", table.get(1, 2).toString());
//    assertEquals("join: full blood -> fullblood", table.get(1, 4).toString());
//    assertEquals("identical: cat", table.get(1, 6).toString());
//  }

  public void testDivision() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(3, 4) };
    Table table = wordAlignmentTable("a fullblood cat", "a full blood cat", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("division: fullblood -> full blood", table.get(1, 4).toString());
    assertEquals("identical: cat", table.get(1, 6).toString());
  }

  public void testDivision2() throws FileNotFoundException, IOException, BlockStructureCascadeException {
    Tuple[] tuples = new Tuple[] { new Tuple(1, 1), new Tuple(3, 5) };
    Table table = wordAlignmentTable("a hotblooded teacher", "a hot blood ed teacher", tuples);
    assertEquals("identical: a", table.get(1, 2).toString());
    assertEquals("division: hotblooded -> hot blood ed", table.get(1, 4).toString());
    assertEquals("identical: teacher", table.get(1, 6).toString());
  }

  private Table wordAlignmentTable(final String baseString, final String witnessString, Tuple[] tuples) throws FileNotFoundException, IOException, BlockStructureCascadeException {
    BlockStructure base = new StringInputPlugin(baseString).readFile();
    BlockStructure variant = new StringInputPlugin(witnessString).readFile();
    TupleToTable tupleToTable = new TupleToTable(base, variant, tuples);
    Table table = tupleToTable.getTable();
    return table;
  }

}
