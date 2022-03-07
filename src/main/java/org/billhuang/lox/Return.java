package org.billhuang.lox;

/**
 * @Description
 * @Data 2022/3/7 16:30
 * @Author huangshb
 **/
public class Return extends RuntimeException{
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
