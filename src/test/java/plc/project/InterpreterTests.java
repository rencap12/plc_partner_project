package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                Arguments.of("Fields & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Field("y", false, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")                                ))
                        )))
                ), Environment.NIL.getValue()),
                Arguments.of("Field Addition",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                        new Ast.Field("y", false, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Return(
                                                        new Ast.Expression.Binary("+",
                                                                new Ast.Expression.Access(Optional.empty(), "x"),
                                                                new Ast.Expression.Access(Optional.empty(), "y")
                                                        )
                                                )
                                        ))
                                )
                        ),
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testField(String test, Ast.Field ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
                Arguments.of("Declaration", new Ast.Field("name", false, Optional.empty()), Environment.NIL.getValue()),
                Arguments.of("Initialization", new Ast.Field("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethod(String test, Ast.Method ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Main",
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                Arguments.of("Arguments",
                        new Ast.Method("main", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                ),Arguments.of("Square with One Parameter",
                        new Ast.Method("square", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.valueOf(5))),
                        BigInteger.valueOf(25)
                ),
                Arguments.of("Square with Zero",
                        new Ast.Method("square", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.ZERO)),
                        BigInteger.ZERO
                )
        );
    }

    @Test
    void testExpressionStatement() { // try to add given test?
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Statement.Expression(
                    new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());


        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        new Ast.Statement.Declaration("name", Optional.empty()),
                        Environment.NIL.getValue()
                ),
                Arguments.of("Initialization",
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", false, Environment.create("variable"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"variable"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testFieldAssignmentStatement() {
        Scope scope = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")),"field"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, object.lookupVariable("field").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("True Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                Arguments.of("False Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }



    @Test
    void testForStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("sum", false, Environment.create(BigInteger.ZERO));
        scope.defineVariable("num", false, Environment.NIL);
        test(new Ast.Statement.For(
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "num"), new Ast.Expression.Literal(BigInteger.ZERO)),
                new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Literal(BigInteger.valueOf(5))),
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "num"),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "num"),
                                new Ast.Expression.Literal(BigInteger.ONE))),
                Arrays.asList(new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(),"sum"),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(),"sum"),
                                new Ast.Expression.Access(Optional.empty(),"num")
                        )
                ))
        ), Environment.NIL.getValue(), scope);

        // you can evaluate the state of each variable in scope one at a time, here is an example:
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
        Assertions.assertEquals(BigInteger.valueOf(5), scope.lookupVariable("num").getValue().getValue());

        // you can also build a list of the expected results, comparing all as a group
        // expected is what the test case expects to be produced by your solution
        ArrayList<BigInteger> expected = new ArrayList<BigInteger>(2);
        expected.add(BigInteger.TEN);
        expected.add(BigInteger.valueOf(5));

        // actual is the result actually produced by your solution
        ArrayList<Object> actual = new ArrayList<Object>(2);
        actual.add(scope.lookupVariable("sum").getValue().getValue());
        actual.add(scope.lookupVariable("num").getValue().getValue());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testWhileStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.create(BigInteger.ZERO));
        test(new Ast.Statement.While(
                new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Literal(BigInteger.TEN)
                ),
                Arrays.asList(new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(),"num"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        )
                ))
        ),Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil", new Ast.Expression.Literal(null), Environment.NIL.getValue()), //remember, special case
                Arguments.of("Boolean", new Ast.Expression.Literal(true), true),
                Arguments.of("Integer", new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE),
                Arguments.of("Decimal", new Ast.Expression.Literal(BigDecimal.ONE), BigDecimal.ONE),
                Arguments.of("Character", new Ast.Expression.Literal('c'), 'c'),
                Arguments.of("String", new Ast.Expression.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)), BigInteger.ONE),
                Arguments.of("Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                Arguments.of("Less Than",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                Arguments.of("Greater Than or Equal",
                        new Ast.Expression.Binary(">=",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Equal",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Concatenation",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        ),
                        "ab"
                ),
                Arguments.of("Addition",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),
                Arguments.of("Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(new BigDecimal("1.2")),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", false, Environment.create("variable"));
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                // FIX THESE TO MATCH Access (2):
//                Variable: variable, scope = {variable = 1}
//                Field: object.field, scope = {object = PlcObject{field = 1}}
//                Arguments.of("Variable",
//                        new Ast.Expression.Access(Optional.empty(), "variable"),
//                        BigInteger.ONE
//                ),
//                Arguments.of("Field",
//                        new Ast.Expression.Access(
//                                Optional.of(new Ast.Expression.Access(Optional.empty(), "object")),
//                                "field"
//                        ),
//                        BigInteger.ONE
//                ),
                // Original string-based tests maintained for compatibility
                Arguments.of("Variable String",
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        "variable"
                ),
                Arguments.of("Field String",
                        new Ast.Expression.Access(
                                Optional.of(new Ast.Expression.Access(Optional.empty(), "object")),
                                "field"
                        ),
                        "object.field"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        Scope object = new Scope(null);
        object.defineFunction("method", 1, args -> Environment.create("object.method"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                // from given
                Arguments.of("Function",
                        new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()),
                        "function"
                ),
                Arguments.of("Method",
                        new Ast.Expression.Function(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "method", Arrays.asList()),
                        "object.method"
                ),
                Arguments.of("Print",
                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expression.Literal("Hello, World!"))),
                        Environment.NIL.getValue()
                )
        );
    }

    @Test
    void testBooleanLiteral() {
        test(new Ast.Expression.Literal(true), true, new Scope(null));
    }

    @Test
    void testIntegerLiteral() {
        test(new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE, new Scope(null));
    }

    @Test
    void testStringLiteral() {
        test(new Ast.Expression.Literal("string"), "string", new Scope(null));
    }

    @Test
    void testBinaryAddition() {
        test(new Ast.Expression.Binary("+",
                new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.TEN)
        ), BigInteger.valueOf(11), new Scope(null));
    }

    @Test
    void testVariableAccess() {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.create(BigInteger.ONE));
        test(new Ast.Expression.Access(Optional.empty(), "num"), BigInteger.ONE, scope);
    }

    /**
     * Tests that visiting a function expression properly calls the function and
     * returns the result.
     *
     * When the {@code log(obj)} function is called, {@code obj} is appended to
     * the {@link StringBuilder} and then returned by the function. The last
     * assertion checks that the writer contains the correct value.
     */
    @Test
    void testFunctionCall() {
        Scope scope = new Scope(null);
        StringBuilder builder = new StringBuilder();
        scope.defineFunction("log", 1, args -> {
            builder.append(args.get(0).getValue());
            return args.get(0);
        });
        test(new Ast.Expression.Function(
                Optional.empty(),
                "log",
                Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)
                )), BigInteger.ONE, scope);
        Assertions.assertEquals("1", builder.toString());
    }



    /**
     * Tests that visiting an expression statement evaluates the expression and
     * returns {@code NIL}. This tests relies on function calls.
     *
     * See {@link #testFunctionCall()} for an explanation of {@code log(obj)}.
     */
    @Test
    void testExpressionStatementBaseline() {
        Scope scope = new Scope(null);
        StringBuilder builder = new StringBuilder();
        scope.defineFunction("log", 1, args -> {
            builder.append(args.get(0).getValue());
            return args.get(0);
        });
        test(new Ast.Statement.Expression(
                new Ast.Expression.Function(
                        Optional.empty(),
                        "log",
                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE))
                )
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals("1", builder.toString());
    }

    // errors
    @Test
    public void testWhileWithString() {
        // Create a parent scope for the interpreter
        Scope parentScope = new Scope(null); // Assuming the parent scope can be null for the root scope

        // Create an interpreter instance with the parent scope
        Interpreter interpreter = new Interpreter(parentScope);

        // Create a while statement with a string condition
        Ast.Statement.While whileStatement = new Ast.Statement.While(
                new Ast.Expression.Literal("false"), // String condition
                new ArrayList<>() // No statements in the body
        );

        // Execute the interpreter and expect a RuntimeException
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            interpreter.visit(whileStatement); // Call the visit method of the interpreter
        });

        // Assert that the exception message matches the expected output
        Assertions.assertEquals("Expected type java.lang.Boolean, received java.lang.String.", exception.getMessage());
    }


    @Test
    public void testIntegerDecimalSubtraction() {
        // Create the AST for the expression 1 - 1.0
        Ast.Expression left = new Ast.Expression.Literal(1);  // Integer literal
        Ast.Expression right = new Ast.Expression.Literal(1.0); // Decimal literal
        Ast.Expression subtraction = new Ast.Expression.Binary("-", left, right);

        // Call the test method with expected result
        test(subtraction, null, new Scope(null)); // Expecting the result to be 0.0
    }

