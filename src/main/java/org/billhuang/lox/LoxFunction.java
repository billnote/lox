package org.billhuang.lox;

import java.util.List;

/**
 * @Description
 * @Data 2022/3/4 16:27
 * @Author huangshb
 **/
class LoxFunction implements LoxCallable{

    private final String name;
    private final Expr.Function declaration;
    private final Environment closure;

    LoxFunction(String name, Expr.Function declaration, Environment closure) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < arguments.size(); i++) {
             environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return r) {
            return r.value;
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("<fn %s>", name);
    }
}
