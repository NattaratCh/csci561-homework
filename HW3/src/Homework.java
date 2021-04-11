import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nuning on 3/27/21.
 */
public class Homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases/input4.txt");
        List<Boolean> result = inferenceSystem.startInference();
        inferenceSystem.writeOutput(result, "./src/output.txt");
    }
}

class InferenceSystem {
    private InferenceInput inferenceInput;
    private int LIMIT_KB_SIZE = 8000;
    private final double LIMIT_TIME_IN_SECONDS = 300.0;
    private final SentenceComparator sc = new SentenceComparator();
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
                    Predicate p = s.getPredicates().get(0);
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

    public List<Boolean> startInference() {
        List<Boolean> result = new ArrayList<>();
        LIMIT_KB_SIZE = inferenceInput.getKB().size() * 1000;
        for(Sentence q: inferenceInput.getQueries()) {
            List<Sentence> KB = new ArrayList<>(inferenceInput.getKB());
            boolean valid = ask(KB, q);
            System.out.println(valid);
            result.add(valid);
        }
        return result;
    }

    public void writeOutput(List<Boolean> result, String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            for (Boolean v: result) {
                writer.write(String.valueOf(v).toUpperCase());
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void standardizeVariable(Sentence s, int n) {
        List<Predicate> predicates = s.getPredicates();
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

        sentence.sortPredicates();
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

        Sentence sentence = new Sentence(predicateList);
        sentence.sortPredicates();
        return sentence;
    }

    private void resolutionLog(Sentence s1, Sentence s2) {
        File file = new File("./src/resolution.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(s1.toString());
            br.write(System.lineSeparator());
            br.write(s2.toString());
            br.write(System.lineSeparator());
            br.write("===========================");
            br.write(System.lineSeparator());

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Sentence resolution(Sentence s1, Sentence s2) {
        List<Predicate> s1Predicates = s1.getPredicates();
        List<Predicate> s2Positives = s2.getPositivePredicates();
        List<Predicate> s2Negatives = s2.getNegativePredicates();
        List<Predicate> s2Predicates;
        //resolutionLog(s1, s2);

        for (Predicate p1: s1Predicates) {
            s2Predicates = p1.isNegative() ? s2Positives : s2Negatives;
            for(Predicate p2: s2Predicates) {
                //System.out.println("resolution | p1 = " + p1.toString() + " ;; p2 = " + p2.toString() );
                if (p1.isComplement(p2)) {
                    // exact complement
                    // p(x) v ~q(x), q(x) v r(x) = p v r
                    Sentence cloneS1 = new Sentence(s1, false);
                    Sentence cloneS2 = new Sentence(s2, false);
                    cloneS1.removePredicate(p1);
                    cloneS2.removePredicate(p2);
                    Sentence result = mergeSentence(cloneS1, cloneS2);
                    return result;
                } else if (p1.isPredicateComplement(p2)) {
                    // unification
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isFailure()) {
                        System.out.println("resolution | cannot unify");
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        System.out.println("resolution | Unification is found " + unifier.toString());

                        Sentence cloneS1 = new Sentence(s1, false);
                        Sentence cloneS2 = new Sentence(s2, false);
                        cloneS1.removePredicate(p1);
                        cloneS2.removePredicate(p2);
//                        System.out.println("resolution | cloneS1 " + cloneS1.toString());
//                        System.out.println("resolution | cloneS2 " + cloneS2.toString());

                        Sentence ss1 = substitute(cloneS1, unifier);
                        Sentence ss2 = substitute(cloneS2, unifier);
                        Sentence result = mergeSentence(ss1, ss2);
//                        System.out.println("resolution | result " + result.toString());
                        return result;
                    }
                }
            }
        }

        Sentence s = new Sentence();
        s.setFailure(true);
        //System.out.println("resolution | failure");
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
        if (!p1.getPredicate().equals(p2.getPredicate())) {
            return new Unifier(true);
        };
        Unifier unifier = new Unifier();

        if (unifier.isFailure()) return unifier;
        if (p1.isEqualPredicate(p2)) return unifier;

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
            // { x/y, Constant/y }
            unifyVariable(arg2, arg1, unifier);
        } else {
            unifier.setFailure(false);
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

//    public void unifyVariable(String variable, String constant, Unifier unifier) {
//        String s1 = unifier.getSubstitution(variable);
//        String s2 = unifier.getSubstitution(constant);
//        System.out.println("unifyVariable | " + variable + " " + constant + " " + s1 + " " + s2);
//        if (s1 != null && s2 != null) {
//            // both variables have substitution
//            // TODO
//            System.out.println("unifyVariable | both variables have substitution");
//            return;
//        } else if (s1 == null && s2 == null) {
//            // no substitution for this variable -> add
//            System.out.println("unifyVariable | add substitution");
//            unifier.addSubstitution(variable, constant);
//            return;
//        } else if (s1 != null) {
//            if (!Utility.isVariable(s1) && !Utility.isVariable(constant)) {
//                if (s1.equals(constant)) return;
//                // both s1 and constant are constant but not equal
//                System.out.println("unifyVariable | both s1 and constant are constant but not equal");
//                unifier.setFailure(true);
//                return;
//            } else if (!Utility.isVariable(s1) && Utility.isVariable(constant)) {
//                System.out.println("unifyVariable | add Substitution21");
//                // variable = x, s1 = John, constant = y
//                // { x/y, x/John } => { y/John }
//                unifier.addSubstitution(constant, s1);
//                return;
//            } else if (Utility.isVariable(s1) && !Utility.isVariable(constant)) {
//                System.out.println("unifyVariable | add Substitution2");
//                // variable = x, s1 = y, constant = Joe
//                // { x/y, x/John } => { y/John }
//                unifier.addSubstitution(s1, constant);
//                return;
//            } else {
//                System.out.println("both s1 and constant are variables");
//                // both s1 and constant are variables
//                // variable = x, s1 = y, constant = z
//                // { x/y, x/z } => { y/z }
//                unifier.addSubstitution(s1, constant);
//                return;
//            }
//        } else if (s2 != null) {
//            // constant is also variable and its substitution exists
//            // recursive substitution
//            unifyArgument(variable, s2, unifier);
//        }
//    }

    public boolean isTautology(Sentence s) {
        for(Predicate p1: s.getPositivePredicates()) {
            for(Predicate p2: s.getNegativePredicates()) {
                if (p1.isComplement(p2)) {
                    return true;
                }
                if (p1.isPredicateComplement(p2)) {
                    // p(x) , ~p(K)
                    Unifier unifier = unify(p1, p2);
                    System.out.println("isTautology | " + p1 + " ## " + p2 + " unifier = " + unifier.isFailure() + ", " + unifier.toString());
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
            // if (binarySearch(KB, s, 0, KB.size()-1)) {
            if (!KB.contains(s)) {
                //count++;
                return false;
            }
        }
        //return count == newClauses.size();
        return true;
    }

    public boolean binarySearch(List<Sentence> KB, Sentence s, int start, int end) {
        //System.out.println("binarySearch | range " + start + " " + end);
        if (start > end) return false;
        int mid = start + (end-start)/2;
        //System.out.println("binarySearch | s = " + s.toString() + " mid = " + KB.get(mid));
        if (KB.get(mid).equals(s)) {
            return true;
        }

        if (KB.get(mid).compareTo(s) > 1) {
            //System.out.println("binarySearch | start to mid-1");
            return binarySearch(KB, s, start, mid - 1);
        } else {
            //System.out.println("binarySearch | mid+1 to end");
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
        Sentence cloneS = new Sentence(s, true);
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            for(int j=i+1; j<predicates.size(); j++) {
                Predicate p2 = predicates.get(j);
                if (p1.isSimilar(p2)) {
                    Unifier unifier = unify(p1, p2);
                    if (unifier.isFailure()) {
                        System.out.println("factoring | cannot unify");
                        cloneS.setFailure(true);
                        return cloneS;
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        // P(x) v P(C) converts to P(C)
                        System.out.println("factoring | Unification is found " + unifier.toString());
                        cloneS.removePredicate(p2);
                        Sentence result = substitute(cloneS, unifier);
                        result.sortPredicates();
                        System.out.println("factoring | result: " + result.toString());
                        return cloneS;
                    }

//                    else if (!unifier.isFailure()) {
//                        // exactly same predicate: P(x) v P(x), P(John) v P(John), drop one
//                        s.removePredicate(p2);
//                        System.out.println("factoring | drop " + p2.toString());
//                        cloneS.setFailure(false);
//                    }
                }
            }
        }
        //System.out.println("factoring | after factoring " + cloneS.toString());
        cloneS.setFailure(true);
        return cloneS;
    }

    private double getUsedTime() {
        long currentTime = System.nanoTime();
        long usedTime = currentTime - startTime;
        //System.out.println("getUsedTime | " + (usedTime / 1000000000.0));
        return usedTime / 1000000000.0;
    }

    private void addToKB(List<Sentence> KB, Sentence s, Map<String, TableBasedIndex> kbMap) {
        KB.add(s);
        addNewSentenceToTableBasedIndex(s, kbMap);
    }

    private void processTableBasedIndexing(List<Sentence> KB, Map<String, TableBasedIndex> kbMap) {
        for(Sentence s: KB) {
            addNewSentenceToTableBasedIndex(s, kbMap);
        }
    }

    private void addNewSentenceToTableBasedIndex(Sentence s, Map<String, TableBasedIndex> kbMap) {
        for (Predicate p: s.getPredicates()) {
            TableBasedIndex table = kbMap.getOrDefault(p.getPredicate(), new TableBasedIndex());
            if (p.isNegative()) {
                table.addNegatives(s);
            } else {
                table.addPositives(s);
            }
            kbMap.put(p.getPredicate(), table);
        }
    }

    public List<Sentence> getResolvingClauses(Sentence s, Map<String, TableBasedIndex> kbMap) {
        List<Sentence> resolvingClauses = new ArrayList<>();
        for (Predicate p : s.getPredicates()) {
            TableBasedIndex tableBasedIndex = kbMap.getOrDefault(p.getPredicate(), null);
            if (tableBasedIndex == null) {
                continue;
            }
            List<Sentence> clauses = p.isNegative() ? tableBasedIndex.getPositives() : tableBasedIndex.getNegatives();
            resolvingClauses.addAll(clauses);
        }
        return resolvingClauses;
    }

    private void prepareKB(List<Sentence> KB, Sentence query, Map<String, TableBasedIndex> kbMap) {
        KB.add(query);
        processTableBasedIndexing(KB, kbMap);
        //Collections.sort(KB, sc);
    }

    public boolean ask(List<Sentence> KB, Sentence query) {
//        List<Sentence> clauses = new ArrayList<>();
//        clauses.addAll(KB);
//        clauses.add(query);
//        Collections.sort(clauses, sc);
//        processTableBasedIndexing(clauses);
        Map<String, TableBasedIndex> kbMap = new HashMap<>();
        prepareKB(KB, query, kbMap);

        List<Sentence> newClauses;
        List<Sentence> newKB = new ArrayList<>();
        Set<Pair<Sentence, Sentence>> visited = new HashSet<>();

        startTime = System.nanoTime();
        int round = 0;

        while (true) {
            newClauses = new ArrayList<>();
            round += 1;
            System.out.println("--------------");
            System.out.println("ROUND: " + round);
            System.out.println("--------------");

            if (newKB.size() > LIMIT_KB_SIZE) {
                System.out.println("ask | KB size exceeds the limit, return false");
                return false;
            }

//            if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
//                System.out.println("ask | Time limit exceed, return false");
//                return false;
//            }

            for (int i=0; i<KB.size(); i++) {
                Sentence a = KB.get(i);

                List<Sentence> resolvingClauses = getResolvingClauses(a, kbMap);
                for (Sentence b : resolvingClauses) {
                    if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
                        System.out.println("ask | Time limit exceed, return false");
                        return false;
                    }

                    if (a.getId() == b.getId()) {
                        // Avoid resolution with itself
                        continue;
                    }
                    if (visited.contains(new Pair<>(a, b)) || visited.contains(new Pair<>(b, a))) {
                        //System.out.println("ask | Visited skip");
                        continue;
                    }
                    ;

                    Sentence result = resolution(a, b);
                    visited.add(new Pair<>(a, b));
                    visited.add(new Pair<>(b, a));

                    if (!result.isFailure() && result.isEmpty()) {
                        // Contradiction
                        System.out.println("ask | Contradiction round: " + round);
                        System.out.println("ask | KB size = " + KB.size());
                        System.out.println("ask | time = " + getUsedTime());
                        System.out.println("ask | id = " + a.getId() + " a = " + a.toString());
                        System.out.println("ask | id = " + b.getId() + " b = " + b.toString());
                        return true;
                    }

                    if (result.isFailure()) continue;

                    System.out.println("#################");
                    System.out.println("ask | id = " + a.getId() + " a = " + a.toString());
                    System.out.println("ask | id = " + b.getId() + " b = " + b.toString());
                    System.out.println("ask | resolution result = " + result.isFailure() + ", " + result.toString());

                    if (isTautology(result)) {
                        System.out.println("ask | resolution is tautology");
                        continue;
                    }

                    if (!newClauses.contains(result)) {
                        newClauses.add(result);
                        System.out.println("ask | add " + result.toString() + " to newClauses");
//                        Sentence factorSentence = factoring(result);
//                        if (!factorSentence.isFailure() && !isTautology(factorSentence)) {
//                            newClauses.add(factorSentence);
//                            System.out.println("ask | [factoring] add " + factorSentence.toString() + " to newClauses");
//                        }
                    }
                }
            }

            // if newClauses is subset of KB, return false
            //if (isSubset(KB, new ArrayList<>(newClauses))) {
            if (KB.containsAll(newClauses)) {
                System.out.println("ask | newClauses is subset of KB, return false");
                return false;
            }
            // update only new clauses to KB
            List<Sentence> updateClauses = difference(KB, new ArrayList<>(newClauses));
            System.out.println("ask | updateClauses size = " + updateClauses.size());

            // reset kbMap to contains only newly inferred clauses
            kbMap = new HashMap<>();
            if (!updateClauses.isEmpty()) {
                for(Sentence s: updateClauses) {
                    addToKB(KB, s, kbMap);
                }
                newKB.addAll(updateClauses);
//                KB.addAll(updateClauses);
//                processTableBasedIndexing(updateClauses);
                //Collections.sort(KB, sc);
            }

//         System.out.println("*******");
//         for(Sentence s: KB) {
//             System.out.println(s.getId() + " " +s.toString());
//         }
////            Collections.sort(clauses, sc);
            writeKB(updateClauses, round);
        }
        //return false;
    }

    private void writeKB(List<Sentence> KB, int round) {
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
            if (!Utility.isVariable(arg1) && !Utility.isVariable(arg2) && !arg1.equals(arg2)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}

class PredicateComparator implements Comparator<Predicate> {
    @Override
    public int compare(Predicate o1, Predicate o2) {
        return o1.compareTo(o2);
    }
}

class Sentence {
    private static long runningID = 1;
    private final PredicateComparator pc = new PredicateComparator();
    private long id;
    private List<Predicate> predicates = new ArrayList<>();
    private List<Predicate> positivePredicates = new ArrayList<>();
    private List<Predicate> negativePredicates = new ArrayList<>();
    private boolean isFailure = false;

    public Sentence() {
        id = runningID++;
        predicates = new ArrayList<>();
        positivePredicates = new ArrayList<>();
        negativePredicates = new ArrayList<>();
    }

    public Sentence(List<Predicate> predicates) {
        id = runningID++;
        this.predicates.addAll(predicates);
        for(Predicate p: predicates) {
            if (p.isNegative()) {
                negativePredicates.add(p);
            } else {
                positivePredicates.add(p);
            }
        }
    }

    public Sentence(Sentence s, boolean isNew) {
        this.id = isNew ? runningID++ : s.id;
        this.isFailure = s.isFailure;
        Iterator<Predicate> it = s.getPredicates().iterator();
        while (it.hasNext()) {
            Predicate p = new Predicate(it.next());
            predicates.add(p);
            if (p.isNegative()) {
                negativePredicates.add(p);
            } else {
                positivePredicates.add(p);
            }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!( o instanceof  Sentence)) return false;
        Sentence s = (Sentence) o;
        if (predicates.size() != ((Sentence) o).getPredicates().size()) return false;
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            Predicate p2 = s.getPredicates().get(i);
            if (!p1.equals(p2)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public void addPredicate(Predicate predicate) {
        predicates.add(predicate);
        if (predicate.isNegative()) {
            negativePredicates.add(predicate);
        } else {
            positivePredicates.add(predicate);
        }
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

    public void sortPredicates() {
        Collections.sort(predicates, pc);
    }

    public boolean isEmpty() {
        return predicates.size() == 0;
    }

    public void removePredicate(Predicate p) {
        for(int i=0; i<predicates.size(); i++) {
            Predicate predicate = predicates.get(i);
            if (p.toString().equals(predicate.toString())) {
                this.predicates.remove(i);
            }
        }

        if (p.isNegative()) {
            for(int i=0; i<negativePredicates.size(); i++) {
                Predicate predicate = negativePredicates.get(i);
                if (p.toString().equals(predicate.toString())) {
                    negativePredicates.remove(i);
                }
            }
        } else {
            for(int i=0; i<positivePredicates.size(); i++) {
                Predicate predicate = positivePredicates.get(i);
                if (p.toString().equals(predicate.toString())) {
                    positivePredicates.remove(i);
                }
            }
        }

    }

    public int compareTo(Sentence s2) {
        return this.toString().compareTo(s2.toString());
    }

    public List<Predicate> getPositivePredicates() {
        return positivePredicates;
    }

    public void setPositivePredicates(List<Predicate> positivePredicates) {
        this.positivePredicates = positivePredicates;
    }

    public List<Predicate> getNegativePredicates() {
        return negativePredicates;
    }

    public void setNegativePredicates(List<Predicate> negativePredicates) {
        this.negativePredicates = negativePredicates;
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
            System.out.println(s.getId() + " " + s.toString());
        }

        System.out.println("====== KB ======");
        for(Sentence s: KB) {
            System.out.println(s.getId() + " " + s.toString());
        }
    }
}

class TableBasedIndex {
    private List<Sentence> positives;
    private List<Sentence> negatives;

    public TableBasedIndex() {
        positives = new ArrayList<>();
        negatives = new ArrayList<>();
    }

    public void addPositives(Sentence s) {
        positives.add(s);
    }

    public void addNegatives(Sentence s) {
        negatives.add(s);
    }

    public List<Sentence> getPositives() {
        return positives;
    }

    public List<Sentence> getNegatives() {
        return negatives;
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