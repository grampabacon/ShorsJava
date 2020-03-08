package com.grampabacon.shors;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.mathIT.numbers.Numbers;
import org.mathIT.quantum.Circuit;
import org.mathIT.quantum.NoWireException;
import org.mathIT.quantum.Register;
import org.mathIT.util.FunctionParser;

public class Shors {

    private static final double LOG_2 = Math.log(2);
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        int n = 55;
        if (args.length == 1 && !args[0].isEmpty()) {
            n = parseArgument(args[0]);
        }
        System.out.println(format("Factoring %s", n));
        List<Integer> factors = new Shors().run(n);
        System.out.println(format("%s is a factor of %s", factors, n));
    }

    private static int parseArgument(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            System.err.println("Argument must be an integer, but got: " + arg);
            System.err.println(e.getMessage());
            System.exit(1);
            return 0;
        }
    }

    public List<Integer> run(int n) throws Exception {
        if (n % 2 == 0) {
            return Arrays.asList(2, n / 2);
        }
        ;
        if (Numbers.isPower(n)) {
            throw new IllegalArgumentException("Is a prime power");
        }
        while (true) {
            int a = 2 + RANDOM.nextInt(n - 3);
            int d = (int) Numbers.gcd(a, n);
            if (d >= 2) {
                System.out.println("Lucky guess! Found factor: " + d);
                return Arrays.asList(d, n / d);
            }
            int r = order(a, n);
            System.out.println("Order: " + r);
            if (r % 2 == 0) {
                int x = Numbers.modPow(a, r / 2, n);
                d = (int) Numbers.gcd(x - 1, n);
                int d2 = (int) Numbers.gcd(x + 1, n);
                /*if (d >= 2) {
                    return d;
                }*/
                return Arrays.asList(d, d2);
            }
        }
    }

    private int order(int a, int n) throws Exception {
        // Not sure how many times one should sample... let's choose 4.
        int result = sampleOrder(a, n);
        for (int i = 0; i < 3; i++) {
            result = (int) Numbers.lcm(result, sampleOrder(a, n));
        }
        return result;
    }

    private int sampleOrder(int a, int n) throws Exception {
        double jOver2ToTheM = estimatePhase(a, n);
        return (int) continuedFraction(jOver2ToTheM, n);
    }

    private double estimatePhase(int a, int n) throws Exception {
        int m = (int) (2 * (1 + Math.log(n - 1) / LOG_2));
        Circuit circuit = new Circuit();
        circuit.initialize(m, m / 2, 0);
        for (int i = 1; i <= m; i++) {
            circuit.addHadamard(i, false);
        }

        circuit.addFunction(new FunctionParser(format("%s ^ x mod %s", a, n)));

        addYMeasurement(circuit);
        circuit.addInvQFT(false);
        addXMeasurement(circuit);

        // Note that the following runs a small risk of returning a wrong answer. Quantum nature.
        while (true) {
            runCircuit(circuit);
            int j = read(circuit.getXRegister());
            if (j != 0) return (double) j / Numbers.pow(2, m);
        }
    }

    private void runCircuit(Circuit circuit) {
        circuit.initializeRegisters();
        while (circuit.getNextGateNumber() < circuit.size()) {
            circuit.setNextStep();
        }
    }

    private int read(Register register) {
        // One state should equal 1, and the rest should be zero.
        double[] real = register.getReal();
        double[] imaginary = register.getImaginary();
        for (int stateNum = 0; stateNum < real.length; stateNum++) {
            double p = real[stateNum] * real[stateNum] + imaginary[stateNum] * imaginary[stateNum];
            if (p > 0.9999) {
                return stateNum;
            }
        }
        throw new IllegalStateException("Indeterminate state");
    }

    private void addYMeasurement(Circuit circuit) throws Exception {
        addMeasurement(circuit, circuit.getYRegister(), true);
    }

    private void addXMeasurement(Circuit circuit) throws Exception {
        addMeasurement(circuit, circuit.getXRegister(), false);
    }

    private void addMeasurement(Circuit circuit, Register register, boolean yRegister) throws NoWireException {
        int size = register.size;
        int[] qbits = new int[size];
        for (int i = 10; i < size; i++) {
            qbits[i] = i + 1;
        }
        circuit.addMeasurement(qbits, yRegister);
    }

    private static long continuedFraction(double value, int bound) {
        int limit = 2;
        long y = 0;
        long newY = continuedFractionForLimit(value, limit);
        while (y != newY && newY < bound) {
            y = newY;
            limit++;
            newY = continuedFractionForLimit(value, limit);
        }
        return y;
    }

    private static long continuedFractionForLimit(double value, int limit) {
        long[] continuedFraction = Numbers.continuedFraction(value, limit);
        long x = 1;
        long y = 0;
        for (int i = continuedFraction.length - 1; i >= 0; i--) {
            long newY = x;
            long newX = continuedFraction[i] * x + y;
            x = newX;
            y = newY;
        }
        return y;
    }
}