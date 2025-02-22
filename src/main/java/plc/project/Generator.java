package plc.project;

import java.util.List;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Generate the class declaration
        print("public class Main {");
        indent++;
        newline(0);

        // Generate the static main method
        newline(1);
        print("public static void main(String[] args) {");
        newline(2);
        print("System.exit(new Main().main());");
        newline(1);
        print("}");

        //newline(0);

        if (!ast.getFields().isEmpty()) {
            newline(0);
            for (int i = 0; i < ast.getFields().size(); i++) {
                newline(indent);
                print(ast.getFields().get(i));
            }
        }

        newline(0);

        // Visit each method
        for (int i = 0; i < ast.getMethods().size(); i++) {
            if (i == 0) {
                newline(1);
            }
            print(ast.getMethods().get(i));
            // Only add a newline if there are more methods after this one
            if (i < ast.getMethods().size() - 1) {
                newline(0);
                newline(1);
            }
        }

        // Close the class
        newline(0);
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
//        print(ast.getTypeName(), " ", ast.getName());
//        ast.getValue().ifPresent(value -> {
//            print(" = ");
//            visit(value);
//        });
//        print(";");
//        return null;
        // Print Java type name
        if (ast.getTypeName().equals("Integer")) {
            print("int");
        }
        else if (ast.getTypeName().equals("Decimal")) {
          //  System.out.println("HERE DECIMAL");
            print("double");
        }
        else if (ast.getTypeName().equals("Boolean")) {
            print("boolean");
        }
        else if (ast.getTypeName().equals("Character")) {
            print("char");
        }
        else if (ast.getTypeName().equals("String")) {
            print("String");
        }

        print(" ");
        print(ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
//        // Generate the method signature
//        newline(1);
//        print(ast.getReturnTypeName().orElse("void").equals("Integer") ? "int" : ast.getReturnTypeName().orElse("void"), " ", ast.getName(), "(");
//
//        // Parameters (if any)
//        for (int i = 0; i < ast.getParameters().size(); i++) {
//            if (i > 0) {
//                print(", ");
//            }
//            print(ast.getParameterTypeNames().get(i), " ", ast.getParameters().get(i));
//        }
//        print(") {");
//        newline(2);
//
//        for (int i = 0; i < ast.getStatements().size(); i++) {
//            visit(ast.getStatements().get(i));
//            // Only add a newline if there are more methods after this one
//            if (i < ast.getStatements().size() - 1) {
//                newline(2);
//            }
//        }
//
//        // Close the method
//        newline(1);
//        print("}");
//        return null;


        // Generate the method signature
       // newline(1);

        if (ast.getReturnTypeName().orElse("void") == "void") {
            print(ast.getReturnTypeName().orElse("void"));
        } else {
            print(ast.getFunction().getReturnType().getJvmName());
        }
        print(" ");
        print(ast.getName());
        print("(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
           // print(ast.getParameterTypeNames().get(i));
            String paramType = ast.getParameterTypeNames().get(i);
            if (paramType.equals("Decimal")) {
                print("double");
            } else if (paramType.equals("Integer")) {
                print("int");
            } else {
                print(paramType);
            }
            print(" ");
            print(ast.getParameters().get(i));
            if (i != ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        print(") {");

        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            indent--;
            newline(indent);
        }

        if (ast.getStatements().isEmpty() && ast.getName().equals("main") && ast.getReturnTypeName().isPresent() && ast.getReturnTypeName().get().equals("Integer")) {
            indent++;
            newline(indent);
            print("return 0;");
            indent--;
            newline(indent);
        }

        print("}");


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
//        // Determine the correct type for printing
//        String typeName = ast.getTypeName().orElseGet(() -> {
//            // If there's a value, use its type to infer the primitive type
//            return ast.getValue()
//                    .map(value -> {
//                        // Map the type to its primitive equivalent
//                        if (value.getType() == Environment.Type.INTEGER) {
//                            return "int";      // Map to "int" for integers
//                        } else if (value.getType() == Environment.Type.DECIMAL) {
//                            return "double";   // Map to "double" for decimals
//                        } else if (value.getType() == Environment.Type.BOOLEAN) {
//                            return "boolean";  // Map to "boolean" for booleans
//                        } else if (value.getType() == Environment.Type.STRING) {
//                            return "String";   // Map to "String" for strings
//                        }
//                        return "var"; // Default if no match
//                    })
//                    .orElse("var"); // Fallback to "var" if no value is present
//        });
//
//        // If the type is one of the wrapped types, convert to the primitive equivalent
//        if (typeName.equals("Integer")) {
//            typeName = "int";  // Convert "Integer" to "int"
//        } else if (typeName.equals("Double")) {
//            typeName = "double";  // Convert "Double" to "double"
//        } else if (typeName.equals("Boolean")) {
//            typeName = "boolean";  // Convert "Boolean" to "boolean"
//        }
//
//        // Print the type and variable name
//        print(typeName, " ", ast.getName());
//
//        // If there is a value assigned, print it
//        ast.getValue().ifPresent(value -> {
//            print(" = ");
//            visit(value);
//        });
//
//        // End with a semicolon
//        print(";");
//
//        return null;
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");

        return null;
    }





    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        // Start the "if" statement and visit the condition
        print("if (");
        visit(ast.getCondition());
        print(") {");

        // Move to the next line and increase indentation for the "then" block
        newline(++indent);
        List<Ast.Statement> thenStatements = ast.getThenStatements();
        for (int i = 0; i < thenStatements.size(); i++) {
            visit(thenStatements.get(i));
            if (i < thenStatements.size() - 1) { // Don't print semicolon for the last statement
                print(";");
                newline(indent);  // Ensure proper indentation after each statement
            }
        }

        // Decrease the indentation after the "then" block and print closing brace
        newline(--indent);
        print("}");

        // If there are "else" statements, handle the "else" block
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");

            // Move to the next line and increase indentation for the "else" block
            newline(++indent);
            List<Ast.Statement> elseStatements = ast.getElseStatements();
            for (int i = 0; i < elseStatements.size(); i++) {
                visit(elseStatements.get(i));
                if (i < elseStatements.size() - 1) { // Don't print semicolon for the last statement
                    print(";");
                    newline(indent);  // Ensure proper indentation after each statement
                }
            }

            // Decrease the indentation after the "else" block and print closing brace
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        boolean isnull = true;
        print("for ( ");

        // Handle the initialization
        if (ast.getInitialization() != null) {
            if (ast.getInitialization() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment assignment = (Ast.Statement.Assignment) ast.getInitialization();
                visit(assignment.getReceiver());
                print(" = ");
                visit(assignment.getValue());
            } else {
                visit(ast.getInitialization());
            }
        }

        print("; ");

        // Handle the condition
        if (ast.getCondition() != null) {
            visit(ast.getCondition());
        }

        // Only print the second semicolon if there is a condition or increment
        if (ast.getCondition() != null || ast.getIncrement() != null) {
            print("; ");
        }

        // Handle the increment
        if (ast.getIncrement() != null) {
            isnull = false;
            if (ast.getIncrement() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment assignment = (Ast.Statement.Assignment) ast.getIncrement();
                visit(assignment.getReceiver());
                print(" = ");
                visit(assignment.getValue());
            } else {
                visit(ast.getIncrement());
            }
        }
        if (isnull){
            print(")");
        }
        else{
            print(" )");
        }

        print(" {");

        // Handle the body of the loop
        newline(++indent);
        boolean first = true;
        for (Ast.Statement statement : ast.getStatements()) {
            if (!first) {
                newline(indent);
            }
            visit(statement);
            first = false;
        }
        newline(--indent);
        print("}");
        return null;
    }

