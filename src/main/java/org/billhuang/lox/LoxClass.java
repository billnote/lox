package org.billhuang.lox;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Data 2022/3/9 17:41
 * @Author huangshb
 **/
public class LoxClass implements LoxCallable{
    private final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods ) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return String.format("<class %s>", name);
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        return instance;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }
}
