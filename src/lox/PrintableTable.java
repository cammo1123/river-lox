package lox;

import java.util.ArrayList;
import java.util.List;

public final class PrintableTable {
  public enum BarStyle { SINGLE, DOUBLE }
  public enum Align { LEFT, RIGHT, CENTER }

  private final boolean leftFirst;
  private final BarStyle defaultBar;
  private final List<Element> elements = new ArrayList<>();

  private interface Element {}
  private static final class Title implements Element { final String text; Title(String t){ text = t; } }
  private static final class Row implements Element { final List<String> cells; Row(List<String> c){ cells = c; } }
  private static final class HeaderDivider implements Element { final BarStyle horizStyle; HeaderDivider(BarStyle s){ horizStyle = s; } }
  private static final class Divider implements Element { final BarStyle horizStyle; Divider(BarStyle s){ horizStyle = s; } }
  private static final class EndCap implements Element {}

  public PrintableTable() { this(true, BarStyle.DOUBLE); }
  public PrintableTable(boolean leftFirst, BarStyle defaultBar) {
    this.leftFirst = leftFirst;
    this.defaultBar = defaultBar;
  }

  public PrintableTable addTitle(String title) { elements.add(new Title(title)); return this; }
  public PrintableTable addRow(List<String> cells) { elements.add(new Row(new ArrayList<>(cells))); return this; }
  public PrintableTable addHeaderDivider() { elements.add(new Divider(BarStyle.DOUBLE)); return this; }
  public PrintableTable addDivider() { elements.add(new HeaderDivider(BarStyle.SINGLE)); return this; }
  public PrintableTable addEndCap() { elements.add(new EndCap()); return this; }

  public String render() {
    int maxCols = 0;
    for (Element e : elements) if (e instanceof Row r) maxCols = Math.max(maxCols, r.cells.size());
    if (maxCols == 0) maxCols = 1;

    List<Integer> widths = new ArrayList<>();
    for (int i = 0; i < maxCols; i++) widths.add(0);

    for (Element e : elements) {
      if (e instanceof Row r) {
        for (int i = 0; i < r.cells.size(); i++) {
          String s = r.cells.get(i) == null ? "" : r.cells.get(i);
          widths.set(i, Math.max(widths.get(i), s.length()));
        }
      }
    }
    for (int i = 0; i < widths.size(); i++) if (widths.get(i) <= 0) widths.set(i, 1);

    int tableW = tableWidth(widths);
    int maxTitleRequired = 0;
    
    for (Element e : elements) {
      if (e instanceof Title t) {
        int req = t.text == null ? 0 : t.text.trim().length() + 4;
        if (req > maxTitleRequired) maxTitleRequired = req;
      }
    }
    
    if (maxTitleRequired > tableW) {
      int extra = maxTitleRequired - tableW;
      int last = widths.size() - 1;
      widths.set(last, widths.get(last) + extra);
      tableW = tableWidth(widths);
    }

    StringBuilder sb = new StringBuilder();
    for (Element e : elements) {
      if (e instanceof Title t) {
        sb.append(renderTopWithTitle(t.text, tableW, defaultBar)).append('\n');
      } else if (e instanceof Row r) {
        sb.append(renderRowWithWidths(r.cells, widths, defaultBar));
      } else if (e instanceof HeaderDivider hd) {
        sb.append(renderHeaderDivider(widths, hd.horizStyle));
      } else if (e instanceof Divider d) {
        sb.append(renderMidDivider(widths, d.horizStyle));
      } else if (e instanceof EndCap) {
        sb.append(renderEndCap(widths, defaultBar));
      }
    }
    return sb.toString();
  }

  @Override public String toString() { return render(); }

  private String renderTopWithTitle(String title, int totalWidth, BarStyle style) {
    String t = " " + title.trim() + " ";
    int inner = Math.max(0, totalWidth - 2 - t.length());
    int left = inner / 2;
    int right = inner - left;
    StringBuilder sb = new StringBuilder();
    sb.append(topLeft(style));
    sb.append(repeat(horizChar(style), left));
    sb.append(t);
    sb.append(repeat(horizChar(style), right));
    sb.append(topRight(style));
    return sb.toString();
  }

  private String renderRowWithWidths(List<String> cells, List<Integer> widths, BarStyle bar) {
    List<String> clipped = clipToWidths(cells, widths);
    List<String> padded = new ArrayList<>(widths.size());
    if (leftFirst) {
      padded.add(padRight(clipped.get(0), widths.get(0)));
      for (int i = 1; i < widths.size(); i++) padded.add(padLeft(clipped.get(i), widths.get(i)));
    } else {
      for (int i = 0; i < widths.size(); i++) padded.add(padLeft(clipped.get(i), widths.get(i)));
    }
    return renderRowPadded(padded);
  }

