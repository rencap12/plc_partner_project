package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testIfStatementErrors(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        Parser parser = new Parser(tokens);
        try {
            parser.parseStatement();
            Assertions.fail("Expected ParseException was not thrown");
        } catch (ParseException e) {
            System.out.println("Test: " + test);
            System.out.println("Expected index: " + expectedIndex);
            System.out.println("Actual index: " + e.getIndex());
            System.out.println("Error message: " + e.getMessage());

            Assertions.assertEquals(expectedMessage, e.getMessage(),
                    "Expected message: '" + expectedMessage + "' but got: '" + e.getMessage() + "'");
            Assertions.assertEquals(expectedIndex, e.getIndex(),
                    "Expected error at index " + expectedIndex + " but got error at index " + e.getIndex());
        }
    }

    private static Stream<Arguments> testIfStatementErrors() {
        return Stream.of(
                // Case 1: Missing DO after expr
                // IF expr
                //       ^ error here (after expr)
                Arguments.of("Missing DO",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)
                                // Missing DO token - error should be at index 7 (after "expr")
                        ),
                        "Expected 'DO'",
                        7  // 3 (start of expr) + 4 (length of "expr")
                ),

                // Case 2: Invalid DO (THEN instead)
                // IF expr THEN
                //       ^ error here (at THEN)
                Arguments.of("Invalid DO",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "THEN", 8)
                                // THEN instead of DO - error should be at index 8
                        ),
                        "Expected 'DO'",
                        8  // index where THEN starts
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testParenthesisErrors(String test, List<Token> tokens, String expectedMessage, int expectedIndex) {
        Parser parser = new Parser(tokens);
        try {
            parser.parseExpression();
            Assertions.fail("Expected ParseException was not thrown");
        } catch (ParseException e) {
            // Print the actual error details for debugging
            System.out.println("Test case: " + test);
            System.out.println("Actual error message: " + e.getMessage());
            System.out.println("Actual error index: " + e.getIndex());

            // Assert both message and index
            Assertions.assertEquals(expectedMessage, e.getMessage(),
                    "Expected message: '" + expectedMessage + "' but got: '" + e.getMessage() + "'");
            Assertions.assertEquals(expectedIndex, e.getIndex(),
                    "Expected error at index " + expectedIndex + " but got error at index " + e.getIndex());
        }
    }

    private static Stream<Arguments> testParenthesisErrors() {
        return Stream.of(
                // Case 1: Missing closing parenthesis completely
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        "Expected closing parentheses ')'",
                        5  // index should be where we detect the missing parenthesis
                ),

                // Case 2: Invalid character instead of closing parenthesis
                Arguments.of("Invalid Closing Character",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        ),
                        "Expected closing parentheses ')'",
                        5  // index should be at the invalid closing character
                ),

                // Case 3: Missing closing parenthesis with invalid operator
                Arguments.of("Missing Parenthesis With Invalid Operator",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "@", 5)
                        ),
                        "Expected closing parentheses ')'",
                        5  // index should be at the invalid operator
                )
        );
    }
    @ParameterizedTest
    @MethodSource
    void testInvalidExpression(String test, List<Token> tokens, Ast.Expression expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testInvalidExpression() {
        return Stream.of(
                Arguments.of("Invalid Expression",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "?", 0)
                        ),
                        null
                ),
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        null
                )
                ,
                Arguments.of("Invalid Closing Parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Zero Statements",
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of("Field",
                        Arrays.asList(
                                // LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("name", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Method",
                        Arrays.asList(
                                // DEF name() DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "DEF", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                )))
                        )
                ),
                Arguments.of("Field Method",
                        Arrays.asList(
                                // LET name = expr; \n DEF name() DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "DEF", 17),
                                new Token(Token.Type.IDENTIFIER, "name", 21),
                                new Token(Token.Type.OPERATOR, "(", 25),
                                new Token(Token.Type.OPERATOR, ")", 26),
                                new Token(Token.Type.IDENTIFIER, "DO", 28),
                                new Token(Token.Type.IDENTIFIER, "stmt", 31),
                                new Token(Token.Type.OPERATOR, ";", 35),
                                new Token(Token.Type.IDENTIFIER, "END", 37)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("name", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))),
                                Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))
                                )))
                        )
                ),
                Arguments.of("Method Field",
                        Arrays.asList(
                                // DEF name() DO stmt; END \nLET name = expr;
                                new Token(Token.Type.IDENTIFIER, "DEF", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20),
                                new Token(Token.Type.IDENTIFIER, "LET", 22),
                                new Token(Token.Type.IDENTIFIER, "name", 26),
                                new Token(Token.Type.OPERATOR, "=", 31),
                                new Token(Token.Type.IDENTIFIER, "expr", 33),
                                new Token(Token.Type.OPERATOR, ";", 37)
                        ),
                       null // met end but still had more after it, not allowed
                ),
                Arguments.of("Mixed Fields",
                        Arrays.asList(
                                // LET x = expr; LET y; LET CONST z = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr", 8),
                                new Token(Token.Type.OPERATOR, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "LET", 14),
                                new Token(Token.Type.IDENTIFIER, "y", 18),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                new Token(Token.Type.IDENTIFIER, "LET", 21),
                                new Token(Token.Type.IDENTIFIER, "CONST", 25),
                                new Token(Token.Type.IDENTIFIER, "z", 31),
                                new Token(Token.Type.OPERATOR, "=", 33),
                                new Token(Token.Type.IDENTIFIER, "expr", 35),
                                new Token(Token.Type.OPERATOR, ";", 39)
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("x", false, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Field("y", false, Optional.empty()),
                                        new Ast.Field("z", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Declaration", // LET x;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, ";", 5)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("x", false, Optional.empty())),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Initialization", // LET x = 1;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.INTEGER, "1", 8),
                                new Token(Token.Type.OPERATOR, ";", 9)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(new BigInteger("1"))))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Constant Declaration", // LET CONST x;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "CONST", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 10),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("x", true, Optional.empty())),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Constant Initialization", // LET CONST pi = 3.14;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "CONST", 4),
                                new Token(Token.Type.IDENTIFIER, "pi", 10),
                                new Token(Token.Type.OPERATOR, "=", 13),
                                new Token(Token.Type.DECIMAL, "3.14", 15),
                                new Token(Token.Type.OPERATOR, ";", 19)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("pi", true, Optional.of(new Ast.Expression.Literal(new BigDecimal("3.14"))))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Missing Expression", // LET name = ;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.OPERATOR, ";", 10)
                        ),
                        null // Expect parsing to fail due to missing expression
                ),
                Arguments.of("Missing Semicolon", // LET x
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4)
                        ),
                        null // Expect parsing to fail due to missing semicolon
                )

        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                // name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList()))
                ), Arguments.of("Variable Expression",
                        Arrays.asList(
                                // expr;
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 4)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                // f
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        null // throws err
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Definition",
                        Arrays.asList(
                                // LET name;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ";", 8)
                        ),
                        null // expected types now
                ),
                Arguments.of("Initialization",
                        Arrays.asList(
                                // LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                ),
                Arguments.of("Type Annotation",
                        Arrays.asList(
                                // LET name: Type = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 8),
                                new Token(Token.Type.IDENTIFIER, "Type", 9),
                                new Token(Token.Type.OPERATOR, "=", 13),
                                new Token(Token.Type.IDENTIFIER, "expr", 15),
                                new Token(Token.Type.OPERATOR, ";", 19)
                        ),
                        //  public Declaration(String name, Optional<String> typeName, Optional<Ast.Expression> value) {
                        new Ast.Statement.Declaration("name", Optional.of("Type"), Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                ),
                Arguments.of("Missing Expression", // LET name = ;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.OPERATOR, ";", 10)
                        ),
                        null // Expect parsing to fail due to missing expression
                ), Arguments.of("Missing Semicolon", // LET name
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4)
                        ),
                        null // Expect parsing to fail due to missing semicolon
                ),
                Arguments.of("No Initialization with Semicolon", // LET name;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ";", 8)
                        ),
                        null
                ),
                Arguments.of("Initialization without Semicolon", // LET name = expr
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11)
                        ),
                        null // Expect parsing to fail due to missing semicolon
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                // name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                ),
                Arguments.of("Missing Value",
                        Arrays.asList(
                                // name = ;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                       null // throws an error
                )
        );
    }


    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        Arrays.asList(
                                // IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Missing DO",
                        Arrays.asList(
                                // IF expr stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "stmt", 8),
                                new Token(Token.Type.OPERATOR, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "END", 14)
                        ),
                       null // throws an error
                ),
                Arguments.of("Missing DO",
                        Arrays.asList(
                                // IF expr
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)
                        ),
                        null // Expect parsing to fail due to missing DO
                ),
                Arguments.of("Invalid DO",
                        Arrays.asList(
                                // IF expr THEN
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "THEN", 8)
                        ),
                        null // Expect parsing to fail due to invalid DO keyword
                ),
                Arguments.of("Else",
                        Arrays.asList(
                                // IF expr DO stmt1; ELSE stmt2; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                                new Token(Token.Type.OPERATOR, ";", 28),
                                new Token(Token.Type.IDENTIFIER, "END", 30)
                        ),
                        new Ast.Statement.If(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testForStatement")
    void testForStatement(String test, List<Token> tokens, Ast.Statement.For expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("Basic For Loop",
                        Arrays.asList(
                                // FOR (id = expr1; expr2; id = expr3) stmt1; END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "id", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "id", 25),
                                new Token(Token.Type.OPERATOR, "=", 27),
                                new Token(Token.Type.IDENTIFIER, "expr3", 29),
                                new Token(Token.Type.OPERATOR, ")", 35),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 37),
                                new Token(Token.Type.OPERATOR, ";", 42),
                                new Token(Token.Type.IDENTIFIER, "END", 44)
                        ),
                        new Ast.Statement.For(
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "id"),
                                        new Ast.Expression.Access(Optional.empty(), "expr1")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "id"),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")
                                ),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))) // Body statements
                        )
                ),  Arguments.of("Condition Only",
                        Arrays.asList(
                                // FOR ( ; expr; ) stmt; END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ";", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.OPERATOR, ";", 10),
                                new Token(Token.Type.OPERATOR, ")", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ), null
                ),  Arguments.of("Initialization Condition",
                        Arrays.asList(
                                // FOR (x = expr1; expr2; ) END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.OPERATOR, ")", 25),
                                new Token(Token.Type.IDENTIFIER, "END", 27)
                        ), null
                ), Arguments.of("Condition Increment",
                        Arrays.asList(
                                // FOR (; expr1; x = expr2) END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ";", 5),
                                new Token(Token.Type.IDENTIFIER, "expr1", 6),
                                new Token(Token.Type.OPERATOR, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "x", 14),
                                new Token(Token.Type.OPERATOR, "=", 16),
                                new Token(Token.Type.IDENTIFIER, "expr2", 18),
                                new Token(Token.Type.OPERATOR, ")", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ), null
                ),
                Arguments.of("Missing END",
                        Arrays.asList(
                                // FOR (id = expr1; expr2; id = expr3) stmt1;
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "id", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "id", 25),
                                new Token(Token.Type.OPERATOR, "=", 27),
                                new Token(Token.Type.IDENTIFIER, "expr3", 29),
                                new Token(Token.Type.OPERATOR, ")", 35),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 37),
                                new Token(Token.Type.OPERATOR, ";", 42)
                        ),
                        null // Should throw error for missing 'END'
                ),
                Arguments.of("Missing Initialization",
                        Arrays.asList(
                                // FOR (x = ; expr1; y = expr3) END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.OPERATOR, ";", 9),
                                new Token(Token.Type.IDENTIFIER, "expr1", 11),
                                new Token(Token.Type.OPERATOR, ";", 17),
                                new Token(Token.Type.IDENTIFIER, "y", 19),
                                new Token(Token.Type.OPERATOR, "=", 21),
                                new Token(Token.Type.IDENTIFIER, "expr3", 23),
                                new Token(Token.Type.OPERATOR, ")", 29),
                                new Token(Token.Type.IDENTIFIER, "END", 31)
                        ), null
//
                ),
                Arguments.of("Missing Increment",
                        Arrays.asList(
                                // FOR (x = expr1; expr2; y = ) END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "y", 25),
                                new Token(Token.Type.OPERATOR, "=", 27),
                                new Token(Token.Type.OPERATOR, ";", 29),
                                new Token(Token.Type.OPERATOR, ")", 31),
                                new Token(Token.Type.IDENTIFIER, "END", 33)
                        ), null
//
                ),
                Arguments.of("Missing Syntax - Opening Parenthesis",
                        Arrays.asList(
                                // FOR x = expr1; expr2; y = expr3) stmt; END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr1", 8),
                                new Token(Token.Type.OPERATOR, ";", 14),
                                new Token(Token.Type.IDENTIFIER, "expr2", 16),
                                new Token(Token.Type.OPERATOR, ";", 22),
                                new Token(Token.Type.IDENTIFIER, "y", 24),
                                new Token(Token.Type.OPERATOR, "=", 26),
                                new Token(Token.Type.IDENTIFIER, "expr3", 28),
                                new Token(Token.Type.OPERATOR, ")", 34),
                                new Token(Token.Type.IDENTIFIER, "stmt", 36),
                                new Token(Token.Type.OPERATOR, ";", 41),
                                new Token(Token.Type.IDENTIFIER, "END", 43)
                        ),
                        null // Should throw error for missing '('
                ),
                Arguments.of("Missing Syntax - Closing Parenthesis",
                        Arrays.asList(
                                // FOR (x = expr1; expr2; y = expr3 stmt; END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "y", 25),
                                new Token(Token.Type.OPERATOR, "=", 27),
                                new Token(Token.Type.IDENTIFIER, "expr3", 29),
                                new Token(Token.Type.OPERATOR, "stmt", 35),
                                new Token(Token.Type.OPERATOR, ";", 39),
                                new Token(Token.Type.IDENTIFIER, "END", 41)
                        ),
                        null // Should throw error for missing ')'
                ),
                Arguments.of("Missing Syntax - END",
                        Arrays.asList(
                                // FOR (x = expr1; expr2; y = expr3) stmt;
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "x", 5),
                                new Token(Token.Type.OPERATOR, "=", 7),
                                new Token(Token.Type.IDENTIFIER, "expr1", 9),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "expr2", 17),
                                new Token(Token.Type.OPERATOR, ";", 23),
                                new Token(Token.Type.IDENTIFIER, "y", 25),
                                new Token(Token.Type.OPERATOR, "=", 27),
                                new Token(Token.Type.IDENTIFIER, "expr3", 29),
                                new Token(Token.Type.OPERATOR, ")", 35),
                                new Token(Token.Type.IDENTIFIER, "stmt", 37),
                                new Token(Token.Type.OPERATOR, ";", 42)
                        ),
                        null // Should throw error for missing 'END'
                )
        );
    }


    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Statement.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("While",
                        Arrays.asList(
                                // WHILE expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                        )
                ),    Arguments.of("Multiple Statements",
                        Arrays.asList(
                                // WHILE expr DO stmt1; stmt2; stmt3; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 14),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 21),
                                new Token(Token.Type.OPERATOR, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 28),
                                new Token(Token.Type.OPERATOR, ";", 33),
                                new Token(Token.Type.IDENTIFIER, "END", 35)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3"))
                                ) // Multiple body statements
                        )
                ),
                // Missing END: WHILE expr DO stmt;
                Arguments.of("Missing END",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18)
                        ),
                        null // throws error
                ),
                Arguments.of("Missing DO",
                        Arrays.asList(
                                // WHILE expr stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        null // Should throw error for missing 'DO'
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of("Return Statement",
                        Arrays.asList(
                                // RETURN expr;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                // Missing Value: RETURN;
                Arguments.of("Missing Value",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                       null // throws an error
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                // (expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                // (expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                // expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                // expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                // expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                // expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        // name
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("Field Access",
                        Arrays.asList(
                                // obj.field
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "field")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                // name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                // name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Method Call",
                        Arrays.asList(
                                // obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "method", Arrays.asList())
                )
        );
    }

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
                /**
                 *  LET first = 1;
                 *  DEF main() DO
                 *      WHILE first != 10 DO
                 *          print(first);
                 *          first = first + 1;
                 *      END
                 *  END
                 */
                // LET first = 1;
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "first", 4),
                new Token(Token.Type.OPERATOR, "=", 10),
                new Token(Token.Type.INTEGER, "1", 12),
                new Token(Token.Type.OPERATOR, ";", 13),
                //DEF main() DO
                new Token(Token.Type.IDENTIFIER, "DEF", 15),
                new Token(Token.Type.IDENTIFIER, "main", 19),
                new Token(Token.Type.OPERATOR, "(", 23),
                new Token(Token.Type.OPERATOR, ")", 24),
                new Token(Token.Type.IDENTIFIER, "DO", 26),
                //    WHILE first != 10 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 33),
                new Token(Token.Type.IDENTIFIER, "first", 39),
                new Token(Token.Type.OPERATOR, "!=", 45),
                new Token(Token.Type.INTEGER, "10", 48),
                new Token(Token.Type.IDENTIFIER, "DO", 51),
                //        print(first);
                new Token(Token.Type.IDENTIFIER, "print", 62),
                new Token(Token.Type.OPERATOR, "(", 67),
                new Token(Token.Type.IDENTIFIER, "first", 68),
                new Token(Token.Type.OPERATOR, ")", 73),
                new Token(Token.Type.OPERATOR, ";", 74),
                //        first = first + 1;
                new Token(Token.Type.IDENTIFIER, "first", 84),
                new Token(Token.Type.OPERATOR, "=", 90),
                new Token(Token.Type.IDENTIFIER, "first", 92),
                new Token(Token.Type.OPERATOR, "+", 98),
                new Token(Token.Type.INTEGER, "1", 100),
                new Token(Token.Type.OPERATOR, ";", 101),
                //    END
                new Token(Token.Type.IDENTIFIER, "END", 107),
                //END
                new Token(Token.Type.IDENTIFIER, "END", 111)
        );
        Ast.Source expected = new Ast.Source(
                Arrays.asList(new Ast.Field("first", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                        new Ast.Statement.While(
                                new Ast.Expression.Binary("!=",
                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                        new Ast.Expression.Access(Optional.empty(), "first"))
                                                )
                                        ),
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "first"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "first"),
                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                )
                                        )
                                )
                        )
                ))
        ));
        test(input, expected, Parser::parseSource);
    }

    @ParameterizedTest
    @MethodSource("provideLiteralTestCases")
    void testLiteral(String testName, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> provideLiteralTestCases() {
        return Stream.of(
                Arguments.of("Nil Literal", List.of(new Token(Token.Type.IDENTIFIER, "NIL", 0)), new Ast.Expression.Literal(null)),
                Arguments.of("Boolean Literal", List.of(new Token(Token.Type.IDENTIFIER, "TRUE", 0)), new Ast.Expression.Literal(true)),
//                Arguments.of("Integer Literal", List.of(new Token(Token.Type.INTEGER, "1", 0)), new Ast.Expression.Literal(1)),
//                Arguments.of("Decimal Literal", List.of(new Token(Token.Type.DECIMAL, "2.0", 0)), new Ast.Expression.Literal(2.0)),
                Arguments.of("Character Literal", List.of(new Token(Token.Type.CHARACTER, "'c'", 0)), new Ast.Expression.Literal('c')),
                Arguments.of("String Literal", List.of(new Token(Token.Type.STRING, "\"string\"", 0)), new Ast.Expression.Literal("string")),
                Arguments.of("Escape Character", List.of(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)), new Ast.Expression.Literal("Hello,\nWorld!"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideGroupTestCases")
    void testGroup(String testName, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> provideGroupTestCases() {
        return Stream.of(
                Arguments.of("Grouped Variable", List.of(new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.IDENTIFIER, "expr", 0), new Token(Token.Type.OPERATOR, ")", 0)), new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))),
                Arguments.of("Grouped Binary", List.of(new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, "+", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0), new Token(Token.Type.OPERATOR, ")", 0)), new Ast.Expression.Group(new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2")))),
                Arguments.of("Missing Closing Parenthesis", List.of(new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.IDENTIFIER, "expr", 0)), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideBinaryTestCases")
    void testBinary(String testName, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> provideBinaryTestCases() {
        return Stream.of(
                Arguments.of("Binary And", List.of(new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, "&&", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0)), new Ast.Expression.Binary("&&", new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2"))),
                Arguments.of("Binary Equality", List.of(new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, "==", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0)), new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2"))),
                Arguments.of("Binary Addition", List.of(new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, "+", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0)), new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2"))),
                Arguments.of("Binary Multiplication", List.of(new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, "*", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0)), new Ast.Expression.Binary("*", new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2"))),
                Arguments.of("Missing Operand", List.of(new Token(Token.Type.IDENTIFIER, "expr", 0), new Token(Token.Type.OPERATOR, "-", 0)), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideAccessTestCases")
    void testAccess(String testName, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> provideAccessTestCases() {
        return Stream.of(
                Arguments.of("Variable", List.of(new Token(Token.Type.IDENTIFIER, "name", 0)), new Ast.Expression.Access(Optional.empty(), "name")),
                Arguments.of("Invalid Name", List.of(new Token(Token.Type.IDENTIFIER, "obj", 0), new Token(Token.Type.OPERATOR, ".", 0), new Token(Token.Type.INTEGER, "5", 0)), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideFunctionTestCases")
    void testFunction(String testName, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> provideFunctionTestCases() {
        return Stream.of(
                Arguments.of("Zero Arguments", List.of(new Token(Token.Type.IDENTIFIER, "name", 0), new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.OPERATOR, ")", 0)), new Ast.Expression.Function(Optional.empty(), "name", List.of())),
                Arguments.of("Multiple Arguments", List.of(new Token(Token.Type.IDENTIFIER, "name", 0), new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.IDENTIFIER, "expr1", 0), new Token(Token.Type.OPERATOR, ",", 0), new Token(Token.Type.IDENTIFIER, "expr2", 0), new Token(Token.Type.OPERATOR, ",", 0), new Token(Token.Type.IDENTIFIER, "expr3", 0), new Token(Token.Type.OPERATOR, ")", 0)), new Ast.Expression.Function(Optional.empty(), "name", List.of(new Ast.Expression.Access(Optional.empty(), "expr1"), new Ast.Expression.Access(Optional.empty(), "expr2"), new Ast.Expression.Access(Optional.empty(), "expr3")))),
                Arguments.of("Method Call", List.of(new Token(Token.Type.IDENTIFIER, "obj", 0), new Token(Token.Type.OPERATOR, ".", 0), new Token(Token.Type.IDENTIFIER, "method", 0), new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.OPERATOR, ")", 0)), new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "obj")), "method", List.of())),
                Arguments.of("Trailing Comma", List.of(new Token(Token.Type.IDENTIFIER, "name", 0), new Token(Token.Type.OPERATOR, "(", 0), new Token(Token.Type.IDENTIFIER, "expr", 0), new Token(Token.Type.OPERATOR, ",", 0), new Token(Token.Type.OPERATOR, ")", 0)), null)
        );
    }


    @ParameterizedTest
    @MethodSource
    void testBaseline(String test, List<Token> tokens, Object expected) {
        if (expected instanceof Ast.Expression) {
            test(tokens, (Ast.Expression) expected, Parser::parseExpression);
        } else if (expected instanceof Ast.Statement) {
            test(tokens, (Ast.Statement) expected, Parser::parseStatement);
        }
    }

    private static Stream<Arguments> testBaseline() {
        return Stream.of(
                // Baseline 1: Integer Expression
                Arguments.of("Integer Expression",
                        List.of(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(1)),

                // Baseline 2: Variable Expression
                Arguments.of("Variable Expression",
                        List.of(new Token(Token.Type.IDENTIFIER, "expr", 0)),
                        new Ast.Expression.Access(Optional.empty(), "expr")),

                // Baseline 3: Function Call Expression
                Arguments.of("Function Call Expression",
                        List.of(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, ")",9)
                        ),
                        new Ast.Expression.Function(Optional.empty(), "name",
                                List.of(new Ast.Expression.Access(Optional.empty(), "expr")))),

                // Baseline 4: Addition Expression
                Arguments.of("Addition Expression",
                        List.of(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 5),
                                new Token(Token.Type.IDENTIFIER, "expr2", 6)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"))),

                // Baseline 5: Statement
                Arguments.of("Statement",
                        List.of(new Token(Token.Type.IDENTIFIER, "stmt", 0), new Token(Token.Type.OPERATOR, ";", 4)),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
        );
    }


    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
