package org.billhuang.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @Description
 * @Data 2022/3/8 15:01
 * @Author huangshb
 **/
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUPERCLASS
    }

    private class Variable {
        VariableState state;
        int slot;
    }

    private enum VariableState {
        DECLARE,
        DEFINE,
        READ
    }

    private final Interpreter interpreter;
    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name, false);
        return null;
    }

    @Override
    public Void visitCommaExpr(Expr.Comma expr) {
        return null;
    }

    @Override
    public Void visitConditionalExpr(Expr.Conditional expr) {
        resolve(expr.cond);
        resolve(expr.thenBranch);
        resolve(expr.elseBranch);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword, true);

        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUPERCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword, true);
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {
        resolveFunction(expr, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().containsKey(expr.name.lexeme)
                && scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARE){
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name, true);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null) {
            if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)){
                Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
            }

            currentClass = ClassType.SUPERCLASS;
            resolve(stmt.superclass);

            beginScope();
            Variable _super = new Variable();
            _super.state = VariableState.READ;
            scopes.peek().put("super", _super);
        }

        beginScope();

        Variable _this = new Variable();
        _this.state = VariableState.READ;
        scopes.peek().put("this", _this);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = "init".equals(method.name.lexeme)
                    ? FunctionType.INITIALIZER
                    : FunctionType.METHOD;
            resolveFunction(method.function, declaration);
        }

        for (Stmt.Function method : stmt.staticMethods) {
            FunctionType declaration = FunctionType.METHOD;
            resolveFunction(method.function, declaration);
        }

        endScope();

        if (stmt.superclass != null) {
            endScope();
        }

        currentClass = enclosingClass;

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt.function, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        resolve(stmt.initializer);
        resolve(stmt.condition);
        resolve(stmt.increment);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.");
            }

            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);

        return null;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        Map<String, Variable> variableStatus = scopes.pop();
        for (Map.Entry<String, Variable> entry: variableStatus.entrySet()) {
            if (entry.getValue().state == VariableState.DEFINE) {
                // TODO report error
                System.out.println("variable never used. var: " + entry.getKey());
            }
        }
    }

    private void declare(Token name) {
        if (scopes.isEmpty()){
            return;
        }

        Map<String, Variable> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope");
        }

        Variable variable = new Variable();
        variable.state = VariableState.DECLARE;
        variable.slot = scope.size();

        scope.put(name.lexeme, variable);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }

        scopes.peek().get(name.lexeme).state = VariableState.DEFINE;
    }

    private void resolveLocal(Expr expr, Token name, boolean isRead) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Variable> scope = scopes.get(i);
            if (scope.containsKey(name.lexeme)) {
                Variable variable = scope.get(name.lexeme);
                if (isRead) {
                    variable.state = VariableState.READ;
                }

                interpreter.resolve(expr, scopes.size() - 1 - i, variable.slot);
                return;
            }
        }
    }

    private void resolveFunction(Expr.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }
}
