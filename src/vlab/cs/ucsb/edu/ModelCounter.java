package vlab.cs.ucsb.edu;

import vlab.cs.ucsb.edu.DriverProxy.Option;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//import edu.ucsb.cs.vlab.translate.smtlib.from.abc.ABCTranslator;


public class ModelCounter {

  DriverProxy abc;
  int bound;
  BigInteger total_model_count;

  public ModelCounter(int bound) {
    this.abc = new DriverProxy();
    //this.abc.setOption(DriverProxy.Option.DISABLE_EQUIVALENCE_CLASSES);
    this.bound = bound;
    this.total_model_count = new BigInteger("0");
  }

  public BigDecimal getModelCount(String PCTranslation) {

    String modelCountMode = "abc.string";
    int MIN = Integer.MIN_VALUE;
    int MAX = Integer.MAX_VALUE;


    if (MIN >= 0) {
      abc.setOption(Option.USE_UNSIGNED_INTEGERS);
    }

    //ABCTranslator translator = new ABCTranslator();

    HashSet<String> additional_assertions = new HashSet<String>();
    String range_constraint = "A-Z";
    List<String> model_counting_vars = new ArrayList<>();
    model_counting_vars.add("l");
    model_counting_vars.add("h");

    for (final String var_name : model_counting_vars) {
      if (range_constraint != null) {
        additional_assertions.add("(assert (in " + var_name
                + " " + range_constraint.trim() + "))");
      }
    }

    //String PCTranslation = translator.translate(p.spc, model_counting_vars, additional_assertions);
    //System.out.println(PCTranslation);

    long startTime = System.nanoTime();
    boolean result = abc.isSatisfiable(PCTranslation);
    long endTime = System.nanoTime();

    System.out.println("Constraint solving time: " + (endTime - startTime) / 1000000.0);

    BigDecimal count = null;
    if (result) {
      startTime = System.nanoTime();
      if (modelCountMode.equals("abc.string")) {
        count = new BigDecimal(1);
        for (String var_name : model_counting_vars) {
          count = count.multiply(new BigDecimal(abc.countVariable(var_name, bound)));
        }
      } else if (modelCountMode.equals("abc.linear_integer_arithmetic")) {
        double bound = Math.ceil(Math.log(MAX) / Math.log(2)) + 1;
        count = new BigDecimal(abc.countInts((long) bound));
      }

      endTime = System.nanoTime();
      System.out.println("Model counting time: " + (endTime - startTime) / 1000000.0);
    } else {
      System.out.println("Unsatisfiable");
    }

    return count;
  }

  public void disposeABC() {
    this.abc.dispose();
  }

  public static void main(String[] args) {

    ModelCounter mc = new ModelCounter(4);

    String constraint = "(declare-fun h () String)\n" +
            "(declare-fun l () String)\n" +
            "(assert (in h /[A-Z]{0,4}/))\n" +
            "(assert (in l /[A-Z]{0,4}/))\n" +
            "\n" +
            "(assert (not (= (len  l) (len  h))))\n" +
            "(assert (<= (len  l) 4))\n" +
            "(assert (<= (len  h) 4))\n" +
            "(check-sat)";

    BigDecimal count = mc.getModelCount(constraint);

    System.out.println(count);

    mc.disposeABC();
  }

}