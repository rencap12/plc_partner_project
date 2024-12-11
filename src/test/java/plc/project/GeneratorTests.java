package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.text.html.Option;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Fields & Methods",
                        // LET x: Integer;
                        // LET y: Decimal;
                        // LET z: String;
                        // DEF f(): Integer DO RETURN x; END
                        // DEF g(): Decimal DO RETURN y; END
                        // DEF h(): String DO RETURN z; END
                        // DEF main(): Integer DO END
                        new Ast.Source(
                                Arrays.asList(
                                        init(new Ast.Field("x", "Integer", false,  Optional.empty()), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, true, Environment.NIL))),
                                        init(new Ast.Field("y", "Decimal", false, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, true, Environment.NIL))),
                                        init(new Ast.Field("z", "String", false, Optional.empty()), ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, true, Environment.NIL)))

                                ),
                                Arrays.asList(
                                        init(new Ast.Method("f", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "x"), ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, true, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("f", "f", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))),
                                        init(new Ast.Method("g", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "y"), ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, true, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("g", "g", Arrays.asList(), Environment.Type.DECIMAL, args -> Environment.NIL))),
                                        init(new Ast.Method("h", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Access(Optional.empty(), "z"), ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, true, Environment.NIL))))
                                        )), ast -> ast.setFunction(new Environment.Function("h", "h", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL))),
//                                        init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList()), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                                        (init(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                              new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int x;",
                                "    double y;",
                                "    String z;",
                                "",
                                "    int f() {",
                                "        return x;",
                                "    }",
                                "",
                                "    double g() {",
                                "        return y;",
                                "    }",
                                "",
                                "    String h() {",
                                "        return z;",
                                "    }",
                                "",
                                "    int main() {",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF expr DO
                        //     stmt;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF expr DO
                        //     stmt1;
                        // ELSE
                        //     stmt2;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                ),  Arguments.of("If another",
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                        init(new Ast.Expression.Literal("cond is true."), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (cond) {",
                                "    System.out.println(\"cond is true.\");",
                                "}")
                ),
                Arguments.of("If Else",
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                        init(new Ast.Expression.Literal("cond is true."), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
                                        init(new Ast.Expression.Literal("cond is false."), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (cond) {",
                                "    System.out.println(\"cond is true.\");",
                                "} else {",
                                "    System.out.println(\"cond is false.\");",
                                "}")
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testForStatement(String test, Ast.Statement.For ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("For",
                            // for (num = 0; num < 5; num = num + 1)
                            //     print(num);
                            // END
                        new Ast.Statement.For(
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(0)),
                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                new Ast.Statement.Assignment(
                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                        init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                        init(new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                                ast -> ast.setType(Environment.Type.INTEGER))),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))))),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "for ( num = 0; num < 5; num = num + 1 ) {",
                                "    System.out.println(num);",
                                "}"
                        )
                ),
                Arguments.of("Missing Signature",
                        // for (; num < 5;)
                        //     print(num);
                        // END
                        new Ast.Statement.For(
                                null,
                                init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                null,
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))))),
                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Assignment(
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Binary("+",
                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                                init(new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                                        ast -> ast.setType(Environment.Type.INTEGER)
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "for ( ; num < 5; ) {",
                                "    System.out.println(num);",
                                "    num = num + 1;",
                                "}"
                        )
                )
        );
    }

    // for (; num < 5;)
    //     print(num);
    //     num = num + 1;
    // END

