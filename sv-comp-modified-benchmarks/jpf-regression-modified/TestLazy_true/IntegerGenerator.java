import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class IntegerGenerator extends Generator<Integer> {

    public IntegerGenerator() {
        super(Integer.class); // Register the type of objects that we can create
    }

    // This method is invoked to generate a single test case
    @Override
    public Integer generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        
        int v;
        v = random.nextInt();
        return v;
    }
}