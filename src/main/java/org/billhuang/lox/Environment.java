package org.billhuang.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Data 2022/3/2 17:02
 * @Author huangshb
 **/
public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexeme));
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexeme));
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    private Environment ancestor(int distance) {
        Environment env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }

        return env;
    }
}
