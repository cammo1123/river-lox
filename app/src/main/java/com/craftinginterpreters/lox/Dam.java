package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public 
class Dam extends WaterNode {
  private final double flowPercent;

  public Dam(String name, double flowPercent) {
    super(name);
    this.flowPercent = flowPercent;
  }

  @Override
  protected int localLagDays() {
    return 0;
  }

  @Override
  public double[] calculate(double[] rainfall) {
    if (rainfall == null) throw new IllegalArgumentException("rainfall null");
    Map<WaterNode, double[]> memo = new HashMap<>();
    return calculateWithMemo(rainfall, memo);
  }

  @Override
  protected double[] calculateWithMemo(double[] rainfall,
                                       Map<WaterNode, double[]> memo) {
    if (memo.containsKey(this)) return memo.get(this);

    int extraLag = maxAccumulatedLag();
    int outLen = rainfall.length + extraLag;
    double[] out = new double[outLen];

    // sum upstream outflows
    for (WaterNode in : inflows) {
      double[] inOut = in.calculateWithMemo(rainfall, memo);
      for (int i = 0; i < inOut.length; i++) out[i] += inOut[i];
    }

    // dams currently pass inflow through (flowPercent kept for later use)
    if (flowPercent != 1.0) {
      for (int i = 0; i < outLen; i++) out[i] *= flowPercent;
    }

    memo.put(this, out);
    return out;
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ flowPercent=" + flowPercent + " ]";
  }
}
