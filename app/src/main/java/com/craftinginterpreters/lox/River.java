package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class River extends WaterNode {
  private final double area;
  private final int lagDays;

  public River(String name, double area, int lagDays) {
    super(name);
    this.area = area;
    this.lagDays = lagDays;
  }

  public double getArea() {
    return area;
  }

  public int getLagDays() {
    return lagDays;
  }

  @Override
  protected int localLagDays() {
    return lagDays;
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

    // add local rainfall contributions shifted by this river's lag
    for (int day = 0; day < rainfall.length; day++) {
      // rainfall in mm, area in sqkm → megaLitres:
      double contrib = rainfall[day] * area;
      int idx = day + lagDays;
      if (idx < outLen) out[idx] += contrib;
    }

    memo.put(this, out);
    return out;
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ area=" + area + ", lag=" + lagDays + " ]";
  }
}
