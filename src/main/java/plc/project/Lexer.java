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
                chars.length = 0; //Ensures that the whitespace or escape character are not included in the token
            }

            if(chars.has(0)){
                tokens.add(lexToken());
            }
        }
        for (Token token: tokens){
            System.out.println("token:" + token.toString());
        }

        return tokens;
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
        if (peek("!=") || peek("==")) {
            return lexOperator(); // Handle this in lexOperator()
        }  else if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (peek("[0-9]") || peek("[-\\+]", "[0-9]")) { // Include negative sign for numbers
            return lexNumber();
        } else if (peek("-")) { // Check if it's a hyphen for operator
            return lexOperator(); // Treat hyphen as an operator
        } else if (peek("'")) {
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } else if (peek("[|,:;!@#$%^&*()=<>+/-\\\\.]")) {
            return lexOperator(); // Handle single-character operators
        } else {
            throw new ParseException("Unknown token", chars.index);
        }
    }

    public Token lexIdentifier() {
        // Ensure the identifier starts with a letter or underscore
        if (!peek("[A-Za-z_]")) {
            throw new ParseException("Invalid identifier start", chars.index);
        }

        // Match valid identifier characters, including letters, digits, underscores
        while (match("[A-Za-z0-9_-]")) {
            // Keep matching valid identifier characters
        }

        return chars.emit(Token.Type.IDENTIFIER);
    }


    public Token lexNumber() throws ParseException {
        StringBuilder number = new StringBuilder();
        boolean hasDigits = false;

        // Handle the optional '+' or '-' sign
        if (peek("[-\\+]")) {
            if (chars.get(chars.index) == '-') {
                match("-");
                number.append('-'); // Append the negative sign
            } else if (peek("\\+")) {
                match("\\+");
            }
        }

        // Check for leading zeros
        if (peek("0")) {
            number.append('0');
            chars.advance();

            // No other digits allowed after a leading zero
            if (peek("\\d")) {
                throw new ParseException("Invalid leading zero in number", chars.index);
            }
        } else {
            // Capture digits for numbers without leading zero
            while (peek("\\d")) {
                hasDigits = true;  // At least one digit is present
                number.append(chars.get(0));
                chars.advance();
            }

            // If no digits were found, throw an error
            if (!hasDigits) {
                return lexOperator();
               // throw new ParseException("No digits found in number", chars.index);
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

            // Check if the number exceeds the integer precision of 9007199254740993
            if (number.length() > 16 ||
                    (number.length() == 16 && number.toString().compareTo("9007199254740993") > 0)) {
                throw new ParseException("Number exceeds maximum integer precision: 9007199254740993", chars.index);
            }

            // Check for -0.0 case
            if (number.toString().equals("-0.0")) {
                throw new ParseException("Invalid number: -0.0 is not allowed", chars.index);
            }

            // Emit a DECIMAL token
            return chars.emit(Token.Type.DECIMAL);
        }

        // Check if the number exceeds Integer.MAX_VALUE
        String numberStr = number.toString();
        if (numberStr.length() > 10 || // Integer.MAX_VALUE has 10 digits
                (numberStr.length() == 10 && numberStr.compareTo(String.valueOf(Integer.MAX_VALUE)) > 0)) {
            throw new ParseException("Number exceeds maximum integer value", chars.index);
        }

        // Emit an INTEGER token if no decimal point was found
        return chars.emit(Token.Type.INTEGER);
    }




    public Token lexCharacter() {
        // Ensure the character starts with a single quote
        if (!match("'")) {
            throw new ParseException("Character literal must start with a single quote", chars.index);
        }

        // Check if the next character is a valid single character or an escape sequence
        if (peek("^[^'\\\\]$") || peek("^\\s$")) {  // Match any character except ' or \
            chars.advance();  // Consume the character
        } else if (peek("\\\\")) {  // If it's a backslash, handle the escape sequence
            lexEscape();  // lexEscape() handles the escape sequence and throws an error if invalid
        } else {
            throw new ParseException("Invalid character literal", chars.index);
        }

        while(peek("[ \b\n\r\t]")){
            chars.advance(); // skip over white space
        }

        // Ensure the character is terminated by a single quote
        if (!match("'")) {
            throw new ParseException("Character literal must end with a single quote", chars.index);
        }

        // Emit the character token
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        if (!match("\"")) {
            throw new ParseException("String literal must start with a double quote", chars.index);
        }

        while (peek("[^\"\\\\]") || peek("\\\\")) {  // Match regular characters or escape sequences
            if (peek("\\\\")) {
                lexEscape();  // Handle escape sequence
            } else if (match("[\\n]")) {
               throw new ParseException("Unterminated newline in string!", chars.index);
            } else {
                chars.advance();
            }
        }

        if (!match("\"")) {
            throw new ParseException("String literal must end with a double quote", chars.index);
        }
        else {
            match("\"");
        }

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (match("\\\\")) {  // Escape sequence starts with backslash
            if (!match("[bnrt'\"\\\\]")) {  // Valid escape characters
                throw new ParseException("Invalid escape sequence", chars.index);
            }
        } else {
            throw new ParseException("Expected escape sequence after backslash", chars.index);
        }
    }


    public Token lexOperator() {
        // Check for multi-character operators first

        if (match("-")) {
            return chars.emit(Token.Type.OPERATOR); // Emit the hyphen as an operator
        }

        // Not equals (!=)
        if (peek("!")) {
            chars.advance(); // Consume '!'
            if (peek("=")) {
                chars.advance(); // Consume '='
                return chars.emit(Token.Type.OPERATOR); // '!='
            }
            return chars.emit(Token.Type.OPERATOR); // Just '!'
        }

        // Equals (==)
        if (peek("=")) {
            chars.advance(); // Consume '='
            if (peek("=")) {
                chars.advance(); // Consume '='
                return chars.emit(Token.Type.OPERATOR); // '=='
            }
            return chars.emit(Token.Type.OPERATOR); // Just '='
        }

        // Less than or equals (<=)
        if (peek("<")) {
            chars.advance(); // Consume '<'
            if (peek("=")) {
                chars.advance(); // Consume '='
                return chars.emit(Token.Type.OPERATOR); // '<='
            }
            return chars.emit(Token.Type.OPERATOR); // Just '<'
        }

        // Greater than or equals (>=)
        if (peek(">")) {
            chars.advance(); // Consume '>'
            if (peek("=")) {
                chars.advance(); // Consume '='
                return chars.emit(Token.Type.OPERATOR); // '>='
            }
            return chars.emit(Token.Type.OPERATOR); // Just '>'
        }

        // Logical AND (&&)
        if (peek("&")) {
            chars.advance(); // Consume '&'
            if (peek("&")) {
                chars.advance(); // Consume '&'
                return chars.emit(Token.Type.OPERATOR); // '&&'
            }
        }

        // Logical OR (||)
        if (peek("\\|")) {
            chars.advance(); // Consume '|'
            if (peek("\\|")) {
                chars.advance(); // Consume '|'
                return chars.emit(Token.Type.OPERATOR); // '||'
            }
        }

        // Now check for single-character operators, including Unicode
        // WORK ON UNICODE
        if (match("[.,:;!@#$%^&*()+=/<>\\-]")) {  // Added '<' and '>', and ensure unicode is properly checked
            return chars.emit(Token.Type.OPERATOR);
        }

        throw new ParseException("Invalid operator", chars.index);
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
          //  System.out.println("Updated length: " + length);  // Log the updated length

        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
//            Token token = new Token(type, input.substring(start, index), start);
//            System.out.println("this is was emitted: " + token.toString() + ", " + token.getType());
            return new Token(type, input.substring(start, index), start);
        }

    }

}
