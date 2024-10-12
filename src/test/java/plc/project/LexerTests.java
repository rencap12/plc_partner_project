package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @Test
    void testTokensGenerated() {
        String input = "LET x = 5;";
        List<Token> tokens = new Lexer(input).lex();

        System.out.println("Generated Tokens:");
        for (Token token : tokens) {
            System.out.println(token.toString());
        }

        Assertions.assertEquals(Arrays.asList(
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "x", 4),
                new Token(Token.Type.OPERATOR, "=", 6),
                new Token(Token.Type.INTEGER, "5", 8),
                new Token(Token.Type.OPERATOR, ";", 9)
        ), tokens);
    }

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "abc", true),
                Arguments.of("Alphanumeric", "abc123", true),
                Arguments.of("Hyphens", "a-b-c", true), // Depending on the lexer rules, if hyphens are allowed, set to true
                Arguments.of("Only Underscores", "___", true),
                Arguments.of("Leading Underscore", "_abc", true),
                Arguments.of("Capitals", "ABC", true),
                Arguments.of("hyphen start", "-five", false),
                Arguments.of("digit start", "1fish2fish3fishbluefish", false),
                Arguments.of("Short Identifier", "a", true),
                Arguments.of("Long Identifier", "abcdefghijklmnopqrstuvwxyz012346789_-", true) // Set to false if the length exceeds limits or has illegal characters
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
               Arguments.of("Single Digit", "1", true),
               Arguments.of("Multiple Digits", "12345", true),
               Arguments.of("Negative", "-188", true),
               Arguments.of("Leading Zero", "01", false),
                Arguments.of("Positive Integer", "+100", true),
               Arguments.of("Comma Separated", "1,234", false),
               Arguments.of("Leading Zeros", "007", false),
               Arguments.of("Above Long Max", "123456789123456789123456789", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("single digit", "1.0", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("simple decimal", "0.9", true),
                Arguments.of("positive Decimal w/ sign", "+123.321", true),
                Arguments.of("Negative zero", "-0.0", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Single Digit Int", "1", false),
                Arguments.of("Trailing Zeros", "7.000", true),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("leading zero", "08.0", false),
                Arguments.of("above int precision", "9007199254740993.0", false),
                Arguments.of("Multiple Decimals", "1.2.3", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Unterminated", "'", false),
                Arguments.of("Newline", "\'\\n\'", true),
                Arguments.of("unterm char", "'c", false),
                Arguments.of("digit char", "\'1\'", true),
                Arguments.of("unicode", "\'p\'", true),
                Arguments.of("char with a space", "\'a\s\'", true),
                Arguments.of("space", "\'\s\'", true),
                Arguments.of("single quote escape", "\'\\\'\'", true),
                Arguments.of("backslash escape", "\'\\\\\'", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

//    Character Reference:
//    Line Feed: ␊, \n
//    Carriage Return: ␍, \r
//    Horizontal Tab: ␉, \t
//    Backspace: ␈, \b
//    Form Feed: ␌, \f
//    Vertical Tab: ␋, \u000B
//    Example: [ ␊␍␉], "[ \n\r\t]"
    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),  // Test for empty string
                Arguments.of("Single Character", "\"c\"", true),  // Single character string
                Arguments.of("Alphabetic", "\"abc\"", true),  // Alphabetic characters
                Arguments.of("Numeric", "\"123\"", true),  // Numeric string
                Arguments.of("Symbols", "\"!@#$%^&*\"", true),  // String with symbols
                Arguments.of("Unicode", "\"ρ★⚡\"", true),  // String with Unicode characters
                Arguments.of("Whitespace", "\" ␈␉\"", true),  // String with whitespace
                Arguments.of("Escape", "\"Hello, \\nWorld!\"", true),  // Newline escape
                Arguments.of("Alphabetic Escapes", "\"a\\bcdefghijklm\\nopq\\rs\\tuvwxyz\"", true),  // Multiple escapes
                Arguments.of("Special Escapes", "\"sq\\'dq\\\"bs\\\\\"", true),  // Escaping quotes and backslash
                Arguments.of("Invalid Escape", "\"abc\\0123\"", false),  // Invalid escape sequence
                Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\"", false),  // Unicode escape sequences
                Arguments.of("Unterminated", "\"unterminated", false),  // Missing ending quote
                Arguments.of("Unterminated Newline", "\"unterminated\n\"", false),  // Newline in unterminated string
                Arguments.of("Unterminated Empty Quote", "\"", false)  // Empty opening quote
        );
    }


    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Equal Equal", "==", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Symbol", "$", true),
                Arguments.of("Plus Sign", "+", true),
                Arguments.of("Remainder", "%", true),
                Arguments.of("Unicode", "ρ", true),
                Arguments.of("Greater Than", ">", true),
                Arguments.of("Not Equals", "!=", true),
                Arguments.of("Less Than Equals", "<=", true),
                Arguments.of("Greater Than Equals", ">=", true),
                Arguments.of("And", "&&", true),
                Arguments.of("Or", "||", true),
                Arguments.of("Plus", "+", true),
                Arguments.of("Hyphen", "-", true), // Ensure this is correct
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Form Feed", "\f", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, List<Token> expected) {
        // Whitespace should be skipped, so it won't appear in the expected tokens.
        test(input, expected, true);
    }

    private static Stream<Arguments> testWhitespace() {
        return Stream.of(
                Arguments.of("Multiple Spaces", "one   two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 6) // Skip the spaces
                )),
                Arguments.of("Trailing Newline", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0) // Skip the newline
                )),
                Arguments.of("Not Whitespace", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ))
        );
    }


    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 3", "double i = 2.0;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "double", 0),
                        new Token(Token.Type.IDENTIFIER, "i", 7),
                        new Token(Token.Type.OPERATOR, "=", 9),
                        new Token(Token.Type.DECIMAL, "2.0", 11),
                        new Token(Token.Type.OPERATOR, ";", 14)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Equals Combinations", "!====", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "!=", 0),
                        new Token(Token.Type.OPERATOR, "==", 2),
                        new Token(Token.Type.OPERATOR, "=", 4)
                )),
                Arguments.of("Weird Quotes", "'\"'string\"'\"", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "'\"'", 0),
                        new Token(Token.Type.IDENTIFIER, "string", 3),
                        new Token(Token.Type.STRING, "\"'\"", 9)
                )),
                // Simple Function Definition: FUN f(x, y, z) DO END
                Arguments.of("Simple Function Definition", "FUN f(x, y, z) DO END", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "FUN", 0),
                        new Token(Token.Type.IDENTIFIER, "f", 4),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.IDENTIFIER, "x", 6),
                        new Token(Token.Type.OPERATOR, ",", 7),
                        new Token(Token.Type.IDENTIFIER, "y", 9),
                        new Token(Token.Type.OPERATOR, ",", 10),
                        new Token(Token.Type.IDENTIFIER, "z", 12),
                        new Token(Token.Type.OPERATOR, ")", 13),
                        new Token(Token.Type.IDENTIFIER, "DO", 15),
                        new Token(Token.Type.IDENTIFIER, "END", 18)
                )),

                // Declaration: LET name;
                Arguments.of("Simple Declaration", "LET name;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "name", 4),
                        new Token(Token.Type.OPERATOR, ";", 8)
                )),

                // Initialization: LET name = expr;
                Arguments.of("Initialization", "LET name = expr;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "name", 4),
                        new Token(Token.Type.OPERATOR, "=", 9),
                        new Token(Token.Type.IDENTIFIER, "expr", 11),
                        new Token(Token.Type.OPERATOR, ";", 15)
                )),

                // Assignment: name = expr;
                Arguments.of("Assignment", "name = expr;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "=", 5),
                        new Token(Token.Type.IDENTIFIER, "expr", 7),
                        new Token(Token.Type.OPERATOR, ";", 11)
                )),

                // If Statement: IF expr DO stmt1; ELSE stmt2; END
                Arguments.of("If Statement", "IF expr DO stmt1; ELSE stmt2; END", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "IF", 0),
                        new Token(Token.Type.IDENTIFIER, "expr", 3),
                        new Token(Token.Type.IDENTIFIER, "DO", 8),
                        new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                        new Token(Token.Type.OPERATOR, ";", 16),
                        new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                        new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                        new Token(Token.Type.OPERATOR, ";", 28),
                        new Token(Token.Type.IDENTIFIER, "END", 30)
                )),

                // While Statement: WHILE expr DO stmt; END
                Arguments.of("While Loop", "WHILE expr DO stmt; END", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                        new Token(Token.Type.IDENTIFIER, "expr", 6),
                        new Token(Token.Type.IDENTIFIER, "DO", 11),
                        new Token(Token.Type.IDENTIFIER, "stmt", 14),
                        new Token(Token.Type.OPERATOR, ";", 18),
                        new Token(Token.Type.IDENTIFIER, "END", 20)
                )),

                // Binary Expression: x + 1 == y / 2.0 - 3
                Arguments.of("Binary Expression", "x + 1 == y / 2.0 - 3", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "x", 0),
                        new Token(Token.Type.OPERATOR, "+", 2),
                        new Token(Token.Type.INTEGER, "1", 4),
                        new Token(Token.Type.OPERATOR, "==", 6),
                        new Token(Token.Type.IDENTIFIER, "y", 9),
                        new Token(Token.Type.OPERATOR, "/", 11),
                        new Token(Token.Type.DECIMAL, "2.0", 13),
                        new Token(Token.Type.OPERATOR, "-", 17),
                        new Token(Token.Type.INTEGER, "3", 19)
                )),

                // Function Call: print("Hello", separator, "World!")
                Arguments.of("Function Call", "print(\"Hello\", separator, \"World!\")", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello\"", 6),
                        new Token(Token.Type.OPERATOR, ",", 13),
                        new Token(Token.Type.IDENTIFIER, "separator", 15),
                        new Token(Token.Type.OPERATOR, ",", 24),
                        new Token(Token.Type.STRING, "\"World!\"", 26),
                        new Token(Token.Type.OPERATOR, ")", 34)
                )),

                // Chained Method Calls: obj.field.method()
                Arguments.of("Method Call Chain", "obj.field.method()", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "obj", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.IDENTIFIER, "field", 4),
                        new Token(Token.Type.OPERATOR, ".", 9),
                        new Token(Token.Type.IDENTIFIER, "method", 10),
                        new Token(Token.Type.OPERATOR, "(", 16),
                        new Token(Token.Type.OPERATOR, ")", 17)
                ))
        );
    }



    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                System.out.println("Testing: " + input + ", Expected: " + expected );

                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                System.out.println("Testing: " + input + ", Expected: " + expected );

                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                System.out.println("Testing: " + input + ", Expected: " + expected );
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                System.out.println("Testing: " + input + ", Expected: " + expected );
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
