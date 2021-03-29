import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nuning on 3/27/21.
 */
public class Homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases/input4.txt");
        //inferenceSystem.startInference();
    }
}

class InferenceSystem {
    private InferenceInput inferenceInput;
    private final int LIMIT_KB_SIZE = 1000;
    private final double LIMIT_TIME_IN_SECONDS = 60.0;
    private int lastIndexOfNewClauses = 0;
    private int STANDARD_VARIABLE_COUNT = 0;
    private long startTime = 0;
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
            String line = reader.readLine();
            while (line != null){
                String sentence = line.trim();
                Sentence s = parseSentence(sentence);
                standardizeVariable(s);
                inferenceInput.addKB(s);
                Sentence factorSentence = factoring(s);
                if (!factorSentence.isFailure() && !isTautology(factorSentence)) {
                    inferenceInput.addKB(factorSentence);
                }
                line = reader.readLine();
            }

            return inferenceInput;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public void startInference() {
        List<Boolean> result = new ArrayList<>();
        for(Sentence q: inferenceInput.getQueries()) {
            lastIndexOfNewClauses = 0;
            STANDARD_VARIABLE_COUNT = 0;
            startTime = System.nanoTime();
            boolean valid = ask(inferenceInput.getKB(), q);
            System.out.println(valid);
            result.add(valid);
        }
        writeOutput(result);
    }

