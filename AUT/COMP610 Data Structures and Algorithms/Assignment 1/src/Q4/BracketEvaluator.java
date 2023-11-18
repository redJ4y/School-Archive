package Q4;

// @author Jared Scholz
import java.util.Deque;
import java.util.LinkedList;

// I opted for a boolean return instead of throwing exceptions. It's more practical.
public final class BracketEvaluator {

    public static boolean evaluate(String input) {
        System.out.print("String: \"" + input + "\" - ");
        Deque<Character> bracketStack = new LinkedList<>();
        // Deque provides stack methods push() and pop()
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (isOpeningBracket(current)) {
                bracketStack.push(current);
            } else if (isClosingBracket(current)) {
                if (bracketStack.isEmpty()) {
                    System.out.println("INVALID: preemptive closing bracket: " + current);
                    return false;
                }
                char openingBracket = bracketStack.pop();
                if (openingBracket != closingToOpening(current)) {
                    System.out.printf("INVALID: brackets %c %c do not match\n", openingBracket, current);
                    return false;
                }
            }
        }
        if (!bracketStack.isEmpty()) {
            System.out.print("INVALID: unclosed bracket(s): ");
            for (char current : bracketStack) {
                System.out.print(current + " ");
            }
            System.out.println();
            return false;
        }
        System.out.println("VALID");
        return true;
    }

    private static char closingToOpening(char c) {
        /* Assumes input (c) is a valid closing bracket */
        if (c < 42) {
            return (char) ((int) c - 1);
        }
        return (char) ((int) c - 2);
    }

    private static boolean isOpeningBracket(char c) {
        return (c == '(' || c == '{' || c == '<' || c == '[');
    }

    private static boolean isClosingBracket(char c) {
        return (c == ')' || c == '}' || c == '>' || c == ']');
    }

    public static void main(String[] args) {
        evaluate("(147)");
        evaluate("(147}");
        evaluate("{((2 x 5)+(3*-2 + 5))}");
        evaluate("{ (2 x 5)+(3*-2 + 5))}");
        evaluate("List<List<E>>");
        evaluate("List<List<E>");
        evaluate("{(<<eeeek>>){}{...}(e(e)e){hello}}");
        evaluate("{(< eeeek>>){}{...} e(e)e){hello}");
    }
}
