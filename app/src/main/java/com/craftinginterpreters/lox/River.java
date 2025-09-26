package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.UnitVal.Kind;
import com.craftinginterpreters.lox.UnitVal.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class River extends WaterNode {
  private final double area;
  private final double flowDays;
  private final LoxCallable flowShape;

  public River(Interpreter interpreter, String name, double area, double flowDays,
               LoxCallable flowShape) {
    super(interpreter, name);
    this.area = area;
    this.flowDays = flowDays;
    this.flowShape = flowShape;
  }

  public double getArea() { return area; }

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

    double areaVal = getArea();
    double[] totalOut = new double[days];
    double[] backlog = new double[days];
    double backlogSum = 0.0;

    // number of days to consider from the flow shape
    int shapeLen = Math.max(1, (int) Math.ceil(flowDays));

    for (int day = 0; day < days; day++) {
      // Amount that was due today from previous scheduling.
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
        // mm * km^2 => ML (canonical)
        incoming += UnitVal.of((rainfall[day] * areaVal), Unit.MEGALITER).asCanonical();
      }

      // Distribute incoming across subsequent days according to flowShape.
      for (int k = 0; k < shapeLen; k++) {
        int idx = day + k;
        if (idx >= days) {
          // Outside simulation window: drop (consistent with prior behavior).
          continue;
        }

        Object fracObj = flowShape.call(interpreter, List.of((double) k, flowDays));
        if (!(fracObj instanceof Double f)) {
          throw new RuntimeError(
              new Token(TokenType.EOF, name, (Object) null, 0),
              "Property 'flow_shape' must return a number.");
        }
        double frac = f;
        if (Double.isNaN(frac) || Double.isInfinite(frac)) {
          frac = 0.0;
        }

        double amount = incoming * frac;
        totalOut[idx] += amount;

        // If this portion is scheduled for a future day (k > 0) and within
        // the simulated horizon, count it toward backlog.
        if (k > 0) {
          backlogSum += amount;
        }
      }

      // Remove what's due today from backlog (it is leaving storage now).
      backlogSum -= prevDue;
      if (backlogSum < 0.0) backlogSum = 0.0; // guard against tiny negatives
      backlog[day] = backlogSum;
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

  @Override
  protected String nodeLabel() {
    return super.nodeLabel() +
        " [ area=" + UnitVal.ofCanonical(area, Kind.AREA).toString() +
        ", flow_days=" + flowDays +
        ", flow_shape=" + flowShape + " ]";
  }
}