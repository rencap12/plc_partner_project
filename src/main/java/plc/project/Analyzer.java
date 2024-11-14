package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import plc.project.Environment.PlcObject;

/**
 * Analyzer class that performs semantic analysis on the AST.
 * Validates type consistency, scope rules, and other semantic constraints.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Environment.Type currentFunctionReturnType;

    private static final String MAIN_METHOD_NAME = "main";
    private static final String INTEGER_TYPE_NAME = "Integer";
    private static final String NIL_TYPE_NAME = "Nil";

    public Analyzer(Scope parent) {
        this.scope = new Scope(parent);
        initializeBuiltInFunctions();
    }

    private void initializeBuiltInFunctions() {
        scope.defineFunction("print", "System.out.println",
                Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        try {
            ast.getFields().forEach(this::visit);
            ast.getMethods().forEach(this::visit);
            validateMainMethodExists(ast.getMethods());
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in Source AST", e);
        }
        return null;
    }

    private void validateMainMethodExists(List<Ast.Method> methods) {
        boolean hasValidMainMethod = methods.stream()
                .anyMatch(this::isValidMainMethod);

        if (!hasValidMainMethod) {
            throw new RuntimeException("No main method with proper arguments!");
        }
    }

    private boolean isValidMainMethod(Ast.Method method) {
        return method.getName().equals(MAIN_METHOD_NAME) &&
                method.getReturnTypeName().get().equals(INTEGER_TYPE_NAME) &&
                method.getParameters().isEmpty();
    }

    @Override
    public Void visit(Ast.Field ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(Environment.getType(ast.getTypeName()),
                        ast.getValue().get().getType());
                defineFieldVariable(ast);
            } else {
                scope.defineVariable(ast.getName(), ast.getConstant(), Environment.NIL);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in Field AST", e);
        }
        return null;
    }

    private void defineFieldVariable(Ast.Field ast) {
        scope.defineVariable(
                ast.getName(),
                ast.getName(),
                ast.getVariable().getType(),
                ast.getConstant(),
                Environment.NIL
        );
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> paramTypes = getParameterTypes(ast);
        defineFunction(ast, paramTypes);

        Scope parentScope = scope;
        try {
            scope = new Scope(parentScope);
            defineParameters(ast);
            setCurrentFunctionContext(ast);
            ast.getStatements().forEach(this::visit);
        } finally {
            resetFunctionContext(parentScope);
        }
        return null;
    }

    private List<Environment.Type> getParameterTypes(Ast.Method ast) {
        return ast.getParameterTypeNames().stream()
                .map(Environment::getType)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void defineFunction(Ast.Method ast, List<Environment.Type> paramTypes) {
        Environment.Type returnType = Environment.getType(
                ast.getReturnTypeName().orElse(NIL_TYPE_NAME)
        );
        scope.defineFunction(ast.getName(), ast.getName(), paramTypes, returnType, args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
    }

    private void defineParameters(Ast.Method ast) {
        for (int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(
                    ast.getParameters().get(i),
                    ast.getParameters().get(i),
                    Environment.getType(ast.getParameterTypeNames().get(i)),
                    true,
                    Environment.NIL
            );
        }
    }

    private void setCurrentFunctionContext(Ast.Method ast) {
        currentFunctionReturnType = Environment.getType(
                ast.getReturnTypeName().orElse(NIL_TYPE_NAME)
        );
    }

    private void resetFunctionContext(Scope parentScope) {
        currentFunctionReturnType = null;
        scope = parentScope;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expected a function expression.");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type = determineDeclarationType(ast);
        scope.defineVariable(ast.getName(), ast.getName(), type, false, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    private Environment.Type determineDeclarationType(Ast.Statement.Declaration ast) {
        if (!ast.getTypeName().isPresent()) {
            if (!ast.getValue().isPresent()) {
                throw new RuntimeException("Type of declared variable could not be discerned.");
            }
            visit(ast.getValue().get());
            return ast.getValue().get().getType();
        }

        Environment.Type type = Environment.getType(ast.getTypeName().get());
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());
        }
        return type;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Invalid assignment operation.");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        validateIfStatement(ast);
        executeInNewScope(() -> ast.getElseStatements().forEach(this::visit));
        executeInNewScope(() -> ast.getThenStatements().forEach(this::visit));
        return null;
    }

    private void validateIfStatement(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Invalid condition in IF statement.");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("THEN block cannot have an empty body");
        }
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        validateWhileCondition(ast);
        executeInNewScope(() -> ast.getStatements().forEach(this::visit));
        return null;
    }

    private void validateWhileCondition(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Invalid condition in WHILE statement.");
        }
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(currentFunctionReturnType, ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        ast.setType(determineLiteralType(ast.getLiteral()));
        return null;
    }

    private Environment.Type determineLiteralType(Object literal) {
        if (literal instanceof BigInteger) {
            validateIntegerLiteral((BigInteger) literal);
            return Environment.Type.INTEGER;
        } else if (literal instanceof BigDecimal) {
            validateDecimalLiteral((BigDecimal) literal);
            return Environment.Type.DECIMAL;
        } else if (literal instanceof Boolean) {
            return Environment.Type.BOOLEAN;
        } else if (literal instanceof Character) {
            return Environment.Type.CHARACTER;
        } else if (literal instanceof String) {
            return Environment.Type.STRING;
        }
        return Environment.Type.NIL;
    }

    private void validateIntegerLiteral(BigInteger value) {
        if (value.toByteArray().length > 4) {
            throw new RuntimeException("Integer value will overflow.");
        }
    }

    private void validateDecimalLiteral(BigDecimal value) {
        double doubleVal = value.doubleValue();
        if (Double.isInfinite(doubleVal)) {
            throw new RuntimeException("Decimal value will overflow.");
        }
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("The grouped expression is not binary.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                handleLogicalOperator(ast);
                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                handleComparisonOperator(ast);
                break;
            case "+":
                handleAdditionOperator(ast);
                break;
            case "-":
            case "*":
            case "/":
                handleArithmeticOperator(ast);
                break;
            case "^":
                handleExponentialOperator(ast);
                break;
        }
        return null;
    }

    private void handleLogicalOperator(Ast.Expression.Binary ast) {
        if (ast.getLeft().getType().equals(Environment.Type.BOOLEAN) &&
                ast.getRight().getType().equals(Environment.Type.BOOLEAN)) {
            ast.setType(Environment.Type.BOOLEAN);
        } else {
            throw new RuntimeException("Expected boolean values on both sides of the binary expression.");
        }
    }

    private void handleComparisonOperator(Ast.Expression.Binary ast) {
        requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
        requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());

        if (ast.getLeft().getType().equals(ast.getRight().getType())) {
            ast.setType(Environment.Type.BOOLEAN);
        } else {
            throw new RuntimeException("Left and right sides of equality statement must match.");
        }
    }

    private void handleAdditionOperator(Ast.Expression.Binary ast) {
        if (ast.getLeft().getType().equals(Environment.Type.STRING) ||
                ast.getRight().getType().equals(Environment.Type.STRING)) {
            ast.setType(Environment.Type.STRING);
        } else {
            checkNumericTypesMatch(ast);
        }
    }

    private void handleArithmeticOperator(Ast.Expression.Binary ast) {
        checkNumericTypesMatch(ast);
    }

    private void handleExponentialOperator(Ast.Expression.Binary ast) {
        if ((ast.getLeft().getType().equals(Environment.Type.INTEGER) ||
                ast.getLeft().getType().equals(Environment.Type.DECIMAL)) &&
                ast.getRight().getType().equals(Environment.Type.INTEGER)) {
            ast.setType(ast.getLeft().getType());
        } else {
            throw new RuntimeException("Invalid exponential expression.");
        }
    }

    private void checkNumericTypesMatch(Ast.Expression.Binary ast) {
        if (ast.getLeft().getType().equals(Environment.Type.INTEGER) &&
                ast.getRight().getType().equals(Environment.Type.INTEGER)) {
            ast.setType(Environment.Type.INTEGER);
        } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) &&
                ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
            ast.setType(Environment.Type.DECIMAL);
        } else {
            throw new RuntimeException("Invalid numeric expression.");
        }
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        validateFunctionArguments(ast);
        return null;
    }

    private void validateFunctionArguments(Ast.Expression.Function ast) {
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.Type> params = ast.getFunction().getParameterTypes();

        for (int i = 0; i < args.size(); i++) {
            visit(args.get(i));
            requireAssignable(params.get(i), args.get(i).getType());
        }
    }

    private void executeInNewScope(Runnable action) {
        Scope parentScope = scope;
        try {
            scope = new Scope(parentScope);
            action.run();
        } finally {
            scope = parentScope;
        }
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException();
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type) || target.equals(Environment.Type.ANY)) {
            return;
        }

        if (target.equals(Environment.Type.COMPARABLE) &&
                (type.getName().equals("Integer") ||
                        type.getName().equals("Decimal") ||
                        type.getName().equals("Character") ||
                        type.getName().equals("String"))) {
            return;
        }

        throw new RuntimeException(String.format(
                "Invalid assignment: attempting to assign %s to a %s variable.",
                type.getName(),
                target.getName()
        ));
    }
}