//                "for ( num = 0; num < 5; num = num + 1 ) {",
//        "    System.out.println(num);",
//                "END"


    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, Ast.Statement.While ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("One Statement",
                        // WHILE cond DO
                        //     function(1);
                        // END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond) {",
                                "    function(1);",
                                "}")
                ),
                Arguments.of("Multiple Statements",
                        // WHILE cond DO
                        //     function(1);
                        //     function(2);
                        //     function(3);
                        // END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                        init(new Ast.Expression.Literal(BigInteger.valueOf(2)), ast -> ast.setType(Environment.Type.INTEGER))
                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                        init(new Ast.Expression.Literal(BigInteger.valueOf(3)), ast -> ast.setType(Environment.Type.INTEGER))
                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond) {",
                                "    function(1);",
                                "    function(2);",
                                "    function(3);",
                                "}")
                ),
                Arguments.of("No Statements",
                        // WHILE cond DO END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond) {",
                                "}")
                ),
                Arguments.of("Nested While",
                        // WHILE cond1 DO
                        //     WHILE cond2 DO
                        //         function(1);
                        //     END
                        // END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Access(Optional.empty(), "cond1"),
                                        ast -> ast.setVariable(new Environment.Variable("cond1", "cond1", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(
                                        new Ast.Statement.While(
                                                init(new Ast.Expression.Access(Optional.empty(), "cond2"),
                                                        ast -> ast.setVariable(new Environment.Variable("cond2", "cond2", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                                        )
                                                )
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (cond1) {",
                                "    while (cond2) {",
                                "        function(1);",
                                "    }",
                                "}")
                ),
                Arguments.of("Comparison Condition",
                        // WHILE num < 10 DO
                        //     function(num);
                        // END
                        new Ast.Statement.While(
                                init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
                                                init(new Ast.Expression.Literal(BigInteger.TEN),
                                                        ast -> ast.setType(Environment.Type.INTEGER))),
                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList(
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL)))
                                                )), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
                                        )
                                )
                        ),
                        String.join(System.lineSeparator(),
                                "while (num < 10) {",
                                "    function(num);",
                                "}")
                )
        );
    }

