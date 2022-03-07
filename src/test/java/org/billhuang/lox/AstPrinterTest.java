package org.billhuang.lox;

import org.junit.Assert;
import org.junit.Test;

/**
 * @Description
 * @Data 2022/2/25 15:49
 * @Author huangshb
 **/
public class AstPrinterTest {
    @Test
    public void printTest() {
        Expr expr = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(12.34)));

        String ast = new AstPrinter().print(expr);
        // System.out.println(ast);
        Assert.assertEquals("(* (- 123) (group 12.34))", ast);
    }
}
