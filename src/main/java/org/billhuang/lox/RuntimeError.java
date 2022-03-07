package org.billhuang.lox;

/**
 * @Description
 * @Data 2022/2/25 18:56
 * @Author huangshb
 **/
public class RuntimeError extends RuntimeException{
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
