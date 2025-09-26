package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class River extends WaterNode {
  private final LoxCallable area;
  private final LoxCallable lagDays;

  // Assume calculate will be called once per day.
  // Should have a stack and you put the current in to the arr[lagDays] pos.
  // Then each day you should pop the bottom, and that's your output.

  public River(Interpreter interpreter, String name, LoxCallable area, LoxCallable lagDays) {
    super(interpreter, name);
    this.area = area;
    this.lagDays = lagDays;
  }

  public double getArea() {
    Object areaObject = area.call(interpreter, List.of());

    if (areaObject instanceof Double calcArea) {
      return calcArea;
    }

    throw new RuntimeError(new Token(TokenType.EOF, name, (Object) null, 0), "Property 'area' must return a number.");
  }

  public int getLagDays(int day) {
    Object lagObject = lagDays.call(interpreter, List.of((double) day));
    if (lagObject instanceof Double calcLag) {
      return calcLag.intValue();
    }

    throw new RuntimeError(new Token(TokenType.EOF, name, (Object) null, 0), "Property 'lag' must return a number.");
  }

  @Override
  protected int localLagDays(int day) {
    return getLagDays(day);
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

    // add local rainfall contributions shifted by this river's lag
    for (int day = 0; day < rainfall.length; day++) {
      // rainfall in mm, area in sqkm → megaLitres:
      double contrib = rainfall[day] * getArea();
      int idx = day + getLagDays(day);
      if (idx < days) out[idx] += contrib;
    }

    memo.put(this, out);
    return out;
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ area=" + area + ", lag=" + lagDays + " ]";
  }
}
