package plc.project;

import java.util.ArrayList;
import java.util.List;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while(chars.has(0)) {
            while(peek("[ \b\n\r\t]")){
                chars.advance();
            }

            if(chars.has(0)){
                tokens.add(lexToken());
            }
        }
        for (Token token: tokens){
            System.out.println("token:" + token.toString());
        }

        return tokens;
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (peek("[0-9+-]")) {
            return lexNumber();
        } else if (peek("'")) {
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } else if (peek("[!@#$%^&*()=<>+/-]")) {
            return lexOperator();
        } else {
            throw new ParseException("Unknown token", chars.index);
        }

        // throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {
        if (!peek("[A-Za-z_]")) {
            throw new ParseException("Invalid identifier start", chars.index);
        }
        while (match("[A-Za-z0-9_-]")) {
            // Keep matching valid identifier characters
        }
        return chars.emit(Token.Type.IDENTIFIER);
        // throw new UnsupportedOperationException(); //TODO
    }

    public Token lexNumber() {
        // Handle negative numbers
        boolean isNegative = match("-");

        // Capture the digits
        StringBuilder number = new StringBuilder();

        // Check for leading zeros
        if (peek("0")) {
            number.append(chars.get(0));
            chars.advance();

            // If there is another digit after the zero, it's invalid (leading zero case)
            if (peek("\\d")) {
                throw new ParseException("Invalid leading zero in number", chars.index);
            }
        } else {
            // Capture digits for numbers without leading zero
            while (peek("\\d")) {
                number.append(chars.get(0));
                chars.advance();
            }
        }

        // Check if a decimal point follows
        if (peek("\\.")) {
            number.append('.'); // Include the decimal point
            chars.advance(); // Consume the decimal point

            // Ensure there are digits following the decimal point
            if (!peek("\\d")) {
                throw new ParseException("Invalid decimal number", chars.index);
            }

            // Capture digits after the decimal point
            while (peek("\\d")) {
                number.append(chars.get(0));
                chars.advance();
            }

            // Emit a DECIMAL token, accounting for negativity
            return chars.emit(Token.Type.DECIMAL);
        }

        // Emit an INTEGER token if no decimal point was found
        return chars.emit(isNegative ? Token.Type.INTEGER : Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        // Ensure the character starts with a single quote
        if (!match("'")) {
            throw new ParseException("Character literal must start with a single quote", chars.index);
        }

        // Check if the next character is a valid single character or an escape sequence
        if (peek("[^'\\\\]")) {  // Match any character except ' or \
            chars.advance();  // Consume the character
        } else if (peek("\\\\")) {  // If it's a backslash, handle the escape sequence
            lexEscape();  // lexEscape() handles the escape sequence and throws an error if invalid
        } else {
            throw new ParseException("Invalid character literal", chars.index);
        }

        // Ensure the character is terminated by a single quote
        if (!match("'")) {
            throw new ParseException("Character literal must end with a single quote", chars.index);
        }

        // Emit the character token
        return chars.emit(Token.Type.CHARACTER);
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        if (!match("\"")) {
            throw new ParseException("String literal must start with a double quote", chars.index);
        }

        while (peek("[^\"\\\\]") || peek("\\\\")) {  // Match regular characters or escape sequences
            if (peek("\\\\")) {
                lexEscape();  // Handle escape sequence
            } else {
                chars.advance();
            }
        }

        if (!match("\"")) {
            throw new ParseException("String literal must end with a double quote", chars.index);
        }

        return chars.emit(Token.Type.STRING);
        //throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        if (match("\\\\")) {  // Escape sequence starts with backslash
            if (!match("[bnrt'\"\\\\]")) {  // Valid escape characters
                throw new ParseException("Invalid escape sequence", chars.index);
            }
        } else {
            throw new ParseException("Expected escape sequence after backslash", chars.index);
        }
        //throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        if (match("[!@#$%^&*()+=\\-/]")) {
            return chars.emit(Token.Type.OPERATOR);
        } else {
            throw new ParseException("Invalid operator", chars.index);
        }
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
