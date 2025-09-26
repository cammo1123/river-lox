package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Dam extends WaterNode {
  // Lox callable: (curr_vol) => volAsOutflow
  private final LoxCallable outFlow;

  public Dam(Interpreter interpreter, String name, LoxCallable outFlow) {
    super(interpreter, name);
    this.outFlow = outFlow;
  }

  @Override
  protected int localLagDays(int day) {
    return 0;
  }

  @Override
  protected NodeOutputs doCalculateDetailed(int days, double[] rainfall,
                                            Set<WaterNode> visiting,
                                            DetailedResult res) {
    // Gather upstream per-edge outflows.
    List<double[]> upstreamPerEdge = new ArrayList<>(inflows.size());
    for (WaterNode in : inflows) {
      NodeOutputs child = evalChildDetailed(in, days, rainfall, visiting, res);
      upstreamPerEdge.add(child.perEdgeOut);
    }

    double[] totalOut = new double[days];
    double[] backlog = new double[days];

    // Stored water carried day-to-day.
    double stored = 0.0;

    for (int day = 0; day < days; day++) {
      // Today's inflow (already split by upstream).
      double incoming = 0.0;
      for (int k = 0; k < upstreamPerEdge.size(); k++) {
        double[] inOut = upstreamPerEdge.get(k);
        if (day < inOut.length)
          incoming += inOut[day];
      }

      // Current available volume before release.
      double currVol = stored + incoming;

      // Ask policy how much to release today.
      double requested = computeRelease(currVol);

      // Clamp to [0, currVol].
      double outToday = Math.max(0.0, Math.min(requested, currVol));
      totalOut[day] = outToday;

      // Remaining stays in storage for future days.
      stored = currVol - outToday;
      backlog[day] = stored;
    }

    // Split evenly across downstream outflows for propagation.
    int branches = Math.max(1, downstreamCount());
    double[] perEdgeOut;
    if (branches > 1) {
      perEdgeOut = new double[days];
      for (int i = 0; i < days; i++)
        perEdgeOut[i] = totalOut[i] / branches;
    } else {
      perEdgeOut = totalOut.clone();
    }

    return new NodeOutputs(totalOut, perEdgeOut, backlog);
  }

  private double computeRelease(double currVol) {
    Object flowObj = outFlow.call(interpreter, List.of(currVol));
    double out;
    if (flowObj instanceof Double d) {
      out = d;
    } else {
      throw new RuntimeError(
          new Token(TokenType.EOF, name, (Object)null, 0),
          "Property 'out_flow' must be a function returning a number.");
    }
    if (Double.isNaN(out) || Double.isInfinite(out))
      return 0.0;
    return out;
  }

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() + " [ out_flow=" + outFlow + " ]";
  }
}