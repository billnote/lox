package org.billhuang.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Data 2022/3/9 18:04
 * @Author huangshb
 **/
public class LoxInstance {
    private LoxClass loxClass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        throw new RuntimeError(name, String.format("Undefined property '%s'.", name.lexeme));
    }

    @Override
    public String toString() {
        return String.format("%s instance.", loxClass);
    }
}
