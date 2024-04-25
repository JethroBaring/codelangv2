package code;

import java.util.List;

public class CodeFunction implements CodeCallable {
    private final Stmt.Function declaration;

    CodeFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(interpreter.globals);

        for (int i = 0; i < declaration.params.size(); i++) {
            TokenType type = declaration.params.get(i).type.type;
            Object value = arguments.get(i);
            if (type == TokenType.STRING) {
                environment.define(declaration.params.get(i).name.lexeme, value, declaration.params.get(i).type.type,
                        true);
            } else if (type == TokenType.CHAR) {
                environment.define(declaration.params.get(i).name.lexeme, value, declaration.params.get(i).type.type,
                        true);

            } else if (type == TokenType.INT) {
                environment.define(declaration.params.get(i).name.lexeme, value, declaration.params.get(i).type.type,
                        true);

            } else if (type == TokenType.FLOAT) {
                environment.define(declaration.params.get(i).name.lexeme, value, declaration.params.get(i).type.type,
                        true);

            } else {
                environment.define(declaration.params.get(i).name.lexeme, value, declaration.params.get(i).type.type,
                        true);

            }
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (declaration.returnType != null && returnValue != null) {
                if (declaration.returnType.type == TokenType.STRING) {
                    if (returnValue.value instanceof String) {
                        return returnValue.value;
                    }
                    return new RuntimeError(declaration.name, "Return value must be of type String");
                } else if (declaration.returnType.type == TokenType.CHAR) {
                    if (returnValue.value instanceof Character) {
                        return returnValue.value;
                    }
                    return new RuntimeError(declaration.name, "Return value must be of type Character");

                } else if (declaration.returnType.type == TokenType.INT) {
                    if (returnValue.value instanceof Integer) {
                        return returnValue.value;
                    }
                    return new RuntimeError(declaration.name, "Return value must be of type Integer");

                } else if (declaration.returnType.type == TokenType.FLOAT) {
                    if (returnValue.value instanceof Float) {
                        return returnValue.value;
                    }
                    return new RuntimeError(declaration.name, "Return value must be of type Float");

                } else if (declaration.returnType.type == TokenType.BOOL) {
                    if (returnValue.value instanceof Boolean) {
                        return returnValue.value;
                    }
                    return new RuntimeError(declaration.name, "Return value must be of type Boolean");
                }
            } 

            if(declaration.returnType == null) {
                if(returnValue.value != null) {
                    throw new RuntimeError(declaration.name, "Function with void return type shouldn't return anything. ");
                }

                return null;
            }

        }

        if (declaration.returnType != null) {
            throw new RuntimeError(declaration.returnType, "Function must return a value of type "
                    + declaration.returnType.lexeme + " or remove the return type of the function.");
        }
        return null;
    }

}
