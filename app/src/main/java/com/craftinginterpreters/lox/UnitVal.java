package com.craftinginterpreters.lox;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public final class UnitVal {
  public enum Kind {
    LENGTH("mm"),
    AREA("sqkm"),
    VOLUME("L");

    private final String canonicalLabel;

    Kind(String canonicalLabel) { this.canonicalLabel = canonicalLabel; }

    public String canonicalLabel() { return canonicalLabel; }
  }

  public enum Unit {
    // LENGTH (canonical: mm)
    KM(Kind.LENGTH, "km", 1_000_000.0),
    M(Kind.LENGTH, "m", 1000.0),
    CM(Kind.LENGTH, "cm", 10.0),
    MM(Kind.LENGTH, "mm", 1.0),

    // AREA (canonical: sqkm)
    SQM(Kind.AREA, "sqm", 0.000001),
    HA(Kind.AREA, "ha", 0.01),
    SQKM(Kind.AREA, "sqkm", 1.0),

    // VOLUME (canonical: L)
    L(Kind.VOLUME, "L", 1.0),
    ML(Kind.VOLUME, "ml", 0.001),
    KL(Kind.VOLUME, "kl", 1000.0),
    MEGALITER(Kind.VOLUME, "ML", 1_000_000.0),
    ;

    public final Kind kind;
    public final String label;
    public final double factor;

    Unit(Kind kind, String label, double factor) {
      this.kind = kind;
      this.label = label;
      this.factor = factor;
    }

    public double toCanonical(double valueInThisUnit) {
      return valueInThisUnit * factor;
    }

    public double fromCanonical(double canonicalValue) {
      return canonicalValue / factor;
    }

    public static Unit parse(String s) {
      if (s == null)
        return null;
      String norm = s.trim();

      for (Unit u : values()) {
        if (u.label.equals(norm))
          return u;
      }

      String lower = norm.toLowerCase();
      for (Unit u : values()) {
        if (u.label.toLowerCase().equals(lower))
          return u;
      }

      return null;
    }

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

  private final Kind kind;
  private final double canonicalValue;
  private final Unit preferred;

  private UnitVal(Kind kind, double canonicalValue, Unit preferred) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.canonicalValue = canonicalValue;
    this.preferred = preferred;
    if (preferred != null && preferred.kind != kind) {
      throw new IllegalArgumentException("Preferred unit must match kind");
    }
  }

  public static UnitVal of(double value, Unit unit) {
    if (unit == null)
      throw new IllegalArgumentException("unit required");
    return new UnitVal(unit.kind, unit.toCanonical(value), unit);
  }

  public static UnitVal ofCanonical(double canonicalValue, Kind kind) {
    return new UnitVal(kind, canonicalValue, null);
  }

  public static UnitVal parse(Double val, String unit) {
    Unit u = Unit.parse(unit);
    if (u == null)
      throw new IllegalArgumentException("Unknown unit: " + unit);
    return of(val, u);
  }

  public Kind kind() { return kind; }
  public double asCanonical() { return canonicalValue; }

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

    // Remove tiny floating-point noise that we don't want to see.
    // - For |x| >= 1: round to 12 decimal places.
    // - For |x| < 1: round to 12 significant digits.

    BigDecimal bd = BigDecimal.valueOf(d);
    BigDecimal rounded;

    double abs = Math.abs(d);

    if (abs >= 1.0) {
      rounded = bd.setScale(12, RoundingMode.HALF_UP);
    } else {
      rounded = bd.round(new MathContext(12, RoundingMode.HALF_UP));
    }

    rounded = rounded.stripTrailingZeros();
    return rounded.toPlainString();
  }
}