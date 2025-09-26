package com.craftinginterpreters.lox;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnitVal implements Comparable<UnitVal> {
  public enum Kind {
    LENGTH("mm"),
    AREA("sqkm"),
    VOLUME("L");

    private final String canonicalLabel;

    Kind(String canonicalLabel) { this.canonicalLabel = canonicalLabel; }

    public String canonicalLabel() { return canonicalLabel; }
  }

  public enum Unit {
    // LENGTH (factor = how many canonical units per 1 unit)
    KM(Kind.LENGTH, "km", 1_000_000.0, "kilometer", "kilometre"),
    M(Kind.LENGTH, "m", 1000.0, "meter", "metre"),
    CM(Kind.LENGTH, "cm", 10.0, "centimeter", "centimetre"),
    MM(Kind.LENGTH, "mm", 1.0, "millimeter", "millimetre"),

    // AREA (canonical: sqkm)
    SQKM(Kind.AREA, "sqkm", 1.0, "km2", "km^2", "km²"),
    HA(Kind.AREA, "ha", 0.01, "hectare"),
    SQM(Kind.AREA, "sqm", 1e-6, "m2", "m^2", "m²", "m"),

    // VOLUME (canonical: L)
    MEGALITER(Kind.VOLUME, "ML", 10000.0, "megaliter", "megalitre"),
    KL(Kind.VOLUME, "kl", 1000.0, "kiloliter", "kliter"),
    L(Kind.VOLUME, "L", 1.0, "l", "liter", "litre"),
    ML(Kind.VOLUME, "ml", 0.001, "milliliter", "millilitre");

    public final Kind kind;
    public final String label;
    public final double factor;
    private final String[] aliases;
    private final String labelLower;

    Unit(Kind kind, String label, double factor, String... aliases) {
      this.kind = kind;
      this.label = label;
      this.factor = factor;
      this.aliases = aliases == null ? new String[0] : aliases;
      this.labelLower = label.toLowerCase(Locale.ROOT);
    }

    // Convert value in this unit -> canonical units
    public double toCanonical(double valueInThisUnit) {
      return valueInThisUnit * factor;
    }

    // Convert canonical value -> this unit
    public double fromCanonical(double canonicalValue) {
      return canonicalValue / factor;
    }

    public static Unit parse(String s) {
      if (s == null)
        return null;
      String norm = s.trim();
      // 1) Try exact (case-sensitive) match against label or aliases so "ML" !=
      // "mL".
      for (Unit u : values()) {
        if (u.label.equals(norm))
          return u;
        for (String a : u.aliases) {
          if (a.equals(norm))
            return u;
        }
      }
      // 2) Fallback: case-insensitive match for user convenience.
      String lower = norm.toLowerCase(Locale.ROOT);
      for (Unit u : values()) {
        if (u.labelLower.equals(lower))
          return u;
        for (String a : u.aliases) {
          if (a.toLowerCase(Locale.ROOT).equals(lower))
            return u;
        }
      }
      return null;
    }

    // Choose the "best" display unit for a canonical magnitude (prefer larger
    // units).
    public static Unit bestFor(Kind kind, double canonicalValue) {
      Unit[] candidates;
      switch (kind) {
      case LENGTH:
        candidates = new Unit[] {KM, M, CM, MM};
        break;
      case AREA:
        candidates = new Unit[] {SQKM, HA, SQM};
        break;
      case VOLUME:
        candidates = new Unit[] {MEGALITER, KL, L, ML};
        break;
      default:
        return null;
      }
      double abs = Math.abs(canonicalValue);
      for (Unit u : candidates) {
        if (abs >= u.factor)
          return u;
      }
      return candidates[candidates.length - 1];
    }
  }

  // canonical value (Kind.LENGH -> mm, AREA -> sqkm, VOLUME -> L)
  private final Kind kind;
  private final double canonicalValue;
  // optional preferred unit for toString; if null, we pick the best
  // automatically
  private final Unit preferred;

  private UnitVal(Kind kind, double canonicalValue, Unit preferred) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.canonicalValue = canonicalValue;
    this.preferred = preferred;
    if (preferred != null && preferred.kind != kind) {
      throw new IllegalArgumentException("Preferred unit must match kind");
    }
  }

  // Create from a numeric value in a specific unit (e.g. UnitVal.of(1, Unit.M)
  // is 1000 mm)
  public static UnitVal of(double value, Unit unit) {
    if (unit == null)
      throw new IllegalArgumentException("unit required");
    return new UnitVal(unit.kind, unit.toCanonical(value), unit);
  }

  // Create from canonical numeric value
  public static UnitVal ofCanonical(double canonicalValue, Kind kind) {
    return new UnitVal(kind, canonicalValue, null);
  }

  // Parse strings like "1.5 km", "1000mm", "0.5 L"
  private static final Pattern PARSE_RE =
      Pattern.compile("^\\s*([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?"
                      + "\\d+)?)\\s*([A-Za-z0-9^²]+)\\s*$");

  public static UnitVal parse(String text) {
    if (text == null)
      throw new IllegalArgumentException("null");
    Matcher m = PARSE_RE.matcher(text);
    if (!m.matches())
      throw new IllegalArgumentException("Cannot parse: " + text);
    double val = Double.parseDouble(m.group(1));
    String unitStr = m.group(2);
    Unit u = Unit.parse(unitStr);
    if (u == null)
      throw new IllegalArgumentException("Unknown unit: " + unitStr);
    return of(val, u);
  }

  // Convert to a value in the specified unit
  public double asUnit(Unit target) {
    if (target == null)
      throw new IllegalArgumentException("target unit");
    if (target.kind != kind)
      throw new IllegalArgumentException("unit kind mismatch");
    return target.fromCanonical(canonicalValue);
  }

  // Return a UnitVal that has a preferred display unit
  public UnitVal withPreferred(Unit u) {
    if (u != null && u.kind != kind)
      throw new IllegalArgumentException("unit kind mismatch");
    return new UnitVal(kind, canonicalValue, u);
  }

  public Kind kind() { return kind; }

  // numeric value in canonical units
  public double asCanonical() { return canonicalValue; }

  // Arithmetic
  public UnitVal add(UnitVal other) {
    requireSameKind(other);
    return new UnitVal(kind, canonicalValue + other.canonicalValue, null);
  }

  public UnitVal subtract(UnitVal other) {
    requireSameKind(other);
    return new UnitVal(kind, canonicalValue - other.canonicalValue, null);
  }

  public UnitVal negate() { return new UnitVal(kind, -canonicalValue, null); }

  public UnitVal multiply(double scalar) {
    return new UnitVal(kind, canonicalValue * scalar, null);
  }

  public UnitVal divide(double scalar) {
    return new UnitVal(kind, canonicalValue / scalar, null);
  }

  private void requireSameKind(UnitVal other) {
    if (other == null)
      throw new IllegalArgumentException("other required");
    if (other.kind != this.kind)
      throw new IllegalArgumentException("Kind mismatch");
  }

  // Compare (only meaningful for same kind)
  @Override
  public int compareTo(UnitVal other) {
    requireSameKind(other);
    return Double.compare(this.canonicalValue, other.canonicalValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof UnitVal))
      return false;
    UnitVal other = (UnitVal)o;
    return this.kind == other.kind &&
        Double.compare(this.canonicalValue, other.canonicalValue) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, Double.valueOf(canonicalValue));
  }

  @Override
  public String toString() {
    Unit display =
        preferred != null ? preferred : Unit.bestFor(kind, canonicalValue);

    double d = display.fromCanonical(canonicalValue);
    return fmt(d) + display.label;
  }

  private static String fmt(double d) {
    if (Double.isNaN(d) || Double.isInfinite(d))
      return Double.toString(d);
    BigDecimal bd = BigDecimal.valueOf(d).stripTrailingZeros();
    return bd.toPlainString();
  }
}