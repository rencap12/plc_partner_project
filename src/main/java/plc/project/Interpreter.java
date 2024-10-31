package plc.project;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        return scope.lookupFunction("main", 0).invoke(new java.util.ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), true, value); // Assuming fields are mutable by default; adjust if needed
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope functionScope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                functionScope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
            Scope previousScope = scope;
            scope = functionScope;
            try {
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return returnException) {
                return returnException.value;
            } finally {
                scope = previousScope;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value = ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL;
        scope.defineVariable(ast.getName(), true, value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Assignment target must be an access expression.");
        }
        Ast.Expression.Access receiver = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());

        if (receiver.getReceiver().isPresent()) {
            Environment.PlcObject object = visit(receiver.getReceiver().get());
            object.setField(receiver.getName(), value);
        } else {
            scope.lookupVariable(receiver.getName()).setValue(value);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        Scope ifScope = new Scope(scope);
        scope = ifScope;
        try {
            if (condition) {
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            } else {
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
        } finally {
            scope = ifScope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        Scope forScope = new Scope(scope);
        scope = forScope;
        try {
            visit(ast.getInitialization());
            while (requireType(Boolean.class, visit(ast.getCondition()))) {
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
                // If there's a way to specify the update outside of `getUpdate()`, handle it here.
            }
        } finally {
            scope = forScope.getParent();
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject leftObject = visit(ast.getLeft());
        Environment.PlcObject rightObject = visit(ast.getRight());

        switch (ast.getOperator()) {
            case "&&":
                return Environment.create(requireType(Boolean.class, leftObject) && requireType(Boolean.class, rightObject));
            case "||":
                return Environment.create(requireType(Boolean.class, leftObject) || requireType(Boolean.class, rightObject));
            case "<":
                return Environment.create(requireType(Comparable.class, leftObject).compareTo(requireType(leftObject.getValue().getClass(), rightObject)) < 0);
            case "<=":
                return Environment.create(requireType(Comparable.class, leftObject).compareTo(requireType(leftObject.getValue().getClass(), rightObject)) <= 0);
            case ">":
                return Environment.create(requireType(Comparable.class, leftObject).compareTo(requireType(leftObject.getValue().getClass(), rightObject)) > 0);
            case ">=":
                return Environment.create(requireType(Comparable.class, leftObject).compareTo(requireType(leftObject.getValue().getClass(), rightObject)) >= 0);
            case "==":
                return Environment.create(java.util.Objects.equals(leftObject.getValue(), rightObject.getValue()));
            case "!=":
                return Environment.create(!java.util.Objects.equals(leftObject.getValue(), rightObject.getValue()));
            case "+":
                if (leftObject.getValue() instanceof String || rightObject.getValue() instanceof String) {
                    return Environment.create(leftObject.getValue().toString() + rightObject.getValue().toString());
                } else if (leftObject.getValue() instanceof java.math.BigInteger && rightObject.getValue() instanceof java.math.BigInteger) {
                    return Environment.create(((java.math.BigInteger) leftObject.getValue()).add((java.math.BigInteger) rightObject.getValue()));
                }
                break;
            case "-":
                if (leftObject.getValue() instanceof java.math.BigInteger && rightObject.getValue() instanceof java.math.BigInteger) {
                    return Environment.create(((java.math.BigInteger) leftObject.getValue()).subtract((java.math.BigInteger) rightObject.getValue()));
                }
                break;
            case "*":
                if (leftObject.getValue() instanceof java.math.BigInteger && rightObject.getValue() instanceof java.math.BigInteger) {
                    return Environment.create(((java.math.BigInteger) leftObject.getValue()).multiply((java.math.BigInteger) rightObject.getValue()));
                }
                break;
            case "/":
                if (leftObject.getValue() instanceof java.math.BigInteger && rightObject.getValue() instanceof java.math.BigInteger) {
                    if (((java.math.BigInteger) rightObject.getValue()).equals(java.math.BigInteger.ZERO)) {
                        throw new RuntimeException("Division by zero.");
                    }
                    return Environment.create(((java.math.BigInteger) leftObject.getValue()).divide((java.math.BigInteger) rightObject.getValue()));
                }
                break;
        }
        throw new RuntimeException("Invalid binary operation for operator: " + ast.getOperator());
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            // Get the receiver object first
            Environment.PlcObject receiverObject = visit(ast.getReceiver().get());

            // Retrieve the field from the receiver using getField which returns a Variable
            Environment.Variable variable = receiverObject.getField(ast.getName());

            // Return the PlcObject from the retrieved variable
            return variable.getValue(); // Assuming this returns the PlcObject
        } else {
            // Lookup the variable directly
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            return variable.getValue(); // Ensure this returns the PlcObject
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.PlcObject receiver = ast.getReceiver().isPresent() ? visit(ast.getReceiver().get()) : null;
        return receiver != null ? receiver.callMethod(ast.getName(), ast.getArguments().stream().map(this::visit).toList())
                : scope.lookupFunction(ast.getName(), ast.getArguments().size())
                .invoke(ast.getArguments().stream().map(this::visit).toList());
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
