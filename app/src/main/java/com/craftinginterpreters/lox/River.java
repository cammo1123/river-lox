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
    List<double[]> upstreamPerEdge = new ArrayList<>(inflows.size());
    for (WaterNode in : inflows) {
      NodeOutputs child = evalChildDetailed(in, days, rainfall, visiting, res);
      upstreamPerEdge.add(child.perEdgeOut);
    }

    double areaVal = getArea();
    double[] totalOut = new double[days];
    double[] backlog = new double[days];
    double backlogSum = 0.0;

    int shapeLen = Math.max(1, (int) Math.ceil(flowDays));
    
    for (int day = 0; day < days; day++) {
      double prevDue = totalOut[day];
      double incoming = 0.0;

      for (int k = 0; k < upstreamPerEdge.size(); k++) {
        double[] inOut = upstreamPerEdge.get(k);
        if (day < inOut.length) {
          incoming += inOut[day];
        }
      }

      if (day < rainfall.length) {
        // mm * km^2 * 1,000,000 => L (canonical)
        incoming += UnitVal.of((rainfall[day] * areaVal * 1_000_000), Unit.L).asCanonical();
      }

      double totalUsed = 0.0;
      for (int k = 0; k < shapeLen; k++) {
        int idx = day + k;
        if (idx >= days) {
          continue;
        }

        Object fracObj = flowShape.call(interpreter, List.of((double) k + 1, flowDays));
        if (!(fracObj instanceof Double frac)) {
          throw new RuntimeError(
              new Token(TokenType.EOF, name, (Object) null, 0),
              "Property 'flow_shape' must return a number.");
        }

        if (Double.isNaN(frac) || Double.isInfinite(frac)) {
          frac = 0.0;
        }

        if (frac > 1) {
          frac = 1.0;
        }
        
        totalUsed += frac;
        if (totalUsed > 1) {
          frac = 0.0;
        }

        double amount = incoming * frac;
        totalOut[idx] += amount;

        if (k > 0) {
          backlogSum += amount;
        }
      }

      backlogSum -= prevDue;
      if (backlogSum < 0.0) backlogSum = 0.0;
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