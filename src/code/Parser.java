package code;

import java.util.ArrayList;
import java.util.List;

import code.Stmt.Function;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (match(TokenType.FUNCTION) && !isAtEnd()) {
            statements.add(function("function"));
        }

        consume(TokenType.BEGIN, "Expecting BEGIN.");
        consume(TokenType.CODE, "Expecting 'CODE' after BEGIN");

        while (match(TokenType.STRING, TokenType.CHAR, TokenType.INT, TokenType.FLOAT, TokenType.BOOL,
                TokenType.IMMUTABLE)) {
            statements.addAll(varDeclaration());
        }

        while (!isAtEnd() && !check(TokenType.END)) {
            statements.add(statement());
        }

        consume(TokenType.END, "Expecting END.");
        consume(TokenType.CODE, "Expecting 'CODE' after END");
        System.out.println(tokens.get(current));
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Stmt statement() {
        if (match(TokenType.DISPLAY)) {
            consume(TokenType.COLON, "Expecting ':' after DISPLAY");
            return displayStatement();
        }

        if (match(TokenType.SCAN)) {
            consume(TokenType.COLON, "Expecting ':' after SCAN");
            return scanStatement();
        }

        if(match(TokenType.RETURN)) {
            return returnStatement();
        }

        if (match(TokenType.IF)) {
            return ifStatement();
        } else if (match(TokenType.WHILE)) {
            return whileStatement();
        }

        return expressionStatement();
    }

    private Stmt displayStatement() {
        Expr value = expression();
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;

        if(!check(TokenType.END)) {
            value = expression();
        }
        
        return new Stmt.Return(keyword, value);
    }

    private Stmt scanStatement() {
        List<Token> identifiers = new ArrayList<>();

        do {
            identifiers.add(consume(TokenType.IDENTIFIER, "Expecting identifier after 'scan'."));
        } while (match(TokenType.COMMA));

        return new Stmt.Scan(identifiers);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PARENTHESIS, "Expecting '(' after IF.");
        Expr condition = or();
        consume(TokenType.RIGHT_PARENTHESIS, "Expecting ')' after an expression");

        consume(TokenType.BEGIN, "Expecting a BEGIN after Condition.");
        consume(TokenType.IF, "Expecting an IF after BEGIN");

        List<Stmt> thenBranch = new ArrayList<>();
        while (!isAtEnd() && !check(TokenType.END)) {
            thenBranch.add(statement());
        }

        consume(TokenType.END, "Expecting END after a statement.");
        consume(TokenType.IF, "Expecting IF after END");

        List<Expr> elseIfConditions = new ArrayList<>();

        List<List<Stmt>> elseIfBranches = new ArrayList<>();

        while (peek().type == TokenType.ELSE && peekNext().type == TokenType.IF && !isAtEnd()) {
            consume(TokenType.ELSE, "Expect ELSE.");
            consume(TokenType.IF, "Expecti IF.");
            consume(TokenType.LEFT_PARENTHESIS, "Expecting '(' after IF.");
            Expr elseIfCondition = or();
            elseIfConditions.add(elseIfCondition);
            consume(TokenType.RIGHT_PARENTHESIS, "Expecting ')' after an expression");
            consume(TokenType.BEGIN, "Expecting a BEGIN after Condition.");
            consume(TokenType.IF, "Expecting an IF after BEGIN");

            List<Stmt> elseIfBranch = new ArrayList<>();
            while (!isAtEnd() && !check(TokenType.END)) {
                elseIfBranch.add(statement());
            }

            elseIfBranches.add(elseIfBranch);
            consume(TokenType.END, "Expecting END after a statement.");
            consume(TokenType.IF, "Expecting IF after END");
        }

        List<Stmt> elseBranch = null;

        if (match(TokenType.ELSE)) {
            elseBranch = new ArrayList<>();
            consume(TokenType.BEGIN, "Expecting a BEGIN after Condition.");
            consume(TokenType.IF, "Expecting an IF after BEGIN");

            while (!isAtEnd() && !check(TokenType.END)) {
                elseBranch.add(statement());
            }

            consume(TokenType.END, "Expecting END after a statement.");
            consume(TokenType.IF, "Expecting IF after END");

        }

        return new Stmt.If(condition, thenBranch, elseIfConditions, elseIfBranches, elseBranch);

    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PARENTHESIS, "Expecting '(' after IF.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PARENTHESIS, "Expecting ')' after an expression");

        consume(TokenType.BEGIN, "Expecting a BEGIN after Condition.");
        consume(TokenType.WHILE, "Expecting an IF after BEGIN");

        List<Stmt> body = new ArrayList<>();
        while (!isAtEnd() && !check(TokenType.END)) {
            body.add(statement());
        }

        consume(TokenType.END, "Expecting END after a statement.");
        consume(TokenType.WHILE, "Expecting IF after END");

        return new Stmt.While(condition, body);

    }

    @SuppressWarnings("unused")
    private Stmt declaration() {
        try {
            if(match(TokenType.STRING)) {
                return statement();
            }

            return statement();

        } catch (Exception e) {
            synchronize();
            return null;
        }
    }

    private List<Stmt> varDeclaration() {
        Token immut = previous();
        Token token = previous();
        boolean mutable = true;
        List<Token> names = new ArrayList<>();
        List<Expr> initializers = new ArrayList<>();

        if (immut.type == TokenType.IMMUTABLE) {
            mutable = false;
            token = consume(peek().type, "Expecting a variable type after 'IMMUT' keyword.");
        }

        do {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
            names.add(name);
            Expr initializer = null;
            if (match(TokenType.EQUAL)) {
                initializer = expression();
            }

            initializers.add(initializer);
        } while (match(TokenType.COMMA));

        List<Stmt> statements = new ArrayList<>();

        switch (token.type) {
            case STRING:
                for (int i = 0; i < names.size(); i++) {
                    Stmt statement = new Stmt.String(names.get(i), initializers.get(i), mutable);
                    statements.add(statement);
                }
                break;
            case CHAR:
                for (int i = 0; i < names.size(); i++) {
                    Stmt statement = new Stmt.Char(names.get(i), initializers.get(i), mutable);
                    statements.add(statement);
                }
                break;
            case INT:
                for (int i = 0; i < names.size(); i++) {
                    Stmt statement = new Stmt.Int(names.get(i), initializers.get(i), mutable);
                    statements.add(statement);
                }
                break;
            case FLOAT:
                for (int i = 0; i < names.size(); i++) {
                    Stmt statement = new Stmt.Float(names.get(i), initializers.get(i), mutable);
                    statements.add(statement);
                }
                break;
            default:
                for (int i = 0; i < names.size(); i++) {
                    Stmt statement = new Stmt.Bool(names.get(i), initializers.get(i), mutable);
                    statements.add(statement);
                }
                break;
        }
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    private Function function(String kind) {
        Token returnType = null;
        if(match(TokenType.STRING, TokenType.CHAR, TokenType.INT, TokenType.FLOAT, TokenType.BOOL)) {
            returnType = previous();
        }

        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PARENTHESIS, "Expect '(' after " + kind + " name.");
        List<Parameter> parameters = new ArrayList<>();

        if (!check(TokenType.RIGHT_PARENTHESIS)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                Token type = advance();
                Token paramName = advance();
                parameters.add(new Parameter(type, paramName));

            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PARENTHESIS, "Expect ')' after parameters.");

        consume(TokenType.BEGIN, "Expect 'BEGIN' before 'FN'.");
        consume(TokenType.FUNCTION, "Expect 'FN' before " + kind + " body.");

        List<Stmt> body = block(TokenType.FUNCTION);
        return new Stmt.Function(name, parameters, body, returnType);
    }

    private List<Stmt> block(TokenType type) {

        List<Stmt> statements = new ArrayList<>();
        while (match(TokenType.STRING, TokenType.CHAR, TokenType.INT, TokenType.FLOAT, TokenType.BOOL,
                TokenType.IMMUTABLE)) {
            statements.addAll(varDeclaration());
        }

        
        while (match(TokenType.FUNCTION) && !isAtEnd()) {
            statements.add(function("function"));
        }

        

        while (!isAtEnd() && !check(TokenType.END)) {
            statements.add(statement());
        }

        consume(TokenType.END, "Expect 'END' after function body.");
        if (peek().type == type) {
            consume(peek().type, "Expecting 'FN' after END");
        }

        return statements;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.NOT_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER_THAN, TokenType.GREATER_THAN_EQUAL, TokenType.LESS_THAN,
                TokenType.LESS_THAN_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS, TokenType.AMPERSAND)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR, TokenType.MODULO)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.NOT, TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        if (match(TokenType.LEFT_PARENTHESIS)) {
            expr = finishCall(expr);
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PARENTHESIS)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments");
                }
                Expr expr = expression();
                arguments.add(expr);
            } while (match(TokenType.COMMA));
        }

        Token rightParen = consume(TokenType.RIGHT_PARENTHESIS, "Expecting a parenthesis after a function call.");

        return new Expr.Call(callee, rightParen, arguments);
    }

    private Expr primary() {
        if (match(TokenType.TRUE_LITERAL))
            return new Expr.Literal(true);
        if (match(TokenType.FALSE_LITERAL))
            return new Expr.Literal(false);
        if (match(TokenType.NULL))
            return new Expr.Literal(null);
        if (match(TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL,
                TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL, TokenType.DOLLAR_SIGN))
            return new Expr.Literal(previous().literal);
        if (match(TokenType.LEFT_PARENTHESIS)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PARENTHESIS, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Code.error(token, message);
        return new ParseError();
    }

    @SuppressWarnings("unused")
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON)
                return;
            switch (peek().type) {
                case STRING:
                case INT:
                case CHAR:
                case BOOL:
                case DISPLAY:
                case SCAN:
                    return;
                default:
                    break;
            }
            advance();
        }
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        return tokens.get(current + 1);
    }

    private Token advance() {
        if (!isAtEnd())
            current++;

        return previous();
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
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

}