//    private static Stream<Arguments> testWhileStatement() {
//        return Stream.of(
//                Arguments.of("Empty While",
//                        new Ast.Statement.While(
//                                init(new Ast.Expression.Access(Optional.empty(), "cond"),
//                                        ast -> ast.setVariable(new Environment.Variable("cond", "cond", Environment.Type.BOOLEAN, true, Environment.NIL))),
//                                Arrays.asList()
//                        ),
//                        String.join(System.lineSeparator(),
//                                "while (cond) {",
//                                "}")
//                ),
//                Arguments.of("Multiple Statements While",
//                        new Ast.Statement.While(
//                                init(new Ast.Expression.Binary("<",
//                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
//                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
//                                                init(new Ast.Expression.Literal(BigInteger.TEN),
//                                                        ast -> ast.setType(Environment.Type.INTEGER))),
//                                        ast -> ast.setType(Environment.Type.BOOLEAN)),
//                                Arrays.asList(
//                                        // First statement: print(num + "\n")
//                                        new Ast.Statement.Expression(
//                                                init(new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(
//                                                        init(new Ast.Expression.Binary("+",
//                                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
//                                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
//                                                                        init(new Ast.Expression.Literal("\\n"),
//                                                                                ast -> ast.setType(Environment.Type.STRING))),
//                                                                ast -> ast.setType(Environment.Type.STRING))
//                                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))
//                                        ),
//                                        // Second statement: num = num + 1
//                                        new Ast.Statement.Assignment(
//                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
//                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
//                                                init(new Ast.Expression.Binary("+",
//                                                                init(new Ast.Expression.Access(Optional.empty(), "num"),
//                                                                        ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, false, Environment.NIL))),
//                                                                init(new Ast.Expression.Literal(BigInteger.ONE),
//                                                                        ast -> ast.setType(Environment.Type.INTEGER))),
//                                                        ast -> ast.setType(Environment.Type.INTEGER))
//                                        )
//                                )
//                        ),
//                        String.join(System.lineSeparator(),
//                                "while (num < 10) {",
//                                "    System.out.println(num + \"\\n\");",
//                                "    num = num + 1;",
//                                "}")
//                )
//        );
//    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Ben\" + 10"
                ),
                Arguments.of("Comparison",
                        init(new Ast.Expression.Binary(">",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "1 > 10"
                ),
                Arguments.of("Addition",
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "1 + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignmentStatement(String test, Ast.Statement.Assignment ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Variable Assignment",
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "variable"),
                                        ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, false, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE),
                                        ast -> ast.setType(Environment.Type.INTEGER))
                        ),
                        "variable = 1;"
                ),
                Arguments.of("Field Assignment",
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(
                                                Optional.of(init(new Ast.Expression.Access(Optional.empty(), "object"),
                                                        ast -> ast.setVariable(new Environment.Variable("object", "object", Environment.Type.NIL, false, Environment.NIL)))),
                                                "field"),
                                        ast -> ast.setVariable(new Environment.Variable("field", "field", Environment.Type.INTEGER, false, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE),
                                        ast -> ast.setType(Environment.Type.INTEGER))
                        ),
                        "object.field = 1;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteralExpression(String test, Ast.Expression.Literal ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean",
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true"
                ),
                Arguments.of("Integer",
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                        "1"
                ),
                Arguments.of("Decimal",
                        init(new Ast.Expression.Literal(new BigDecimal("123.456")), ast -> ast.setType(Environment.Type.DECIMAL)),
                        "123.456"
                ),
                Arguments.of("String",
                        init(new Ast.Expression.Literal("Hello World"), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Hello World\""
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, Ast.Expression.Group ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Binary",
                        init(new Ast.Expression.Group(
                                init(new Ast.Expression.Binary("+",
                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "(1 + 10)"
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function(Optional.empty(),"print", Arrays.asList(
                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))),
                        "System.out.println(\"Hello, World!\")"
                ),
                Arguments.of("Zero Arguments",
                        init(new Ast.Expression.Function(Optional.empty(), "function", Arrays.asList()),
                                ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL))),
                        "function()"
                ),
                Arguments.of("String Slice",
                        init(new Ast.Expression.Function(
                                Optional.of(init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))),
                                "slice",
                                Arrays.asList(
                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(BigInteger.valueOf(5)), ast -> ast.setType(Environment.Type.INTEGER))
                                )), ast -> ast.setFunction(new Environment.Function("slice", "substring", Arrays.asList(Environment.Type.INTEGER, Environment.Type.INTEGER), Environment.Type.STRING, args -> Environment.NIL))),
                        "\"string\".substring(1, 5)"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testMethod(String test, Ast.Method ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Square Method",
                        // DEF square(num: Decimal): Decimal DO RETURN num * num; END
                        init(new Ast.Method(
                                "square",
                                Arrays.asList("num"), // Parameters
                                Arrays.asList("double"), // Parameter type names
                                Optional.of("double"), // Return type name
                                Arrays.asList( // Statements
                                        new Ast.Statement.Return(
                                                init(new Ast.Expression.Binary(
                                                        "*",
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, false, null))),
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"),
                                                                ast -> ast.setVariable(new Environment.Variable("num", "num", Environment.Type.DECIMAL, false, null)))
                                                ), ast -> ast.setType(Environment.Type.DECIMAL))
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function(
                                "square",
                                "square",
                                Arrays.asList(Environment.Type.DECIMAL),
                                Environment.Type.DECIMAL,
                                args -> Environment.NIL
                        ))),
                        "double square(double num) {" + System.lineSeparator() +
                                "    return num * num;" + System.lineSeparator() +
                                "}"
                ),
                Arguments.of("Multiple Statements in func",
                        // DEF func(x: Integer, y: Decimal, z: String) DO print(x); print(y); print(z); END
                        init(new Ast.Method(
                                "func",
                                Arrays.asList("x", "y", "z"), // Parameters
                                Arrays.asList("int", "double", "String"), // Parameter type names
                                Optional.empty(), // Return type name (void)
                                Arrays.asList( // Statements
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(
                                                        Optional.empty(),
                                                        "print",
                                                        Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "x"),
                                                                ast -> ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, false, null))))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> null)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(
                                                        Optional.empty(),
                                                        "print",
                                                        Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "y"),
                                                                ast -> ast.setVariable(new Environment.Variable("y", "y", Environment.Type.DECIMAL, false, null))))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> null)))
                                        ),
                                        new Ast.Statement.Expression(
                                                init(new Ast.Expression.Function(
                                                        Optional.empty(),
                                                        "print",
                                                        Arrays.asList(init(new Ast.Expression.Access(Optional.empty(), "z"),
                                                                ast -> ast.setVariable(new Environment.Variable("z", "z", Environment.Type.STRING, false, null))))
                                                ), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> null)))
                                        )
                                )
                        ), ast -> ast.setFunction(new Environment.Function(
                                "func",
                                "func",
                                Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING),
                                Environment.Type.NIL,
                                args -> null
                        ))),
                        "void func(int x, double y, String z) {" + System.lineSeparator() +
                                "    System.out.println(x);" + System.lineSeparator() +
                                "    System.out.println(y);" + System.lineSeparator() +
                                "    System.out.println(z);" + System.lineSeparator() +
                                "}"
                )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
        System.out.println(writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
