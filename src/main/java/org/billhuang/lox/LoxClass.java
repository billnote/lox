package org.billhuang.lox;

import java.util.List;

/**
 * @Description
 * @Data 2022/3/9 17:41
 * @Author huangshb
 **/
public class LoxClass implements LoxCallable{
    private final String name;

    LoxClass(String name ) {
        this.name = name;
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
}
