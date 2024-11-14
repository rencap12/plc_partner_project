package plc.project;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import plc.project.Environment.PlcObject;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;
    private Environment.Type funcRet;   // Used to store the return type of functions undergoing analysis

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    private void checkTypesMatch(Ast.Expression.Binary ast) {   // Used in visit(Ast.Expression.Binary) -- checks if rhs and lhs types match and sets the ast type accordingly
        if (ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
            ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
            ast.setType(Environment.Type.DECIMAL);
        }
        else {
            throw new RuntimeException("Invalid binary expression.");
        }
    }



    @Override
    public Void visit(Ast.Source ast) {
        boolean mainPresent = false;
        try {
            for (Ast.Field field : ast.getFields()) {
                visit(field);
            }

            for (Ast.Method method : ast.getMethods()) {
                visit(method);
                if (method.getName().equals("main") && method.getReturnTypeName().get().equals("Integer") && method.getParameters().isEmpty()) {
                    mainPresent = true;
                }
            }

            if (!mainPresent) {
                throw new RuntimeException("No main method with proper arguments!");
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in Source AST", e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
                scope.defineVariable(ast.getName(), ast.getName(),  ast.getVariable().getType(), ast.getConstant() ,Environment.NIL);
            } else {
                scope.defineVariable(ast.getName(), ast.getConstant(), Environment.NIL);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in Field AST", e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // Collect the parameter types in a list:
        List<Environment.Type> paramTypes = new ArrayList<>();
        for (String i : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(i));
        }
        // Define the function in the current scope:
        scope.defineFunction(ast.getName(), ast.getName(), paramTypes, Environment.getType(ast.getReturnTypeName().orElse("Nil")), args -> Environment.NIL);
        // Set the function in the AST:
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        // Define a new scope, store the function's return type in a variable (to be used by visit(Ast.Statement.Return)), then visit each statement in the function:
        scope = new Scope(scope);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), Environment.getType(ast.getParameterTypeNames().get(i)), true, Environment.NIL);
        }
        funcRet = Environment.getType(ast.getReturnTypeName().orElse("Nil"));
        ast.getStatements().forEach(this::visit);
        // Restore funcRet to null and scope to parent:
        funcRet = null;
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        // Check for the exception condition:
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expected a function expression.");
        }
        // Visit the function expression:
        visit(ast.getExpression());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Determine the type of the variable being declared and check exception conditions:
        Environment.Type type;
        if (!(ast.getTypeName().isPresent())) {
            if (!(ast.getValue().isPresent())) {
                throw new RuntimeException("Type of declared variable could not be discerned.");
            }
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        }
        else {
            type = Environment.getType(ast.getTypeName().get());
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(type, ast.getValue().get().getType());
            }
        }
        // Define the variable in the current scope:
        scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        // Set the variable in the AST:
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Check the exception condition:
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Invalid assignment operation.");
        }
        // Visit both sides of the assignment expression:
        visit(ast.getReceiver());
        visit(ast.getValue());
        // Ensure the right side is assignable to the left side:
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;

    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        // Visit the condition:
        visit(ast.getCondition());
        // Check both exception conditions:
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Invalid condition in IF statement.");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("THEN block cannot have an empty body");
        }
        // Evaluate the else statements inside a new scope:
        scope = new Scope(scope);
        ast.getElseStatements().forEach(this::visit);
        scope = scope.getParent();
        // Evaluate the then statements inside a new scope:
        scope = new Scope(scope);
        ast.getThenStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {

        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        // Visit the condition:
        visit(ast.getCondition());
        // Check the exception condition:
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Invalid condition in WHILE statement.");
        }
        // Visit each of the block's statements in a new scope:
        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {

        // Visit the return value:
        visit(ast.getValue());
        // Check that the given value is assignable to the function's return type (stored in funcRet):
        requireAssignable(funcRet, ast.getValue().getType());
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        // Check for integer exception condition (byte array has length greater than 4 bytes (32 bits))
        if (ast.getLiteral() instanceof BigInteger) {
            if (((BigInteger) ast.getLiteral()).toByteArray().length > 4) {
                throw new RuntimeException("Integer value will overflow.");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        // Check for decimal exception condition (overflow occurs when casting from BigDecimal to double)
        else if (ast.getLiteral() instanceof BigDecimal) {
            double doubleVal = ((BigDecimal) ast.getLiteral()).doubleValue();
            if (doubleVal == Double.POSITIVE_INFINITY || doubleVal == Double.NEGATIVE_INFINITY) {
                throw new RuntimeException("Decimal value will overflow.");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else {
            ast.setType(Environment.Type.NIL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        // Check the exception condition:
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("The grouped expression is not binary.");
        }
        // Visit the contained expression:
        visit(ast.getExpression());
        // Set the type of the AST based on the type of the contained expression:
        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        switch (ast.getOperator()) {
            case "&&":
            case"||": {
                // Visit the left and right sides of the expression:
                visit(ast.getLeft());
                visit(ast.getRight());
                // Assign a type to the AST:
                if (ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN)) {
                    ast.setType(Environment.Type.BOOLEAN);
                }
                else {
                    throw new RuntimeException("Expected boolean values on both sides of the binary expression.");
                }
                break;
            }
            case "<":
            case ">":
            case "==":
            case "!=": {
                // Visit the left and right sides of the expression:
                visit(ast.getLeft());
                visit(ast.getRight());
                // Check that both sides of the expression are subtypes of COMPARABLE:
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                // Check that both sides of the expression are of the same type. If so, assign a type to the AST:
                if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                    ast.setType(Environment.Type.BOOLEAN);
                }
                else {
                    throw new RuntimeException("Left and right sides of equality statement must match.");
                }
                break;
            }
            case "+": {
                // Visit the left and right sides of the expression:
                visit(ast.getLeft());
                visit(ast.getRight());
                // Assign a type to the AST:
                if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                }
                else checkTypesMatch(ast);
                break;
            }
            case "-":
            case "*":
            case "/": {
                // Visit the left and right sides of the expression:
                visit(ast.getLeft());
                visit(ast.getRight());
                // Assign a type to the AST:
                checkTypesMatch(ast);
                break;
            }
            case "^": {
                // Visit the left and right sides of the expression:
                visit(ast.getLeft());
                visit(ast.getRight());
                // Assign a type to the AST:
                if ((ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("Invalid binary expression.");
                }
                break;
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

//        // Check the exception condition:
//        if (ast.().isPresent()) {
//            visit(ast.getOffset().get());
//            if (!ast.getOffset().get().getType().equals(Environment.Type.INTEGER)) {
//                throw new RuntimeException("Offset must be an integer value.");
//            }
//        }
        // Set the variable of the expression:
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        // Set the function of the expression:
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        // Check that the argument types are assignable to the parameter types:
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.Type> params = ast.getFunction().getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            visit(args.get(i)); // Visit each argument
            requireAssignable(params.get(i), args.get(i).getType());
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type)) {
            return;
        }
        if (target.equals(Environment.Type.ANY)) {
            return;
        }
        if (target.equals(Environment.Type.COMPARABLE)) {
            if (type.getName().equals("Integer") || type.getName().equals("Decimal") || type.getName().equals("Character")
                    || type.getName().equals("String")) {
                return;
            }
        }
        throw new RuntimeException("Invalid assignment: attempting to assign " + type.getName() + " to a " + target.getName() + " variable.");
    }

}
