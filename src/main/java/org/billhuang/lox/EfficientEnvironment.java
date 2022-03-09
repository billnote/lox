package org.billhuang.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Data 2022/3/9 15:54
 * @Author huangshb
 **/
public class EfficientEnvironment {
    final Map<String, Object> globals;
    final EfficientEnvironment enclosing;
    private final List<Object> values;

    EfficientEnvironment() {
        globals = new HashMap<>();
        enclosing = null;
        values = null;
    }

    EfficientEnvironment(EfficientEnvironment environment) {
        globals = environment.globals;
        this.enclosing = environment;
        this.values = new ArrayList<>();
    }

    void define(String name, Object value) {
        if (values == null){
            globals.put(name, value);
        } else {
            values.add(value);
        }
    }

    Object getAt(Integer distance, Integer slot, Token name) {
        if (distance == null) {
            if (globals.containsKey(name.lexeme)) {
                return globals.get(name.lexeme);
            } else {
                throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexeme));
            }
        } else {
            return ancestor(distance).values.get(slot);
        }

    }

    void assign(Token name, Object value) {
        assignAt(null, 0, name, value);
    }

    void assignAt(Integer distance, int slot, Token name, Object value) {
        if (distance == null) {
            if (globals.containsKey(name.lexeme)) {
                globals.put(name.lexeme, value);
            } else {
                throw new RuntimeError(name, String.format("Undefined variable '%s'.", name));
            }
        } else {
            ancestor(distance).values.add(slot, value);
        }

    }

    private EfficientEnvironment ancestor(int distance) {
        EfficientEnvironment env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }

        return env;
    }
}
