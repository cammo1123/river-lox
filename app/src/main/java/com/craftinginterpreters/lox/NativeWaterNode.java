package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.UnitVal.Kind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

          // Determine column widths.
          int dayColWidth = Math.max(3, String.valueOf(daysToSim).length());
          List<String> headers = new ArrayList<>();
          headers.add("DAY");
          for (WaterNode r : rivers)
            headers.add(r.name);

          List<List<String>> volCells =
              formatMlColumns(rivers, res.volumeByNode, daysToSim);
          List<Integer> colWidths =
              computeColumnWidths(headers.subList(1, headers.size()), volCells);

          List<Integer> volumeRowWidths = joinWidths(dayColWidth, colWidths);
          int volumeTableWidth = tableWidth(volumeRowWidths);
          sb.append(centerEquals("= Volume (After Outflow) =", volumeTableWidth))
              .append('\n');

          sb.append(renderRow(headers, volumeRowWidths));
          sb.append(renderDivider(volumeRowWidths, '_'));

          for (int d = 0; d < daysToSim; d++) {
            List<String> row = new ArrayList<>(1 + rivers.size());
            row.add(String.valueOf(d + 1));
            for (int c = 0; c < rivers.size(); c++) {
              row.add(volCells.get(c).get(d));
            }
            sb.append(renderRow(row, volumeRowWidths));
          }

          sb.append(renderDivider(volumeRowWidths, '=')).append('\n');

          double[] rootOut =
              res.totalOutByNode.getOrDefault(node, new double[daysToSim]);
          double[] rootBacklog =
              res.volumeByNode.getOrDefault(node, new double[daysToSim]);


          int dayNumWidth = Math.max(2, String.valueOf(daysToSim).length());
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
          int cellWidth = Math.max(dayNumWidth, valWidth);

          int firstLabelWidth = Math.max(
              Math.max("Day".length(), "Outflow".length()),
              Math.max("Storage".length(), "Accumulated".length()));

          List<Integer> outflowRowWidths =
              joinWidths(firstLabelWidth, repeatWidth(cellWidth, daysToSim));
          int outflowTableWidth = tableWidth(outflowRowWidths);
          sb.append(centerEquals(" " + node.name + " Outflow ", outflowTableWidth)).append('\n');

          List<String> dayHeader = new ArrayList<>();
          dayHeader.add("Day");
          for (int d = 1; d <= daysToSim; d++)
            dayHeader.add(String.valueOf(d));
          sb.append(renderRowLeftFirst(dayHeader, outflowRowWidths));

          sb.append(renderDivider(outflowRowWidths, '_'));

          List<String> outflowRow = new ArrayList<>();
          outflowRow.add("Outflow");
          outflowRow.addAll(outVals);
          sb.append(renderRowLeftFirst(outflowRow, outflowRowWidths));

          List<String> storageRow = new ArrayList<>();
          storageRow.add("Storage");
          storageRow.addAll(storeVals);
          sb.append(renderRowLeftFirst(storageRow, outflowRowWidths));

          sb.append(renderDivider(outflowRowWidths, '-'));

          List<String> accumRow = new ArrayList<>();
          accumRow.add("Accumulated");
          accumRow.addAll(accumVals);
          sb.append(renderRowLeftFirst(accumRow, outflowRowWidths));

          sb.append(renderDivider(outflowRowWidths, '=')).append('\n');

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

  private static String padLeft(String s, int w) {
    int n = Math.max(0, w - s.length());
    return " ".repeat(n) + s;
  }

  private static String padRight(String s, int w) {
    int n = Math.max(0, w - s.length());
    return s + " ".repeat(n);
  }

  private static String center(String s, int w, char fill) {
    int pad = w - s.length();
    if (pad <= 0)
      return s;
    int left = pad / 2;
    int right = pad - left;
    return repeat(fill, left) + s + repeat(fill, right);
  }

  private static String centerEquals(String s, int w) {
    return center(s, w, '=');
  }

  private static String repeat(char c, int n) {
    if (n <= 0)
      return "";
    return String.valueOf(c).repeat(n);
  }

  private static List<WaterNode>
  sortedRivers(Map<WaterNode, double[]> volumeByNode) {
    List<WaterNode> rivers = new ArrayList<>();
    for (Map.Entry<WaterNode, double[]> e : volumeByNode.entrySet()) {
      WaterNode wn = e.getKey();
      if (wn instanceof River)
        rivers.add(wn);
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
      for (int d = 0; d < days; d++)
        col.add(UnitVal.ofCanonical(vol[d], Kind.VOLUME).toString());
      cols.add(col);
    }
    return cols;
  }

  private static List<Integer>
  computeColumnWidths(List<String> headersAfterFirst,
                      List<List<String>> dataCols) {
    List<Integer> widths = new ArrayList<>(dataCols.size());
    for (int c = 0; c < dataCols.size(); c++) {
      int w = headersAfterFirst.get(c).length();
      for (String v : dataCols.get(c))
        w = Math.max(w, v.length());
      widths.add(w);
    }
    return widths;
  }

  private static String renderRow(List<String> cells, List<Integer> widths) {
    StringBuilder sb = new StringBuilder();
    sb.append("| ");
    sb.append(padLeft(cells.get(0), widths.get(0))).append(" |");
    for (int i = 1; i < cells.size(); i++) {
      sb.append(' ').append(padLeft(cells.get(i), widths.get(i))).append(" |");
    }
    sb.append('\n');
    return sb.toString();
  }

  private static String renderRowLeftFirst(List<String> cells,
                                           List<Integer> widths) {
    StringBuilder sb = new StringBuilder();
    sb.append("| ");
    sb.append(padRight(cells.get(0), widths.get(0))).append(" |");
    for (int i = 1; i < cells.size(); i++) {
      sb.append(' ').append(padLeft(cells.get(i), widths.get(i))).append(" |");
    }
    sb.append('\n');
    return sb.toString();
  }

  private static String renderDivider(List<Integer> widths, char fill) {
    List<String> cells =
        widths.stream().map(w -> repeat(fill, w)).collect(Collectors.toList());
    return renderRowLeftFirst(cells, widths);
  }

  private static int tableWidth(List<Integer> widths) {
    int sum = 0;
    for (int w : widths)
      sum += w;
    int k = widths.size();
    return sum + (3 * k) + 1;
  }

  private static List<Integer> joinWidths(int first, List<Integer> rest) {
    List<Integer> all = new ArrayList<>(1 + rest.size());
    all.add(first);
    all.addAll(rest);
    return all;
  }

  private static List<Integer> repeatWidth(int w, int n) {
    List<Integer> list = new ArrayList<>(n);
    for (int i = 0; i < n; i++)
      list.add(w);
    return list;
  }
}