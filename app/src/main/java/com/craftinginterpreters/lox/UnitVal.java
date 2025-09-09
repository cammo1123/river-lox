package com.craftinginterpreters.lox;

public final class UnitVal {
  public enum Kind {
    LENGTH, // canonical: mm
    AREA,   // canonical: sqkm
    VOLUME  // canonical: L
  }

  public final Kind kind;
  public final double value; // canonical numeric value
  public final String unit;  // canonical unit label: "mm", "sqkm", "L"

  private UnitVal(Kind kind, double value, String unit) {
    this.kind = kind;
    this.value = value;
    this.unit = unit;
  }

  public static UnitVal of(double rawValue, String unitRaw) {
    if (unitRaw == null || unitRaw.isEmpty()) {
      throw new IllegalArgumentException("Missing unit");
    }
    String u = unitRaw.trim().toLowerCase();

    // Length → mm
    if (u.equals("mm")) return new UnitVal(Kind.LENGTH, rawValue, "mm");
    if (u.equals("cm")) return new UnitVal(Kind.LENGTH, rawValue * 10.0, "mm");
    if (u.equals("m")) return new UnitVal(Kind.LENGTH, rawValue * 1000.0, "mm");
    if (u.equals("km")) return new UnitVal(Kind.LENGTH, rawValue * 1_000_000.0, "mm");

    // Area → sqkm
    if (u.equals("sqkm")) return new UnitVal(Kind.AREA, rawValue, "sqkm");
    if (u.equals("sqm")) return new UnitVal(Kind.AREA, rawValue * 1e-6, "sqkm");
    if (u.equals("ha")) return new UnitVal(Kind.AREA, rawValue * 0.01, "sqkm"); // hectares

    // Volume → L
    if (u.equals("l")) return new UnitVal(Kind.VOLUME, rawValue, "L");
    if (u.equals("ml")) return new UnitVal(Kind.VOLUME, rawValue * 0.001, "L");
    if (u.equals("kl")) return new UnitVal(Kind.VOLUME, rawValue * 1000.0, "L");

    throw new IllegalArgumentException("Unknown unit: " + unitRaw);
  }

  public UnitVal negate() {
    return new UnitVal(kind, -value, unit);
  }

  // Numeric value in canonical units
  public double asDouble() {
    return value;
  }

  @Override
  public String toString() {
    double v = value;
    String s = Double.toString(v);
    if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
    return s + unit;
  }
}