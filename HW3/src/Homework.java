import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nuning on 3/27/21.
 */
public class Homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases/input1.txt");
    }
}

class InferenceSystem {
    private InferenceInput inferenceInput;
    public InferenceSystem(String filename) {
        inferenceInput = readInput(filename);
        inferenceInput.print();
    }

    public InferenceInput readInput(String filename) {
        File file = new File(filename);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            InferenceInput inferenceInput = new InferenceInput();

            Integer numberOfQueries = Integer.valueOf(reader.readLine().trim());
            for(int i=0; i<numberOfQueries; i++) {
                // Query is an atomic sentence / no variables
                String query = reader.readLine().trim();
                Sentence s = parseSentence(query);
                inferenceInput.addQuery(s);
            }

            Integer numberOfKB = Integer.valueOf(reader.readLine().trim());
            for(int i=0; i<numberOfKB; i++) {
                String sentence = reader.readLine().trim();
                Sentence s = parseSentence(sentence);
                inferenceInput.addKB(s);
            }

            return inferenceInput;
        } catch (Exception e) {
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public Sentence parseSentence(String sentenceStr) {
        Sentence sentence = new Sentence();
        int index = sentenceStr.indexOf("=>");
        if (index > -1) {
            // p => q
            String premiseStr = sentenceStr.substring(0, index).trim();
            String[] premises = premiseStr.split("&");
            for(String pm: premises) {
                Predicate p = parsePredicate(pm.trim());
                // Convert to ~p
                p.negate();
                sentence.addPredicate(p);
            }

            // conclusion is an atomic sentence
            String conclusion = sentenceStr.substring(index + 2).trim();
            Predicate conclusionPredicate = parsePredicate(conclusion);
            sentence.addPredicate(conclusionPredicate);
        } else {
            // atomic sentence p or ~p
            Predicate predicate = parsePredicate(sentenceStr);
            sentence.addPredicate(predicate);
        }
        return sentence;
    }

    public Predicate parsePredicate(String predicateStr) {
        int open = predicateStr.indexOf("(");
        int close = predicateStr.indexOf(")");
        String predicate = predicateStr.substring(0, open);
        Predicate p = new Predicate(predicate);
        String argStr = predicateStr.substring(open+1, close);
        String[] args = argStr.split(",");
        for(String arg: args) {
            p.addArgument(arg.trim());
        }
        return p;
    }
}

class Predicate {
    private boolean isNegative = false;
    private String predicate;
    private List<String> arguments;

    public Predicate() {
        arguments = new ArrayList<>();
    }

    public Predicate(String predicate) {
        if (predicate.charAt(0) == '~') {
            isNegative = false;
            predicate = predicate.substring(1);
        }
        this.predicate = predicate;
        arguments = new ArrayList<>();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isNegative) sb.append("~");
        sb.append(predicate);
        sb.append("(");
        for(int i=0; i<arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i < arguments.size() - 1) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean isNegative() {
        return isNegative;
    }

    public void negate() {
        isNegative = !isNegative;
    }

    public String getPredicate() {
        return predicate;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void addArgument(String argument) {
        arguments.add(argument);
    }
}

class Sentence {
    private static long runningID = 1;
    private long id;
    private List<Predicate> predicates;
    private boolean isFalse = false;

    public Sentence() {
        id = runningID++;
        predicates = new ArrayList<>();
    }
    public Sentence(Predicate predicate) {
        id = runningID++;
        predicates.add(predicate);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<predicates.size(); i++) {
            sb.append(predicates.get(i).toString());
            if (i != predicates.size()-1) sb.append(" | ");
        }
        return sb.toString();
    }

    public void addPredicate(Predicate predicate) {
        predicates.add(predicate);
    }

    public boolean isFalse() {
        return isFalse;
    }

    public void setFalse(boolean aFalse) {
        isFalse = aFalse;
    }

    public long getId() {
        return id;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }
}

class InferenceInput {
    private List<Sentence> KB;
    private List<Sentence> queries;

    public InferenceInput() {
        KB = new ArrayList<>();
        queries = new ArrayList<>();
    }

    public void addKB(Sentence sentence) {
        KB.add(sentence);
    }

    public void addQuery(Sentence sentence) {
        queries.add(sentence);
    }

    public List<Sentence> getKB() {
        return KB;
    }

    public List<Sentence> getQueries() {
        return queries;
    }

    public void print() {
        System.out.println("====== Queries ======");
        for(Sentence s: queries) {
            System.out.println(s.toString());
        }

        System.out.println("====== KB ======");
        for(Sentence s: KB) {
            System.out.println(s.toString());
        }
    }
}
