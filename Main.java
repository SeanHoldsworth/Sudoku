import java.util.stream.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

class Main {
    private static void display_row(String s, int r) {
        for (int c = 0; c < 9; c++) {
            int i = 9 * r + c;
            char d = s.charAt(i);

            if (d == '0') d = '.';

            if ((c + 1) % 3 == 0)
                System.out.printf("%c | ", d);
            else
                System.out.printf("%c ", d);
        }
    }

    private static void display(String p, String s) {
        String separator = "+-------+-------+-------+";

        System.out.printf("%s   %s%n", separator, separator);
        for (int r = 0; r < 9; r++) {
            System.out.print("| ");
            display_row(p, r);
            System.out.print("  | ");
            display_row(s, r);
            System.out.println();

            if ((r + 1) % 3 == 0)
                System.out.printf("%s   %s%n", separator, separator);
        }
        System.out.println();
    }
            
    public static void main(String[] args) {
        String filename;

        if (args.length > 0)
            filename = args[0];
        else
            filename = "top95.txt";

        double start = System.currentTimeMillis();
        int count = 0;

        Sudoku s = new Sudoku();

        try {
            Stream<String> lines = Files.lines(Paths.get(filename));
            for (String line : (Iterable<String>) lines::iterator) {
                int m[] = s.markup(line);
                int solution[] = s.solve(m);

                if (solution != null)
                    display(line, s.flatten(solution));
                else
                    System.out.println("No solution");

                count += 1;
            }
        } catch (IOException e) {
            System.out.printf("Error reading data file %s%n", filename);
        }

        double elapsed = System.currentTimeMillis() - start;

        System.out.printf("Ran %d problems in %1.3f seconds%n",
            count, elapsed / 1000);
    }
}
