package lox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lox.PrintableTable.BarStyle;
import lox.UnitVal.Kind;

class NativeWaterNode {
  final WaterNode node;

  NativeWaterNode(WaterNode node) { this.node = node; }

  Object get(Token name) {
    String n = name.lexeme;
    switch (n) {
    case "tree":
      return node.tree();
    case "calculate":
      return new LoxCallable() {
        @Override
        public int arity() {
          return 2;
        }

        @Override
        public Object call(Interpreter i, java.util.List<Object> args) {
          double[] rainfall = toDoubleArray(args.get(1), name);
          int daysToSim = ((Double)args.get(0)).intValue();

          WaterNode.DetailedResult res =
              node.calculateDetailed(daysToSim, rainfall);
          List<WaterNode> rivers = sortedRivers(res.volumeByNode);
          StringBuilder sb = new StringBuilder();

          // Volume table
          List<String> headers = new ArrayList<>();
          headers.add("");
          for (WaterNode r : rivers)
            headers.add(r.name);

          List<List<String>> volCells =
              formatMlColumns(rivers, res.volumeByNode, daysToSim);

          PrintableTable volumeTable =
              new PrintableTable(false, BarStyle.DOUBLE);
          volumeTable.addTitle(" Volume (After Outflow) ")
              .addRow(headers)
              .addHeaderDivider();

          for (int d = 0; d < daysToSim; d++) {
            List<String> row = new ArrayList<>(1 + rivers.size());
            row.add("Day " + (d + 1));
            for (int c = 0; c < rivers.size(); c++) {
              row.add(volCells.get(c).get(d));
            }
            volumeTable.addRow(row);
          }

          volumeTable.addEndCap();
          sb.append(volumeTable.render());

          // Outflow table
          double[] rootOut =
              res.totalOutByNode.getOrDefault(node, new double[daysToSim]);
          double[] rootBacklog =
              res.volumeByNode.getOrDefault(node, new double[daysToSim]);

          int valWidth = 0;
          List<String> outVals = new ArrayList<>();
          List<String> accumVals = new ArrayList<>();
          List<String> storeVals = new ArrayList<>();
          double accum = 0.0;
          for (int d = 0; d < daysToSim; d++) {
            String v = UnitVal.ofCanonical(rootOut[d], Kind.VOLUME).toString();
            accum += rootOut[d];
            String a = UnitVal.ofCanonical(accum, Kind.VOLUME).toString();
            String s =
                UnitVal.ofCanonical(rootBacklog[d], Kind.VOLUME).toString();
            outVals.add(v);
            accumVals.add(a);
            storeVals.add(s);
            valWidth =
                Math.max(valWidth, Math.max(Math.max(v.length(), a.length()),
                                            s.length()));
          }

          PrintableTable outflowTable =
              new PrintableTable(true, BarStyle.DOUBLE);
          outflowTable.addTitle(" " + node.name + " Outflow ")
              .addRow(buildDayHeader(daysToSim))
              .addHeaderDivider()
              .addRow(joinRow("Outflow", outVals))
              .addRow(joinRow("Storage", storeVals))
              .addDivider()
              .addRow(joinRow("Accumulated", accumVals))
              .addEndCap();
          sb.append(outflowTable.render());

          return sb.toString();
        }

        @Override
        public String toString() {
          return "<native fn calculate>";
        }
      };
    default:
      throw new RuntimeError(name, "Unknown property '" + n + "'.");
    }
  }

  void addInflow(NativeWaterNode upstream) { node.addInflow(upstream.node); }

  private static double[] toDoubleArray(Object value, Token where) {
    if (!(value instanceof java.util.List<?> list)) {
      throw new RuntimeError(where, "Expected array (list) of numbers.");
    }
    double[] out = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      Object v = list.get(i);
      if (v instanceof Double d) {
        out[i] = d; // mm
      } else {
        throw new RuntimeError(where, "Array element " + i +
                                          " is not a number or unit.");
      }
    }
    return out;
  }

  private static List<WaterNode>
  sortedRivers(Map<WaterNode, double[]> volumeByNode) {
    List<WaterNode> rivers = new ArrayList<>();
    for (Map.Entry<WaterNode, double[]> e : volumeByNode.entrySet()) {
      WaterNode wn = e.getKey();
      if (wn instanceof River) {
        rivers.add(wn);
      }
    }
    rivers.sort(Comparator.comparing(w -> w.name));
    return rivers;
  }

  private static List<List<String>>
  formatMlColumns(List<WaterNode> rivers, Map<WaterNode, double[]> volumeByNode,
                  int days) {
    List<List<String>> cols = new ArrayList<>();
    for (WaterNode r : rivers) {
      double[] vol = volumeByNode.get(r);
      List<String> col = new ArrayList<>(days);
      for (int d = 0; d < days; d++) {
        col.add(UnitVal.ofCanonical(vol[d], Kind.VOLUME).toString());
      }
      cols.add(col);
    }
    return cols;
  }

  private static List<String> buildDayHeader(int daysToSim) {
    List<String> dayHeader = new ArrayList<>();
    dayHeader.add("");
    for (int d = 1; d <= daysToSim; d++)
      dayHeader.add("Day " + d);
    return dayHeader;
  }

  private static List<String> joinRow(String label, List<String> vals) {
    List<String> row = new ArrayList<>(1 + vals.size());
    row.add(label);
    row.addAll(vals);
    return row;
  }
}