//    @Test
//    public void testRedefinedFieldThrowsError() {
//        Scope scope = new Scope(null);
//        Ast.Field field1 = new Ast.Field("name", false, Optional.empty()); // LET name;
//        Ast.Field field2 = new Ast.Field("name", false, Optional.of(new Ast.Expression.Literal(1))); // LET name = 1;
//
////        // First, define the initial field
////        test(field1, null, scope); // Expect no result for field definition
//
//        // Attempt to redefine the field and check that it throws a RuntimeException
//        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            test(field2, null, scope); // This should trigger a redefinition error
//        });
//
//        Assertions.assertEquals("Field 'name' is already defined.", exception.getMessage());
//    }



    @Test
    void testBinaryOperationTypeMismatch() {
        Interpreter interpreter = new Interpreter(new Scope(null));
        Scope scope = interpreter.getScope();
        Ast.Expression.Binary expression = new Ast.Expression.Binary(
                "-",
                new Ast.Expression.Literal(java.math.BigInteger.ONE),
                new Ast.Expression.Literal(java.math.BigDecimal.valueOf(1.0))
        );

        // Type mismatch between BigInteger and BigDecimal should throw a RuntimeException
        Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(expression));
    }

    @Test
    void testVariableRedefinitionError() {
        Interpreter interpreter = new Interpreter(new Scope(null));
        Scope scope = interpreter.getScope();

        // Declare a variable "x" in the scope
        Ast.Statement.Declaration declaration1 = new Ast.Statement.Declaration("x", Optional.of(new Ast.Expression.Literal(java.math.BigInteger.ONE)));
        interpreter.visit(declaration1);

        // Attempt to declare "x" again should throw an error
        Ast.Statement.Declaration declaration2 = new Ast.Statement.Declaration("x", Optional.of(new Ast.Expression.Literal(java.math.BigInteger.TEN)));
        Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(declaration2));
    }

    @Test
    void testDivisionByZeroBigInteger() {
        Interpreter interpreter = new Interpreter(new Scope(null));

        // Divide BigInteger by zero
        Ast.Expression.Binary expression = new Ast.Expression.Binary(
                "/",
                new Ast.Expression.Literal(java.math.BigInteger.ONE),
                new Ast.Expression.Literal(java.math.BigInteger.ZERO)
        );

        // Expect RuntimeException for division by zero
        Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(expression));
    }

    @Test
    void testDivisionByZeroBigDecimal() {
        Interpreter interpreter = new Interpreter(new Scope(null));

        // Divide BigDecimal by zero
        Ast.Expression.Binary expression = new Ast.Expression.Binary(
                "/",
                new Ast.Expression.Literal(new java.math.BigDecimal("1.0")),
                new Ast.Expression.Literal(new java.math.BigDecimal("0.0"))
        );

        // Expect RuntimeException for division by zero
        Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(expression));
    }

    @Test
    void testUndefinedVariableAccess() {
        Interpreter interpreter = new Interpreter(new Scope(null));

        // Access an undefined variable "y"
        Ast.Expression.Access accessExpression = new Ast.Expression.Access(Optional.empty(), "y");

        // Expect RuntimeException due to undefined variable
        Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(accessExpression));
    }


    /**
     * Tests that visiting the source rule invokes the main/0 function and
     * returns the result.
     */
    @Test
    void testSourceInvokeMain() {
        Scope scope = new Scope(null);
        scope.defineFunction("main", 0, args -> Environment.create(BigInteger.ZERO));
        test(new Ast.Source(Arrays.asList(), Arrays.asList()), BigInteger.ZERO, scope);
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

}
