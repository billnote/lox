package org.billhuang.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ======== statement =========
 *
 * program     -> declaration* EOF;
 *
 * declaration -> classDecl
 *              | funDecl
 *              | varDecl
 *              | statement ;
 * classDecl   -> "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
 * funDecl     -> "fun" function ;
 * function    -> ( "class" )? IDENTIFIER "(" parameters? ")" block ;
 * parameters  -> IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl     -> "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement   -> exprStmt
 *              | ifStmt
 *              | whileStmt
 *              | forStmt
 *              | printStmt
 *              | breakStmt
 *              | returnStmt
 *              | block ;
 * ifStmt      -> "if" "(" expression ")" statement ( "else" statement )? ;
 * whileStmt   -> "while" "(" expression ")" statement ;
 * forStmt     -> "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;
 * block       -> "{" declaration* "}" ;
 * exprStmt    -> expression ";" ;
 * printStmt   -> "print" expression ";" ;
 * breakStmt   -> "break" ";";
 * returnStmt  -> "return" expression? ";" ;
 *
 *
 * ======== expression =========
 *
 * expression  -> assignment ;
 * assignment  -> (call ".")? IDENTIFIER "=" assignment
 *              | logic_or;
 * logic_or    -> logic_and ( "or" logic_and)* ;
 * logic_and   -> conditional ( "and" conditional)* ;
 * conditional -> equality ( "?" expression ":" conditional )? ;
 * equality    -> comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison  -> term ( ( ">" | ">=" | "<" | "<=") term )* ;
 * term        -> factor ( ( "-" | "+" ) factor )* ;
 * factor      -> unary ( ( "/" | "*" ) unary )* ;
 * unary       -> ( "!" | "-") unary
 *              | call ;
 * call        -> primary ( "(" arguments? ")" | "." IDENTIFIER )*;
 * arguments   -> expression ( "," expression  )* ;
 * functionBody -> "fun" "(" parameters? ")" block ;
 * primary     -> NUMBER | STRING | "true" | "false | "nil"
 *              | "(" expression ")"
 *              | IDENTIFIER ;
 *              | functionBody
 *              | "this"
 *              | "supper" "." IDENTIFIER
 *
 * @Description
 * @Data 2022/2/25 11:16
 * @Author huangshb
 **/
