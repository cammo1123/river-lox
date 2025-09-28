# Assignment 1

## Team Contributions

Team discussions gave me the idea for having notated units, I came up with the idea of "canonical units" that allows 1L and 1ml to be converted under the hood to the same unit.

## Original Lox (ch13):

```
program        → declaration* EOF ;
declaration    → varDecl | funDecl | classDecl | statement ;
statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
expr           → assignment ;
assignment     → IDENTIFIER "=" assignment | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expr ")" | IDENTIFIER | "super" "." IDENTIFIER | "this"
```

## With River-Lox Additions:
```
program        → declaration* EOF ;
declaration    → nodeDecl | edgeStmt | varDecl | funDecl | classDecl | statement ;
nodeDecl       → ("river" | "dam") IDENTIFIER "{" nodeProps "}" ";" ;
nodeProps      → (IDENTIFIER ":" expression ("," IDENTIFIER ":" expression)*)? ;
edgeStmt       → IDENTIFIER ">>" IDENTIFIER ";" ;
statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
expr           → assignment ;
assignment     → IDENTIFIER "=" assignment | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expr ")" | IDENTIFIER | arrayLiteral | lambda | "super" "." IDENTIFIER | "this"
arrayLiteral   → "[" (expression ("," expression)*)? "]"
lambda         → "(" parameters? ")" "=>" (expression | block)
parameters     → IDENTIFIER ("," IDENTIFIER)*
```

### Note
- `UnitVal` represents a value with a unit (e.g., area in sqkm, volume in L). These are converted to "canonical" units internally. When the scanner encounters one e.g(`2cm`). It converts it to a Number token with the value in canonical units (e.g., `20` for `2cm` as the canonical unit is `mm`).

## Example River-Lox Code:
```
river River1 { 
	area: 2.5sqm, 
	flow_days: 3, 
	flow_shape: Shape_LastDay,
};

river River2 { 
	area: 1.0sqm, 
	flow_days: 2, 
	flow_shape: (day, max_days) => { return 1 / max_days; } 
};

dam Dam1 { 
	out_flow: (vol) => { return min(vol, 10L); } 
};

River1 >> River2;
River2 >> Dam1;

var rainfall = [10mm, 0mm, 5mm, 0mm];
print Dam1.calculate(8, rainfall);
```

### Parsing steps
1. The parser starts by reading the `river` keyword, indicating the start of a river declaration.
2. It then reads the identifier `River1`, which is the name of the river.
3. The parser expects an opening curly brace `{` to denote the beginning of the river's properties.
4. Inside the braces, it reads the properties of the river:
   - `area: 2.5sqm,` is parsed as a property with the identifier `area` and a value of `2.5sqm`.
   - `flow_days: 3,` is parsed as a property with the identifier `flow_days` and a value of `3`.
   - `flow_shape: (day, max_days) => { ... }` is parsed as a property with the identifier `flow_shape` and a lambda function as its value.
5. The parser expects a closing curly brace `}` to denote the end of the river's properties, followed by a semicolon `;` to complete the declaration.

6. The parser repeats steps 1-5 for the second river declaration `River2`;
7. The parser repeats steps 1-5 for the dam declaration `Dam1` with the following differences:
	- It starts with the `dam` keyword instead of `river`.
	- It has a single property `out_flow` with a lambda function as its value.

8. Next, the parser encounters the edge statement `River1 >> River2;`. It reads the identifier `River1`, expects the `>>` operator, and then reads the identifier `River2`, followed by a semicolon `;` to complete the statement.
9. The parser repeats step 8 for the edge statement `River2 >> Dam1;`.

10. The parser then reads the variable declaration `var rainfall = [10mm, 0mm, 5mm, 0mm];`. It expects the `var` keyword, reads the identifier `rainfall`, expects the `=` operator, and then parses the array literal `[10mm, 0mm, 5mm, 0mm]` as the value of the variable. It expects a semicolon `;` to complete the declaration.

11. Finally, the parser reads the print statement `print Dam1.calculate(7, rainfall);`. It expects the `print` keyword, reads the expression `Dam1.calculate(7, rainfall)`, and expects a semicolon `;` to complete the statement.

## Improvement to the Chapter 6 Lox code

Everything including ch13 of Lox is included, plus:
  - Arrays (e.g., `[10mm, 0mm, 5mm, 0mm]`)
  - Lambdas (e.g., `(day, max_days) => { ... }`)
  - Unit values (e.g., `2.5sqm`, `10mm`, `5L`) that are converted to canonical units
  - `min(a, b)`, `max(a, b)` and `clamp(a, min, max)` functions
  - Edge statements (e.g., `River1 >> River2;`)
  - Node declarations (e.g., `river River1 { ... };` and `dam Dam1 { ... };`)
  - Node properties (e.g., `area: 2.5sqm, flow_days: 3, flow_shape: ...`)
  - Static predefined flow_shapes for rivers (Shape_LastDay, Shape_Linear)

