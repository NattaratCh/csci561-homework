import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nuning on 3/27/21.
 */
public class Homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases/input4.txt", "./src/output.txt");
        inferenceSystem.startInference();
    }
}

class InferenceSystem {
    private InferenceInput inferenceInput;
    private int LIMIT_KB_SIZE = 8000;
    private final double LIMIT_TIME_IN_SECONDS = 200.0;
    private final SentenceComparator sc = new SentenceComparator();
    private long startTime = 0;
    private String outputFileName;

    public InferenceSystem(String filename, String outputFileName) {
        this.outputFileName = outputFileName;
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
                for (Predicate p: s.getPredicates()) {
                    // Add negation of query to proof by contradiction
                    s.removePredicate(p);
                    p.negate();
                    s.addPredicate(p);
                }
                inferenceInput.addQuery(s);
            }

            Integer numberOfKB = Integer.valueOf(reader.readLine().trim());
            String line = reader.readLine();
            int n = 1;
            while (line != null){
                String sentence = line.trim();
                Sentence s = parseSentence(sentence);
                standardizeVariable(s, n);
                inferenceInput.addKB(s);
//                Sentence factorSentence = factoring(s);
//                if (!factorSentence.isFailure() && !isTautology(factorSentence)) {
//                    inferenceInput.addKB(factorSentence);
//                }
                line = reader.readLine();
                n++;
            }