    public void writeOutput(List<Boolean> result) {
        try {
            // TODO change output path
            FileWriter writer = new FileWriter("./src/output.txt");
            for (Boolean v: result) {
                writer.write(String.valueOf(v).toUpperCase());
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void standardizeVariable(Sentence s) {
        List<Predicate> predicates = s.getPredicates();
        Map<String, String> map = new HashMap<>();
        for(Predicate p: predicates) {
            for(int i=0; i<p.getArguments().size(); i++) {
                String arg = p.getArguments().get(i);
                if (Utility.isVariable(arg)) {
                    if (map.containsKey(arg)) {
                        p.setArgument(i, map.get(arg));
                    } else {
                        String newVariable = Utility.getStandardVariable(STANDARD_VARIABLE_COUNT++);
                        map.put(arg, newVariable);
                        p.setArgument(i, newVariable);
                    }
                }
            }
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
            if (!conclusionPredicate.getPredicate().equals("False")) {
                sentence.addPredicate(conclusionPredicate);
            }
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

        List<Predicate> predicateList = new ArrayList<>(predicates);
        Collections.sort(predicateList, new PredicateComparator());

        Sentence sentence = new Sentence(predicateList);
        return sentence;
    }

    public Sentence resolution(Sentence s1, Sentence s2) {
        List<Predicate> s1Predicates = s1.getPredicates();
        List<Predicate> s2Predicates = s2.getPredicates();

        for (Predicate p1: s1Predicates) {
            for(Predicate p2: s2Predicates) {
                //System.out.println("resolution | p1 = " + p1.toString() + " ;; p2 = " + p2.toString() );
                if (p1.isExactComplement(p2)) {
                    // p v ~q, q v r = p v r
                    Sentence cloneS1 = new Sentence(s1);
                    Sentence cloneS2 = new Sentence(s2);
                    cloneS1.removePredicate(p1);
                    cloneS2.removePredicate(p2);
                    Sentence result = mergeSentence(cloneS1, cloneS2);
                    return result;
                } else if (p1.isComplement(p2)) {
                    // unification
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isFailure()) {
                        System.out.println("resolution | cannot unify");
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        System.out.println("resolution | Unification is found " + unifier.toString());

                        Sentence cloneS1 = new Sentence(s1);
                        Sentence cloneS2 = new Sentence(s2);
                        cloneS1.removePredicate(p1);
                        cloneS2.removePredicate(p2);

                        Sentence ss1 = substitute(cloneS1, unifier);
                        Sentence ss2 = substitute(cloneS2, unifier);
                        Sentence result = mergeSentence(ss1, ss2);
                        return result;
                    }
                }
            }
        }

        Sentence s = new Sentence();
        s.setFailure(true);
        System.out.println("resolution | failure");
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

            unifyArgument(arg1, arg2, unifier);
        }

        return unifier;
    }

    public void unifyArgument(String arg1, String arg2, Unifier unifier) {
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
            unifyArgument(unifier.getSubstitution(variable), constant, unifier);
        } else if (unifier.getSubstitution(constant) != null) {
            unifyArgument(variable, unifier.getSubstitution(constant), unifier);
        } else {
            unifier.addSubstitution(variable, constant);
        }
    }

    public boolean isTautology(Sentence s) {
        List<Predicate> predicates = s.getPredicates();
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            for(int j=i+1; j<predicates.size(); j++) {
                Predicate p2 = predicates.get(j);
                if (p1.isExactComplement(p2)) {
                    return true;
                }
                if (p1.isComplement(p2)) {
                    // p(x) , ~p(K)
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isEmpty() || unifier.isFailure()) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSubset(List<Sentence> KB, List<Sentence> newClauses) {
        int count = 0;
        //System.out.println("isSubset | newClauses size " + newClauses.size() );
        // start searching from new index that added to newClauses
        for(int i=0; i<newClauses.size(); i++) {
            Sentence s = newClauses.get(i);
            //System.out.println("isSubset | s " + s.toString() );
            if (binarySearch(KB, s, 0, KB.size()-1)) {
                count++;
            }
        }
        return count == newClauses.size();
    }

    public boolean binarySearch(List<Sentence> KB, Sentence s, int start, int end) {
        if (start > end) return false;
        int mid = start + (end-start)/2;
        //System.out.println("binarySearch | mid = " + KB.get(mid));
        if (KB.get(mid).equals(s)) {
            return true;
        }

        if (KB.get(mid).compareTo(s) > 1) {
            return binarySearch(KB, s, start, mid - 1);
        } else {
            return binarySearch(KB, s, mid + 1, end);
        }
    }

    public List<Sentence> difference(List<Sentence> KB, List<Sentence> newClauses) {
        return newClauses.stream()
                .filter(e -> !KB.contains(e))
                .collect(Collectors.toList());
    }

    public Sentence factoring(Sentence s) {
        // e.g. P(x) v P(C) v Q(x) = P(C) v Q(C)
        // System.out.println("factoring | try factoring " + s.toString());
        List<Predicate> predicates = s.getPredicates();
        Sentence cloneS = new Sentence(s);
        cloneS.setFailure(true);
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            for(int j=i+1; j<predicates.size(); j++) {
                Predicate p2 = predicates.get(j);
                if (p1.isSimilar(p2)) {
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isFailure()) {
                        System.out.println("factoring | cannot unify");
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        // P(x) v P(C) converts to P(C)
                        System.out.println("factoring | Unification is found " + unifier.toString());
                        cloneS.removePredicate(p2);
                        Sentence result = substitute(cloneS, unifier);
                        System.out.println("factoring | result: " + result.toString());
                        cloneS = new Sentence(result);
                        cloneS.setFailure(false);
                    } else if (!unifier.isFailure()) {
                        // exactly same predicate: P(x) v P(x), P(John) v P(John), drop one
                        cloneS.removePredicate(p2);
                        System.out.println("factoring | drop " + p2.toString());
                        cloneS.setFailure(false);
                    }
                }
            }
        }
        //System.out.println("factoring | after factoring " + cloneS.toString());
        return cloneS;
    }

    private double getUsedTime() {
        long currentTime = System.nanoTime();
        long usedTime = currentTime - startTime;
        //System.out.println("getUsedTime | " + ((int) usedTime / 1000000000.0));
        return (int) usedTime / 1000000000.0;
    }

    public boolean ask(List<Sentence> KB, Sentence query) {
        List<Sentence> clauses = new ArrayList<>();
        clauses.addAll(KB);
        clauses.add(query);

        Set<Sentence> newClauses = new HashSet<>();
        Set<Pair<Sentence, Sentence>> visited = new HashSet<>();

        int round = 0;

        while (true) {
            round++;
            System.out.println("--------------");
            System.out.println("ROUND: " + round);
           // for(Sentence s: clauses) System.out.println(s.toString());
            System.out.println("--------------");

            if (clauses.size() > LIMIT_KB_SIZE) {
                System.out.println("ask | KB size exceeds the limit, return false");
                return false;
            }

            if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
                System.out.println("ask | Time limit exceed, return false");
                return false;
            }

            for (int i=0; i<clauses.size(); i++) {
                Sentence a = clauses.get(i);
                for (int j=i+1; j<clauses.size(); j++) {
                    if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
                        System.out.println("ask | Time limit exceed, return false");
                        return false;
                    }
                    Sentence b = clauses.get(j);
                    System.out.println("#################");
                    System.out.println("ask | id = " + a.getId() + " a = " + a.toString());
                    System.out.println("ask | id = " + b.getId() +" b = " + b.toString());
                    if (visited.contains(new Pair<>(a, b)) || visited.contains(new Pair<>(b, a))) continue;

                    Sentence result = resolution(a, b);
                    visited.add(new Pair<>(a, b));
                    visited.add(new Pair<>(b, a));
                    System.out.println("ask | resolution result = " + result.toString());

                    if (!result.isFailure() && result.isEmpty()) {
                        // Contradiction
                        return true;
                    }

                    if (result.isFailure()) continue;

                    if (isTautology(result)) {
                        System.out.println("ask | resolution is tautology");
                        continue;
                    }

                    if (!newClauses.contains(result)) {
                        newClauses.add(result);
                        System.out.println("ask | add " + result.toString() + " to newClauses");
                        Sentence factorSentence = factoring(result);
                        if (!factorSentence.isFailure() && !isTautology(factorSentence)) {
                            newClauses.add(factorSentence);
                            System.out.println("ask | [factoring] add " + factorSentence.toString() + " to newClauses");
                        }
                    }
                }
            }

            // if newClauses is subset of KB, return false
            Collections.sort(clauses, new SentenceComparator());
            if (isSubset(clauses, new ArrayList<>(newClauses))) {
                System.out.println("ask | newClauses is subset of KB, return false");
                return false;
            }
            // update only new clauses to KB
            List<Sentence> updateClauses = difference(clauses, new ArrayList<>(newClauses));
            clauses.addAll(updateClauses);
         //for(Sentence s: clauses) System.out.println(s.toString());
        }
        //return false;
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
            isNegative = true;
            predicate = predicate.substring(1);
        }
        this.predicate = predicate;
        arguments = new ArrayList<>();
    }

    public Predicate(Predicate p) {
        this.isNegative = p.isNegative;
        this.predicate = p.predicate;
        this.arguments = new ArrayList<>();
        this.arguments.addAll(p.arguments);
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

    public void setArgument(int index, String value) {
        this.arguments.set(index, value);
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

    public boolean isSimilar(Predicate p2) {
//        if (this.predicate.equals(p2.predicate) && this.isNegative == p2.isNegative) {
//            if (this.arguments.size() != p2.arguments.size()) return false;
//            for(int i=0; i< this.arguments.size(); i++) {
//                if (!Utility.isVariable(this.arguments.get(i)) && !Utility.isVariable(p2.arguments.get(i))) {
//                    return false;
//                }
//            }
//        }
        return this.predicate.equals(p2.predicate) && this.isNegative == p2.isNegative;
    }

    public void substitute(String constant, int index) {
        arguments.remove(index);
        arguments.add(index, constant);
    }

    public int compareTo(Predicate p2) {
        return this.toString().compareTo(p2.toString());
    }
}

class PredicateComparator implements Comparator<Predicate> {
    @Override
    public int compare(Predicate o1, Predicate o2) {
        if (o1.getPredicate().equals(o2.getPredicate())) {
            return o1.toString().compareTo(o2.toString());
        } else {
            return o1.getPredicate().compareTo(o2.getPredicate());
        }
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

    public Sentence(Sentence s) {
        this.id = s.id;
        this.isFailure = s.isFailure;
        this.predicates = new ArrayList<>();
        Iterator<Predicate> it = s.getPredicates().iterator();
        while (it.hasNext()) {
            predicates.add(new Predicate(it.next()));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<predicates.size(); i++) {
            sb.append(predicates.get(i).toString());
            if (i != predicates.size()-1) sb.append(" | ");
        }
        return sb.toString();
    }

    public boolean equals(Sentence s) {
        if (this == s) return true;
        return this.toString().equals(s.toString());
    }

    public int hashCode() {
        return 31 + (int) id;
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

    public boolean isEmpty() {
        return predicates.size() == 0;
    }

    public void removePredicate(Predicate p) {
        for(Predicate predicate: predicates) {
            if (p.toString().equals(predicate.toString())) {
                this.predicates.remove(predicate);
                return;
            }
        }
    }

    public int compareTo(Sentence s2) {
        return this.toString().compareTo(s2.toString());
    }
}

class SentenceComparator implements Comparator<Sentence> {

    @Override
    public int compare(Sentence o1, Sentence o2) {
        return (int) (o1.getId() - o2.getId());
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

class ResolutionResult {
    private boolean isContradiction = false;
    private List<Sentence> inferredSentence;

    public ResolutionResult() {
        this.inferredSentence = new ArrayList<>();
    }

    public boolean isContradiction() {
        return isContradiction;
    }

    public void setContradiction(boolean contradiction) {
        isContradiction = contradiction;
    }

    public List<Sentence> getInferredSentence() {
        return inferredSentence;
    }

    public void setInferredSentence(List<Sentence> inferredSentence) {
        this.inferredSentence = inferredSentence;
    }
}

class Utility {
    public static boolean isVariable(String argument) {
        return Character.isLowerCase(argument.charAt(0));
    }

    public static String getStandardVariable(int count) {
        if (count < 26) {
            return Character.toString((char)(count + 'a'));
        }
        int a = 'a';
        StringBuilder sb = new StringBuilder();
        while (count >= 26) {
            int mod = count % 26;
            sb.append((char) (mod + a));
            count = count / 26;
        }

        sb.append((char) ((count-1) + a));
        return sb.toString();
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