  private String renderRowPadded(List<String> padded) {
    String v = vert(defaultBar);
    StringBuilder sb = new StringBuilder();
    sb.append(v).append(' ').append(padded.get(0)).append(' ').append(v);
    for (int i = 1; i < padded.size(); i++) {
      sb.append(' ').append(padded.get(i)).append(' ').append(v);
    }
    sb.append('\n');
    return sb.toString();
  }

  private String renderHeaderDivider(List<Integer> widths, BarStyle horizStyle) {
    char fill = horizChar(horizStyle);
    String left = headerLeft(defaultBar);
    String join = headerJoin(defaultBar);
    String right = headerRight(defaultBar);
    StringBuilder sb = new StringBuilder();
    sb.append(left);
    sb.append(repeat(fill, widths.get(0) + 2));
    for (int i = 1; i < widths.size(); i++) {
      sb.append(join);
      sb.append(repeat(fill, widths.get(i) + 2));
    }
    sb.append(right);
    sb.append('\n');
    return sb.toString();
  }

  private String renderMidDivider(List<Integer> widths, BarStyle horizStyle) {
    char fill = horizChar(horizStyle);
    String left = midLeft(defaultBar);
    String join = midJoin(defaultBar);
    String right = midRight(defaultBar);
    StringBuilder sb = new StringBuilder();
    sb.append(left);
    sb.append(repeat(fill, widths.get(0) + 2));
    for (int i = 1; i < widths.size(); i++) {
      sb.append(join);
      sb.append(repeat(fill, widths.get(i) + 2));
    }
    sb.append(right);
    sb.append('\n');
    return sb.toString();
  }

  private String renderEndCap(List<Integer> widths, BarStyle style) {
    StringBuilder sb = new StringBuilder();
    sb.append(bottomLeft(defaultBar));
    sb.append(repeat(horizChar(defaultBar), widths.get(0) + 2));
    for (int i = 1; i < widths.size(); i++) {
      sb.append(bottomJoin(defaultBar));
      sb.append(repeat(horizChar(defaultBar), widths.get(i) + 2));
    }
    sb.append(bottomRight(defaultBar));
    sb.append('\n');
    return sb.toString();
  }

  private List<String> clipToWidths(List<String> cells, List<Integer> widths) {
    List<String> out = new ArrayList<>(widths.size());
    for (int i = 0; i < widths.size(); i++) {
      String s = i < cells.size() && cells.get(i) != null ? cells.get(i) : "";
      if (s.length() > widths.get(i)) s = s.substring(0, widths.get(i));
      out.add(s);
    }
    return out;
  }

  private static String vert(BarStyle t) { return t == BarStyle.DOUBLE ? "║" : "│"; }
  private static char horizChar(BarStyle t) { return t == BarStyle.DOUBLE ? '═' : '─'; }

  private static String topLeft(BarStyle t) { return t == BarStyle.DOUBLE ? "╔" : "┌"; }
  private static String topRight(BarStyle t) { return t == BarStyle.DOUBLE ? "╗" : "┐"; }

  private static String bottomLeft(BarStyle t) { return t == BarStyle.DOUBLE ? "╚" : "└"; }
  private static String bottomRight(BarStyle t) { return t == BarStyle.DOUBLE ? "╝" : "┘"; }
  private static String bottomJoin(BarStyle t) { return t == BarStyle.DOUBLE ? "╩" : "┴"; }

  private static String midLeft(BarStyle t) { return t == BarStyle.DOUBLE ? "╠" : "├"; }
  private static String midRight(BarStyle t) { return t == BarStyle.DOUBLE ? "╣" : "┤"; }
  private static String midJoin(BarStyle t) { return t == BarStyle.DOUBLE ? "╬" : "┼"; }

  private static String headerLeft(BarStyle t) { return t == BarStyle.DOUBLE ? "╟" : "├"; }
  private static String headerRight(BarStyle t) { return t == BarStyle.DOUBLE ? "╢" : "┤"; }
  private static String headerJoin(BarStyle t) { return t == BarStyle.DOUBLE ? "╫" : "┼"; }

  private static String padLeft(String s, int w) { int n = Math.max(0, w - s.length()); return " ".repeat(n) + s; }
  private static String padRight(String s, int w) { int n = Math.max(0, w - s.length()); return s + " ".repeat(n); }
  private static String repeat(char c, int n) { if (n <= 0) return ""; return String.valueOf(c).repeat(n); }

  public static int tableWidth(List<Integer> widths) {
    int sum = 0; for (int w : widths) sum += w; int k = widths.size(); return sum + (3 * k) + 1;
  }
}