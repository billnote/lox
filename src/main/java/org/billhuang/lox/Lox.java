package org.billhuang.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @Description
 * @Data 2022/2/22 16:34
 * @Author huangshb
 **/
public class Lox {
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: lox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }

            runRepl(line);
        }
    }

    static void runRepl(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Object syntax = parser.parseRepl();

         if (hadError) {
            return;
         }

        if (syntax instanceof List) {
            Resolver resolver = new Resolver(interpreter);
            List<Stmt> statements = (List<Stmt>) syntax;
            resolver.resolve(statements);

            if (hadError) {
                return;
            }

            interpreter.interpret(statements);
        } else if (syntax instanceof Expr) {
            String result = interpreter.interpret((Expr) syntax);
            System.out.println(result);
        }
    }

    static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        //Expr expression = parser.parse();
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) {
            return;
        }

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) {
            return;
        }

        // System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, String.format(" at '%s'", token.lexeme), message);
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println(String.format("[line %s ], Error%s: %s", line, where, message));
        hadError =true;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(String.format("%s \n[line %s ]", error.getMessage(), error.token.line));
        hadRuntimeError = true;
    }
}
