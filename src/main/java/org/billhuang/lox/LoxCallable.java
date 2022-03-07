package org.billhuang.lox;

import java.util.List;

/**
 * @Description
 * @Data 2022/3/4 14:57
 * @Author huangshb
 **/
public interface LoxCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
