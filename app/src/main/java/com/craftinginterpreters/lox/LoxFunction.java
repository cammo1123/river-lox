package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;
  private final boolean isInitializer;
  private final String name;

  LoxFunction(Stmt.Function declaration, Environment closure,
              boolean isInitializer) {
    this.params = declaration.params;
    this.body = declaration.body;
    this.name = declaration.name.lexeme;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  LoxFunction(List<Token> params, List<Stmt> body, Environment closure) {
    this.params = params;
    this.body = body;
    this.name = null; // anonymous
    this.closure = closure;
    this.isInitializer = false;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    if (name != null) {
      // keep named version for methods
      return new LoxFunction(params, body, environment, isInitializer, name);
    }
    return new LoxFunction(params, body, environment);
  }

  private LoxFunction(List<Token> params, List<Stmt> body, Environment closure,
                      boolean isInitializer, String name) {
    this.params = params;
    this.body = body;
    this.closure = closure;
    this.isInitializer = isInitializer;
    this.name = name;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);

    if (arguments.size() != arity()) {
      throw new RuntimeException("Incorrect amount of call arguments");
    }

    for (int i = 0; i < params.size(); i++) {
      environment.define(params.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      if (isInitializer) {
        return closure.getAt(0, "this");
      }

      return returnValue.value;
    }

    if (isInitializer) {
      return closure.getAt(0, "this");
    }
    return null;
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public String toString() {
    if (name != null) {
      return "<fn " + name + ">";
    }
    return "<lambda>";
  }
}