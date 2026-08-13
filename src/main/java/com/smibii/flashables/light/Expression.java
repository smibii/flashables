package com.smibii.flashables.light;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Expression {

    private final Node root;

    private Expression(Node root) {
        this.root = root;
    }

    public static Expression compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        Parser parser = new Parser(expression);
        Node root = parser.parse();

        if (!parser.isAtEnd()) {
            throw parser.error("Unexpected token");
        }

        return new Expression(root);
    }

    public double evaluate(double time) {
        return evaluate(time, Map.of());
    }

    public double evaluate(double time, Map<String, Double> variables) {
        EvaluationContext context = new EvaluationContext(time, variables);
        return root.evaluate(context);
    }

    /*
     * ============================================================
     * Evaluation
     * ============================================================
     */

    private record EvaluationContext(
            double time,
            Map<String, Double> variables
    ) {
    }

    private interface Node {
        double evaluate(EvaluationContext context);
    }

    private record NumberNode(double value) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {
            return value;
        }
    }

    private record VariableNode(String name) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {

            if (name.equals("time")) {
                return context.time();
            }

            Double value = context.variables().get(name);

            if (value != null) {
                return value;
            }

            throw new IllegalArgumentException(
                    "Unknown variable: " + name
            );
        }
    }

    private record UnaryNode(
            char operator,
            Node value
    ) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {

            double value = this.value.evaluate(context);

            return switch (operator) {
                case '+' -> value;
                case '-' -> -value;

                default -> throw new IllegalStateException(
                        "Unknown unary operator: " + operator
                );
            };
        }
    }

    private record BinaryNode(
            Node left,
            char operator,
            Node right
    ) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {

            double left = this.left.evaluate(context);
            double right = this.right.evaluate(context);

            return switch (operator) {

                case '+' -> left + right;
                case '-' -> left - right;
                case '*' -> left * right;
                case '/' -> left / right;
                case '%' -> left % right;
                case '^' -> Math.pow(left, right);

                default -> throw new IllegalStateException(
                        "Unknown binary operator: " + operator
                );
            };
        }
    }

    private record FunctionNode(
            String name,
            List<Node> arguments
    ) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {

            List<Double> values = new ArrayList<>(arguments.size());

            for (Node argument : arguments) {
                values.add(argument.evaluate(context));
            }

            return Functions.call(name, values);
        }
    }

    private record ConstantNode(double value) implements Node {

        @Override
        public double evaluate(EvaluationContext context) {
            return value;
        }
    }

    /*
     * ============================================================
     * Functions
     * ============================================================
     */

    private static final class Functions {

        private Functions() {
        }

        private static double call(
                String name,
                List<Double> args
        ) {
            String function = name.toLowerCase(Locale.ROOT);

            return switch (function) {

                case "sin" ->
                        unary(function, args, Math::sin);

                case "cos" ->
                        unary(function, args, Math::cos);

                case "tan" ->
                        unary(function, args, Math::tan);

                case "asin" ->
                        unary(function, args, Math::asin);

                case "acos" ->
                        unary(function, args, Math::acos);

                case "atan" ->
                        unary(function, args, Math::atan);

                case "sqrt" ->
                        unary(function, args, Math::sqrt);

                case "abs" ->
                        unary(function, args, Math::abs);

                case "floor" ->
                        unary(function, args, Math::floor);

                case "ceil" ->
                        unary(function, args, Math::ceil);

                case "round" ->
                        unary(function, args, value ->
                                Math.round(value)
                        );

                case "exp" ->
                        unary(function, args, Math::exp);

                case "log" ->
                        unary(function, args, Math::log);

                case "log10" ->
                        unary(function, args, Math::log10);

                case "random" -> {

                    requireArgs(function, args, 0);

                    yield Math.random();
                }

                case "min" -> {

                    requireArgs(function, args, 2);

                    double result = args.get(0);

                    for (int i = 1; i < args.size(); i++) {
                        result = Math.min(result, args.get(i));
                    }

                    yield result;
                }

                case "max" -> {

                    requireArgs(function, args, 2);

                    double result = args.get(0);

                    for (int i = 1; i < args.size(); i++) {
                        result = Math.max(result, args.get(i));
                    }

                    yield result;
                }

                case "pow" -> {

                    requireArgs(function, args, 2);

                    yield Math.pow(
                            args.get(0),
                            args.get(1)
                    );
                }

                case "clamp" -> {

                    requireArgs(function, args, 3);

                    double value = args.get(0);
                    double min = args.get(1);
                    double max = args.get(2);

                    yield Math.max(
                            min,
                            Math.min(max, value)
                    );
                }

                default -> throw new IllegalArgumentException(
                        "Unknown function: " + name
                );
            };
        }

        private static double unary(
                String name,
                List<Double> args,
                DoubleUnaryOperation operation
        ) {
            requireArgs(name, args, 1);

            return operation.apply(args.get(0));
        }

        private static void requireArgs(
                String name,
                List<Double> args,
                int count
        ) {
            if (args.size() != count) {
                throw new IllegalArgumentException(
                        "Function '" + name +
                                "' expects " + count +
                                " argument(s), got " + args.size()
                );
            }
        }

        private interface DoubleUnaryOperation {
            double apply(double value);
        }
    }

    /*
     * ============================================================
     * Parser
     *
     * Grammar:
     *
     * expression  = addition
     * addition    = multiplication (('+' | '-') multiplication)*
     * multiplication = power (('*' | '/' | '%') power)*
     * power       = unary ('^' unary)*
     * unary       = ('+' | '-') unary | primary
     * primary     = number
     *             | identifier
     *             | identifier '(' arguments ')'
     *             | '(' expression ')'
     * ============================================================
     */

    private static final class Parser {

        private final String input;
        private int position;

        private Parser(String input) {
            this.input = input;
        }

        private Node parse() {
            skipWhitespace();

            Node result = parseExpression();

            skipWhitespace();

            return result;
        }

        private Node parseExpression() {
            return parseAddition();
        }

        private Node parseAddition() {

            Node left = parseMultiplication();

            while (true) {

                skipWhitespace();

                if (match('+')) {
                    Node right = parseMultiplication();

                    left = new BinaryNode(
                            left,
                            '+',
                            right
                    );

                } else if (match('-')) {
                    Node right = parseMultiplication();

                    left = new BinaryNode(
                            left,
                            '-',
                            right
                    );

                } else {
                    break;
                }
            }

            return left;
        }

        private Node parseMultiplication() {

            Node left = parsePower();

            while (true) {

                skipWhitespace();

                if (match('*')) {
                    Node right = parsePower();

                    left = new BinaryNode(
                            left,
                            '*',
                            right
                    );

                } else if (match('/')) {
                    Node right = parsePower();

                    left = new BinaryNode(
                            left,
                            '/',
                            right
                    );

                } else if (match('%')) {
                    Node right = parsePower();

                    left = new BinaryNode(
                            left,
                            '%',
                            right
                    );

                } else {
                    break;
                }
            }

            return left;
        }

        private Node parsePower() {

            Node left = parseUnary();

            skipWhitespace();

            if (match('^')) {

                Node right = parsePower();

                return new BinaryNode(
                        left,
                        '^',
                        right
                );
            }

            return left;
        }

        private Node parseUnary() {

            skipWhitespace();

            if (match('+')) {
                return new UnaryNode(
                        '+',
                        parseUnary()
                );
            }

            if (match('-')) {
                return new UnaryNode(
                        '-',
                        parseUnary()
                );
            }

            return parsePrimary();
        }

        private Node parsePrimary() {

            skipWhitespace();

            if (match('(')) {

                Node expression = parseExpression();

                skipWhitespace();

                expect(
                        ')',
                        "Expected ')'"
                );

                return expression;
            }

            if (isDigit(peek()) || peek() == '.') {
                return parseNumber();
            }

            if (isIdentifierStart(peek())) {
                return parseIdentifier();
            }

            throw error(
                    "Expected number, identifier or '('"
            );
        }

        private Node parseNumber() {

            int start = position;

            boolean hasDecimal = false;

            while (!isAtEnd()) {

                char c = peek();

                if (isDigit(c)) {
                    position++;
                    continue;
                }

                if (c == '.' && !hasDecimal) {
                    hasDecimal = true;
                    position++;
                    continue;
                }

                break;
            }

            /*
             * Scientific notation:
             *
             * 1e3
             * 1.5e-2
             */

            if (!isAtEnd() &&
                    (peek() == 'e' || peek() == 'E')) {

                position++;

                if (!isAtEnd() &&
                        (peek() == '+' || peek() == '-')) {
                    position++;
                }

                while (!isAtEnd() && isDigit(peek())) {
                    position++;
                }
            }

            String value = input.substring(
                    start,
                    position
            );

            try {
                return new NumberNode(
                        Double.parseDouble(value)
                );
            } catch (NumberFormatException exception) {
                throw error(
                        "Invalid number: " + value
                );
            }
        }

        private Node parseIdentifier() {

            int start = position;

            while (!isAtEnd() &&
                    isIdentifierPart(peek())) {
                position++;
            }

            String name = input.substring(
                    start,
                    position
            );

            /*
             * Support:
             *
             * Math.sin
             * Math.random
             * Math.PI
             */

            if (match('.')) {

                int memberStart = position;

                while (!isAtEnd() &&
                        isIdentifierPart(peek())) {
                    position++;
                }

                if (memberStart == position) {
                    throw error(
                            "Expected identifier after '.'"
                    );
                }

                String member = input.substring(
                        memberStart,
                        position
                );

                String fullName =
                        name + "." + member;

                skipWhitespace();

                if (match('(')) {
                    return parseFunction(fullName);
                }

                return parseConstant(fullName);
            }

            skipWhitespace();

            if (match('(')) {
                return parseFunction(name);
            }

            return parseConstant(name);
        }

        private Node parseFunction(String name) {

            List<Node> arguments = new ArrayList<>();

            skipWhitespace();

            if (match(')')) {
                return new FunctionNode(
                        normalizeFunction(name),
                        arguments
                );
            }

            while (true) {

                arguments.add(
                        parseExpression()
                );

                skipWhitespace();

                if (match(')')) {
                    break;
                }

                expect(
                        ',',
                        "Expected ',' or ')'"
                );
            }

            return new FunctionNode(
                    normalizeFunction(name),
                    arguments
            );
        }

        private Node parseConstant(String name) {

            String normalized =
                    name.toLowerCase(Locale.ROOT);

            return switch (normalized) {

                case "pi", "math.pi" ->
                        new ConstantNode(Math.PI);

                case "e", "math.e" ->
                        new ConstantNode(Math.E);

                default ->
                        new VariableNode(name);
            };
        }

        private String normalizeFunction(String name) {

            if (name.startsWith("Math.")) {
                return name.substring(5);
            }

            return name;
        }

        private boolean match(char expected) {

            skipWhitespace();

            if (isAtEnd() ||
                    input.charAt(position) != expected) {
                return false;
            }

            position++;

            return true;
        }

        private void expect(
                char expected,
                String message
        ) {
            if (!match(expected)) {
                throw error(message);
            }
        }

        private void skipWhitespace() {

            while (!isAtEnd() &&
                    Character.isWhitespace(
                            input.charAt(position)
                    )) {
                position++;
            }
        }

        private char peek() {
            if (isAtEnd()) {
                return '\0';
            }

            return input.charAt(position);
        }

        private boolean isAtEnd() {
            return position >= input.length();
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_';
        }

        private boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) ||
                    c == '_';
        }

        private RuntimeException error(String message) {

            return new IllegalArgumentException(
                    message +
                            " at position " +
                            position +
                            " in expression: " +
                            input
            );
        }
    }
}