//    @Override
//    public Void visit(Ast.Statement.While ast) {
//        print("while (");
//        visit(ast.getCondition());
//        print(") {");
//        newline(++indent);
//        for (Ast.Statement statement : ast.getStatements()) {
//            visit(statement);
//            newline(indent);
//        }
//        newline(--indent);
//        print("}");
//        return null;
//    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");

        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                visit(ast.getStatements().get(i));
            }
            indent--;
            newline(indent);
        } else {
            // Add newline and indentation for empty while loops
            newline(indent);
           // newline(indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType() == Environment.Type.CHARACTER) {
            print("'");
            print(ast.getLiteral());
            print("'");
        }
        else if (ast.getType() == Environment.Type.STRING) {
            print("\"");
            print(ast.getLiteral());
            print("\"");
        }
        else if (ast.getType() == Environment.Type.DECIMAL) {
            BigDecimal temp = BigDecimal.class.cast(ast.getLiteral());
            print(temp.doubleValue());
        }
        else if (ast.getType() == Environment.Type.INTEGER) {
            BigInteger temp = BigInteger.class.cast(ast.getLiteral());
            print(temp.intValue());
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
//        // If the left expression is a string, wrap it in quotes
//        if (ast.getLeft().getType().equals(Environment.Type.STRING)) {
//            print("\"");
//        }
//
//        // Visit and print the left expression
//        visit(ast.getLeft());
//
//        // If the left expression was a string, close the quote
//        if (ast.getLeft().getType().equals(Environment.Type.STRING)) {
//            print("\"");
//        }
//
//        // Print the operator (e.g., + or &&)
//        print(" ", ast.getOperator(), " ");
//
//        // If the right expression is a string, wrap it in quotes
//        if (ast.getRight().getType().equals(Environment.Type.STRING)) {
//            print("\"");
//        }
//
//        // Visit and print the right expression
//        visit(ast.getRight());
//
//        // If the right expression was a string, close the quote
//        if (ast.getRight().getType().equals(Environment.Type.STRING)) {
//            print("\"");
//        }

        // Visit and print the left expression
        visit(ast.getLeft());

        // Print the operator (e.g., + or &&)
        print(" ", ast.getOperator(), " ");

        // Visit and print the right expression
        visit(ast.getRight());

        return null;
    }



    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // Special case for print function
        if (ast.getName().equals("print")) {
            print("System.out.println(");

            // Handle the arguments
            for (int i = 0; i < ast.getArguments().size(); i++) {
                if (i > 0) print(", ");
                visit(ast.getArguments().get(i));  // Let the Literal visitor handle the quotes
            }

            print(")");
        } else {
            // Handle other functions including string slice
            if (ast.getReceiver().isPresent()) {
                visit(ast.getReceiver().get());
                print(".");
            }

            String functionName = ast.getFunction().getJvmName();
            print(functionName);
            print("(");

            for (int i = 0; i < ast.getArguments().size(); i++) {
                if (i > 0) print(", ");
                visit(ast.getArguments().get(i));
            }
            print(")");
        }

        return null;
    }

}
