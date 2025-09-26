package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class River extends WaterNode {
  private final LoxCallable area;
  private final LoxCallable lagDays;

  public River(
      Interpreter interpreter, String name, LoxCallable area,
      LoxCallable lagDays) {
    super(interpreter, name);
    this.area = area;
    this.lagDays = lagDays;
  }

  public double getArea() {
    Object areaObject = area.call(interpreter, List.of());
    if (areaObject instanceof Double calcArea) {
      return calcArea;
    }
    throw new RuntimeError(
        new Token(TokenType.EOF, name, (Object) null, 0),
        "Property 'area' must return a number.");
  }

  public int getLagDays(int day) {
    Object lagObject = lagDays.call(interpreter, List.of((double) day));
    if (lagObject instanceof Double calcLag) {
      return Math.max(0, calcLag.intValue());
    }
    throw new RuntimeError(
        new Token(TokenType.EOF, name, (Object) null, 0),
        "Property 'lag' must return a number.");
  }

  @Override
  protected int localLagDays(int day) {
    return getLagDays(day);
  }

  @Override
  protected NodeOutputs doCalculateDetailed(
      int days,
      double[] rainfall,
      Set<WaterNode> visiting,
      DetailedResult res) {
    // Gather upstream per-edge outflows.
    List<double[]> upstreamPerEdge = new ArrayList<>(inflows.size());
    for (WaterNode in : inflows) {
      NodeOutputs child =
          evalChildDetailed(in, days, rainfall, visiting, res);
      upstreamPerEdge.add(child.perEdgeOut);
    }

    double areaVal = getArea();
    double[] totalOut = new double[days];
    double[] backlog = new double[days];
    double backlogSum = 0.0;

    for (int day = 0; day < days; day++) {
      double prevDue = totalOut[day];

      double incoming = 0.0;

      // Sum upstream outflows entering this node today (already split).
      for (int k = 0; k < upstreamPerEdge.size(); k++) {
        double[] inOut = upstreamPerEdge.get(k);
        if (day < inOut.length) {
          incoming += inOut[day];
        }
      }

      // Local rainfall contribution at this node.
      if (day < rainfall.length) {
        // mm * km^2 => ML
        incoming += rainfall[day] * areaVal;
      }

      // Apply node's lag to total incoming.
      int lag = localLagDays(day);
      int idx = day + lag;
      
      if (idx < days) {
        totalOut[idx] += incoming;
        if (lag > 0) {
          // Newly queued for a future day; counts as backlog.
          backlogSum += incoming;
        }
      }

      // Today's outflow is totalOut[day]; remove only what was already queued
      // before today (prevDue). New lag==0 additions should not affect backlog.
      backlogSum -= prevDue;
      backlog[day] = backlogSum;
    }

    // Split evenly across downstream outflows for propagation.
    int branches = Math.max(1, downstreamCount());
    double[] perEdgeOut;
    if (branches > 1) {
      perEdgeOut = new double[days];
      for (int i = 0; i < days; i++) perEdgeOut[i] = totalOut[i] / branches;
    } else {
      perEdgeOut = totalOut.clone();
    }

    return new NodeOutputs(totalOut, perEdgeOut, backlog);
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ area=" + area + ", lag=" + lagDays + " ]";
  }
}