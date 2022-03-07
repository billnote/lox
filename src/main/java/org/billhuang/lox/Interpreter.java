package org.billhuang.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Data 2022/2/25 18:13
 * @Author huangshb
 **/
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    final Environment globals = new Environment();
    private Environment environment = globals;
    //private boolean isBreak = false;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis()/1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitCommaExpr(Expr.Comma expr) {
        evaluate(expr.left);
        return evaluate(expr.right);
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        return  (boolean) evaluate(expr.cond) ? evaluate(expr.thenBranch) : evaluate(expr.elseBranch);
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
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                } else if (isStringOrNum(left) && isStringOrNum(right)) {
                    return left.toString() + right.toString();
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                double rightNum = (double)right;
                if (Double.compare(rightNum, 0.0) == 0 ) {
                    throw new RuntimeError(expr.operator, "The divisor cannot be zero.");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        List<Object> arguments = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            arguments.add(evaluate(arg));
        }

        LoxCallable callable = (LoxCallable) callee;
        if (arguments.size() != callable.arity()) {
            throw new RuntimeError(expr.paren, String.format("Expected %s arguments but got %s.",
                    callable.arity(), arguments.size()));
        }

        return callable.call(this, arguments);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if ((isTruthy(left) && expr.operator.type == TokenType.OR)
                || (!isTruthy(left) && expr.operator.type == TokenType.AND)) {
            return left;
        } else {
            return evaluate(expr.right);
        }
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private Object evaluate(Expr expr) {
        if (expr == null) {
            return null;
        }
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof  Boolean) {
            return (boolean) object;
        } else {
            return true;
        }
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if ( !(operand instanceof Double)) {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (Object operand : operands) {
            checkNumberOperand(operator, operand);
        }
    }

    String interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            return stringify(value);
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
            return null;
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

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
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

    private boolean isStringOrNum(Object v) {
        return v instanceof String || v instanceof Double;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        this.environment.define(stmt.name.lexeme, new LoxFunction(stmt, this.environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        }  else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                execute(stmt.body);
                /* if (isBreak) {
                      break;
                }*/
            }
        } catch (BreakException ex) {

        }

        //isBreak = false;
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        for (execute(stmt.initializer); evaluateForCondition(stmt); evaluate(stmt.increment)){
            execute(stmt.body);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }

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

    private void execute(Stmt stmt) {
        if (stmt == null) {
            return;
        }
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment blockEnv) {
        Environment previous = this.environment;
        try {
            this.environment = blockEnv;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private boolean evaluateForCondition(Stmt.For stmt) {
        if (stmt.condition != null) {
            return isTruthy(evaluate(stmt.condition));
        }

        return true;
    }
}