package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
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
            throw new ParseException("Expected 'LET' or 'VAR'", tokens.index);
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier", tokens.index);
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.index);
        }

        return new Ast.Field(name, isConstant, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!match("FUN")) {
            throw new ParseException("Expected 'FUN'", tokens.index);
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected method name", tokens.index);
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        if (!match("(")) {
            throw new ParseException("Expected '(' after method name", tokens.index);
        }

        List<String> parameters = new ArrayList<>();
        if (!peek(")")) {
            do {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter name", tokens.index);
                }
                parameters.add(tokens.get(0).getLiteral());
                tokens.advance();
            } while (match(","));
        }

        if (!match(")")) {
            throw new ParseException("Expected ')'", tokens.index);
        }

        if (!match("{")) {
            throw new ParseException("Expected '{' at start of method body", tokens.index);
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("}")) {
            statements.add(parseStatement());
        }

        if (!match("}")) {
            throw new ParseException("Expected '}' at end of method body", tokens.index);
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
            throw new ParseException("Expected 'LET'", tokens.index);
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected variable name", tokens.index);
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.index);
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
            throw new ParseException("Expected 'IF'", tokens.index);
        }

        Ast.Expression condition = parseExpression();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'IF' condition", tokens.index);
        }

        List<Ast.Statement> thenStatements = new ArrayList<>();
        while (!peek("}")) {
            thenStatements.add(parseStatement());
        }

        if (!match("}")) {
            throw new ParseException("Expected '}' at end of 'then' block", tokens.index);
        }

        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (match("ELSE")) {
            if (!match("{")) {
                throw new ParseException("Expected '{' after 'ELSE'", tokens.index);
            }
            while (!peek("}")) {
                elseStatements.add(parseStatement());
            }
            if (!match("}")) {
                throw new ParseException("Expected '}' at end of 'else' block", tokens.index);
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
            throw new ParseException("Expected 'FOR'", tokens.index);
        }

        Ast.Statement initialization = parseStatement();

        Ast.Expression condition = parseExpression();

        Ast.Statement increment = parseStatement();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'FOR' condition", tokens.index);
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
            throw new ParseException("Expected 'WHILE'", tokens.index);
        }

        Ast.Expression condition = parseExpression();

        if (!match("{")) {
            throw new ParseException("Expected '{' after 'WHILE' condition", tokens.index);
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
            throw new ParseException("Expected 'RETURN'", tokens.index);
        }

        Ast.Expression value = parseExpression();

        if (!match(";")) {
            throw new ParseException("Expected ';' after return statement", tokens.index);
        }

        return new Ast.Statement.Return(value);
    }

    /* ADDED, MAYBE REMOVE LATER*/
    public Ast.Statement parseAssignmentOrExpressionStatement() throws ParseException {
        Ast.Expression receiver = parseExpression();

        if (match("=")) {
            Ast.Expression value = parseExpression();
            if (!match(";")) {
                throw new ParseException("Expected ';' after assignment", tokens.index);
            }
            return new Ast.Statement.Assignment(receiver, value);
        } else if (peek(";")) {
            match(";");
            return new Ast.Statement.Expression(receiver);
        } else {
            throw new ParseException("Expected '=' or ';'", tokens.index);
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

        while (peek("&&") || peek("||")) {
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression right = parseEqualityExpression();
            if (!peek("&&") && !peek("||"))
                return new Ast.Expression.Binary(operator, left, right);
            else
                left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();

        while (peek("==") || peek("!=") || peek("<") || peek("<=") || peek(">") || peek(">=")) {
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression right = parseAdditiveExpression();
            if (!peek("==") && !peek("!=") || peek("<") || peek("<=") || peek(">") || peek(">="))
                return new Ast.Expression.Binary(operator, left, right);
            else
                left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();

        while (peek("-") || peek("+")) {
            match(Token.Type.OPERATOR);
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
        //Ast.Expression left = parsePrimaryExpression();
        Ast.Expression left = parseSecondaryExpression();

        while (peek ("*") || peek("/")) {
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression right = parseSecondaryExpression();
            if (!peek("*") && !peek("/"))
                return new Ast.Expression.Binary(operator, left, right);
            else
                left = new Ast.Expression.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression receiver = parsePrimaryExpression();
        if (!peek(".")) { // nothing after primary
            return receiver;
        } else {
            String member = "";
            while (peek(".")) { // continue to get members
                match(".");
                if (peek(Token.Type.IDENTIFIER)) {
                    member = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                } // throw error if not identifier - implement later

                // get ( could be start of function call
                if (peek("(")) {
                    match("(");

                    List<Ast.Expression> arguments = new ArrayList<Ast.Expression>();

                    while (!peek(")")) {
                        arguments.add(parseExpression());
                        if (peek(","))
                            match(",");
                    }
                    // ensure close )
                    match(")");
                    if (!peek(".")) { // just function call
                        return new Ast.Expression.Function(Optional.of(receiver), member, arguments);
                    } else { // keep going
                        receiver = new Ast.Expression.Function(Optional.of(receiver), member, arguments);
                    }
                } else {
                    if (!peek(".")) {
                        return new Ast.Expression.Access(Optional.of(receiver), member);
                    } else {
                        receiver = new Ast.Expression.Access(Optional.of(receiver), member);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek(Token.Type.INTEGER)) {
            // Integer literal
            BigInteger val = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(val);
        } else if (peek(Token.Type.DECIMAL)) {
            // Decimal literal
            BigDecimal val = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(val);
        } else if (peek(Token.Type.CHARACTER)) {
            // Character literal
            // 's' e.g.
            if (tokens.get(0).getLiteral().length() < 4) { // not escape char
                char c = tokens.get(0).getLiteral().charAt(1);
                // System.out.println(c);
                match(Token.Type.CHARACTER);
                return new Ast.Expression.Literal(c);
            } else { // escape char
//                char c = tokens.get(0).getLiteral().charAt(1);
//                match(Token.Type.CHARACTER);
//                return new Ast.Expression.Literal(c);
                String temp = tokens.get(0).getLiteral();
                // BREAK THIS INTO MODULAR METHOD: ////////////////////////////
                temp = temp.replace("\\b", "\b");
                temp = temp.replace("\\n", "\n");
                temp = temp.replace("\\r", "\r");
                temp = temp.replace("\\t", "\t");
                if (temp.equals("'\\\"'"))
                    temp = "'\"'";
                if (temp.equals("'\\\\'"))
                    temp = "'\\'";
                if (temp.equals("'\\\''"))
                    temp = "'\''";
                Character c = temp.charAt(1);
                match(Token.Type.CHARACTER);
                return new Ast.Expression.Literal(c);
            }
        } else if (peek(Token.Type.STRING)) {
            // String literal
            match(Token.Type.STRING);
            String temp = tokens.get(-1).getLiteral();
            temp = temp.replace("\\\"", "\"");
            temp = temp.replace("\\\\", "\\");
            temp = temp.replace("\\\'", "\'");
            temp = temp.replace("\\b", "\b");
            temp = temp.replace("\\n", "\n");
            temp = temp.replace("\\r", "\r");
            temp = temp.replace("\\t", "\t");
            temp = temp.substring(1,temp.length() - 1); // remove ""
            return new Ast.Expression.Literal(temp);
        } else if (peek(Token.Type.IDENTIFIER)) {

            // Variable reference or function call
            String identifier = tokens.get(0).getLiteral();

            match(Token.Type.IDENTIFIER);
            if (tokens.get(-1).getLiteral().equals("NIL")) {
                match("NIL");
                return new Ast.Expression.Literal(null);
            }
            else if (tokens.get(-1).getLiteral().equals("TRUE")) {
                Boolean temp = true;
                return new Ast.Expression.Literal(temp);
            }
            else if (tokens.get(-1).getLiteral().equals("FALSE")) {
                Boolean temp = false;
                return new Ast.Expression.Literal(temp);
            }


            // Check if it's a function call
            if (peek("(")) {
                // Parse function call
                return parseFunctionCall(Optional.empty(), identifier);
            } else {
                // Variable reference
                return new Ast.Expression.Access(Optional.empty(), identifier);
            }
        } else if (match("(")) {
            // Grouped expression (e.g., "(expr)")
            Ast.Expression expression = parseExpression();
            if (!peek(")")) {
                if (!peek("+") && !peek("-") && !peek("*") && !peek("/") && !peek("&&") && !peek("||") && !peek("<") && !peek("<=") && !peek(">") && !peek(">=") && !peek("==") && !peek("!=")) {
                    match(")");
                    throw new ParseException("Expected closing parenthesis ')'", tokens.index);
                }
            }
            return new Ast.Expression.Group(expression);

        } else {
            throw new ParseException("Expected expression", tokens.index);
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
            throw new ParseException("Expected closing parenthesis ')'", tokens.index);
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
