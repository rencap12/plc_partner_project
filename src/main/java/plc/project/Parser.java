package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        // Parse fields and methods, assuming they are interleaved.
        while (peek("LET", "VAR", "FUN")) {
            if (match("LET", "VAR")) {
                fields.add(parseField());
            } else if (match("FUN")) {
                methods.add(parseMethod());
            }
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        boolean isConstant = match("LET");
        if (!isConstant && !match("VAR")) {
            throw new ParseException("Expected 'LET' or 'VAR'", tokens.get(0).getIndex());
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        return new Ast.Field(name, isConstant, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!match("FUN")) {
            throw new ParseException("Expected 'FUN'", tokens.get(0).getIndex());
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected method name", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        if (!match("(")) {
            throw new ParseException("Expected '(' after method name", tokens.get(0).getIndex());
        }

        List<String> parameters = new ArrayList<>();
        if (!peek(")")) {
            do {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter name", tokens.get(0).getIndex());
                }
                parameters.add(tokens.get(0).getLiteral());
                tokens.advance();
            } while (match(","));
        }

        if (!match(")")) {
            throw new ParseException("Expected ')'", tokens.get(0).getIndex());
        }

        if (!match("{")) {
            throw new ParseException("Expected '{' at start of method body", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("}")) {
            statements.add(parseStatement());
        }

        if (!match("}")) {
            throw new ParseException("Expected '}' at end of method body", tokens.get(0).getIndex());
        }

        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("FOR")) {
            return parseForStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            return parseAssignmentOrExpressionStatement();
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!match("LET")) {
            throw new ParseException("Expected 'LET'", tokens.get(0).getIndex());
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected variable name", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        if (!match("IF")) {
            throw new ParseException("Expected 'IF'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'IF' condition", tokens.get(0).getIndex());
        }

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("}")) {
            thenStatements.add(parseStatement());
        }

        if (!match("}")) {
            throw new ParseException("Expected '}' at end of 'then' block", tokens.get(0).getIndex());
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            if (!match("{")) {
                throw new ParseException("Expected '{' after 'ELSE'", tokens.get(0).getIndex());
            }
            while (!peek("}")) {
                elseStatements.add(parseStatement());
            }
            if (!match("}")) {
                throw new ParseException("Expected '}' at end of 'else' block", tokens.get(0).getIndex());
            }
        }

        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        if (!match("FOR")) {
            throw new ParseException("Expected 'FOR'", tokens.get(0).getIndex());
        }

        Ast.Statement initialization = parseStatement();

        Ast.Expression condition = parseExpression();

        Ast.Statement increment = parseStatement();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'FOR' condition", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("}")) {
            statements.add(parseStatement());
        }

        return new Ast.Statement.For(initialization, condition, increment, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if (!match("WHILE")) {
            throw new ParseException("Expected 'WHILE'", tokens.get(0).getIndex());
        }

        Ast.Expression condition = parseExpression();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'WHILE' condition", tokens.get(0).getIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("}")) {
            statements.add(parseStatement());
        }

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if (!match("RETURN")) {
            throw new ParseException("Expected 'RETURN'", tokens.get(0).getIndex());
        }

        Ast.Expression value = parseExpression();

        if (!match(";")) {
            throw new ParseException("Expected ';' after return statement", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Return(value);
    }

    /* ADDED, MAYBE REMOVE LATER*/
    public Ast.Statement parseAssignmentOrExpressionStatement() throws ParseException {
        Ast.Expression receiver = parseExpression();

        if (match("=")) {
            Ast.Expression value = parseExpression();
            if (!match(";")) {
                throw new ParseException("Expected ';' after assignment", tokens.get(0).getIndex());
            }
            return new Ast.Statement.Assignment(receiver, value);
        } else if (match(";")) {
            return new Ast.Statement.Expression(receiver);
        } else {
            throw new ParseException("Expected '=' or ';'", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseEqualityExpression();

        while (match("AND", "OR")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseEqualityExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();

        while (match("==", "!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();

        while (match("+", "-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();

        while (match("*", "/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression receiver = parsePrimaryExpression();

        // Handle function calls if '(' is present
        while (match("(")) {
            List<Ast.Expression> arguments = new ArrayList<>();
            if (!peek(")")) {
                do {
                    arguments.add(parseExpression());
                } while (match(","));
            }

            if (!match(")")) {
                throw new ParseException("Expected closing parenthesis ')'", tokens.get(0).getIndex());
            }

            // Convert the current receiver into a function call
            receiver = new Ast.Expression.Function(Optional.of(receiver), null, arguments);
        }

        return receiver;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match(Token.Type.INTEGER)) {
            // Integer literal
            return new Ast.Expression.Literal(Integer.parseInt(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            // Decimal literal
            return new Ast.Expression.Literal(Double.parseDouble(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            // Character literal
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral().charAt(0));
        } else if (match(Token.Type.STRING)) {
            // String literal
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral());
        } else if (match(Token.Type.IDENTIFIER)) {
            // Variable reference or function call
            String identifier = tokens.get(-1).getLiteral();

            // Check if it's a function call
            if (peek("(")) {
                // No receiver for this function, the identifier is the name
                return parseFunctionCall(Optional.empty(), identifier);
            } else {
                // Variable reference
                return new Ast.Expression.Access(Optional.empty(), identifier);
            }
        } else if (match("(")) {
            // Grouped expression (parentheses)
            Ast.Expression expression = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected closing parenthesis ')'", tokens.get(0).getIndex());
            }
            return new Ast.Expression.Group(expression);
        } else {
            // Invalid expression
            throw new ParseException("Expected expression", tokens.get(0).getIndex());
        }
    }

    // helper func
    private Ast.Expression parseFunctionCall(Optional<Ast.Expression> receiver, String name) throws ParseException {
        match("("); // Consume '('

        List<Ast.Expression> arguments = new ArrayList<>();
        if (!peek(")")) {
            do {
                arguments.add(parseExpression());
            } while (match(","));
        }

        if (!match(")")) {
            throw new ParseException("Expected closing parenthesis ')'", tokens.get(0).getIndex());
        }

        return new Ast.Expression.Function(receiver, name, arguments);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;

    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
