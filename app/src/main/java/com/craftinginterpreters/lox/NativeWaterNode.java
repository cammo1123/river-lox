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

          // Detailed run: collect outflows and per-day volumes for all nodes.
          WaterNode.DetailedResult res =
              node.calculateDetailed(daysToSim, rainfall);

          // Build the "Volume" section for ALL rivers (not only leaves).
          // Note: In cascades this double-counts if you sum columns.
          List<WaterNode> rivers = sortedRivers(res.volumeByNode);

          StringBuilder sb = new StringBuilder();

          // Determine column widths.
          int dayColWidth = Math.max(3, String.valueOf(daysToSim).length());
          List<String> headers = new ArrayList<>();
          headers.add("DAY");
          for (WaterNode r : rivers)
            headers.add(r.name);

          // Pre-format ML values to size columns.
          List<List<String>> volCells =
              formatMlColumns(rivers, res.volumeByNode, daysToSim);

          // Day width already set, compute others (for river columns).
          List<Integer> colWidths =
              computeColumnWidths(headers.subList(1, headers.size()), volCells);

          // Build title centered with '=' fill across full row width.
          List<Integer> volumeRowWidths = joinWidths(dayColWidth, colWidths);
          int volumeTableWidth = tableWidth(volumeRowWidths);
          sb.append(centerEquals(" Volume (End of day) ", volumeTableWidth))
              .append('\n');

          // Print header row: DAY + river names
          sb.append(renderRow(headers, volumeRowWidths));

          // Rows per day (1..days)
          for (int d = 0; d < daysToSim; d++) {
            List<String> row = new ArrayList<>(1 + rivers.size());
            row.add(String.valueOf(d + 1));
            for (int c = 0; c < rivers.size(); c++) {
              row.add(volCells.get(c).get(d));
            }
            sb.append(renderRow(row, volumeRowWidths));
          }

          // Horizontal rule between sections
          sb.append(renderDivider(volumeRowWidths, '=')).append('\n');

          // Outflow section (root node total outflow)
          double[] rootOut = res.totalOutByNode.getOrDefault(node, new double[daysToSim]);
          double[] rootBacklog = res.volumeByNode.getOrDefault(node, new double[daysToSim]);

          // Compute col width for day numbers and values
          int dayNumWidth = Math.max(2, String.valueOf(daysToSim).length());
          int valWidth = 0;
          List<String> outVals = new ArrayList<>();
          List<String> storeVals = new ArrayList<>();
          for (int d = 0; d < daysToSim; d++) {
            String v = UnitVal.ofCanonical(rootOut[d], Kind.VOLUME).toString();
            String s = UnitVal.ofCanonical(rootBacklog[d], Kind.VOLUME).toString();
            outVals.add(v);
            storeVals.add(s);
            valWidth = Math.max(valWidth, Math.max(v.length(), s.length()));
          }
          int cellWidth = Math.max(dayNumWidth, valWidth);

          // First column width sized for longest label among
          // Day/Outflow/Storage
          int firstLabelWidth = Math.max("Day".length(), Math.max("Outflow".length(), "Storage".length()));

          List<Integer> outflowRowWidths = joinWidths(firstLabelWidth, repeatWidth(cellWidth, daysToSim));
          int outflowTableWidth = tableWidth(outflowRowWidths);
          sb.append(centerEquals(" Outflow ", outflowTableWidth)).append('\n');

          // "Day" header row
          List<String> dayHeader = new ArrayList<>();
          dayHeader.add("Day");
          for (int d = 1; d <= daysToSim; d++)
            dayHeader.add(String.valueOf(d));
          sb.append(renderRowLeftFirst(dayHeader, outflowRowWidths));

          // Divider row
          sb.append(renderDivider(outflowRowWidths, '-'));

          // Outflow row
          List<String> outflowRow = new ArrayList<>();
          outflowRow.add("Outflow");
          outflowRow.addAll(outVals);
          sb.append(renderRowLeftFirst(outflowRow, outflowRowWidths));

          // Storage row (end-of-day dam volume)
          List<String> storageRow = new ArrayList<>();
          storageRow.add("Storage");
          storageRow.addAll(storeVals);
          sb.append(renderRowLeftFirst(storageRow, outflowRowWidths));

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

  // Center text within a field of width w using the given fill char.
  // If w <= s.length(), returns s unchanged.
  private static String center(String s, int w, char fill) {
    int pad = w - s.length();
    if (pad <= 0)
      return s;
    int left = pad / 2;
    int right = pad - left; // right gets the extra when pad is odd
    return repeat(fill, left) + s + repeat(fill, right);
  }

  // Equivalent of the old padMiddle, but correct and reusable.
  private static String centerEquals(String s, int w) {
    return center(s, w, '=');
  }

  private static String repeat(char c, int n) {
    if (n <= 0)
      return "";
    return String.valueOf(c).repeat(n);
  }

  // ---------- Helpers extracted for table rendering ----------

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

  // For each river, format per-day values to ML strings to size columns.
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

  // Compute widths for columns after the first, by considering headers and
  // cells.
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

  // Render a row like: "| <cell0 padded> | <cell1 padded> | ..."
  // By default, all columns are right-aligned (numbers). Use
  // renderRowLeftFirst if the first column should be left-aligned.
  private static String renderRow(List<String> cells, List<Integer> widths) {
    StringBuilder sb = new StringBuilder();
    sb.append("| ");
    // first column right-aligned by default
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
    // first column left-aligned
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
    // Dividers look better with first column left, but padding is same length.
    return renderRowLeftFirst(cells, widths);
  }

  // Compute full row width for centering: sum(widths) + 3*k + 1
  // Each column contributes: " " + content(width) + " |" => width + 3
  // Plus initial leading "| " => 2, which is included by the formula (3*k + 1).
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