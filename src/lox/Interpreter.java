package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("randN", new LoxCallable() {
      Random random = new Random();

      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg = arguments.get(0);
        if (!(arg instanceof Double)) {
          return 0;
        }

        return Double.valueOf(random.nextInt(((Double)arg).intValue()));
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("clear", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("sleep", new LoxCallable() {
      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg = arguments.get(0);
        if (!(arg instanceof Double)) {
          return null;
        }

        try {
          Thread.sleep(((Double)arg).intValue());
        } catch (InterruptedException err) {
          return null;
        }
        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("max", new LoxCallable() {
      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg1 = arguments.get(0);
        Object arg2 = arguments.get(1);

        if (arg1 instanceof Double d1 && arg2 instanceof Double d2) {
          return Math.max(d1, d2);
        }

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("min", new LoxCallable() {
      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg1 = arguments.get(0);
        Object arg2 = arguments.get(1);

        if (arg1 instanceof Double d1 && arg2 instanceof Double d2) {
          return Math.min(d1, d2);
        }

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("clamp", new LoxCallable() {
      @Override
      public int arity() {
        return 3;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg1 = arguments.get(0);
        Object arg2 = arguments.get(1);
        Object arg3 = arguments.get(2);

        if (arg1 instanceof Double val && arg2 instanceof Double min &&
            arg3 instanceof Double max) {
          return Math.clamp(val, min, max);
        }

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("Shape_Linear", new LoxCallable() {
      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg1 = arguments.get(0);
        Object arg2 = arguments.get(1);

        if (arg1 instanceof Double && arg2 instanceof Double maxDays) {
          return 1 / maxDays;
        }

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    globals.define("Shape_LastDay", new LoxCallable() {
      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg1 = arguments.get(0);
        Object arg2 = arguments.get(1);

        if (arg1 instanceof Double day && arg2 instanceof Double maxDays) {
          if (day.equals(maxDays)) {
            return (double)1;
          }

          return (double)0;
        }

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left))
        return left;
    } else {
      if (!isTruthy(left))
        return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass)environment.getAt(distance, "super");

    LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");

    LoxFunction method = superclass.findMethod(expr.method.lexeme);

    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" +
                                              expr.method.lexeme + "'.");
    }

    return method.bind(object);
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitArrayExpr(Expr.Array expr) {
    List<Object> out = new ArrayList<>();
    for (Expr e : expr.elements) {
      out.add(evaluate(e));
    }
    return out;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
    case BANG:
      return !isTruthy(right);
    case MINUS:
      if (right instanceof Double d) {
        return -d;
      }
      throw new RuntimeError(expr.operator, "Operand must be a number.");
    default:
      // Unreachable.
      return null;
    }
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) { stmt.accept(this); }

  void resolve(Expr expr, int depth) { locals.put(expr, depth); }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof java.util.List<?> list) {
      return list.toString();
    }

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  private Object evaluate(Expr expr) { return expr.accept(this); }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name,
                               "Superclass must be a class.");
      }
    }

    environment.define(stmt.name.lexeme, null);

    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment,
                                             method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    LoxClass klass =
        new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

    if (superclass != null) {
      environment = environment.enclosing;
    }

    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    printString(value);
    return null;
  }

  void printString(Object value) {
    if (!(value instanceof String)) {
      String toPrint = stringify(value);
      System.out.println(toPrint);
      return;
    }

    System.out.println((String)value);
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
    case GREATER:
      checkNumberOperands(expr.operator, left, right);
      return (double)left > (double)right;
    case GREATER_EQUAL:
      checkNumberOperands(expr.operator, left, right);
      return (double)left >= (double)right;
    case LESS:
      checkNumberOperands(expr.operator, left, right);
      return (double)left < (double)right;
    case LESS_EQUAL:
      checkNumberOperands(expr.operator, left, right);
      return (double)left <= (double)right;
    case MINUS:
      checkNumberOperands(expr.operator, left, right);
      return (double)left - (double)right;
    case BANG_EQUAL:
      return !isEqual(left, right);
    case EQUAL_EQUAL:
      return isEqual(left, right);
    case PLUS:
      if (left instanceof Double && right instanceof Double) {
        return (double)left + (double)right;
      }

      if (left instanceof String && right instanceof String) {
        return (String)left + (String)right;
      }

	  if (left instanceof String && right instanceof Object) {
        return (String)left + String.valueOf(right);
      }

	  if (left instanceof Object && right instanceof String) {
        return String.valueOf(left) + (String)right;
      }
      throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
    case SLASH:
      checkNumberOperands(expr.operator, left, right);
      return (double)left / (double)right;
    case STAR:
      checkNumberOperands(expr.operator, left, right);
      return (double)left * (double)right;
    default:
      // Unreachable.
      return null;
    }
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // Allow NativeWaterNode methods (calculate) to be callable
    if (callee instanceof LoxCallable) {
      LoxCallable function = (LoxCallable)callee;
      if (arguments.size() != function.arity()) {
        throw new RuntimeError(expr.paren, "Expected " + function.arity() +
                                               " arguments but got " +
                                               arguments.size() + ".");
      }
      return function.call(this, arguments);
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
                             "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable)callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() +
                                             " arguments but got " +
                                             arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    // Support native water node properties/methods
    if (object instanceof NativeWaterNode n) {
      return n.get(expr.name);
    }

    if (object instanceof LoxInstance loxInstance) {
      return loxInstance.get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Void visitNodeDeclStmt(Stmt.NodeDecl stmt) {
    String name = stmt.name.lexeme;
    NativeWaterNode node;
    if (stmt.kind.type == TokenType.RIVER) {
      Double flow_days = getDouble(stmt, "flow_days");
      Double area = getDouble(stmt, "area");
      LoxCallable flow_shape = getLambda(stmt, "flow_shape", 2);
      node = new NativeWaterNode(
          new River(this, name, area, flow_days, flow_shape));
    } else if (stmt.kind.type == TokenType.DAM) {
      LoxCallable outFlow = getLambda(stmt, "out_flow", 1);
      node = new NativeWaterNode(new Dam(this, name, outFlow));
    } else {
      throw new RuntimeError(stmt.name, "Unknown node kind.");
    }
    environment.define(name, node);
    return null;
  }

  @Override
  public Void visitEdgeStmt(Stmt.Edge stmt) {
    Object up = evaluate(stmt.from);
    Object down = evaluate(stmt.to);
    if (!(up instanceof NativeWaterNode) ||
        !(down instanceof NativeWaterNode)) {
      throw new RuntimeError(stmt.arrow, "Connections require water nodes.");
    }
    ((NativeWaterNode)down).addInflow((NativeWaterNode)up);
    return null;
  }

  private Double getDouble(Stmt.NodeDecl stmt, String key) {
    Expr e = stmt.props.get(key);
    if (e == null) {
      throw new RuntimeError(stmt.name, "Missing property '" + key + "'.");
    }

    Object v = evaluate(e);
    if (v instanceof Double val) {
      return val;
    }

    throw new RuntimeError(stmt.name,
                           "Property '" + key + "' must be number or a unit");
  }

  private LoxCallable getLambda(Stmt.NodeDecl stmt, String key,
                                int expectedArgs) {
    Expr e = stmt.props.get(key);
    if (e == null) {
      throw new RuntimeError(stmt.name, "Missing property '" + key + "'.");
    }

    Object v = evaluate(e);
    if (v instanceof Double val) {
      return new LoxCallable() {
        @Override
        public int arity() {
          return expectedArgs;
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
          return val;
        }

        @Override
        public String toString() {
          return String.valueOf(val);
        }
      };
    }

    // Already callable (lambda or function)
    if (v instanceof LoxCallable f) {
      if (f.arity() != expectedArgs) {
        throw new RuntimeError(stmt.name, "Property '" + key + "' must be a " +
                                              expectedArgs +
                                              " argument lambda or number.");
      }
      return f;
    }

    throw new RuntimeError(stmt.name, "Property '" + key +
                                          "' must be number, unit, or a " +
                                          expectedArgs + " arg lambda.");
  }

  @Override
  public Object visitLambdaExpr(Expr.Lambda expr) {
    return new LoxFunction(expr.params, expr.body, environment);
  }
}
