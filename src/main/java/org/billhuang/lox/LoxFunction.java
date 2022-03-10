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
    private final EfficientEnvironment closure;
    private final boolean isInitializer;

    LoxFunction(String name, Expr.Function declaration, EfficientEnvironment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
    }


    LoxFunction bind(LoxInstance instance) {
        EfficientEnvironment environment = new EfficientEnvironment(closure);
        environment.define("this", instance);

        return new LoxFunction(name, declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        EfficientEnvironment environment = new EfficientEnvironment(closure);
        for (int i = 0; i < arguments.size(); i++) {
             environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return r) {
            if (isInitializer) {
                return closure.getAt(0, 0, "this");
            }

            return r.value;
        }

        if (isInitializer) {
            return closure.getAt(0, 0, "this");
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("<fn %s>", name);
    }
}
