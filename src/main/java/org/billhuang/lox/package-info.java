/**
 * A Grammar for Lox  expressions
 *
 * Literals: Numbers,strings,Booleans, and nil.
 * Unary expressions: A prefix ! to preform a logical not, and - to negate a number.
 * Binary expressions: The infix arithmetic (+,-,*,/) and logic operators (==,!=,<,<=,>,>=) we know and love.
 * Parentheses: A pair of ( and ) wrapped around an expression.
 *
 * BNF:
 *
 * Terminal: quoted string or UPPERCASE words.
 * Nonterminal: lowercase words.
 *
 * expression -> literal
 *             | unary
 *             | binary
 *             | grouping ;
 *
 * literal    -> NUMBER | STRING | "true" | "false" | nil ;
 * grouping   -> "(" expression ")" ;
 * unary      -> ("-" | "!") expression ;
 * binary     -> expression operator expression ;
 * operator   -> "==" | "!=" | "<" | "<=" | ">" | ">="
 *             | "+" | "-" | "*" | "/" ;
 *
 * expression -> equality ;
 * equality   -> comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison -> term ( ( ">" | ">=" | "<" | "<=") term )* ;
 * term       -> factor ( ( "-" | "+" ) factor )* ;
 * factor     -> unary ( ( "/" | "*" ) unary )* ;
 * unary      -> ( "!" | "-" ) unary
 *             | primary ;
 * primary    -> NUMBER | STRING | "true" | "false | "nil"
 *             | "(" expression ")" ;
 *
 * Parser:
 * top-down parser: expression -> Primary
 * bottom-up parser (like: LR): Primary -> expression
 *
 * */

package org.billhuang.lox;