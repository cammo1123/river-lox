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

  protected WaterNode(String name) {
    this.name = name;
  }

  public void addInflow(WaterNode upstream) {
    if (upstream == null) throw new IllegalArgumentException("upstream null");
    inflows.add(upstream);
  }

  /**
   * Public calculate entry; returns outflow time-series. Length is
   * rainfall.length + cumulative lag upstream.
   */
  public abstract double[] calculate(double[] rainfall);

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

  protected void tree(StringBuilder sb, String prefix, boolean isTail, Set<WaterNode> visited) {
    if (visited.contains(this)) {
        sb.append(prefix)
          .append(isTail ? "\\\\-- " : "|-- ")
          .append("[cycle ").append(name).append("]\n");
        return;
    }

    sb.append(prefix)
      .append(isTail ? "\\\\-- " : "|-- ")
      .append(nodeLabel()).append('\n');

    visited.add(this);
    Iterator<WaterNode> it = inflows.iterator();
    while (it.hasNext()) {
        WaterNode child = it.next();
        boolean last = !it.hasNext();
        child.tree(sb, prefix + (isTail ? "    " : "|   "), last, visited);
    }
    visited.remove(this);
}

  protected String nodeLabel() {
    return getClass().getSimpleName().charAt(0) + ": " + name;
  }

  /** Node-specific lag in days (rivers override, dams return 0). */
  protected abstract int localLagDays();

  private int computeMaxAccumulatedLag(Map<WaterNode, Integer> memo, Set<WaterNode> visiting) {
    if (memo.containsKey(this)) return memo.get(this);
    if (visiting.contains(this))
      throw new IllegalStateException("Cycle detected in network at " + name);
    visiting.add(this);

    int maxUp = 0;
    for (WaterNode in : inflows) {
      int t = in.computeMaxAccumulatedLag(memo, visiting);
      if (t > maxUp) maxUp = t;
    }
    int total = localLagDays() + maxUp;
    memo.put(this, total);
    visiting.remove(this);
    return total;
  }

  /** Convenience: maximum accumulated lag (this node + upstream). */
  protected int maxAccumulatedLag() {
    return computeMaxAccumulatedLag(new HashMap<>(), new HashSet<>());
  }

  /**
   * Recursive helper that supports memoization to avoid repeated work when
   * multiple downstream nodes query the same upstream node.
   */
  protected double[] calculateWithMemo(double[] rainfall,
                                       Map<WaterNode, double[]> memo) {
    if (memo.containsKey(this)) return memo.get(this);

    int extraLag = maxAccumulatedLag();
    int outLen = rainfall.length + extraLag;
    double[] out = new double[outLen];

    // Sum upstream outflows (upstreams already include their own lags).
    for (WaterNode in : inflows) {
      double[] inOut = in.calculateWithMemo(rainfall, memo);
      for (int i = 0; i < inOut.length; i++) out[i] += inOut[i];
    }

    memo.put(this, out);
    return out;
  }
}
