package com.craftinginterpreters.lox;

import java.util.List;

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
          @Override public int arity() { return 1; }
          @Override public Object call(Interpreter i, java.util.List<Object> args) {
            Object a = args.get(0);
            double[] rainfall = toDoubleArray(a, name); // mm per step
            double[] out = node.calculate(rainfall);

            String[] days = new String[out.length];
            for (int j = 0; j < out.length; j++) {
              days[j] = String.valueOf(j + 1);
            }

            PrintableTable table = PrintableTable.fromDoubles("Day", "Outflow", days, out, "ML");
            return table.toString();
          }
          @Override public String toString() { return "<native fn calculate>"; }
        };
      default:
        throw new RuntimeError(name, "Unknown property '" + n + "'.");
    }
  }

  void addInflow(NativeWaterNode upstream) {
    node.addInflow(upstream.node);
  }

  private static double[] toDoubleArray(Object value, Token where) {
    if (!(value instanceof java.util.List<?> list)) {
      throw new RuntimeError(where, "Expected array (list) of numbers.");
    }
    double[] out = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      Object v = list.get(i);
      if (v instanceof Double d) {
        out[i] = d; // mm
      } else if (v instanceof UnitVal uv) {
        out[i] = uv.asDouble(); // canonical; for length this is mm
      } else {
        throw new RuntimeError(where, "Array element " + i + " is not a number or unit.");
      }
    }
    return out;
  }
}