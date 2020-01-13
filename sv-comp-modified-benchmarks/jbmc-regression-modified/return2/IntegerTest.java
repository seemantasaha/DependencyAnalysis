import java.util.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.*;
import com.pholser.junit.quickcheck.generator.*;
import edu.berkeley.cs.jqf.fuzz.*;

@RunWith(JQF.class)
public class IntegerTest {

    @Fuzz
    public void test(@From(IntegerGenerator.class) int x, @From(IntegerGenerator.class) int y) {
        //assumeTrue(x >= -32768 && x <= 32767);
		//assumeTrue(x >= -2147483648 && x <= 2147483647);
        //System.out.println("x: " + x);
        Main.test(x, y);
    }
}
