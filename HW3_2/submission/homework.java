import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nuning on 3/27/21.
 */
public class homework {
    public static void main(String[] args) {
        InferenceSystem inferenceSystem = new InferenceSystem("input.txt", "output.txt");
        inferenceSystem.startInference();
    }
}

class InferenceSystem {
    private InferenceInput inferenceInput;
    private int LIMIT_KB_SIZE = 8000;
    private final double LIMIT_TIME_IN_SECONDS = 300.0;
    private long startTime = 0;
    private String outputFileName;
    private Map<String, TableIndex> tableIndexMap = new HashMap<>();

    public InferenceSystem(String filename, String outputFileName) {
        this.outputFileName = outputFileName;
        inferenceInput = readInput(filename);
        //inferenceInput.print();
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
            tableIndexMap.clear();
            tell(inferenceInput.getKB());
            boolean valid = ask2(new HashSet<>(inferenceInput.getKB()), q);
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

    public ResolutionResult resolution3(Sentence s1, Sentence s2, Predicate predicate2) {
        ResolutionResult resolutionResult = new ResolutionResult();

        for (Predicate p1: s1.getPredicates()) {
            final Unifier unifier = new Unifier();
            unifier.setFailure(true);
            if (p1.isNegative() ^ predicate2.isNegative() && p1.getPredicate().equals(predicate2.getPredicate())) {
                unify(p1, predicate2, unifier);
            }

            if (unifier.isFailure()) {
                continue;
            } else {
                Sentence cloneS1 = new Sentence(s1, false);
                Sentence cloneS2 = new Sentence(s2, false);
                List<Predicate> restPredicates1 = cloneS1.getPredicates().stream().filter(x -> !x.equals(p1)).collect(Collectors.toList());
                List<Predicate> restPredicates2 = cloneS2.getPredicates().stream().filter(x -> !x.equals(predicate2)).collect(Collectors.toList());

                if (restPredicates1.isEmpty() && restPredicates2.isEmpty()) {
                    // contradiction
                    resolutionResult.setContradiction(true);
                    return resolutionResult;
//                    Sentence s = new Sentence();
//                    return s;
                }

                restPredicates1 = restPredicates1.stream().map(x -> x.substitute(unifier)).collect(Collectors.toList());
                restPredicates2 = restPredicates2.stream().map(x -> x.substitute(unifier)).collect(Collectors.toList());

                Set<Predicate> newPredicates = new HashSet<>();
                newPredicates.addAll(restPredicates1);
                newPredicates.addAll(restPredicates2);
                Sentence result = new Sentence(newPredicates);
                if (isTautology(result)) continue;
                resolutionResult.addInferredSentence(result);
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

    private void addToKB(Sentence s) {
//        KB.add(s);
//        addNewSentenceToTableBasedIndex(s, kbMap);
        for (Predicate p: s.getPredicates()) {
            TableIndex tableIndex = tableIndexMap.getOrDefault(p.getPredicate(), new TableIndex());
            if (!p.isNegative()) {
                tableIndex.addPositive(s);
                tableIndexMap.put(p.getPredicate(), tableIndex);
            } else {
                tableIndex.addNegative(s);
                tableIndexMap.put(p.getPredicate(), tableIndex);
            }
        }
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

    public boolean isTautology(Sentence s) {
        List<Predicate> predicates = new ArrayList<>(s.getPredicates());
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            for(int j=i+1; j<predicates.size(); j++) {
                Predicate p2 = predicates.get(j);
                if (p1.isComplement(p2)) {
                    return true;
                }
                if (p1.isNegative() ^ p2.isNegative() && p1.getPredicate().equals(p2.getPredicate())) {
                    // p(x) , ~p(K)
                    Unifier unifier = new Unifier();
                    unify(p1, p2, unifier);
                    //System.out.println("isTautology | " + p1 + " ## " + p2 + " unifier = " + unifier.isFailure() + ", " + unifier.toString());
                    if (unifier.isEmpty() || unifier.isFailure()) {
                        continue;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Sentence factoring(Sentence s) {
        // e.g. P(x) v P(C) v Q(x) = P(C) v Q(C)
        // System.out.println("factoring | try factoring " + s.toString());
        List<Predicate> predicates = new ArrayList<>(s.getPredicates());
        Sentence cloneS = new Sentence(s, true);
        for(int i=0; i<predicates.size(); i++) {
            Predicate p1 = predicates.get(i);
            for(int j=i+1; j<predicates.size(); j++) {
                Predicate p2 = predicates.get(j);
                if (p1.isSimilar(p2)) {
                    Unifier unifier = new Unifier();
                    unify(p1, p2, unifier);
                    if (unifier.isFailure()) {
                        cloneS.setFailure(true);
                        return cloneS;
                    } else if (!unifier.isFailure() && !unifier.isEmpty()) {
                        // P(x) v P(C) converts to P(C)
                        cloneS.removePredicate(p2);
                        Sentence result = substitute(cloneS, unifier);
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
        cloneS.setFailure(true);
        return cloneS;
    }

    public void tell(Set<Sentence> KB) {
        for(Sentence s: KB) {
            addToKB(s);
        }
    }

    public boolean ask2(Set<Sentence> KB, Sentence query) {
//        File file = new File("./src/KB.txt");
//        if (file.exists()) {
//            file.delete();
//        }
        //Set<Pair<Sentence, Sentence>> visited = new HashSet<>();
        Set<Sentence> newKB = new HashSet<>();
        Set<Sentence> newClauses = new HashSet<>();
        newClauses.add(query);
        tell(newClauses);

        int round = 0;
        startTime = System.nanoTime();

        Set<Pair<Sentence, Sentence>> visited = new HashSet<>();

        while (true) {
            round += 1;
            newKB.clear();

            for(Sentence q: newClauses) {
                if (getUsedTime() > LIMIT_TIME_IN_SECONDS) {
                    return false;
                }

                for(Predicate p: q.getPredicates()) {
                    TableIndex tableIndex = tableIndexMap.getOrDefault(p.getPredicate(), null);
                    if (tableIndex == null) continue;
                    Set<Sentence> resolvingSentences = p.isNegative() ? tableIndex.getPositives() : tableIndex.getNegatives();
                    if (resolvingSentences == null || resolvingSentences.isEmpty()) continue;

                    for (Sentence s : resolvingSentences) {
                        if (q.getId() == s.getId()) continue;

                        if (visited.contains(new Pair<>(q, s)) || visited.contains(new Pair<>(s, q))) {
                            //System.out.println("ask | Visited skip");
                            continue;
                        }

                        ResolutionResult resolvent = resolution3(s, q, p);
                        visited.add(new Pair<>(q, s));
                        visited.add(new Pair<>(s, q));

                        if (resolvent.isContradiction()) {
                            return true;
                        }

                        if (resolvent.getInferredSentences().isEmpty()) continue;

                        newKB.addAll(resolvent.getInferredSentences());
                    }
                }
            }

            if (KB.containsAll(newKB)) {
                return false;
            }
            // update only new clauses to KB
            newClauses = difference(KB, newKB);
            tell(newClauses);
            KB.addAll(newClauses);
            //writeKB(newClauses, round);
        }
    }

    private void writeKB(Set<Sentence> KB, int round) {
        List<Sentence> list = new ArrayList<>(KB);
        Collections.sort(list, new SentenceComparator());
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
            for(Sentence s: list) {
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
    private StringBuilder sb;

    public Predicate(String predicate) {
        if (predicate.charAt(0) == '~') {
            isNegative = true;
            predicate = predicate.substring(1);
        }
        this.predicate = predicate;
    }

    public Predicate(Predicate p) {
        this.isNegative = p.isNegative;
        this.predicate = p.predicate;
        getOrCreateArguments().addAll(p.getArguments());
    }

    private List<String> getOrCreateArguments() {
        if (arguments == null) arguments = new ArrayList<>();
        return arguments;
    }

    private String getPredicateString() {
        sb = new StringBuilder();
        if (isNegative) sb.append("~");
        sb.append(predicate);
        sb.append("(");
        for(int i=0; i<arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i < arguments.size() - 1) sb.append(",");
        }
        sb.append(")");
        String value = sb.toString();
        sb = null;
        return value;
    }

    public String toString() {
        //return predicateString;
        sb = new StringBuilder();
        if (isNegative) sb.append("~");
        sb.append(predicate);
        sb.append("(");
        for(int i=0; i<arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i < arguments.size() - 1) sb.append(",");
        }
        sb.append(")");
        String value = sb.toString();
        sb = null;
        return value;
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
        getOrCreateArguments().set(index, value);
    }

    public void addArgument(String argument) {
        getOrCreateArguments().add(argument);
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
        getOrCreateArguments().remove(index);
        getOrCreateArguments().add(index, constant);
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
    private Set<Predicate> predicates;
    private boolean isFailure = false;
    private StringBuilder sb;
    private final PredicateComparator pc = new PredicateComparator();

    public Sentence() {
        id = runningID++;
    }

    public Sentence(Set<Predicate> predicates) {
        id = runningID++;
        getOrCreatePredicates().addAll(predicates);
        //sentenceString = getSentenceString();
    }

    public Sentence(Sentence s, boolean isNew) {
        this.id = isNew ? runningID++ : s.id;
        this.isFailure = s.isFailure;
        Iterator<Predicate> it = s.getPredicates().iterator();
        while (it.hasNext()) {
            Predicate p = new Predicate(it.next());
            getOrCreatePredicates().add(p);
        }
        //sentenceString = getSentenceString();
    }

    public Set<Predicate> getOrCreatePredicates() {
        if (predicates == null) predicates = new HashSet<>();
        return predicates;
    }

    private String getSentenceString() {
        sb = new StringBuilder();
        int i = 0;
        for (Predicate p: predicates) {
            sb.append(p.toString());
            if (i != predicates.size()-1) sb.append(" | ");
            i++;
        }
        String value = sb.toString();
        sb = null;
        return value;
    }

    public String toString() {
        //return sentenceString;
        sb = new StringBuilder();
        List<Predicate> predicates = new ArrayList<>(this.predicates);
        Collections.sort(predicates, pc);
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
        String value = sb.toString();
        sb = null;
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!( o instanceof  Sentence)) return false;
        if (predicates.size() != ((Sentence) o).getPredicates().size()) return false;
//
//        Sentence s = (Sentence) o;
//        List<Predicate> predicates1 = new ArrayList<>(this.getPredicates());
//        Collections.sort(predicates1, pc);
//        List<Predicate> predicates2 = new ArrayList<>(s.getPredicates());
//        Collections.sort(predicates2, pc);
//        for(int i=0; i<predicates1.size(); i++) {
//            Predicate p1 = predicates1.get(i);
//            Predicate p2 = predicates2.get(i);
//            if (!p1.equals(p2)) return false;
//        }
//        return true;
        //return this.predicates.equals(((Sentence) o).getPredicates());
        return this.standardize().equals(((Sentence) o).standardize());
    }

    @Override
    public int hashCode() {
//        char[] array = this.toString().toCharArray();
//        Arrays.sort(array);
//        String sortString = new String(array);
//        return sortString.hashCode();
        return this.standardize().hashCode();
    }

    public void addPredicate(Predicate predicate) {
        getOrCreatePredicates().add(predicate);
        //sentenceString = getSentenceString();
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
        return predicates == null || predicates.size() == 0;
    }

    public void removePredicate(Predicate p) {
        getOrCreatePredicates().remove(p);
        //sentenceString = getSentenceString();
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

    public String standardize() {
        Sentence copy = new Sentence(this, false);
        Map<String, String> map = new HashMap<>();
        sb = new StringBuilder();
        int index = 1;
        List<String> stdPredicates = new ArrayList<>();
        for (Predicate p: copy.getPredicates()) {
            if (p.isNegative()) sb.append("~");
            sb.append(p.getPredicate());
            sb.append("(");
            for (int j=0; j<p.getArguments().size(); j++) {
                String arg = p.getArguments().get(j);
                if (Utility.isVariable(arg)) {
                    String std = map.getOrDefault(arg, null);
                    if (std == null) std = "variable" + index++;
                    map.put(arg, std);
                    sb.append(std);
                } else {
                    sb.append(arg);
                }
                if (j < p.getArguments().size()-1) sb.append(",");
            }
            sb.append(")");
            stdPredicates.add(sb.toString());
            sb.setLength(0);
        }
        Collections.sort(stdPredicates);
        for(int i=0; i<stdPredicates.size(); i++) {
            sb.append(stdPredicates.get(i));
            if (i != stdPredicates.size()-1) sb.append(" | ");
        }
        String value = sb.toString();
        sb = null;
        return value;
    }
}

class SentenceComparator implements Comparator<Sentence> {

    @Override
    public int compare(Sentence o1, Sentence o2) {
        return o1.compareTo(o2);
    }
}

class PredicateComparator implements Comparator<Predicate> {
    @Override
    public int compare(Predicate o1, Predicate o2) {
        return o1.compareTo(o2);
    }
}

class Unifier {
    private Map<String, String> substitution = new HashMap<>();
    private boolean isFailure = false;

    public Unifier() {
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
    private Set<Sentence> inferredSentences = new HashSet<>();

    public ResolutionResult() {}

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
    private Set<Sentence> KB = new HashSet<>();
    private List<Sentence> queries = new ArrayList<>();

    public InferenceInput() {
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
            System.out.println(s.toString());
        }

        System.out.println("====== KB ======");
        for(Sentence s: KB) {
            System.out.println(s.toString());
        }
    }
}

class TableIndex {
    private Set<Sentence> positives;
    private Set<Sentence> negatives;

    public TableIndex() {}

    private Set<Sentence> getOrCreatePositives() {
        if (positives == null) positives = new HashSet<>();
        return positives;
    }

    private Set<Sentence> getOrCreateNegatives() {
        if (negatives == null) negatives = new HashSet<>();
        return negatives;
    }

    public void addPositive(Sentence s) {
        getOrCreatePositives().add(s);
    }

    public void addNegative(Sentence s) {
        getOrCreateNegatives().add(s);
    }

    public void addPositives(Set<Sentence> sentences) {
        getOrCreatePositives().addAll(sentences);
    }

    public void addNegatives(Set<Sentence> sentences) {
        getOrCreateNegatives().addAll(sentences);
    }

    public Set<Sentence> getPositives() {
        return positives;
    }

    public Set<Sentence> getNegatives() {
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