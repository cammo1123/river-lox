package com.craftinginterpreters.lox;

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
          @Override public int arity() { return 1; } // one array/list argument
          @Override public Object call(Interpreter i, java.util.List<Object> args) {
            Object a = args.get(0);
            double[] rainfall = toDoubleArray(a, name);
            double[] out = node.calculate(rainfall);
            return toList(out);
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
      if (!(v instanceof Double)) {
        throw new RuntimeError(where, "Array element " + i + " is not a number.");
      }
      out[i] = (Double) v;
    }
    return out;
  }

  private static java.util.List<Double> toList(double[] arr) {
    java.util.List<Double> out = new java.util.ArrayList<>(arr.length);
    for (double v : arr) out.add(v);
    return out;
  }
}