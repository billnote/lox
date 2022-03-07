package org.billhuang.lox;

import org.junit.Test;

/**
 * @Description
 * @Data 2022/2/23 11:07
 * @Author huangshb
 **/
public class ScannerTest {
    @Test
    public void scanTokenTest() {
        String source = "print \"hello, lox\"";
        Scanner scanner = new Scanner(source);

    }
}
