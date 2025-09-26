package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class WaterNode {
  protected final String name;
  protected final List<WaterNode> inflows = new ArrayList<>();
  // Track downstreams to support splitting.
  protected final List<WaterNode> outflows = new ArrayList<>();
  protected final Interpreter interpreter;

  protected WaterNode(Interpreter interpreter, String name) {
    this.interpreter = interpreter;
    this.name = name;
  }

  public void addInflow(WaterNode upstream) {
    if (upstream == null) throw new IllegalArgumentException("upstream null");
    inflows.add(upstream);
    upstream.outflows.add(this); // register downstream link
  }

  public boolean isLeaf() {
    return inflows.isEmpty();
  }

  /**
   * Public calculate entry; returns total outflow time-series for this node.
   * Length is 'days'.
   */
  public double[] calculate(int days, double[] rainfall) {
    DetailedResult res = calculateDetailed(days, rainfall);
    double[] out = res.totalOutByNode.get(this);
    if (out == null) return new double[days];
    return out;
  }

  /**
   * Detailed evaluation returning per-node total outflows and per-day volumes
   * (backlog) within each node. No memoization, cycle-safe.
   */
  public DetailedResult calculateDetailed(int days, double[] rainfall) {
    if (rainfall == null) throw new IllegalArgumentException("rainfall null");
    Set<WaterNode> visiting = new HashSet<>();
    DetailedResult res = new DetailedResult();
    evaluateDetailed(days, rainfall, visiting, res);
    return res;
  }

  // Cycle-safe detailed evaluation wrapper.
  protected final NodeOutputs evaluateDetailed(
      int days,
      double[] rainfall,
      Set<WaterNode> visiting,
      DetailedResult res) {
    if (!visiting.add(this)) {
      throw new RuntimeError(
          new Token(TokenType.EOF, name, (Object) null, 0),
          "Cycle detected at node '" + name + "'.");
    }
    NodeOutputs outs = doCalculateDetailed(days, rainfall, visiting, res);

    double[] totalOut = outs.totalOut == null ? new double[days] : outs.totalOut;
    double[] backlog = outs.backlog == null ? new double[days] : outs.backlog;
    res.totalOutByNode.put(this, totalOut);
    res.volumeByNode.put(this, backlog);

    visiting.remove(this);
    return outs;
  }

  // Helper to evaluate a child node in detailed mode.
  protected final NodeOutputs evalChildDetailed(
      WaterNode node,
      int days,
      double[] rainfall,
      Set<WaterNode> visiting,
      DetailedResult res) {
    return node.evaluateDetailed(days, rainfall, visiting, res);
  }

  protected String nodeLabel() {
    return getClass().getSimpleName().charAt(0) + ": " + name;
  }

  protected int downstreamCount() {
    return outflows.size();
  }

  /** Node-specific lag in days (rivers override, dams return 0). */
  protected abstract int localLagDays(int day);

  /**
   * Subclasses implement per-node logic and must return:
   * - totalOut: the node's total outflow (pre-split)
   * - perEdgeOut: the outflow per downstream edge (post-split),
   *   which is what downstream nodes should receive.
   */
  protected abstract NodeOutputs doCalculateDetailed(
      int days,
      double[] rainfall,
      Set<WaterNode> visiting,
      DetailedResult res);

  public String tree() {
    StringBuilder sb = new StringBuilder();
    Set<WaterNode> visited = new HashSet<>();

    // print root label (no branch glyph for the root)
    sb.append(nodeLabel()).append('\n');
    visited.add(this);

    Iterator<WaterNode> it = inflows.iterator();
    while (it.hasNext()) {
      WaterNode child = it.next();
      boolean last = !it.hasNext();
      child.tree(sb, "", last, visited);
    }

    visited.remove(this);
    return sb.toString();
  }

  protected void tree(
      StringBuilder sb, String prefix, boolean isTail, Set<WaterNode> visited) {
    if (visited.contains(this)) {
      sb.append(prefix)
          .append(isTail ? "\\\\-- " : "|-- ")
          .append("[cycle ")
          .append(name)
          .append("]\n");
      return;
    }

    sb.append(prefix)
        .append(isTail ? "\\\\-- " : "|-- ")
        .append(nodeLabel())
        .append('\n');

    visited.add(this);
    Iterator<WaterNode> it = inflows.iterator();
    while (it.hasNext()) {
      WaterNode child = it.next();
      boolean last = !it.hasNext();
      child.tree(
          sb, prefix + (isTail ? "    " : "|   "), last, visited);
    }
    visited.remove(this);
  }

  // Results container for detailed evaluation.
  public static final class DetailedResult {
    public final Map<WaterNode, double[]> totalOutByNode = new HashMap<>();
    public final Map<WaterNode, double[]> volumeByNode = new HashMap<>();
  }

  // Return both total outflow and per-edge outflow arrays.
  protected static final class NodeOutputs {
    final double[] totalOut;
    final double[] perEdgeOut;
    final double[] backlog;

    NodeOutputs(double[] totalOut, double[] perEdgeOut, double[] backlog) {
      this.totalOut = totalOut;
      this.perEdgeOut = perEdgeOut;
      this.backlog = backlog;
    }
  }
}