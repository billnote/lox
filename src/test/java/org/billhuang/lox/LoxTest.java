package org.billhuang.lox;

import org.junit.Test;

/**
 * @Description
 * @Data 2022/2/25 15:54
 * @Author huangshb
 **/
public class LoxTest {

    @Test
    public void runReplTest() {
        String source = "4 * (3 + 3) / 2 + 1";
        Lox.runRepl(source);

        source = "1 * 2 == 2 ? 5 : 2 + 2";
        Lox.runRepl(source);

        // TODO: fix comma operator
        source = "1 + 1, 1 * 2 == 3 ? 5 : 2 + 2";
        Lox.runRepl(source);
    }

    @Test
    public void runTest() {
        String source = "print \"one\";";
        Lox.run(source);

        source = "var a = 1; var b = 2; print a+b;";
        Lox.run(source);
    }

    @Test
    public void blockTest() {
        String source = "var a = 1; { var a = a + 2; print a;}";
        Lox.run(source);
    }

    @Test
    public void logicalTest() {
        String source = "print nil or \"yes\";";
        Lox.run(source);
    }

    @Test
    public void whileTest() {
        String source = "var a = 2; while (a > 0) {print a; a = a-1;}";
        Lox.run(source);
    }

    @Test
    public void forTest() {
        String source = "for (var i = 4; i > 0; i = i - 1) {print i;}";
        Lox.run(source);
    }

    @Test
    public void breakSyntaxErrorTest() {
        String source = "var a = 2; if (a > 0) {print a; break;}";
        Lox.run(source);
    }

    @Test
    public void breakTest() {
        String source = "for (var i = 4; i > 0; i = i - 1) {print i; break;}";
        Lox.run(source);

        source = "for (var i = 4; i > 0; i = i - 1) {print i; if (i == 2){ break;}}";
        Lox.run(source);

        source = "for (var i = 4; i > 0; i = i - 1) {print i; while (i > 0){ print \"break at i = \" + i; break;}}";
        Lox.run(source);
    }

    @Test
    public void functionTest() {
        String source = "var a = 1; print a; fun nestFun(b, c) {print a + b + c;} nestFun(2, 3);";
        Lox.run(source);
    }

    @Test
    public void functionNestTest() {
        // TODO support closure
        String source = "var a = 1; {print a; var d = 4; fun nestFun(b, c) {print a + b + c + d;} nestFun(2, 3);}";
        Lox.run(source);
    }

    @Test
    public void functionReturnTest() {
        String source = "fun fib(n) { if (n <= 1) return n; return fib(n -2 ) + fib(n - 1);} print fib(4);";
        Lox.run(source);
    }

    @Test
    public void anonymousFunctionTest() {
        // TODO support anonymous fun
        String source = "fun call(fn) {  \n" +
                          " var s = \"anonymous fn\"; \n" +
                          " fn(s);\n" +
                       " }\n" +
                       " call(fun (s) {\n" +
                         " print \"hello \" + s;\n" +
                       "});";
        Lox.run(source);
    }

    @Test
    public void closureTest() {
        String source = "var a = \"global\"; { fun show() {print a;} show(); var a = \"block\"; show();}";
        Lox.run(source);
    }

    @Test
    public void classTest() {
        String source = "class Test { sayHello() {print \"hello.\";}} print Test; var test = Test(); print test;";
        Lox.run(source);
    }

    @Test
    public void testDanglingElse() {
        if (1==1)
            if(1 == 2) System.out.println("top level");
        else System.out.println("nested");
    }
}
