/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InvalidObjectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.TRange;
import org.apache.hadoop.io.Text;

public class RangeTest extends TestCase {
  private Range nr(String k1, String k2) {
    Key ik1 = null;
    if (k1 != null)
      ik1 = new Key(new Text(k1), 0l);

    Key ik2 = null;
    if (k2 != null)
      ik2 = new Key(new Text(k2), 0l);

    return new Range(ik1, ik2);
  }

  private List<Range> nrl(Range... ranges) {
    return Arrays.asList(ranges);
  }

  private void check(List<Range> rl, List<Range> expected) {
    HashSet<Range> s1 = new HashSet<Range>(rl);
    HashSet<Range> s2 = new HashSet<Range>(expected);

    assertTrue("got : " + rl + " expected : " + expected, s1.equals(s2));
  }

  public void testMergeOverlapping1() {
    List<Range> rl = nrl(nr("a", "c"), nr("a", "b"));
    List<Range> expected = nrl(nr("a", "c"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping2() {
    List<Range> rl = nrl(nr("a", "c"), nr("d", "f"));
    List<Range> expected = nrl(nr("a", "c"), nr("d", "f"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping3() {
    List<Range> rl = nrl(nr("a", "e"), nr("b", "f"), nr("c", "r"), nr("g", "j"), nr("t", "x"));
    List<Range> expected = nrl(nr("a", "r"), nr("t", "x"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping4() {
    List<Range> rl = nrl(nr("a", "e"), nr("b", "f"), nr("c", "r"), nr("g", "j"));
    List<Range> expected = nrl(nr("a", "r"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping5() {
    List<Range> rl = nrl(nr("a", "e"));
    List<Range> expected = nrl(nr("a", "e"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping6() {
    List<Range> rl = nrl();
    List<Range> expected = nrl();
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping7() {
    List<Range> rl = nrl(nr("a", "e"), nr("g", "q"), nr("r", "z"));
    List<Range> expected = nrl(nr("a", "e"), nr("g", "q"), nr("r", "z"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping8() {
    List<Range> rl = nrl(nr("a", "c"), nr("a", "c"));
    List<Range> expected = nrl(nr("a", "c"));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping9() {
    List<Range> rl = nrl(nr(null, null));
    List<Range> expected = nrl(nr(null, null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping10() {
    List<Range> rl = nrl(nr(null, null), nr("a", "c"));
    List<Range> expected = nrl(nr(null, null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping11() {
    List<Range> rl = nrl(nr("a", "c"), nr(null, null));
    List<Range> expected = nrl(nr(null, null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping12() {
    List<Range> rl = nrl(nr("b", "d"), nr("c", null));
    List<Range> expected = nrl(nr("b", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping13() {
    List<Range> rl = nrl(nr("b", "d"), nr("a", null));
    List<Range> expected = nrl(nr("a", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping14() {
    List<Range> rl = nrl(nr("b", "d"), nr("e", null));
    List<Range> expected = nrl(nr("b", "d"), nr("e", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping15() {
    List<Range> rl = nrl(nr("b", "d"), nr("e", null), nr("c", "f"));
    List<Range> expected = nrl(nr("b", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping16() {
    List<Range> rl = nrl(nr("b", "d"), nr("f", null), nr("c", "e"));
    List<Range> expected = nrl(nr("b", "e"), nr("f", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping17() {
    List<Range> rl = nrl(nr("b", "d"), nr("r", null), nr("c", "e"), nr("g", "t"));
    List<Range> expected = nrl(nr("b", "e"), nr("g", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping18() {
    List<Range> rl = nrl(nr(null, "d"), nr("r", null), nr("c", "e"), nr("g", "t"));
    List<Range> expected = nrl(nr(null, "e"), nr("g", null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping19() {
    List<Range> rl = nrl(nr(null, "d"), nr("r", null), nr("c", "e"), nr("g", "t"), nr("d", "h"));
    List<Range> expected = nrl(nr(null, null));
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping20() {

    List<Range> rl = nrl(new Range(new Text("a"), true, new Text("b"), false), new Range(new Text("b"), false, new Text("c"), false));
    List<Range> expected = nrl(new Range(new Text("a"), true, new Text("b"), false), new Range(new Text("b"), false, new Text("c"), false));
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(new Range(new Text("a"), true, new Text("b"), false), new Range(new Text("b"), true, new Text("c"), false));
    expected = nrl(new Range(new Text("a"), true, new Text("c"), false));
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(new Range(new Text("a"), true, new Text("b"), true), new Range(new Text("b"), false, new Text("c"), false));
    expected = nrl(new Range(new Text("a"), true, new Text("c"), false));
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(new Range(new Text("a"), true, new Text("b"), true), new Range(new Text("b"), true, new Text("c"), false));
    expected = nrl(new Range(new Text("a"), true, new Text("c"), false));
    check(Range.mergeOverlapping(rl), expected);

  }

  public void testMergeOverlapping22() {

    Range ke1 = new KeyExtent("tab1", new Text("Bank"), null).toMetadataRange();
    Range ke2 = new KeyExtent("tab1", new Text("Fails"), new Text("Bank")).toMetadataRange();
    Range ke3 = new KeyExtent("tab1", new Text("Sam"), new Text("Fails")).toMetadataRange();
    Range ke4 = new KeyExtent("tab1", new Text("bails"), new Text("Sam")).toMetadataRange();
    Range ke5 = new KeyExtent("tab1", null, new Text("bails")).toMetadataRange();

    List<Range> rl = nrl(ke1, ke2, ke3, ke4, ke5);
    List<Range> expected = nrl(new KeyExtent("tab1", null, null).toMetadataRange());
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(ke1, ke2, ke4, ke5);
    expected = nrl(new KeyExtent("tab1", new Text("Fails"), null).toMetadataRange(), new KeyExtent("tab1", null, new Text("Sam")).toMetadataRange());
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(ke2, ke3, ke4, ke5);
    expected = nrl(new KeyExtent("tab1", null, new Text("Bank")).toMetadataRange());
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(ke1, ke2, ke3, ke4);
    expected = nrl(new KeyExtent("tab1", new Text("bails"), null).toMetadataRange());
    check(Range.mergeOverlapping(rl), expected);

    rl = nrl(ke2, ke3, ke4);
    expected = nrl(new KeyExtent("tab1", new Text("bails"), new Text("Bank")).toMetadataRange());
    check(Range.mergeOverlapping(rl), expected);
  }

  public void testMergeOverlapping21() {
    for (boolean b1 : new boolean[] {true, false})
      for (boolean b2 : new boolean[] {true, false})
        for (boolean b3 : new boolean[] {true, false})
          for (boolean b4 : new boolean[] {true, false}) {

            // System.out.println("b1:"+b1+" b2:"+b2+" b3:"+b3+" b4:"+b4);

            List<Range> rl = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("m")), b2), new Range(new Key(new Text("b")), b3,
                new Key(new Text("n")), b4));
            List<Range> expected = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("n")), b4));
            check(Range.mergeOverlapping(rl), expected);

            rl = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("m")), b2), new Range(new Key(new Text("a")), b3, new Key(new Text("n")), b4));
            expected = nrl(new Range(new Key(new Text("a")), b1 || b3, new Key(new Text("n")), b4));
            check(Range.mergeOverlapping(rl), expected);

            rl = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("n")), b2), new Range(new Key(new Text("b")), b3, new Key(new Text("n")), b4));
            expected = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("n")), b2 || b4));
            check(Range.mergeOverlapping(rl), expected);

            rl = nrl(new Range(new Key(new Text("a")), b1, new Key(new Text("n")), b2), new Range(new Key(new Text("a")), b3, new Key(new Text("n")), b4));
            expected = nrl(new Range(new Key(new Text("a")), b1 || b3, new Key(new Text("n")), b2 || b4));
            check(Range.mergeOverlapping(rl), expected);
          }

  }

  public void testEqualsNull() {

    assertTrue(nr(null, "d").equals(nr(null, "d")));

    assertTrue(nr(null, null).equals(nr(null, null)));

    assertTrue(nr("a", null).equals(nr("a", null)));

    assertFalse(nr(null, "d").equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr(null, "d")));

    assertFalse(nr(null, null).equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr(null, null)));

    assertFalse(nr("a", null).equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr("a", null)));
  }

  public void testEquals() {
    assertFalse(nr("b", "d").equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr("b", "d")));

    assertFalse(nr("x", "y").equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr("x", "y")));

    assertFalse(nr("a", "z").equals(nr("a", "d")));
    assertFalse(nr("a", "d").equals(nr("a", "z")));

    assertTrue(nr("a", "z").equals(nr("a", "z")));
  }

  public void testRow1() {
    Range rowRange = new Range(new Text("r1"));

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertFalse(rowRange.contains(new Key(new Text("r11"))));
    assertFalse(rowRange.contains(new Key(new Text("r0"))));
  }

  public void testRow2() {
    Range rowRange = new Range(new Text("r1"), new Text("r2"));

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertTrue(rowRange.contains(new Key(new Text("r2"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r0"))));
    assertFalse(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow3() {
    Range rowRange = new Range(new Text("r1"), false, new Text("r2"), false);

    assertFalse(rowRange.contains(new Key(new Text("r1"))));
    assertFalse(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertFalse(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertFalse(rowRange.contains(new Key(new Text("r2"))));
    assertFalse(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertFalse(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r0"))));
    assertFalse(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow4() {
    Range rowRange = new Range(new Text("r1"), true, new Text("r2"), false);

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertFalse(rowRange.contains(new Key(new Text("r2"))));
    assertFalse(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertFalse(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r0"))));
    assertFalse(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow5() {
    Range rowRange = new Range(new Text("r1"), false, new Text("r2"), true);

    assertFalse(rowRange.contains(new Key(new Text("r1"))));
    assertFalse(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertFalse(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertTrue(rowRange.contains(new Key(new Text("r2"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r0"))));
    assertFalse(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow6() {
    Range rowRange = new Range(new Text("r1"), true, null, true);

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertTrue(rowRange.contains(new Key(new Text("r2"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertFalse(rowRange.contains(new Key(new Text("r0"))));
    assertTrue(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow7() {
    Range rowRange = new Range(null, true, new Text("r2"), true);

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertTrue(rowRange.contains(new Key(new Text("r2"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r0"))));
    assertFalse(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  public void testRow8() {
    Range rowRange = new Range((Text) null);

    assertTrue(rowRange.contains(new Key(new Text("r1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r1"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r1")).followingKey(PartialKey.ROW)));
    assertTrue(rowRange.contains(new Key(new Text("r11"))));

    assertTrue(rowRange.contains(new Key(new Text("r2"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"))));
    assertTrue(rowRange.contains(new Key(new Text("r2"), new Text("cf1"), new Text("cq1"))));

    assertTrue(rowRange.contains(new Key(new Text("r0"))));
    assertTrue(rowRange.contains(new Key(new Text("r2")).followingKey(PartialKey.ROW)));
  }

  private static Range nr(String r1, boolean r1i, String r2, boolean r2i) {
    Text tr1 = null;
    Text tr2 = null;

    if (r1 != null)
      tr1 = new Text(r1);

    if (r2 != null)
      tr2 = new Text(r2);

    return new Range(tr1, r1i, tr2, r2i);

  }

  private static Key nk(String r) {
    return new Key(new Text(r));
  }

  public void testClip1() {
    Range fence = nr("a", false, "c", false);

    runClipTest(fence, nr("a", false, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", false, "c", true), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", true), nr("a", false, "c", false));

    fence = nr("a", true, "c", false);

    runClipTest(fence, nr("a", false, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", false), nr("a", true, "c", false));
    runClipTest(fence, nr("a", false, "c", true), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", true), nr("a", true, "c", false));

    fence = nr("a", false, "c", true);

    runClipTest(fence, nr("a", false, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", false, "c", true), nr("a", false, "c", true));
    runClipTest(fence, nr("a", true, "c", true), nr("a", false, "c", true));

    fence = nr("a", true, "c", true);

    runClipTest(fence, nr("a", false, "c", false), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", false), nr("a", true, "c", false));
    runClipTest(fence, nr("a", false, "c", true), nr("a", false, "c", true));
    runClipTest(fence, nr("a", true, "c", true), nr("a", true, "c", true));
  }

  public void testClip2() {
    Range fence = nr("a", false, "c", false);

    runClipTest(fence, nr(null, true, null, true), nr("a", false, "c", false));
    runClipTest(fence, nr(null, true, "c", true), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, null, true), nr("a", false, "c", false));
    runClipTest(fence, nr("a", true, "c", true), nr("a", false, "c", false));
  }

  public void testClip3() {
    Range fence = nr("a", false, "c", false);

    runClipTest(fence, nr("0", false, "z", false), nr("a", false, "c", false));
    runClipTest(fence, nr("0", true, "z", false), nr("a", false, "c", false));
    runClipTest(fence, nr("0", false, "z", true), nr("a", false, "c", false));
    runClipTest(fence, nr("0", true, "z", true), nr("a", false, "c", false));

    runClipTest(fence, nr("0", false, "b", false), nr("a", false, "b", false));
    runClipTest(fence, nr("0", true, "b", false), nr("a", false, "b", false));
    runClipTest(fence, nr("0", false, "b", true), nr("a", false, "b", true));
    runClipTest(fence, nr("0", true, "b", true), nr("a", false, "b", true));

    runClipTest(fence, nr("a1", false, "z", false), nr("a1", false, "c", false));
    runClipTest(fence, nr("a1", true, "z", false), nr("a1", true, "c", false));
    runClipTest(fence, nr("a1", false, "z", true), nr("a1", false, "c", false));
    runClipTest(fence, nr("a1", true, "z", true), nr("a1", true, "c", false));

    runClipTest(fence, nr("a1", false, "b", false), nr("a1", false, "b", false));
    runClipTest(fence, nr("a1", true, "b", false), nr("a1", true, "b", false));
    runClipTest(fence, nr("a1", false, "b", true), nr("a1", false, "b", true));
    runClipTest(fence, nr("a1", true, "b", true), nr("a1", true, "b", true));
  }

  public void testClip4() {
    Range fence = new Range(nk("c"), false, nk("n"), false);

    runClipTest(fence, new Range(nk("a"), false, nk("c"), false));
    runClipTest(fence, new Range(nk("a"), false, nk("c"), true));
    runClipTest(fence, new Range(nk("n"), false, nk("r"), false));
    runClipTest(fence, new Range(nk("n"), true, nk("r"), false));
    runClipTest(fence, new Range(nk("a"), true, nk("b"), false));
    runClipTest(fence, new Range(nk("a"), true, nk("b"), true));

    fence = new Range(nk("c"), true, nk("n"), true);

    runClipTest(fence, new Range(nk("a"), false, nk("c"), false));
    runClipTest(fence, new Range(nk("a"), false, nk("c"), true), new Range(nk("c"), true, nk("c"), true));
    runClipTest(fence, new Range(nk("n"), false, nk("r"), false));
    runClipTest(fence, new Range(nk("n"), true, nk("r"), false), new Range(nk("n"), true, nk("n"), true));
    runClipTest(fence, new Range(nk("q"), false, nk("r"), false));
    runClipTest(fence, new Range(nk("q"), true, nk("r"), false));

    fence = nr("b", true, "b", true);

    runClipTest(fence, nr("b", false, "c", false));
    runClipTest(fence, nr("b", true, "c", false), nr("b", true, "b", true));
    runClipTest(fence, nr("a", false, "b", false));
    runClipTest(fence, nr("a", false, "b", true), nr("b", true, "b", true));

  }

  public void testBug1() {

    // unit test related to a bug that was observed (bug was not in range, but want to ensure the following works)

    // clip caught the scanner going to a tablet passed the end of the scan range
    Range fence = new Range(new Text("10<"), false, new Text("~"), true);

    Key k1 = new Key(new Text("10<"), new Text("~tab"), new Text("~pr"));
    Range range = new Range(k1, true, k1.followingKey(PartialKey.ROW), false);

    runClipTest(fence, range);

    // scanner was not handling edge case properly...
    Range scanRange = new Range(new Key("10;007cdc5b0".getBytes(), "~tab".getBytes(), "~pr".getBytes(), "".getBytes(), 130962, false), false, new Key(new Text(
        "10<")).followingKey(PartialKey.ROW), false);
    // below is the proper check the scanner now does instead of just comparing the row bytes
    scanRange.afterEndKey(new Key(new Text("10<")).followingKey(PartialKey.ROW));
  }

  private void runClipTest(Range fence, Range range) {
    try {
      fence.clip(range);
      assertFalse(true);
    } catch (IllegalArgumentException e) {

    }

  }

  private void runClipTest(Range fence, Range range, Range expected) {
    Range clipped = fence.clip(range);
    assertEquals(expected, clipped);
  }

  private static Key nk(String r, String cf, String cq) {
    return new Key(new Text(r), new Text(cf), new Text(cq));
  }

  private static Key nk(String r, String cf, String cq, String cv) {
    return new Key(new Text(r), new Text(cf), new Text(cq), new Text(cv));
  }

  private static Column nc(String cf, String cq) {
    return new Column(cf.getBytes(), cq == null ? null : cq.getBytes(), null);
  }

  private static Column nc(String cf) {
    return nc(cf, null);
  }

  private static Range nr(String row) {
    return new Range(new Text(row));
  }

  public void testBound1() {
    Range range1 = nr("row1");

    Range range2 = range1.bound(nc("b"), nc("e"));

    assertFalse(range2.contains(nk("row1")));
    assertFalse(range2.contains(nk("row1", "a", "z")));
    assertTrue(range2.contains(nk("row1", "b", "")));
    assertTrue(range2.contains(nk("row1", "b", "z")));
    assertTrue(range2.contains(nk("row1", "c", "z")));
    assertTrue(range2.contains(nk("row1", "e", "")));
    assertTrue(range2.contains(nk("row1", "e", "z")));
    assertFalse(range2.contains(nk("row1", "e", "").followingKey(PartialKey.ROW_COLFAM)));
    assertFalse(range2.contains(nk("row1", "f", "")));
    assertFalse(range2.contains(nk("row1", "f", "z")));

  }

  public void testBound2() {
    Range range1 = new Range(nk("row1", "b", "x"), true, nk("row1", "f", "x"), true);

    Range range2 = range1.bound(nc("a"), nc("g"));
    assertEquals(range1, range2);
    assertFalse(range2.contains(nk("row1", "a", "x")));
    assertTrue(range2.contains(nk("row1", "b", "x")));
    assertTrue(range2.contains(nk("row1", "f", "x")));
    assertFalse(range2.contains(nk("row1", "g", "")));

    Range range3 = range1.bound(nc("c"), nc("d"));
    assertFalse(range3.contains(nk("row1", "b", "x")));
    assertTrue(range3.contains(nk("row1", "c", "")));
    assertTrue(range3.contains(nk("row1", "c", "z")));
    assertTrue(range3.contains(nk("row1", "d", "")));
    assertTrue(range3.contains(nk("row1", "d", "z")));
    assertFalse(range3.contains(nk("row1", "e", "")));
    assertFalse(range3.contains(nk("row1", "f", "x")));

    Range range4 = range1.bound(nc("c", "w"), nc("d", "z"));
    assertFalse(range4.contains(nk("row1", "b", "x")));
    assertTrue(range4.contains(nk("row1", "c", "w")));
    assertTrue(range4.contains(nk("row1", "c", "w", "")));
    assertTrue(range4.contains(nk("row1", "c", "w", "a")));
    assertTrue(range4.contains(nk("row1", "d", "z", "")));
    assertTrue(range4.contains(nk("row1", "d", "z", "a")));
    assertFalse(range4.contains(nk("row1", "d", "{", "")));
    assertFalse(range4.contains(nk("row1", "d", "z", "a").followingKey(PartialKey.ROW_COLFAM_COLQUAL)));
    assertFalse(range4.contains(nk("row1", "f", "x")));

    Range range5 = range1.bound(nc("b", "w"), nc("f", "z"));
    assertEquals(range1, range5);
    assertFalse(range5.contains(nk("row1", "b", "w")));
    assertTrue(range5.contains(nk("row1", "b", "x")));
    assertTrue(range5.contains(nk("row1", "f", "x")));
    assertFalse(range5.contains(nk("row1", "f", "z")));

    Range range6 = range1.bound(nc("b", "y"), nc("f", "w"));
    assertFalse(range6.contains(nk("row1", "b", "x")));
    assertTrue(range6.contains(nk("row1", "b", "y")));
    assertTrue(range6.contains(nk("row1", "f", "w")));
    assertTrue(range6.contains(nk("row1", "f", "w", "a")));
    assertFalse(range6.contains(nk("row1", "f", "w").followingKey(PartialKey.ROW_COLFAM_COLQUAL)));
    assertFalse(range6.contains(nk("row1", "f", "x")));

    Range range7 = range1.bound(nc("a", "y"), nc("g", "w"));
    assertEquals(range1, range7);
    assertFalse(range7.contains(nk("row1", "b", "w")));
    assertTrue(range7.contains(nk("row1", "b", "x")));
    assertTrue(range7.contains(nk("row1", "f", "x")));
    assertFalse(range7.contains(nk("row1", "f", "z")));
  }

  public void testString() {
    Range r1 = new Range(new Text("r1"));
    Range r2 = new Range("r1");
    assertEquals(r1, r2);

    r1 = new Range(new Text("r1"), new Text("r2"));
    r2 = new Range("r1", "r2");
    assertEquals(r1, r2);

    r1 = new Range(new Text("r1"), false, new Text("r2"), true);
    r2 = new Range("r1", false, "r2", true);
    assertEquals(r1, r2);

    r1 = new Range(new Text("r1"), true, new Text("r2"), false);
    r2 = new Range("r1", true, "r2", false);
    assertEquals(r1, r2);

  }

  public void testExactRange() {
    Range r = Range.exact("abc");
    assertTrue(r.contains(new Key("abc")));
    assertTrue(r.contains(new Key("abc", "def")));
    assertFalse(r.contains(new Key("abcd")));
    assertFalse(r.contains(new Key("abb")));
    assertFalse(r.contains(new Key("abd")));

    r = Range.exact("abc", "def");
    assertTrue(r.contains(new Key("abc", "def", "ghi")));
    assertFalse(r.contains(new Key("abc", "defg")));
    assertFalse(r.contains(new Key("abc", "dee")));
    assertFalse(r.contains(new Key("abc", "deg")));

    r = Range.exact("abc", "def", "ghi");
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&k")));
    assertFalse(r.contains(new Key("abc", "def", "ghij")));
    assertFalse(r.contains(new Key("abc", "def", "ghh")));
    assertFalse(r.contains(new Key("abc", "def", "ghj")));

    r = Range.exact("abc", "def", "ghi", "j&k");
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&k", 7l)));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&kl")));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&j")));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&l")));

    r = Range.exact("abc", "def", "ghi", "j&k", 7l);
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&k", 7l)));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&k", 6l)));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&k", 8l)));
  }

  public void testPrefixRange() {
    Range r = Range.prefix("abc");
    assertTrue(r.contains(new Key("abc")));
    assertTrue(r.contains(new Key("abc", "def")));
    assertTrue(r.contains(new Key("abcd")));
    assertFalse(r.contains(new Key("abb")));
    assertFalse(r.contains(new Key("abd")));

    r = Range.prefix("abc", "def");
    assertTrue(r.contains(new Key("abc", "def", "ghi")));
    assertTrue(r.contains(new Key("abc", "defg")));
    assertFalse(r.contains(new Key("abc", "dee")));
    assertFalse(r.contains(new Key("abc", "deg")));

    r = Range.prefix("abc", "def", "ghi");
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&k")));
    assertTrue(r.contains(new Key("abc", "def", "ghij")));
    assertFalse(r.contains(new Key("abc", "def", "ghh")));
    assertFalse(r.contains(new Key("abc", "def", "ghj")));

    r = Range.prefix("abc", "def", "ghi", "j&k");
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&k", 7l)));
    assertTrue(r.contains(new Key("abc", "def", "ghi", "j&kl")));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&j")));
    assertFalse(r.contains(new Key("abc", "def", "ghi", "j&l")));

    r = Range.prefix(makeText((byte) 0x07, (byte) 0xff));
    assertTrue(r.contains(new Key(makeText((byte) 0x07, (byte) 0xff))));
    assertTrue(r.contains(new Key(makeText((byte) 0x07, (byte) 0xff, (byte) 0x00))));
    assertFalse(r.contains(new Key(makeText((byte) 0x07, (byte) 0xfe))));
    assertFalse(r.contains(new Key(makeText((byte) 0x08))));

    r = Range.prefix(makeText((byte) 0xff));
    assertTrue(r.isInfiniteStopKey());
    assertTrue(r.contains(new Key(makeText((byte) 0xff))));
    assertTrue(r.contains(new Key(makeText((byte) 0xff, (byte) 0x07))));

    r = Range.prefix(new Text("abc"), makeText((byte) 0xff));
    assertTrue(r.contains(new Key(new Text("abc"), makeText((byte) 0xff))));
    assertTrue(r.contains(new Key(new Text("abc"), makeText((byte) 0xff, (byte) 0x07))));
    assertFalse(r.contains(new Key(new Text("abcd"))));
    assertFalse(r.contains(new Key(new Text("abd"))));

    r = Range.prefix(new Text("abc"), new Text("def"), makeText((byte) 0xff));
    assertTrue(r.contains(new Key(new Text("abc"), new Text("def"), makeText((byte) 0xff))));
    assertTrue(r.contains(new Key(new Text("abc"), new Text("def"), makeText((byte) 0xff, (byte) 0x07))));
    assertFalse(r.contains(new Key(new Text("abc"), new Text("defg"))));
    assertFalse(r.contains(new Key(new Text("abc"), new Text("deg"))));

    r = Range.prefix(new Text("abc"), new Text("def"), new Text("ghi"), makeText((byte) 0xff));
    assertTrue(r.contains(new Key(new Text("abc"), new Text("def"), new Text("ghi"), makeText((byte) 0xff))));
    assertTrue(r.contains(new Key(new Text("abc"), new Text("def"), new Text("ghi"), makeText((byte) 0xff, (byte) 0x07))));
    assertFalse(r.contains(new Key(new Text("abc"), new Text("def"), new Text("ghij"))));
    assertFalse(r.contains(new Key(new Text("abc"), new Text("def"), new Text("ghj"))));
  }

  public static Text makeText(byte... b) {
    return new Text(b);
  }

  public void testPrefix() {
    assertEquals(Range.followingPrefix(makeText((byte) 0x07)), new Text(makeText((byte) 0x08)));
    assertEquals(Range.followingPrefix(makeText((byte) 0xfe)), new Text(makeText((byte) 0xff)));
    assertNull(Range.followingPrefix(makeText((byte) 0xff)));
    assertNull(Range.followingPrefix(makeText((byte) 0xff, (byte) 0xff)));
    assertEquals(Range.followingPrefix(makeText((byte) 0x07, (byte) 0xff)), new Text(makeText((byte) 0x08)));
  }

  public void testReadFields() throws Exception {
    Range r = nr("nuts", "soup");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    r.write(dos);
    dos.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    DataInputStream dis = new DataInputStream(bais);
    Range r2 = new Range();
    r2.readFields(dis);
    dis.close();

    assertEquals(r, r2);
  }

  public void testReadFields_Check() throws Exception {
    Range r = new Range(new Key(new Text("soup")), true, false, new Key(new Text("nuts")), true, false);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    r.write(dos);
    dos.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    DataInputStream dis = new DataInputStream(bais);
    Range r2 = new Range();
    try {
      r2.readFields(dis);
      fail("readFields allowed invalid range");
    } catch (InvalidObjectException exc) {
      /* good! */
    } finally {
      dis.close();
    }
  }

  public void testThrift() {
    Range r = nr("nuts", "soup");
    TRange tr = r.toThrift();
    Range r2 = new Range(tr);
    assertEquals(r, r2);
  }

  public void testThrift_Check() {
    Range r = new Range(new Key(new Text("soup")), true, false, new Key(new Text("nuts")), true, false);
    TRange tr = r.toThrift();
    try {
      @SuppressWarnings("unused")
      Range r2 = new Range(tr);
      fail("Thrift constructor allowed invalid range");
    } catch (IllegalArgumentException exc) {
      /* good! */
    }
  }
}
