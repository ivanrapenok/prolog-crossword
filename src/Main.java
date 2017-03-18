import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Main {

    private static final String GRID_FILE = "grid.txt";
    private static final String WORDS_FILE = "words.txt";
    private static final String PROLOG_SCRIPT = "crossword.pl";

    private static int[][] grid;
    private static List<String> words = new ArrayList<>();
    private static List<Word> wordTemplates = new ArrayList<>();
    private static Map<Word, String> names = new TreeMap<>();

    private static String prologCode = "";

    public static void main(String[] args) {
        System.out.println("Read grid from file: " + GRID_FILE);
        readGridFromFile();

        System.out.println("Read words from file: " + WORDS_FILE);
        readWordsFromFile();
        int i = 0;
        words.stream().sorted().forEach(System.out::println);

        System.out.println("\nTake word templates from grid");
        wordTemplatesFromGrid();
        printGrid();

        System.out.println("\nWord templates:");
        wordTemplateNames();
        names.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(System.out::println);

        System.out.println("\nGenerate prolog code");
        generatePrologCode();

        System.out.println("\nSave script to file");
        scriptToFile();
    }

    private static void readGridFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(GRID_FILE))) {

            String sCurrentLine;
            List<String> gridFile = new ArrayList<>();

            while ((sCurrentLine = br.readLine()) != null) {
                gridFile.add(sCurrentLine);
            }

            grid = new int[gridFile.size()][gridFile.get(0).length()];
            System.out.println(grid.length + " " + grid[0].length);
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[0].length; j++) {
                    grid[i][j] = Integer.valueOf(gridFile.get(i).substring(j, j + 1));
                    System.out.print(grid[i][j] + " ");
                }
                System.out.println();
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readWordsFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(WORDS_FILE))) {
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                words.add(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printGrid() {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }

    private enum Position {
        horizontal("hor"),
        vertical("ver");

        private final String id;

        Position(String id) {
            this.id = id;
        }

        public String getValue() {
            return id;
        }
    }

    private static void wordTemplatesFromGrid() {
        int ii, jj;
        wordTemplates.clear();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {

                if (grid[i][j] == 1 || grid[i][j] == 3) {
                    ii = i;
                    jj = j + 1;
                    while (jj < grid[0].length && grid[i][jj] != 0) {
                        grid[i][jj]++;
                        jj++;
                    }
                    if (jj - j > 1) {
                        wordTemplates.add(new Word(j, i, --jj, i));
                        grid[i][j]++;
                    }
                }
                if (grid[i][j] == 1 || grid[i][j] == 2) {
                    ii = i + 1;
                    jj = j;
                    while (ii < grid.length && grid[ii][j] != 0) {
                        grid[ii][j] = (grid[ii][j] == 1) ? 3 : 4;
                        ii++;
                    }
                    if (ii - i > 1) {
                        wordTemplates.add(new Word(j, i, j, --ii));
                        grid[i][j] = (grid[i][j] == 1) ? 3 : 4;
                    }
                }
            }
        }
    }

    private static void wordTemplateNames() {
        wordTemplates.sort(Word::compareTo);
        for (Word w : wordTemplates) {
            names.put(w, "W" + wordTemplates.indexOf(w));
        }
    }

    private static void generatePrologCode() {
        prologCode = "";
        prologCode += generateDictionary();
        prologCode += generateFinalFunctions();
        prologCode += generateCrossword();
    }

    private static String generateDictionary() {
        String res = "% Dictionary:\n";
        int i = 0;
        for (String word : words) {
            res += "word(" + word + "). ";
            if (++i % 4 == 0) res += "\n";
        }
        System.out.println(res);
        return res + "\n";
    }

    private static String generateFinalFunctions() {
        String res = "\ncross(Word1, LetterNumber1, Word2, LetterNumber2) :-\n" +
                "    sub_string(Word1, LetterNumber1, 1, _, X),\n" +
                "    sub_string(Word2, LetterNumber2, 1, _, X).\n" +
                "\n" +
                "word_with_length(Word, Length) :-\n" +
                "    word(Word),\n" +
                "    string_length(Word, Length).\n";
        System.out.println(res);
        return res;
    }

    private static String generateCrossword() {
        String res;
        String list = "";
        for (Map.Entry<Word, String> entry : names.entrySet()) {
            list += entry.getValue() + ", ";
        }
        list = list.substring(0, list.length() - 2);
        res = "\n% To run: crossword(" + list + ").\n";
        res += "crossword(" + list + ") :-\n";

        res += "    % Words lengths\n";
        for (Map.Entry<Word, String> entry : names.entrySet()) {
            res += "    word_with_length(" + entry.getValue() + ", " + entry.getKey().getLen() + "),\n";
        }

        res += "\n    % All words are different\n";
        res += "    append([], [" + list + "], List),\n" +
                "    sort(List, Sorted),\n" +
                "    length(Sorted, Len),\n" +
                "    length(List, Len),\n";

        res += "\n    % Words crosses\n";
        for (Map.Entry<Word, String> hor : names.entrySet().stream()
                .filter(map -> map.getKey().getPosition() == Position.horizontal)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).entrySet()) {
            for (Map.Entry<Word, String> ver : names.entrySet().stream()
                    .filter(map -> {
                        Word wh = hor.getKey();
                        Word wv = map.getKey();
                        return wv.getPosition() == Position.vertical &&
                                (wv.x1 >= wh.x1) && (wv.x1 <= wh.x2) &&
                                (wh.y1 >= wv.y1) && (wh.y1 <= wv.y2);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).entrySet()) {
                res += "    cross(" + hor.getValue() + ", " +
                        (ver.getKey().x1 - hor.getKey().x1) + ", " +
                        ver.getValue() + ", " +
                        (hor.getKey().y1 - ver.getKey().y1) + "),\n";
            }
        }

        res = res.substring(0, res.length() - 2) + ".\n";

        System.out.println(res);
        return res;
    }

    private static void scriptToFile() {
        try(  PrintWriter out = new PrintWriter( PROLOG_SCRIPT )  ){
            out.println( prologCode );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class Word implements Comparable<Word> {
        int x1, y1, x2, y2;

        Word(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        int getLen() {
            return (x2 - x1) + (y2 - y1) + 1;
        }

        Position getPosition() {
            return (x1 == x2) ? Position.vertical : Position.horizontal;
        }

        @Override
        public String toString() {
            return "Word{" +
                    "x1=" + x1 +
                    ", y1=" + y1 +
                    ", x2=" + x2 +
                    ", y2=" + y2 +
                    ", len=" + getLen() +
                    ", pos=" + getPosition().getValue() +
                    '}';
        }

        @Override
        public int compareTo(Word w) {
            int compareY = Integer.valueOf(this.y1).compareTo(w.y1);
            if (compareY != 0) return compareY;
            int compareX = Integer.valueOf(this.x1).compareTo(w.x1);
            if (compareX != 0) return compareX;
            return this.getPosition().getValue().compareTo(w.getPosition().getValue());
        }
    }
}
