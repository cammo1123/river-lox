package com.craftinginterpreters.lox;

public final class PrintableTable {
  private final String firstColLabel;
  private final String rowLabel;
  private final String[] headers;
  private final String[] cells;

  public PrintableTable(String firstColLabel, String rowLabel, String[] headers,
                        String[] cells) {
    if (headers == null || cells == null) {
      throw new IllegalArgumentException("headers and cells must be non-null");
    }
    if (headers.length != cells.length) {
      throw new IllegalArgumentException(
          "headers and cells must have the same length");
    }
    this.firstColLabel = firstColLabel == null ? "" : firstColLabel;
    this.rowLabel = rowLabel == null ? "" : rowLabel;
    this.headers = headers.clone();
    this.cells = cells.clone();
  }

  // Convenience constructor: formats doubles with unit (int vs 2-decimals)
  public static PrintableTable fromDoubles(String firstColLabel,
                                           String rowLabel, String[] headers,
                                           double[] values, String unit) {
    if (headers == null || values == null) {
      throw new IllegalArgumentException("headers and values non-null");
    }
    if (headers.length != values.length) {
      throw new IllegalArgumentException(
          "headers length must match values length");
    }
    String[] cells = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      double v = values[i];
      String fmt;
      if (Math.abs(v - Math.round(v)) < 1e-9) {
        fmt = String.format("%.0f%s", v, unit == null ? "" : unit);
      } else {
        fmt = String.format("%.2f%s", v, unit == null ? "" : unit);
      }
      cells[i] = fmt;
    }
    return new PrintableTable(firstColLabel, rowLabel, headers, cells);
  }

  public String render() {
    final int n = headers.length;
    int firstW = Math.max(firstColLabel.length(), rowLabel.length());
    int[] w = new int[n + 1];
    w[0] = firstW;
    for (int i = 0; i < n; i++) {
      w[i + 1] = Math.max(headers[i].length(), cells[i].length());
    }

    StringBuilder sb = new StringBuilder();
    // header row
    sb.append(padRight(firstColLabel, w[0]));
    for (int i = 0; i < n; i++) {
      sb.append(" | ").append(padCenter(headers[i], w[i + 1]));
    }
    sb.append('\n');

    // separator
    sb.append(padRight("", w[0]));
    for (int i = 0; i < n; i++) {
      sb.append(" | ").append(repeat('-', w[i + 1]));
    }
    sb.append('\n');

    // single data row
    sb.append(padRight(rowLabel, w[0]));
    for (int i = 0; i < n; i++) {
      sb.append(" | ").append(padLeft(cells[i], w[i + 1]));
    }
    sb.append('\n');

    return sb.toString();
  }

  @Override
  public String toString() {
    return render();
  }

  private static String padRight(String s, int w) {
    if (s == null)
      s = "";
    int pad = w - s.length();
    if (pad <= 0)
      return s;
    StringBuilder b = new StringBuilder(w);
    b.append(s);
    for (int i = 0; i < pad; i++)
      b.append(' ');
    return b.toString();
  }

  private static String padLeft(String s, int w) {
    if (s == null)
      s = "";
    int pad = w - s.length();
    if (pad <= 0)
      return s;
    StringBuilder b = new StringBuilder(w);
    for (int i = 0; i < pad; i++)
      b.append(' ');
    b.append(s);
    return b.toString();
  }

  private static String padCenter(String s, int w) {
    if (s == null)
      s = "";
    int pad = w - s.length();
    if (pad <= 0)
      return s;
    int left = pad / 2;
    int right = pad - left;
    StringBuilder b = new StringBuilder(w);
    for (int i = 0; i < left; i++)
      b.append(' ');
    b.append(s);
    for (int i = 0; i < right; i++)
      b.append(' ');
    return b.toString();
  }

  private static String repeat(char c, int n) {
    if (n <= 0)
      return "";
    StringBuilder b = new StringBuilder(n);
    for (int i = 0; i < n; i++)
      b.append(c);
    return b.toString();
  }
}