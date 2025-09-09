package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);
		R visitClassStmt(Class stmt);
		R visitExpressionStmt(Expression stmt);
		R visitEdgeStmt(Edge stmt);
		R visitFunctionStmt(Function stmt);
		R visitIfStmt(If stmt);
		R visitReturnStmt(Return stmt);
		R visitPrintStmt(Print stmt);
		R visitVarStmt(Var stmt);
		R visitWhileStmt(While stmt);
		R visitNodeDeclStmt(NodeDecl stmt);
	}
	static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

		final List<Stmt> statements;
	}
	static class Class extends Stmt {
		Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods) {
			this.name = name;
			this.superclass = superclass;
			this.methods = methods;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}

		final Token name;
		final Expr.Variable superclass;
		final List<Stmt.Function> methods;
	}
	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expression;
	}
	static class Edge extends Stmt {
		Edge(Expr.Variable from, Token arrow, Expr.Variable to) {
			this.from = from;
			this.arrow = arrow;
			this.to = to;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitEdgeStmt(this);
		}

		final Expr.Variable from;
		final Token arrow;
		final Expr.Variable to;
	}
	static class Function extends Stmt {
		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}

		final Token name;
		final List<Token> params;
		final List<Stmt> body;
	}
	static class If extends Stmt {
		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;
	}
	static class Return extends Stmt {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}

		final Token keyword;
		final Expr value;
	}
	static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		final Expr expression;
	}
	static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initializer;
	}
	static class While extends Stmt {
		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		final Expr condition;
		final Stmt body;
	}
	static class NodeDecl extends Stmt {
		NodeDecl(Token kind, Token name, Map<String,Expr> props) {
			this.kind = kind;
			this.name = name;
			this.props = props;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitNodeDeclStmt(this);
		}

		final Token kind;
		final Token name;
		final Map<String,Expr> props;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