public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    private boolean allowExpression;
    private boolean foundExpression = false;

    private int loopDepth = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }*/

    List<Stmt> parse() {
       List<Stmt> statements = new ArrayList<>();
       while (!isAtEnd()) {
           statements.add(declaration());
       }

       return statements;
    }

    Object parseRepl() {
        allowExpression = true;
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());

            if (foundExpression) {
                Stmt last = statements.get(statements.size() - 1);
                return ((Stmt.Expression) last).expression;
            }

            allowExpression = false;
        }
        return statements;
    }

    /**
     * declaration -> classDecl
     *              | funDecl
     *              | varDecl
     *              | statement ;
     * @return
     */
    private Stmt declaration() {
        try{
            if (check(TokenType.CLASS, TokenType.IDENTIFIER, TokenType.LEFT_BRACE)) {
                consume(TokenType.CLASS, null);
                return classDecl();
            }
            if (match(TokenType.CLASS)) {
                return classDecl();
            }
            if (check(TokenType.FUN) && checkNext(TokenType.IDENTIFIER)) {
                consume(TokenType.FUN, null);
                return funDecl();
            }
            if (match(TokenType.VAR)) {
                return varDecl();
            }

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * classDecl   -> "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
     * @return
     */
    private Stmt classDecl() {
        Token className = consume(TokenType.IDENTIFIER, "Expect class name.");

        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect super class name.");
            superclass = new Expr.Variable(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' after class name.");

        List<Stmt.Function> functions = new ArrayList<>();
        List<Stmt.Function> staticFunctions = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.CLASS, TokenType.IDENTIFIER, TokenType.LEFT_PAREN)) {
                consume(TokenType.CLASS, "Expect 'class' keyword before static function name.");
                staticFunctions.add(funDecl());
            } else {
                functions.add(funDecl());
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(className, superclass, functions, staticFunctions);
    }

    /**
     * funDecl    -> "fun" function ;
     * function   -> ( "class" )? IDENTIFIER "(" parameters? ")" block ;
     * parameters -> IDENTIFIER ( "," IDENTIFIER )* ;
     * @return
     */
    private Stmt.Function funDecl() {
        Token funName = consume(TokenType.IDENTIFIER, "Expect function name.");
        Expr.Function function = functionBody();

        return new Stmt.Function(funName, function);
    }

    /**
     * functionBody -> "fun" "(" parameters? ")" block ;
     * @return
     */
    private Expr.Function functionBody() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after function name.");
        List<Token> params = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                params.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

        consume(TokenType.LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = block();

        return new Expr.Function(params, body);
    }

    /**
     * varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
     */
    private Stmt varDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * statement   -> exprStmt
     *              | ifStmt
     *              | whileStmt
     *              | forStmt
     *              | printStmt
     *              | break
     *              | block ;
     * @return
     */
    private Stmt statement() {
        if (match(TokenType.IF)) {
            return ifStmt();
        }
        if (match(TokenType.WHILE)) {
            return whileStmt();
        }
        if (match(TokenType.FOR)) {
            return forStmtDesugar();
        }
        if (match(TokenType.PRINT)) {
            return printStmt();
        }
        if (match(TokenType.BREAK)) {
            return breakStmt();
        }
        if (match(TokenType.RETURN)) {
            return returnStmt();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        return exprStmt();
    }

    /**
     * ifStmt -> "if" "(" expression ")" statement ( "else" statement )? ;
     * @return
     */
    private Stmt ifStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * whileStmt -> "while" "(" expression ")" statement ;
     * @return
     */
    private Stmt whileStmt() {
        try {
            loopDepth++;
            consume(TokenType.LEFT_PAREN, "Expect '(' after where.");
            Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after where condition.");

            Stmt body = statement();

            return new Stmt.While(condition, body);
        } finally {
          loopDepth--;
        }
    }

    /**
     * forStmt -> "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;
     * @return
     */
    private Stmt forStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after for.");

        Stmt first = null;
        if (match(TokenType.VAR)) {
            first = varDecl();
        } else if (!check(TokenType.SEMICOLON)) {
            first = exprStmt();
        } else {
            consume(TokenType.SEMICOLON, "Expect ';' after for first.");
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after for condition.");

        Expr last = null;
        if (!check(TokenType.SEMICOLON)) {
            last = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for last expr.");

        Stmt body = statement();

        return new Stmt.For(first, condition, last, body);
    }

    private Stmt forStmtDesugar() {
        try {
            loopDepth++;
            consume(TokenType.LEFT_PAREN, "Expect '(' after for.");

            Stmt initializer = null;
            if (match(TokenType.VAR)) {
                initializer = varDecl();
            } else if (!check(TokenType.SEMICOLON)) {
                initializer = exprStmt();
            } else {
                consume(TokenType.SEMICOLON, "Expect ';' after for first.");
            }

            Expr condition = null;
            if (!check(TokenType.SEMICOLON)) {
                condition = expression();
            }
            consume(TokenType.SEMICOLON, "Expect ';' after for condition.");

            Expr increment = null;
            if (!check(TokenType.SEMICOLON)) {
                increment = expression();
            }
            consume(TokenType.RIGHT_PAREN, "Expect ')' after for last expr.");

            Stmt body = statement();

            if (increment != null) {
                body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
            }

            if (condition == null) {
                condition = new Expr.Literal(true);
            }
            body = new Stmt.While(condition, body);

            if (initializer != null) {
                body = new Stmt.Block(Arrays.asList(initializer, body));
            }

            return body;
        } finally {
            loopDepth--;
        }
    }

    /**
     * block -> "{" declaration* "}" ;
     * @return
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    /**
     * exprStmt    -> expression ";" ;
     */
    private Stmt exprStmt() {
        Expr expr = expression();
        if (allowExpression && isAtEnd()) {
            foundExpression = true;
        } else {
            consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        }

        return new Stmt.Expression(expr);
    }

    /**
     * printStmt   -> "print" expression ";" ;
     * @return
     */
    private Stmt printStmt() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return  new Stmt.Print(expr);
    }

    private Stmt breakStmt() {
        Token previous = previous();
        if (loopDepth <= 0) {
            throw error(previous, "Break must in loop.");
        }
        consume(TokenType.SEMICOLON, "Except ';' after break.");
        return new Stmt.Break(previous);
    }

    /**
     * returnStmt  -> "return" expression? ";" ;
     * @return
     */
    private Stmt returnStmt() {
        Token keyword = previous();
        Expr expr = null;
        if (!check(TokenType.SEMICOLON)) {
            expr = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after value.");

        return new Stmt.Return(keyword, expr);
    }

    /**
     * expression -> assignment ;
     * @return
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * assignment  -> (call ".")? IDENTIFIER "=" assignment
     *              | logic_or;
     * @return
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * logic_or -> logic_and ( "or" logic_and)* ;
     * @return
     */
    private Expr or() {
        Expr left = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            return new Expr.Logical(left, operator, right);
        }

        return left;
    }

    /**
     * logic_and   -> conditional ( "and" conditional)* ;
     * @return
     */
    private Expr and() {
        Expr left = conditional();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = conditional();
            return new Expr.Logical(left, operator, right);
        }

        return left;
    }
    /**
     * comma -> conditional ( "," conditional )* ;
     * @return

    private Expr comma() {
        Expr expr = conditional();
        while (match(TokenType.COMMA)) {
            Token operator = previous();
            Expr right = conditional();
            expr = new Expr.Binary(expr, operator, right);
        }

        // TODO actual comma operator

        return expr;
    }
     */

    /**
     * conditional -> equality ( "?" expression ":" conditional )?
     */
    private Expr conditional() {
        Expr expr = equality();
        if (match(TokenType.QUESTION_MARK)) {
            Expr expression = expression();
            consume(TokenType.COLON, "Expect ':' after then branch of conditional expression.");
            expr = new Expr.Conditional(expr, expression, conditional());
        }

        return expr;
    }

    /**
     * equality -> comparison ( ( "!=" | "==" ) comparison )* ;
     * @return
     */
    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * comparison -> term ( ( ">" | ">=" | "<" | "<=") term )* ;
     * @return
     */
    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term -> factor ( ( "-" | "+" ) factor )* ;
     * @return
     */
    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor -> unary ( ( "/" | "*" ) unary )* ;
     * @return
     */
    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary -> ( "!" | "-") unary
     *        | call ;
     * @return
     */
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    /**
     * call -> primary ( "(" arguments? ")" | "." IDENTIFIER )*;
     * @return
     */
    private Expr call() {
        Expr primary = primary();
        List<Expr> arguments = new ArrayList<>();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                if (!check(TokenType.RIGHT_PAREN)) {
                    arguments = arguments();
                }
                Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after call arguments.");

                primary = new Expr.Call(primary, paren , arguments);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                primary = new Expr.Get(primary, name);
            } else {
                break;
            }
        }

        return primary;
    }

    /**
     * arguments -> ( funDecl | expression ) ( "," ( funDecl | expression )  )* ;
     * @return
     */
    private List<Expr> arguments() {
        List<Expr> arguments = new ArrayList<>();
        arguments.add(expression());

        while (match(TokenType.COMMA)) {
            if (arguments.size() >= 255) {
                error(peek(), "Can't have more than 255 arguments.");
            }
            arguments.add(expression());
        }
        return arguments;
    }

    /** primary -> NUMBER | STRING | "true" | "false | "nil"
     *           | "(" expression ")"
     *           | IDENTIFIER ;
     *           | functionBody
     *           | "this"
     **/
    private Expr primary() {
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");

            return new Expr.Grouping(expr);
        }

        if (match(TokenType.FUN)) {
            return functionBody();
        }

        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }

        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name");
            return new Expr.Super(keyword, method);

        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        return isAtEnd() ? false : peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        return isAtEnd() || tokens.get(current + 1).type == TokenType.EOF
                ? false
                : tokens.get(current + 1).type == type;
    }

    private boolean checkStep(int step, TokenType type) {
        if (isAtEnd() || current + step >= tokens.size()) {
            return false;
        }

        return tokens.get(current + step).type  == type;
    }

    private boolean check(TokenType... types) {
        for (int i = 0; i < types.length; i++) {
            if (!checkStep(i, types[i])) {
                return false;
            }
        }

        return true;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }

        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return;
            }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private static class ParseError extends RuntimeException{};
}
