package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tests have been provided for a few selective parts of the AST, and are not
 * exhaustive. You should add additional tests for the remaining parts and make
 * sure to handle all of the cases defined in the specification which have not
 * been tested here.
 */
public final class AnalyzerTests {

    private static final Environment.Type OBJECT_TYPE = new Environment.Type("ObjectType", "ObjectType", init(new Scope(null), scope -> {
        scope.defineVariable("field", "field", Environment.Type.INTEGER, false, Environment.NIL);
        scope.defineFunction("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL);
    }));

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSource(String test, Ast.Source ast, Ast.Source expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            expected.getFields().forEach(field -> Assertions.assertEquals(field.getVariable(), analyzer.scope.lookupVariable(field.getName())));
            expected.getMethods().forEach(method -> Assertions.assertEquals(method.getFunction(), analyzer.scope.lookupFunction(method.getName(), method.getParameters().size())));
        }
    }
    private static Stream<Arguments> testSource() {
        return Stream.of(

                // Field Use: LET num: Integer = 1; DEF main(): Integer DO print(num); END
                // Fixed test that matches your Analyzer's expectations
                // 1. Source test modifications - Fix Field Use and Method Use tests
                Arguments.of("Field Use",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("num", "Integer", false,
                                                Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                                        Arrays.asList(new Ast.Expression.Access(Optional.empty(), "num"))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        init(new Ast.Source(
                                Arrays.asList(
                                        init(new Ast.Field("num", "Integer", false,
                                                        Optional.of(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL)))
                                ),
                                Arrays.asList(
                                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                                        Arrays.asList(
                                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL)))
                                                                        )
                                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ), ast -> {})
                ),

                // LET value: Boolean = TRUE; DEF main(): Integer DO RETURN value; END
                Arguments.of("Invalid Return",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("value","Boolean", false, Optional.of(new Ast.Expression.Literal(true)))
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "value")))
                                        )
                                )
                        ),
                        null
                ),
                // DEF main() DO RETURN 0; END
                Arguments.of("Missing Integer Return Type for Main",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                            new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("0"))))
                                        )
                                )
                        ),
                        null
                ),
                // Valid Main: DEF main(): Integer DO END
                Arguments.of("Valid Main",
                        new Ast.Source(
                                Arrays.asList(), // empty fields
                                Arrays.asList(
                                        new Ast.Method("main",
                                                Arrays.asList(), // empty parameters
                                                Arrays.asList(), // empty parameter types
                                                Optional.of("Integer"),
                                                Arrays.asList() // empty statements
                                        )
                                )
                        ),
                        init(new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Method("main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Integer"),
                                                Arrays.asList()
                                        ), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ), ast -> {})
                ),

                // Method Use with String reverse
                Arguments.of("Method Use",
                        new Ast.Source(
                                Arrays.asList(), // No fields
                                Arrays.asList(
                                        // First method: reverse
                                        new Ast.Method("reverse",
                                                Arrays.asList("s"),  // Parameter names
                                                Arrays.asList("String"),  // Parameter types
                                                Optional.of("String"), // Return type
                                                Arrays.asList(
                                                        new Ast.Statement.If(
                                                                // Condition: s.length <= 1
                                                                new Ast.Expression.Binary("<=",
                                                                        new Ast.Expression.Access(
                                                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "s")),
                                                                                "length"
                                                                        ),
                                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                                ),
                                                                // Then statements
                                                                Arrays.asList(
                                                                        new Ast.Statement.Return(
                                                                                new Ast.Expression.Access(Optional.empty(), "s")
                                                                        )
                                                                ),
                                                                Arrays.asList() // No else statements
                                                        ),
                                                        // Return statement
                                                        new Ast.Statement.Return(
                                                                new Ast.Expression.Binary("+",
                                                                        new Ast.Expression.Function(
                                                                                Optional.empty(),
                                                                                "reverse",
                                                                                Arrays.asList(
                                                                                        new Ast.Expression.Function(
                                                                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "s")),
                                                                                                "slice",
                                                                                                Arrays.asList(
                                                                                                        new Ast.Expression.Literal(BigInteger.ONE),
                                                                                                        new Ast.Expression.Access(
                                                                                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "s")),
                                                                                                                "length"
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        ),
                                                                        new Ast.Expression.Function(
                                                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "s")),
                                                                                "slice",
                                                                                Arrays.asList(
                                                                                        new Ast.Expression.Literal(BigInteger.ZERO),
                                                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        ),
                                        // Second method: main
                                        new Ast.Method("main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Integer"),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                new Ast.Expression.Function(
                                                                        Optional.empty(),
                                                                        "print",
                                                                        Arrays.asList(
                                                                                new Ast.Expression.Function(
                                                                                        Optional.empty(),
                                                                                        "reverse",
                                                                                        Arrays.asList(new Ast.Expression.Literal("Hello World"))
                                                                                )
                                                                        )
                                                                )
                                                        ),
                                                        new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                                )
                                        )
                                )
                        ),
                        init(new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        // Initialize reverse method
                                        init(new Ast.Method("reverse",
                                                Arrays.asList("s"),
                                                Arrays.asList("String"),
                                                Optional.of("String"),
                                                Arrays.asList(
                                                        new Ast.Statement.If(
                                                                init(new Ast.Expression.Binary("<=",
                                                                        init(new Ast.Expression.Access(
                                                                                Optional.of(init(new Ast.Expression.Access(Optional.empty(), "s"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("s", "s", Environment.Type.STRING, false, Environment.NIL)))),
                                                                                "length"
                                                                        ), ast -> ast.setVariable(new Environment.Variable("length", "length", Environment.Type.INTEGER, false, Environment.NIL))),
                                                                        init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                                ast -> ast.setType(Environment.Type.INTEGER))
                                                                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                                                Arrays.asList(
                                                                        new Ast.Statement.Return(
                                                                                init(new Ast.Expression.Access(Optional.empty(), "s"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("s", "s", Environment.Type.STRING, false, Environment.NIL)))
                                                                        )
                                                                ),
                                                                Arrays.asList()
                                                        ),
                                                        new Ast.Statement.Return(
                                                                init(new Ast.Expression.Binary("+",
                                                                        init(new Ast.Expression.Function(
                                                                                Optional.empty(),
                                                                                "reverse",
                                                                                Arrays.asList(
                                                                                        init(new Ast.Expression.Function(
                                                                                                Optional.of(init(new Ast.Expression.Access(Optional.empty(), "s"),
                                                                                                        ast -> ast.setVariable(new Environment.Variable("s", "s", Environment.Type.STRING, false, Environment.NIL)))),
                                                                                                "slice",
                                                                                                Arrays.asList(
                                                                                                        init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                                                                ast -> ast.setType(Environment.Type.INTEGER)),
                                                                                                        init(new Ast.Expression.Access(
                                                                                                                Optional.of(init(new Ast.Expression.Access(Optional.empty(), "s"),
                                                                                                                        ast -> ast.setVariable(new Environment.Variable("s", "s", Environment.Type.STRING, false, Environment.NIL)))),
                                                                                                                "length"
                                                                                                        ), ast -> ast.setVariable(new Environment.Variable("length", "length", Environment.Type.INTEGER, false, Environment.NIL)))
                                                                                                )
                                                                                        ), ast -> ast.setFunction(new Environment.Function("slice", "slice", Arrays.asList(Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING, args -> Environment.NIL)))
                                                                                )
                                                                        ), ast -> ast.setFunction(new Environment.Function("reverse", "reverse", Arrays.asList(Environment.Type.STRING), Environment.Type.STRING, args -> Environment.NIL))),
                                                                        init(new Ast.Expression.Function(
                                                                                Optional.of(init(new Ast.Expression.Access(Optional.empty(), "s"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("s", "s", Environment.Type.STRING, false, Environment.NIL)))),
                                                                                "slice",
                                                                                Arrays.asList(
                                                                                        init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                                                                ast -> ast.setType(Environment.Type.INTEGER)),
                                                                                        init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                                                ast -> ast.setType(Environment.Type.INTEGER))
                                                                                )
                                                                        ), ast -> ast.setFunction(new Environment.Function("slice", "slice", Arrays.asList(Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING, args -> Environment.NIL)))
                                                                ), ast -> ast.setType(Environment.Type.STRING))
                                                        )
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("reverse", "reverse", Arrays.asList(Environment.Type.STRING), Environment.Type.STRING, args -> Environment.NIL))),
                                        // Initialize main method
                                        init(new Ast.Method("main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Integer"),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function(
                                                                        Optional.empty(),
                                                                        "print",
                                                                        Arrays.asList(
                                                                                init(new Ast.Expression.Function(
                                                                                        Optional.empty(),
                                                                                        "reverse",
                                                                                        Arrays.asList(init(new Ast.Expression.Literal("Hello World"),
                                                                                                ast -> ast.setType(Environment.Type.STRING)))
                                                                                ), ast -> ast.setFunction(new Environment.Function("reverse", "reverse", Arrays.asList(Environment.Type.STRING), Environment.Type.STRING, args -> Environment.NIL)))
                                                                        )
                                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                                        ),
                                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                )
                                        ), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ), ast -> {})
                ),

                // Invalid Main Return Type: DEF main() DO print("Hello, World!"); END
                Arguments.of("Invalid Main Return Type",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Method("main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.empty(), // Missing return type
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                new Ast.Expression.Function(
                                                                        Optional.empty(),
                                                                        "print",
                                                                        Arrays.asList(new Ast.Expression.Literal("Hello, World!"))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        null
                ),

                // Invalid Main Signature: DEF main(arg: Integer): Integer DO END
                Arguments.of("Invalid Main Signature",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Method("main",
                                                Arrays.asList("arg"),
                                                Arrays.asList("Integer"),
                                                Optional.of("Integer"),
                                                Arrays.asList()
                                        )
                                )
                        ),
                        null
                ),

                // Missing Main: empty source
                Arguments.of("Missing Main",
                        new Ast.Source(Arrays.asList(), Arrays.asList()),
                        null
                ),

                // FIELD TEST CASES
                // Add these cases to your testSource() Stream.of()
                Arguments.of("Declaration",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("name", "Decimal", false, Optional.empty())
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                        ))
                                )
                        ),
                        init(new Ast.Source(
                                Arrays.asList(
                                        init(new Ast.Field("name", "Decimal", false, Optional.empty()),
                                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, false, Environment.NIL)))
                                ),
                                Arrays.asList(
                                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                        ast -> ast.setType(Environment.Type.INTEGER)))
                                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                )
                        ), ast -> {})
                ),

                Arguments.of("Field Initialization",
                        new Ast.Field("name", "Integer", false,
                                Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Field("name", "Integer", false,
                                        Optional.of(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                ast -> ast.setType(Environment.Type.INTEGER)))),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL)))

                ),

                Arguments.of("Invalid Field Initialization",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("name", "Integer", false,
                                                Optional.of(new Ast.Expression.Literal("I love COP4020!")))
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                        ))
                                )
                        ),
                        null
                ),

                Arguments.of("Constant With Type",
                        new Ast.Field("name", "Decimal", true,
                                Optional.of(new Ast.Expression.Literal(new BigDecimal("1.0")))),
                        init(new Ast.Field("name", "Decimal", true,
                                        Optional.of(init(new Ast.Expression.Literal(new BigDecimal("1.0")),
                                                ast -> ast.setType(Environment.Type.DECIMAL)))),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL)))

                ),

                Arguments.of("Invalid Constant",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("name", "Integer", true,
                                                Optional.of(new Ast.Expression.Literal('n')))
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                        ))
                                )
                        ),
                        null
                ),

                Arguments.of("Unknown Type",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Field("name", "Unknown", false, Optional.empty())
                                ),
                                Arrays.asList(
                                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                        ))
                                )
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testMethod(String test, Ast.Method ast, Ast.Method expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getFunction(), analyzer.scope.lookupFunction(expected.getName(), expected.getParameters().size()));
        }
    }

    /**
     *
     Hello World: DEF main(): Integer DO print("Hello, World!"); END
     Return Type Mismatch: DEF increment(num: Integer): Decimal DO RETURN num + 1; END

     */
    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Hello World",
                        // DEF main(): Integer DO print("Hello, World!"); END
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                        new Ast.Expression.Literal("Hello, World!")
                                )))
                        )),
                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                        init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Return Type Mismatch",
                        // DEF increment(num: Integer): Decimal DO RETURN num + 1; END
                        new Ast.Method("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Decimal"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ))
                        )),
                        null
                ),
                Arguments.of("No Explicit Return Type",
                        new Ast.Method("func", Arrays.asList(), Arrays.asList(), Optional.empty(),
                                Arrays.asList()
                        ),
                        null
                ),

                Arguments.of("Valid Return",
                        new Ast.Method("func", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                        new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))
                                )
                        ),
                        init(new Ast.Method("func", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                )
                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),

                Arguments.of("Invalid Return",
                        new Ast.Method("func", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                        new Ast.Statement.Return(new Ast.Expression.Literal(new BigDecimal("0.0")))
                                )
                        ),
                        null
                ),

                // 3. Method test modifications - Fix parameter handling
                Arguments.of("Valid Parameter Use",
                        new Ast.Method("func", Arrays.asList("x"), Arrays.asList("Integer"), Optional.empty(),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(
                                                                new Ast.Expression.Binary("+",
                                                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                                                        new Ast.Expression.Literal(BigInteger.ONE)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        init(new Ast.Method("func", Arrays.asList("x"), Arrays.asList("Integer"), Optional.empty(),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(
                                                                init(new Ast.Expression.Binary("+",
                                                                        init(new Ast.Expression.Access(Optional.empty(), "x"),
                                                                                ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, false, Environment.NIL))),  // Changed to constant=false
                                                                        init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                                ast -> ast.setType(Environment.Type.INTEGER))
                                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                                        )
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function("func", "func", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL)))
                ),

                Arguments.of("Mutable Field Declaration",
                        new Ast.Field("name", "Decimal", false, Optional.empty()),
                        init(new Ast.Field("name", "Decimal", false, Optional.empty()),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, false, Environment.NIL))
                        )
                ),

                Arguments.of("Multi Parameter Use",
                        new Ast.Method("func",
                                Arrays.asList("x", "y", "z"),
                                Arrays.asList("Integer", "Integer", "Integer"),
                                Optional.empty(),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(
                                                                new Ast.Expression.Binary("+",
                                                                        new Ast.Expression.Binary("+",
                                                                                new Ast.Expression.Access(Optional.empty(), "x"),
                                                                                new Ast.Expression.Access(Optional.empty(), "y")
                                                                        ),
                                                                        new Ast.Expression.Access(Optional.empty(), "z")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        init(new Ast.Method("func",
                                Arrays.asList("x", "y", "z"),
                                Arrays.asList("Integer", "Integer", "Integer"),
                                Optional.empty(),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(
                                                                init(new Ast.Expression.Binary("+",
                                                                        init(new Ast.Expression.Binary("+",
                                                                                init(new Ast.Expression.Access(Optional.empty(), "x"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, false, Environment.NIL))),
                                                                                init(new Ast.Expression.Access(Optional.empty(), "y"),
                                                                                        ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.INTEGER, false, Environment.NIL)))
                                                                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                                                                        init(new Ast.Expression.Access(Optional.empty(), "z"),
                                                                                ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.INTEGER, false, Environment.NIL)))
                                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                                        )
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function("func", "func",
                                Arrays.asList(Environment.Type.INTEGER, Environment.Type.INTEGER, Environment.Type.INTEGER),
                                Environment.Type.NIL, args -> Environment.NIL)))
                ),

                Arguments.of("Invalid Parameter Use",
                        new Ast.Method("func", Arrays.asList("x"), Arrays.asList("Integer"), Optional.empty(),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(
                                                                new Ast.Expression.Binary("+",
                                                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                                                        new Ast.Expression.Literal(new BigDecimal("1.0"))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testExpressionStatement(String test, Ast.Statement.Expression ast, Ast.Statement.Expression expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function",
                        // print(1);
                        new Ast.Statement.Expression(
                                new Ast.Expression.Function(Optional.empty(), "print",
                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE))
                                )
                        ),
                        new Ast.Statement.Expression(
                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                        )
                ),
                Arguments.of("Literal",
                        // 1;
                        new Ast.Statement.Expression(new Ast.Expression.Literal(BigInteger.ONE)),
                        null  // Should fail as statements must be function calls
                ),
                Arguments.of("Binary",
                        // TRUE || FALSE;
                        new Ast.Statement.Expression(
                                new Ast.Expression.Binary("||",
                                        new Ast.Expression.Literal(true),
                                        new Ast.Expression.Literal(false))
                        ),
                        null  // Should fail as statements must be function calls
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Ast.Statement.Declaration expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }
    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL));
                        })
                ),
                Arguments.of("Decimal Declaration",
                        // LET name: Decimal;
                        new Ast.Statement.Declaration("name", Optional.of("Decimal"), Optional.empty()),
                        init(new Ast.Statement.Declaration("name", Optional.of("Decimal"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, false, Environment.NIL));
                        })
                ),
                Arguments.of("Initialization",
                        // LET name = 1;
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL)))
                ),
                Arguments.of("Missing Type",
                        // LET name;
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty()),
                        null
                ),
                Arguments.of("Unknown Type",
                        // LET name: Unknown;
                        new Ast.Statement.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                        null // expected runtime exception
                ),
                Arguments.of("Integer Declaration with Initialization",
                        // LET name: Integer = 1;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL)))
                ),
                // Add these cases to your existing testDeclarationStatement()
                Arguments.of("Recursive Reference",
                        // LET name: Integer = name;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"),
                                Optional.of(new Ast.Expression.Access(Optional.empty(), "name"))
                        ),
                        null  // Should fail as variable cannot reference itself in declaration
                ),
                Arguments.of("Invalid Type",
                        // LET name: Integer = 1.0;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"),
                                Optional.of(new Ast.Expression.Literal(new BigDecimal("1.0")))
                        ),
                        null
                ),
                Arguments.of("Shadow Declaration",
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                                ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, false, Environment.NIL))
                        )
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatement(String test, Ast.Statement.Assignment ast, Ast.Statement.Assignment expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                ),
                Arguments.of("Invalid Type",
                        // variable = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal("string")
                        ),
                        null
                ),
                Arguments.of("Field",
                        // object.field = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "field"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.of(
                                        init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                                ), "field"), ast -> ast.setVariable(new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                ),
                // Add these to your existing testAssignmentStatement()
                Arguments.of("Redefining Constant Field",
                        // zero = 1; where zero is constant
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "zero"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        null  // Should fail as cannot assign to constant
                ),
                Arguments.of("Object Field Assignment",
                        // object.integer = 7;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")), "integer"),
                                new Ast.Expression.Literal(BigInteger.valueOf(7))
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.of(
                                        init(new Ast.Expression.Access(Optional.empty(), "object"),
                                                ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                                ), "integer"), ast -> ast.setVariable(new Environment.Variable("integer", "integer", Environment.Type.INTEGER, false, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.valueOf(7)),
                                        ast -> ast.setType(Environment.Type.INTEGER))
                        )
                )

        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testForStatement(String test, Ast.Statement.For ast, Ast.Statement.For expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("num", "num", Environment.Type.INTEGER, false, Environment.NIL);
            scope.defineVariable("sum", "sum", Environment.Type.INTEGER, false, Environment.NIL);
        }));
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("Valid For Loop",
                        // FOR (num = 0; num < 5; num = num + 1) sum = sum + num; END
                        new Ast.Statement.For(
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ZERO)
                                ),
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                                ),
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Binary("+",
                                                new Ast.Expression.Access(Optional.empty(), "num"),
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        )
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ),
                        // Expected AST after analysis
                        new Ast.Statement.For(
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                ast -> ast.setType(Environment.Type.INTEGER))
                                ),
                                init(new Ast.Expression.Binary("<",
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Binary("+",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.ONE),
                                                        ast -> ast.setType(Environment.Type.INTEGER))
                                        ), ast -> ast.setType(Environment.Type.INTEGER))
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                init(new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        ast -> ast.setVariable(new Environment.Variable("sum", "sum", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "sum"),
                                                                ast -> ast.setVariable(new Environment.Variable("sum", "sum", Environment.Type.INTEGER, false, Environment.NIL))),
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL)))
                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                        )
                                )
                        )
                ),
                // You can add more test cases here for invalid conditions, empty statements, etc.
                Arguments.of("Basic For Loop",
                        new Ast.Statement.For(
                                // initialization: num = 0
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ZERO)
                                ),
                                // condition: num < 5
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                                ),
                                // increment: num = num + 1
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Binary("+",
                                                new Ast.Expression.Access(Optional.empty(), "num"),
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        )
                                ),
                                // statement: sum = sum + num
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ),
                        // Expected AST after analysis
                        init(new Ast.Statement.For(
                                // initialization with types set
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.ZERO),
                                                ast -> ast.setType(Environment.Type.INTEGER))
                                ),
                                // condition with types set
                                init(new Ast.Expression.Binary("<",
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                // increment with types set
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Binary("+",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.ONE),
                                                        ast -> ast.setType(Environment.Type.INTEGER))
                                        ), ast -> ast.setType(Environment.Type.INTEGER))
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                init(new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        ast -> ast.setVariable(new Environment.Variable("sum", "sum", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "sum"),
                                                                ast -> ast.setVariable(new Environment.Variable("sum", "sum", Environment.Type.INTEGER, false, Environment.NIL))),
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL)))
                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                        )
                                )
                        ), ast -> {})
                ),

                Arguments.of("Invalid Condition",
                        new Ast.Statement.For(
                                null,
                                new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                                ),
                                null,
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ),
                        null  // Should fail as condition must be boolean
                ),

                Arguments.of("Condition Only",
                        new Ast.Statement.For(
                                null,
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                                ),
                                null,
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ),
                        // Include expected AST with proper type initialization
                        null  // Set to proper AST if this should be valid
                ),

                Arguments.of("No Initialization",
                        new Ast.Statement.For(
                                null,
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5))
                                ),
                                new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Binary("+",
                                                new Ast.Expression.Access(Optional.empty(), "num"),
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        )
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Assignment(
                                                new Ast.Expression.Access(Optional.empty(), "sum"),
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "sum"),
                                                        new Ast.Expression.Access(Optional.empty(), "num")
                                                )
                                        )
                                )
                        ),
                        null  // Set to proper AST if this should be valid
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, Ast.Statement.While ast, Ast.Statement.While expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        new Ast.Statement.While(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))
                                        )
                                )
                        ),
                        init(new Ast.Statement.While(
                                init(new Ast.Expression.Literal(true),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ), ast -> {})
                ),

                Arguments.of("Invalid Condition",
                        new Ast.Statement.While(
                                new Ast.Expression.Literal(BigInteger.ZERO),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))
                                        )
                                )
                        ),
                        null
                ),

                Arguments.of("Empty Statements",
                        new Ast.Statement.While(
                                new Ast.Expression.Literal(true),
                                Arrays.asList()
                        ),
                        null
                ),

                Arguments.of("Invalid Statement",
                        new Ast.Statement.While(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE))))
                                        )
                                )
                        ),
                        null
                ),

                Arguments.of("Multiple Statements",
                        new Ast.Statement.While(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))
                                        ),
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(2))))
                                        ),
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(3))))
                                        )
                                )
                        ),
                        init(new Ast.Statement.While(
                                init(new Ast.Expression.Literal(true),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.valueOf(2)),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.valueOf(3)),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ), ast -> {})
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatement(String test, Ast.Statement.If ast, Ast.Statement.If expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        // IF TRUE DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        new Ast.Statement.If(
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.Expression(
                                        init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Invalid Condition",
                        // IF "FALSE" DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal("FALSE"),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                            new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Invalid Statement",
                        // IF TRUE DO print(9223372036854775807); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE))
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Empty Statements",
                        // IF TRUE DO END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Else Statement",
                        // IF TRUE DO print(1); ELSE print(2); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))
                                        )
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(2))))
                                        )
                                )
                        ),
                        // Expected AST after analysis
                        init(new Ast.Statement.If(
                                init(new Ast.Expression.Literal(Boolean.TRUE),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                ),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "print",
                                                        Arrays.asList(init(new Ast.Expression.Literal(BigInteger.valueOf(2)),
                                                                ast -> ast.setType(Environment.Type.INTEGER)))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ), ast -> {})
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpression(String test, Ast.Expression.Literal ast, Ast.Expression.Literal expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean",
                        // TRUE
                        new Ast.Expression.Literal(true),
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Integer Valid",
                        // 2147483647
                        new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Invalid",
                        // 9223372036854775807
                        new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpression(String test, Ast.Expression.Binary ast, Ast.Expression.Binary expected) {
        test(ast, expected, new Scope(null));
    }
    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Logical AND Valid",
                        // TRUE AND FALSE
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal(Boolean.FALSE)
                        ),
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(Boolean.FALSE), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Logical AND Invalid",
                        // TRUE AND "FALSE"
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal("FALSE")
                        ),
                        null
                ),
                Arguments.of("String Concatenation",
                        // "Ben" + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("Ben"),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("Integer Addition",
                        // 1 + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Decimal Addition",
                        // 1 + 1.0
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigDecimal.ONE)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAccessExpression(String test, Ast.Expression.Access ast, Ast.Expression.Access expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL)))
                ),
                Arguments.of("Field",
                        // object.field
                        new Ast.Expression.Access(Optional.of(
                                new Ast.Expression.Access(Optional.empty(), "object")
                        ), "field"),
                        init(new Ast.Expression.Access(Optional.of(
                                init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                        ), "field"), ast -> ast.setVariable(new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL)))
                ),
                Arguments.of("Invalid Variable Access",
                        // undefinedVar
                        new Ast.Expression.Access(Optional.empty(), "undefinedVar"),
                        null
                ),
                Arguments.of("Invalid Field Access",
                        // plcObject.undefinedField
                        new Ast.Expression.Access(Optional.of(
                                new Ast.Expression.Access(Optional.empty(), "plcObject")
                        ), "undefinedField"),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpression(String test, Ast.Expression.Function ast, Ast.Expression.Function expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineFunction("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL);
            scope.defineVariable("object", "object", OBJECT_TYPE, false, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        // function()
                        new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()),
                        init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Method",
                        // object.method()
                        new Ast.Expression.Function(Optional.of(
                                new Ast.Expression.Access(Optional.empty(), "object")
                        ), "method", Arrays.asList()),
                        init(new Ast.Expression.Function(Optional.of(
                                init(new Ast.Expression.Access(Optional.empty(), "object"), ast -> ast.setVariable(new Environment.Variable("object", "object", OBJECT_TYPE, false, Environment.NIL)))
                        ), "method", Arrays.asList()), ast -> ast.setFunction(new Environment.Function("method", "method", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL)))
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Analyzer.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> Analyzer.requireAssignable(target, type));
        }
    }
    private static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
                Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
                Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
                Arguments.of("Integer to Comparable", Environment.Type.COMPARABLE, Environment.Type.INTEGER,  true),
                Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
                Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false)
        );
    }

    /**
     * Helper function for tests. If {@param expected} is {@code null}, analysis
     * is expected to throw a {@link RuntimeException}.
     */
    private static <T extends Ast> Analyzer test(T ast, T expected, Scope scope) {
        Analyzer analyzer = new Analyzer(scope);
        if (expected != null) {
            analyzer.visit(ast);
            Assertions.assertEquals(expected, ast);
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(ast));
        }
        return analyzer;
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
