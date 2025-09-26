package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public 
class Dam extends WaterNode {
  private final LoxCallable flowPercent;

  public Dam(Interpreter interpreter, String name, LoxCallable flowPercent) {
    super(interpreter, name);
    this.flowPercent = flowPercent;
  }

  public double getFlowPercent() {
    Object flowObject = flowPercent.call(interpreter, List.of());

    if (flowObject instanceof Double calcFlow) {
      return calcFlow;
    }

    throw new RuntimeError(new Token(TokenType.EOF, name, (Object) null, 0), "Property 'lag' must return a number.");
  }

  @Override
  protected int localLagDays(int day) {
    return 0;
  }

  @Override
  public double[] calculate(int days, double[] rainfall) {
    if (rainfall == null) throw new IllegalArgumentException("rainfall null");
    Map<WaterNode, double[]> memo = new HashMap<>();
    return calculateWithMemo(days, rainfall, memo);
  }

  @Override
  protected double[] calculateWithMemo(int days, double[] rainfall, Map<WaterNode, double[]> memo) {
    if (memo.containsKey(this)) return memo.get(this);

    double[] out = new double[days];

    // sum upstream outflows
    for (WaterNode in : inflows) {
      double[] inOut = in.calculateWithMemo(days, rainfall, memo);
      for (int i = 0; i < inOut.length; i++) out[i] += inOut[i];
    }

    // dams currently pass inflow through (flowPercent kept for later use)
    if (getFlowPercent() != 1.0) {
      for (int i = 0; i < days; i++) out[i] *= getFlowPercent();
    }

    memo.put(this, out);
    return out;
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ flowPercent=" + flowPercent + " ]";
  }
}
