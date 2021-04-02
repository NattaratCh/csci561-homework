import java.io.*;
import java.util.List;

/**
 * Created by nuning on 4/1/21.
 */
public class HomeworkTest {
    public static void main(String[] args) {
        for(int i=1; i<=50; i++) {
            InferenceSystem inferenceSystem = new InferenceSystem("./test-cases2/input_" +i+ ".txt");
            List<Boolean> result = inferenceSystem.startInference();
            inferenceSystem.writeOutput(result, "./output2/output_" +i+ ".txt");
            compareOutput(i);
        }
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