            return inferenceInput;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("readInput: " + e.getMessage());
            return null;
        }
    }

    public void startInference() {
        resetOutput();
        for(Sentence q: inferenceInput.getQueries()) {
            boolean valid = ask(new HashSet<>(inferenceInput.getKB()), q);
            System.out.println(valid);
            writeOutput(valid);
        }
    }

    public void resetOutput() {
        File file = new File(outputFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    public void writeOutput(boolean result) {
        try {
            FileWriter writer = new FileWriter(outputFileName, true);
            writer.write(String.valueOf(result).toUpperCase());
            writer.write(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void standardizeVariable(Sentence s, int n) {
        Set<Predicate> predicates = s.getPredicates();
        for(Predicate p: predicates) {
            for(int i=0; i<p.getArguments().size(); i++) {
                String arg = p.getArguments().get(i);
                if (Utility.isVariable(arg)) {
                    p.setArgument(i, arg + "" + n);
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

        //sentence.sortPredicates();
        return sentence;
    }

    public Predicate parsePredicate(String predicateStr) {
        int open = predicateStr.indexOf("(");
        int close = predicateStr.indexOf(")");
        String predicate = predicateStr.substring(0, open);
        Predicate p = new Predicate(predicate.trim());
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

        //List<Predicate> predicateList = new ArrayList<>(predicates);

        Sentence sentence = new Sentence(predicates);
        //sentence.sortPredicates();
        return sentence;
    }

    public ResolutionResult resolution(Sentence s1, Sentence s2) {
        ResolutionResult resolutionResult = new ResolutionResult();

        for (Predicate p1: s1.getPredicates()) {
            for(Predicate p2: s2.getPredicates()) {
                final Unifier unifier = new Unifier();
                unifier.setFailure(true);
                if (p1.isNegative() ^ p2.isNegative() && p1.getPredicate().equals(p2.getPredicate())) {
                    unify(p1, p2, unifier);
                }

                if (unifier.isFailure()) {
                    continue;
                } else {
                    Sentence cloneS1 = new Sentence(s1, false);
                    Sentence cloneS2 = new Sentence(s2, false);
                    List<Predicate> restPredicates1 = cloneS1.getPredicates().stream().filter(x -> !x.equals(p1)).collect(Collectors.toList());
                    List<Predicate> restPredicates2 = cloneS2.getPredicates().stream().filter(x -> !x.equals(p2)).collect(Collectors.toList());

                    if (restPredicates1.isEmpty() && restPredicates2.isEmpty()) {
                        // contradiction
                        resolutionResult.setContradiction(true);
                        return resolutionResult;
                    }

                    restPredicates1 = restPredicates1.stream().map(x -> x.substitute(unifier)).collect(Collectors.toList());
                    restPredicates2 = restPredicates2.stream().map(x -> x.substitute(unifier)).collect(Collectors.toList());

                    Set<Predicate> newPredicates = new HashSet<>();
                    newPredicates.addAll(restPredicates1);
                    newPredicates.addAll(restPredicates2);
                    Sentence result = new Sentence(newPredicates);
                    resolutionResult.addInferredSentence(result);
                }
            }
        }
        return resolutionResult;
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

    public Unifier unify(Predicate p1, Predicate p2, Unifier unifier) {
        if (!p1.getPredicate().equals(p2.getPredicate()) && p1.getArguments().size() != p2.getArguments().size()) {
            unifier.setFailure(true);
            return unifier;
        }

        unifier.setFailure(false);

        for(int i=0; i<p1.getArguments().size(); i++) {
            String arg1 = p1.getArguments().get(i);
            String arg2 = p2.getArguments().get(i);


            unifyArgument(arg1, arg2, unifier);
        }

        return unifier;
    }

    public void unifyArgument(String arg1, String arg2, Unifier unifier) {
        //System.out.println("unifyArgument " + arg1 + " " + arg2);
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

    public void unifyVariable(String variable, String x, Unifier unifier) {
        String s1 = unifier.getSubstitution(variable);
        String s2 = unifier.getSubstitution(x);

        if (s1 != null) {
            unifyArgument(s1, x, unifier);
        } else if (s2 != null) {
            unifyArgument(variable, s2, unifier);
        } else {
            unifier.addSubstitution(variable, x);
        }
    }

    public Set<Sentence> difference(Set<Sentence> KB, Set<Sentence> newClauses) {
        return newClauses.stream()
                .filter(e -> !KB.contains(e))
                .collect(Collectors.toSet());
    }

    private double getUsedTime() {
        long currentTime = System.nanoTime();
        long usedTime = currentTime - startTime;
        //System.out.println("getUsedTime | " + (usedTime / 1000000000.0));
        return usedTime / 1000000000.0;
    }

    private void addToKB(Set<Sentence> KB, Sentence s, Map<String, Set<Sentence>> kbMap) {
        KB.add(s);
        addNewSentenceToTableBasedIndex(s, kbMap);
    }

    private void processTableBasedIndexing(Set<Sentence> KB, Map<String, Set<Sentence>> kbMap) {
        for(Sentence s: KB) {
            addNewSentenceToTableBasedIndex(s, kbMap);
        }
    }

    private void addNewSentenceToTableBasedIndex(Sentence s, Map<String, Set<Sentence>> kbMap) {
        for (Predicate p: s.getPredicates()) {
            Set<Sentence> sentences = kbMap.getOrDefault(p.getPredicate(), new HashSet<>());
            sentences.add(s);
            kbMap.put(p.getPredicate(), sentences);
        }
    }

    public Set<Sentence> getResolvingClauses(Sentence s, Map<String, Set<Sentence>> kbMap) {
        Set<Sentence> resolvingClauses = new HashSet<>();
        for (Predicate p : s.getPredicates()) {
            Set<Sentence> clauses = kbMap.getOrDefault(p.getPredicate(), null);
            if (clauses == null) {
                continue;
            }
            resolvingClauses.addAll(clauses);
        }
        return resolvingClauses;
    }

    private void prepareKB(Set<Sentence> KB, Sentence query, Map<String, Set<Sentence>> kbMap) {
        KB.add(query);
        processTableBasedIndexing(KB, kbMap);
        //Collections.sort(KB, sc);
    }

    public boolean ask(Set<Sentence> KB, Sentence query) {
        Map<String, Set<Sentence>> kbMap = new HashMap<>();
        Set<Sentence> newKB = new HashSet<>();
        addToKB(newKB, query, kbMap);
        addToKB(KB, query, kbMap);

        Set<Sentence> newClauses;
        Set<Pair<Sentence, Sentence>> visited = new HashSet<>();
        Map<Sentence, Set<Sentence>> history;

        startTime = System.nanoTime();
        int round = 0;

        while (true) {
            history = new HashMap<>();
            newClauses = new HashSet<>();
            round += 1;
            System.out.println("--------------");
            System.out.println("ROUND: " + round);
            System.out.println("--------------");

            if (KB.size() > LIMIT_KB_SIZE) {
                System.out.println("ask | KB size exceeds the limit, return false");
                return false;
            }

//            if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
//                System.out.println("ask | Time limit exceed, return false");
//                return false;
//            }

            for (Sentence a: KB) {

                Set<Sentence> resolvingClauses = getResolvingClauses(a, kbMap);
                for (Sentence b : resolvingClauses) {
//                    if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
//                        System.out.println("ask | Time limit exceed, return false");
//                        return false;
//                    }

                    if (a.getId() == b.getId()) {
                        // Avoid resolution with itself
                        continue;
                    }

                    boolean flagA = false;
                    boolean flagB = false;
                    if (history.containsKey(b)) {
                        flagA = true;
                        Set<Sentence> bHistory = history.get(b);
                        if (bHistory.contains(a)) {
                            bHistory.remove(a);
                            history.put(b, bHistory);
                            continue;
                        }
                    }

                    if (history.containsKey(a)) {
                        flagB = true;
                        Set<Sentence> aHistory = history.get(a);
                        if (aHistory.contains(b)) {
                            aHistory.remove(b);
                            history.put(a, aHistory);
                            continue;
                        }
                    }

                    // update history
                    if (flagB) {
                        history.get(a).add(b);
                    } else {
                        Set<Sentence> set = new HashSet<>();
                        set.add(b);
                        history.put(a, set);
                    }

//                    if (visited.contains(new Pair<>(a, b)) || visited.contains(new Pair<>(b, a))) {
//                        //System.out.println("ask | Visited skip");
//                        continue;
//                    }

                    ResolutionResult result = resolution(a, b);
                    visited.add(new Pair<>(a, b));
                    visited.add(new Pair<>(b, a));

                    if (result.isContradiction()) {
                        // Contradiction
                        System.out.println("ask | Contradiction round: " + round);
                        System.out.println("ask | KB size = " + KB.size());
                        System.out.println("ask | time = " + getUsedTime());
                        System.out.println("ask | id = " + a.getId() + " a = " + a.toString());
                        System.out.println("ask | id = " + b.getId() + " b = " + b.toString());
                        return true;
                    }

                    // if (result.isFailure()) continue;

                    if (result.getInferredSentences().isEmpty()) {
                        continue;
                    }

                    System.out.println("#################");
                    System.out.println("ask | id = " + a.getId() + " a = " + a.toString());
                    System.out.println("ask | id = " + b.getId() + " b = " + b.toString());
//                    System.out.println("ask | resolution result = " + result.isFailure() + ", " + result.toString());

                    newClauses.addAll(result.getInferredSentences());
//                    for (Sentence s: result.getInferredSentences()) {
//                        if (!newClauses.contains(s)) {
//                            newClauses.add(s);
//                            System.out.println("ask | add " + s.toString() + " to newClauses");
//                        }
//                    }
                }
            }

            // if newClauses is subset of KB, return false
            //if (isSubset(KB, new ArrayList<>(newClauses))) {
            if (KB.containsAll(newClauses)) {
                System.out.println("ask | newClauses is subset of KB, return false");
                return false;
            }
            // update only new clauses to KB
            newClauses = difference(KB, newClauses);
            System.out.println("ask | updateClauses size = " + newClauses.size());

            // reset kbMap to contains only newly inferred clauses
            kbMap = new HashMap<>();
            newKB = new HashSet<>();
            if (!newClauses.isEmpty()) {
                for (Sentence s : newClauses) {
                    addToKB(newKB, s, kbMap);
                }
                KB.addAll(newClauses);
            }
            writeKB(newClauses, round);
        }
    }

    private void writeKB(Set<Sentence> KB, int round) {
        System.out.println("writeKB");
        File file = new File("./src/KB.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write("################## ROUND: " +round + "#####################");
            br.write(System.lineSeparator());
            br.write("new clauses size: " + KB.size());
            br.write(System.lineSeparator());
            for(Sentence s: KB) {
                br.write(s.getId() + " " + s.toString());
                br.write(System.lineSeparator());
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                if (!a.equals(b)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isPredicateComplement(Predicate p2) {
        if (predicate.equals(p2.predicate) && isNegative == !p2.isNegative() && arguments.size() == p2.getArguments().size()) {
            for(int i=0; i<arguments.size(); i++) {
                String a = arguments.get(i);
                String b = p2.getArguments().get(i);
                // P(x), ~P(x)
                // P(x), ~P(John)
                // P(John), ~P(John)
                if (!Utility.isVariable(a) && !Utility.isVariable(b) && !a.equals(b)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isSimilar(Predicate p2) {
        if (this.predicate.equals(p2.predicate) && this.isNegative == p2.isNegative) {
            if (this.arguments.size() != p2.arguments.size()) return false;
            for(int i=0; i< this.arguments.size(); i++) {
                if (!Utility.isVariable(this.arguments.get(i)) && !Utility.isVariable(p2.arguments.get(i)) &&
                        !this.arguments.get(i).equals(p2.getArguments().get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
        //return this.predicate.equals(p2.predicate) && this.isNegative == p2.isNegative;
    }

    public boolean isEqualPredicate(Predicate p2) {
        if (predicate.equals(p2.getPredicate())) {
            for(int i=0; i<arguments.size(); i++) {
                if (!arguments.get(i).equals(p2.getArguments().get(i))) return false;
            }
            return true;
        }
        return false;
    }

    public void substitute(String constant, int index) {
        arguments.remove(index);
        arguments.add(index, constant);
    }

    public Predicate substitute(Unifier unifier) {
        for (int i=0; i<arguments.size(); i++) {
            String arg = arguments.get(i);
            if (unifier.getSubstitution(arg) != null) {
                arguments.remove(i);
                arguments.add(i, unifier.getSubstitution(arg));
            }
        }
        return this;
    }

    public int compareTo(Predicate p2) {
        if (predicate.equals(p2.getPredicate())) {
            return this.toString().compareTo(p2.toString());
        } else {
            return predicate.compareTo(p2.getPredicate());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof  Predicate)) return false;
        Predicate p = (Predicate) o;
        if (!this.predicate.equals(p.getPredicate())) return false;
        if (this.isNegative != p.isNegative()) return false;
        if (this.arguments.size() != p.getArguments().size()) return false;

        for(int i=0; i<this.arguments.size(); i++) {
            String arg1 = this.arguments.get(i);
            String arg2 = p.getArguments().get(i);
            //if (!Utility.isVariable(arg1) && !Utility.isVariable(arg2) && !arg1.equals(arg2)) return false;
            if (!arg1.equals(arg2)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}

class Sentence {
    private static long runningID = 1;
    private long id;
    private Set<Predicate> predicates = new HashSet<>();
    private boolean isFailure = false;

    public Sentence() {
        id = runningID++;
        predicates = new HashSet<>();
    }

    public Sentence(Set<Predicate> predicates) {
        id = runningID++;
        this.predicates.addAll(predicates);
    }

    public Sentence(Sentence s, boolean isNew) {
        this.id = isNew ? runningID++ : s.id;
        this.isFailure = s.isFailure;
        Iterator<Predicate> it = s.getPredicates().iterator();
        while (it.hasNext()) {
            Predicate p = new Predicate(it.next());
            predicates.add(p);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Predicate p: predicates) {
            sb.append(p.toString());
            if (i != predicates.size()-1) sb.append(" | ");
            i++;
        }
//        for(int i=0; i<predicates.size(); i++) {
//            sb.append(predicates.get(i).toString());
//            if (i != predicates.size()-1) sb.append(" | ");
//        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!( o instanceof  Sentence)) return false;
        Sentence s = (Sentence) o;
//        if (predicates.size() != ((Sentence) o).getPredicates().size()) return false;
//        for(int i=0; i<predicates.size(); i++) {
//            Predicate p1 = predicates.get(i);
//            Predicate p2 = s.getPredicates().get(i);
//            if (!p1.equals(p2)) return false;
//        }
//        return true;
        return this.predicates.equals(((Sentence) o).getPredicates());
    }

    @Override
    public int hashCode() {
        char[] array = this.toString().toCharArray();
        Arrays.sort(array);
        String sortString = new String(array);
        return sortString.hashCode();
        //return this.toString().hashCode();
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

    public Set<Predicate> getPredicates() {
        return predicates;
    }

    public boolean isEmpty() {
        return predicates.size() == 0;
    }

    public void removePredicate(Predicate p) {
        predicates.remove(p);
//        for(int i=0; i<predicates.size(); i++) {
//            Predicate predicate = predicates.get(i);
//            if (p.toString().equals(predicate.toString())) {
//                this.predicates.remove(i);
//            }
//        }

    }

    public int compareTo(Sentence s2) {
        return this.toString().compareTo(s2.toString());
    }
}

class SentenceComparator implements Comparator<Sentence> {

    @Override
    public int compare(Sentence o1, Sentence o2) {
        return o1.compareTo(o2);
    }
}

class Unifier {
    private Map<String, String> substitution;
    private boolean isFailure = false;

    public Unifier() {
        substitution = new HashMap<>();
    }

    public Unifier(boolean isFailure) {
        this.isFailure = isFailure;
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

class ResolutionResult {
    private boolean isContradiction = false;
    private Set<Sentence> inferredSentences;

    public ResolutionResult() {
        inferredSentences = new HashSet<>();
    }

    public void addInferredSentence(Sentence s) {
        inferredSentences.add(s);
    }

    public boolean isContradiction() {
        return isContradiction;
    }

    public void setContradiction(boolean isContradiction) {
        this.isContradiction = isContradiction;
    }

    public Set<Sentence> getInferredSentences() {
        return inferredSentences;
    }
}

class InferenceInput {
    private Set<Sentence> KB;
    private List<Sentence> queries;

    public InferenceInput() {
        KB = new HashSet<>();
        queries = new ArrayList<>();
    }

    public void addKB(Sentence sentence) {
        KB.add(sentence);
    }

    public void addQuery(Sentence sentence) {
        queries.add(sentence);
    }

    public Set<Sentence> getKB() {
        return KB;
    }

    public List<Sentence> getQueries() {
        return queries;
    }

    public void print() {
        System.out.println("====== Queries ======");
        for(Sentence s: queries) {
            System.out.println(s.getId() + " " + s.toString());
        }

        System.out.println("====== KB ======");
        for(Sentence s: KB) {
            System.out.println(s.getId() + " " + s.toString());
        }
    }
}


class Utility {
    public static boolean isVariable(String argument) {
        return Character.isLowerCase(argument.charAt(0));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof  Pair)) return false;
        Pair<U, V> p = (Pair) o;
        return first.equals(p.getFirst()) && second.equals(p.getSecond());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (first != null ? first.hashCode() : 0);
        hash = 79 * hash + (second != null ? second.hashCode() : 0);
        return hash;
    }
}