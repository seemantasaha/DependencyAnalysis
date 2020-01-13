import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

//import java.util.Random;

public class StringGenerator extends Generator<String> {

    public StringGenerator() {
        super(String.class); // Register the type of objects that we can create
    }

    // This method is invoked to generate a single test case
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        
        int bound = 4;

        String result = "";
        for(int i=0; i<bound; i++) {
            result += random.nextChar('0', 'z');
        }

        return result;
    }
}