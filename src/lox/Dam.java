package lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Dam extends WaterNode {
  private final LoxCallable outFlow;

  public Dam(Interpreter interpreter, String name, LoxCallable outFlow) {
    super(interpreter, name);
    this.outFlow = outFlow;
  }

  @Override
  protected NodeOutputs doCalculateDetailed(int days, double[] rainfall,
                                            Set<WaterNode> visiting,
                                            DetailedResult res) {
    List<double[]> upstreamPerEdge = new ArrayList<>(inflows.size());
    for (WaterNode in : inflows) {
      NodeOutputs child = evalChildDetailed(in, days, rainfall, visiting, res);
      upstreamPerEdge.add(child.perEdgeOut);
    }

    double[] totalOut = new double[days];
    double[] backlog = new double[days];
    double stored = 0.0;

    for (int day = 0; day < days; day++) {
      double incoming = 0.0;
      for (int k = 0; k < upstreamPerEdge.size(); k++) {
        double[] inOut = upstreamPerEdge.get(k);
        if (day < inOut.length)
          incoming += inOut[day];
      }

      double currVol = stored + incoming;
      double requested = computeRelease(currVol);
      double outToday = Math.max(0.0, Math.min(requested, currVol));
      totalOut[day] = outToday;
      stored = currVol - outToday;
      backlog[day] = stored;
    }

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