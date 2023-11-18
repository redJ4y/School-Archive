
import java.util.Arrays;
import java.util.Scanner;

// @author Jared Scholz
public class Visualizer {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Auto balance tree? (y/n)\n> ");
        PersistentDynamicSet<String> set = null;
        if (scanner.nextLine().toLowerCase().charAt(0) == 'y') {
            set = new BalancedPersistentDynamicSet<>();
        } else {
            set = new PersistentDynamicSet<>();
        }

        System.out.println("\nInput options:");
        System.out.println("\"+ [string]\"  | add a string to the set");
        System.out.println("\"- [string]\"  | remove a string from the set");
        System.out.println("\"s [integer]\" | set the version");
        System.out.println("\"v\"           | view version options");
        System.out.println("\"q\"           | quit\n");
        String input = null;
        while (!"q".equals(input)) {
            System.out.print("> ");
            input = scanner.nextLine();
            switch (input.toLowerCase().charAt(0)) {
                case '+':
                    if (!set.add(input.substring(1).strip())) {
                        System.out.println("Already included");
                    }
                    break;
                case '-':
                    if (!set.remove(input.substring(1).strip())) {
                        System.out.println("Not found");
                    }
                    break;
                case 's': {
                    try {
                        if (!set.setVersion(Integer.parseInt(input.substring(1).strip()))) {
                            System.out.println("Invalid version");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input!");
                        break;
                    }
                    System.out.println("Set: " + Arrays.toString(set.toArray()));
                    set.printTree();
                    break;
                }
                case 'v':
                    System.out.println("Versions: " + set.versionRoots.keySet());
                    break;
                case 'q':
                    input = "q"; // ensure lowercase
                    break;
                default:
                    System.out.println("Invalid input!");
                    break;
            }
        }
    }
}
