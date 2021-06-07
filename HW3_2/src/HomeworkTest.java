import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nuning on 4/1/21.
 */
public class HomeworkTest {
    public static void main(String[] args) {
        for(int i=25; i<=50; i++) {
            InferenceSystem inferenceSystem = new InferenceSystem("./test-cases3/input_" +i+ ".txt", "./output3/output_" +i+ ".txt");
            inferenceSystem.startInference();
            compareOutput(i);
        }

//        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases2/input_1.txt", "./output2/output_1.txt");
//////        Sentence s = inferenceSystem.parseSentence("Sick(y2,HeartAttack) & Sick(y1,HeartAttack) & Sick(x2,HeartAttack) & Contact(y1,Alice) & Contact(y2,Alice) & Contact(y1,y1) & Contact(x2,y2) & Contact(y2,y2) => False(x)");
//////        Sentence f = inferenceSystem.factoring(s);
//////        System.out.println(f.toString());
//        Sentence s1 = inferenceSystem.parseSentence("Knows(John,v) & ~Knows(John,v) => Hates(John,v)");
//        Sentence s2 = inferenceSystem.parseSentence("~Knows(John,v) => Hates(John,v)");
//        Sentence s3 = inferenceSystem.parseSentence("~Knows(John,v1) => Hates(John,v1)");
//        Sentence s4 = inferenceSystem.parseSentence("~Knows(John,v1) & ~Knows(John,v2) => Hates(John,v1)");
//        Sentence s5 = inferenceSystem.parseSentence("~Sick(x,a) & ~Sick(y,a) & ~Contagious(a) & Contact(x,y) & Sick(w,a) => ~Sick(x,a)");
//        Sentence s6 = inferenceSystem.parseSentence("~Take(Alice,VitD)");
//        Sentence s7 = inferenceSystem.parseSentence("~Take(Alice,VitD)");
//        List<Predicate> predicates = new ArrayList<>(s6.getPredicates());

//        ResolutionResult result = inferenceSystem.resolution2(s5, s6, predicates.get(0));
//        System.out.println("result = " + result.isContradiction());
//        for(Sentence s: result.getInferredSentences()) {
//            System.out.println(s.toString());
//        }
//
//        Predicate p1 = inferenceSystem.parsePredicate("Sick(v6,Flu)");
//        Predicate p2 = inferenceSystem.parsePredicate("Sick(v9,v8)");
//        Unifier unifier = new Unifier();
//        inferenceSystem.unify(p1, p2, unifier);
//        System.out.println(unifier.toString());
//
//        Set<Sentence> KB = new HashSet<>();
//        KB.add(s6);
//
//        Set<Sentence> newClauses = new HashSet<>();
//        newClauses.add(s7);
//
//        System.out.println(KB.containsAll(newClauses));
//        System.out.println(s2.hashCode());
//        System.out.println(s3.hashCode());
//        System.out.println(s2.equals(s3));
//        System.out.println(s5.standardize());
//
//        Sentence f = inferenceSystem.factoring(s4);
//        System.out.println(f.toString());
//
//        Sentence f1 = inferenceSystem.factoring_1(s5);
//        System.out.println(f1.toString());


    }

    private static void compareOutput(int caseNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================");
        sb.append(System.lineSeparator());
        sb.append("Testcase # " + caseNumber);
        sb.append(System.lineSeparator());
        String outputFilename = "./output3/output_" + caseNumber + ".txt";
        String expectedFilename = "./test-cases3/solution_" + caseNumber + ".txt";

        try {
            BufferedReader expectReader = new BufferedReader(new FileReader(expectedFilename));
            BufferedReader outputReader = new BufferedReader(new FileReader(outputFilename));

            String expect = expectReader.readLine();
            String output = outputReader.readLine();
            while (expect != null && output != null) {
                if (expect.equals(output)) {
                    sb.append("PASSED");
                    sb.append(System.lineSeparator());
                } else {
                    sb.append("FAILED");
                    sb.append(System.lineSeparator());
                }
                expect = expectReader.readLine();
                output = outputReader.readLine();
            }

            expectReader.close();
            outputReader.close();
            writeTestResult(sb.toString(), caseNumber != 1);
        } catch (Exception e ){
            e.printStackTrace();
        }
    }

    public static void writeTestResult(String result, boolean append) {
        File file = new File("./output3/test_result.txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileWriter fr = null;
        try {
            fr = new FileWriter(file, append);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(result);

            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
