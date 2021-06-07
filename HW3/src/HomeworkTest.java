import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nuning on 4/1/21.
 */
public class HomeworkTest {
    public static void main(String[] args) {
        for(int i=1; i<=50; i++) {
            InferenceSystem inferenceSystem = new InferenceSystem("./test-cases2/input_" +i+ ".txt", "./output2/output_" +i+ ".txt");
            inferenceSystem.startInference();
            compareOutput(i);
        }

//        InferenceSystem inferenceSystem = new InferenceSystem("./test-cases2/input_23.txt", "./test-cases2/output_23.txt");
//        Sentence s1 = inferenceSystem.parseSentence("Knows(John,v) & ~Knows(John,v) => Hates(John,v)");
//        Sentence s2 = inferenceSystem.parseSentence("~Knows(John,v) => Hates(John,v)");
//        Sentence s3 = inferenceSystem.parseSentence("~Knows(John,v1) => Hates(John,v1)");
//        Sentence s4 = inferenceSystem.parseSentence("~Knows(John,v1) & ~Knows(John,v2) => Hates(John,v1)");
//        Sentence s5 = inferenceSystem.parseSentence("Contact(Alice,y4) & Contact(x3,y3) & Contact(x4,y4) & Contact(y4,x3) & Contact(y4,y2) => Q(x)");
//        Sentence s6 = inferenceSystem.parseSentence("Sick(x,a) & ~Sick(y,a) & ~Contagious(a) & Contact(x,y) => ~Sick(y,a)");
//
//        System.out.println(s1.toString());
//
//        List<Sentence> KB = new ArrayList<>();
//        KB.add(s1);
//        KB.add(s2);
//
//        List<Sentence> newClauses = new ArrayList<>();
//        newClauses.add(s3);
////
//        System.out.println(KB.containsAll(newClauses));
//        System.out.println(s2.equals(s3));
//
//        Sentence f = inferenceSystem.factoring(s4);
//        System.out.println(f.toString());
//
//        Sentence f1 = inferenceSystem.factoring(s5);
//        System.out.println(f1.toString());
//
//        Sentence f2 = inferenceSystem.factoring(s6);
//        System.out.println(f2.toString());
    }

    private static void compareOutput(int caseNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================");
        sb.append(System.lineSeparator());
        sb.append("Testcase # " + caseNumber);
        sb.append(System.lineSeparator());
        String outputFilename = "./output2/output_" + caseNumber + ".txt";
        String expectedFilename = "./test-cases2/output_" + caseNumber + ".txt";

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
        File file = new File("./output2/test_result.txt");
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
