import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by nuning on 3/27/21.
 */
public class Homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases/input1.txt");
        inferenceSystem.startInference();
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
                if (s.getPredicates().size() == 1) {
                    // Add negation of query to proof by contradiction
                    s.getPredicates().get(0).negate();
                }
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

    public void startInference() {
        for(Sentence q: inferenceInput.getQueries()) {
            boolean valid = ask(inferenceInput.getKB(), q);
            System.out.println(valid);
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

    public Sentence mergeSentence(Sentence a, Sentence b) {
        Set<Predicate> predicates = new HashSet<>();
        for(Predicate p: a.getPredicates()) {
            predicates.add(p);
        }

        for(Predicate p: b.getPredicates()) {
            predicates.add(p);
        }

        Sentence sentence = new Sentence(new ArrayList<>(predicates));
        return sentence;
    }

    public Sentence resolution(Sentence s1, Sentence s2) {
        List<Predicate> s1Predicates = s1.getPredicates();
        List<Predicate> s2Predicates = s2.getPredicates();

        for (Predicate p1: s1Predicates) {
            for(Predicate p2: s2Predicates) {
                System.out.println("resolution | p1 = " + p1.toString() + " ;; p2 = " + p2.toString() );
                if (p1.isExactComplement(p2)) {
                    // p v ~q, q v r = p v r
                    s1.removePredicate(p1);
                    s2.removePredicate(p2);
                    Sentence result = mergeSentence(s1, s2);
                    System.out.println("resolution | result: " + result.toString());
                    return result;
                } else if (p1.isComplement(p2)) {
                    // unification
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isFailure()) {
                        System.out.println("resolution | cannot unify");
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        System.out.println("resolution | Unification is found");
                        System.out.println(unifier.toString());

                        s1.removePredicate(p1);
                        s2.removePredicate(p2);

                        System.out.println(s1.toString());
                        System.out.println(s2.toString());

                        Sentence ss1 = substitute(s1, unifier);
                        Sentence ss2 = substitute(s2, unifier);
                        Sentence result = mergeSentence(ss1, ss2);
                        System.out.println("resolution | result: " + result.toString());
                        return result;
                    }
                }
            }
        }

        Sentence s = new Sentence();
        s.setFailure(true);
        return s;
    }

    public Sentence substitute(Sentence s, Unifier unifier) {
        for(Predicate p: s.getPredicates()) {
            for(int i=0; i<p.getArguments().size(); i++) {
                String arg = p.getArguments().get(i);
                if (Utility.isVariable(arg)) {
                    String constant = unifier.getSubstitution(arg);
                    if (constant != null) {
                        p.substitute(constant, i);
                    }
                }
            }
        }
        return s;
    }

    public Unifier unify(Predicate p1, Predicate p2) {
        if (!p1.getPredicate().equals(p2.getPredicate())) return null;
        Unifier unifier = new Unifier();

        for(int i=0; i<p1.getArguments().size(); i++) {
            String arg1 = p1.getArguments().get(i);
            String arg2 = p2.getArguments().get(i);

            unifySingleTerm(arg1, arg2, unifier);
        }

        return unifier;
    }

    public void unifySingleTerm(String arg1, String arg2, Unifier unifier) {
        if (unifier.isFailure()) {
            return;
        } else if (arg1.equals(arg2)) {
            // { Constant/Constant }, { x/x }
            return;
        } else if (Utility.isVariable(arg1)) {
            // { x/y, x/Constant }
            unifyVariable(arg1, arg2, unifier);
        } else if (Utility.isVariable(arg2)) {
            // // { x/y, Constant/y }
            unifyVariable(arg2, arg1, unifier);
        } else {
            unifier.setFailure(true);
        }
    }

    public void unifyVariable(String variable, String constant, Unifier unifier) {
        if (unifier.getSubstitution(variable) != null) {
            unifySingleTerm(unifier.getSubstitution(variable), constant, unifier);
        } else if (unifier.getSubstitution(constant) != null) {
            unifySingleTerm(variable, unifier.getSubstitution(constant), unifier);
        } else {
            unifier.addSubstitution(variable, constant);
        }
    }

    public boolean ask(List<Sentence> KB, Sentence query) {
        List<Sentence> clauses = new ArrayList<>();
        clauses.addAll(KB);
        clauses.add(query);

        Set<Pair<Sentence, Sentence>> visited = new HashSet<>();
        for (int i=0; i<clauses.size(); i++) {
            Sentence a = clauses.get(i);
            for (int j=i+1; j<clauses.size(); j++) {
                Sentence b = clauses.get(j);
                if (visited.contains(new Pair<>(a, b)) || visited.contains(new Pair<>(b, a))) continue;

                Sentence result = resolution(a, b);
                visited.add(new Pair<>(a, b));
                visited.add(new Pair<>(b, a));

                if (!result.isFailure() && result.isEmpty()) {
                    // Contradiction
                    return true;
                }

                if (result.isFailure()) continue;

            }
        }
        return false;
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

    public boolean isComplement(Predicate p2) {
        if (predicate.equals(p2.predicate) && isNegative == !p2.isNegative() && arguments.size() == p2.getArguments().size()) {
            for(int i=0; i<arguments.size(); i++) {
                String a = arguments.get(i);
                String b = p2.getArguments().get(i);
                if (!Utility.isVariable(a) && !Utility.isVariable(b) && !a.equals(b)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isExactComplement(Predicate p2) {
        if (predicate.equals(p2.predicate) && isNegative == !p2.isNegative() && arguments.size() == p2.getArguments().size()) {
            for(int i=0; i<arguments.size(); i++) {
                String a = arguments.get(i);
                String b = p2.getArguments().get(i);
                if (!a.equals(b)) return false;
            }
            return true;
        }
        return false;
    }

    public void substitute(String constant, int index) {
        arguments.remove(index);
        arguments.add(index, constant);
    }
}

class Sentence {
    private static long runningID = 1;
    private long id;
    private List<Predicate> predicates;
    private boolean isFailure = false;

    public Sentence() {
        id = runningID++;
        predicates = new ArrayList<>();
    }

    public Sentence(List<Predicate> predicates) {
        id = runningID++;
        this.predicates = predicates;
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

    public boolean isFailure() {
        return isFailure;
    }

    public void setFailure(boolean isFailure) {
        this.isFailure = isFailure;
    }

    public long getId() {
        return id;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public boolean equals(Sentence s) {
        if (this == s) return true;
        return id == s.id;
    }

    public int hashCode() {
        return 31 + (int) id;
    }

    public boolean isEmpty() {
        return predicates.size() == 0;
    }

    public void removePredicate(Predicate p) {
        predicates.remove(p);
    }
}

class Unifier {
    private Map<String, String> substitution;
    private boolean isFailure = false;

    public Unifier() {
        substitution = new HashMap<>();
    }

    public void addSubstitution(String key, String value) {
        substitution.put(key, value);
    }

    public String getSubstitution(String key) {
        return substitution.getOrDefault(key, null);
    }

    public boolean isFailure() {
        return isFailure;
    }

    public void setFailure(boolean failure) {
        isFailure = failure;
    }

    public boolean isEmpty() {
        return substitution.size() == 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i=0;
        for(String key: substitution.keySet()) {
            String value = substitution.get(key);
            sb.append(key + "/" + value);
            if (i != substitution.size()-1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        if (isFailure()) sb.append("[FAILURE]");
        return sb.toString();
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

class Utility {
    public static boolean isVariable(String argument) {
        return Character.isUpperCase(argument.charAt(0));
    }
}

class Pair<U, V> {
    private U first;
    private V second;

    public Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    public U getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    public boolean equals(Pair<U, V> p) {
        if (this == p) return true;
        return first.equals(p.getFirst()) && second.equals(p.getSecond());
    }

    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (first != null ? first.hashCode() : 0);
        hash = 79 * hash + (second != null ? second.hashCode() : 0);
        return hash;
    }
}