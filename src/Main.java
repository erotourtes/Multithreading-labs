/**
 * Program header: «ПРОГРАМНЕ ЗАБЕЗПЕЧЕННЯ ВИСОКОПРОДУКТИВНИХ КОМП’ЮТЕРНИХ
 * СИСТЕМ.»
 * Lab 1: «Програмування ̈́потоків (ЛР1)»
 * Functions:
 * - F1: 1.20 D = MIN(A + B) * (B + C) *(MA*MD)
 * - F2: 2.24 MG = SORT(MF - MH * MK)
 * - F3: 3.25 S = (O + P + V)*(MR * MS)
 * Name: Сірик Максим Олександрович
 * Date: 15.02.2025
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Data {
    private static final Random random = new Random();

    private static int N = 0;
    private static MethodType methodType = MethodType.Manual;

    public static String THREAD_NAME_1 = "T1";
    public static String THREAD_NAME_2 = "T2";
    public static String THREAD_NAME_3 = "T3";

    public static Data.Matrix calculateF1(Data.Vector A, Data.Vector B,
                                          Data.Vector C, Data.Matrix MA,
                                          Data.Matrix MD) {
        return B.sum(C)
                .scalarMultiply(A.sum(B).min())
                .toMatrix()
                .multiply(MA.multiply(MD));
    }

    public static Data.Matrix calculateF2(Data.Matrix MF, Data.Matrix MH,
                                          Data.Matrix MK) {
        return MF.subtract(MH.multiply(MK)).sort();
    }

    public static Data.Matrix calculateF3(Data.Vector O, Data.Vector P,
                                          Data.Vector V,
                                          Data.Matrix MR, Data.Matrix MS) {
        return O.sum(P).sum(V).toMatrix().multiply(MR.multiply(MS));
    }

    public static void readInput() {
        var scanner = new Scanner(System.in);
        System.out.println("Enter N: ");
        N = scanner.nextInt();
        scanner.nextLine();
        if (isManualInput()) {
            methodType = MethodType.Manual;
            return;
        }

        System.out.println("""
                Enter:
                1 - for files input;
                2 - for all elements are the same input;
                3 - random input;
                """);
        var method = scanner.nextInt();
        scanner.nextLine();
        methodType = MethodType.fromValue(method);
    }

    private static boolean isManualInput() {
        return N <= 3;
    }

    public static Vector vectorGet(String name, String threadName) {
        var scanner = new Scanner(System.in);
        switch (methodType) {
            case Manual -> {
                if (name != null) {
                    System.out.printf("\n[%s] Enter input for the vector(%s): ", threadName, name);
                }
                var vector = vectorFromString(scanner.nextLine());
                return new Vector(vector);
            }
            case File -> {
                System.out.printf("\n[%s] Enter a file path with vector(%s): ", threadName, name);
                var path = scanner.nextLine();
                return vectorFromFile(path);
            }
            case AllElementsSame -> {
                System.out.printf("\n[%s] Enter a number to fill the vector(%s): ", threadName, name);
                var number = scanner.nextInt();
                scanner.nextLine();
                return vectorFromNumber(number);
            }
            case Random -> {
                return vectorFromRandom();
            }
            default -> throw new RuntimeException();
        }

    }

    private static Vector vectorFromFile(String path) {
        return new Vector(matrixFromFile(path).data[0]);
    }

    private static Vector vectorFromNumber(int number) {
        var vector = new int[N];
        Arrays.fill(vector, number);
        return new Vector(vector);
    }

    private static Vector vectorFromRandom() {
        var vector = IntStream.range(0, N)
                .map((i) -> random.nextInt())
                .toArray();
        return new Vector(vector);
    }

    public static Matrix matrixGet(String name, String threadName) {
        var scanner = new Scanner(System.in);
        switch (methodType) {
            case Manual -> {
                System.out.printf("\n[%s] Enter input for the matrix(%s): ", threadName, name);
                var vector = vectorFromString(scanner.nextLine(), N * N);
                var matrix = IntStream.range(0, N)
                        .mapToObj(i -> Arrays.copyOfRange(vector, i * N,
                                (i + 1) * N))
                        .toArray(int[][]::new);

                return new Matrix(matrix);
            }
            case File -> {
                System.out.printf("\n[%s]Enter a file path with matrix(%s): ", threadName, name);
                var path = scanner.nextLine();
                return matrixFromFile(path);
            }
            case AllElementsSame -> {
                System.out.printf("\n[%s] Enter a number to fill the matrix(%s) ", threadName, name);
                var number = scanner.nextInt();
                scanner.nextLine();
                return matrixFromNumber(number);
            }
            case Random -> {
                return matrixFromRandom();
            }
        }

        return new Matrix(new int[0][0]);
    }

    private static Matrix matrixFromFile(String path) {
        try {
            var matrix = Files.lines(Paths.get(path))
                    .map(Data::vectorFromString)
                    .toArray(int[][]::new);
            return new Matrix(matrix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] vectorFromString(String line) {
        return vectorFromString(line, N);
    }

    private static int[] vectorFromString(String line, int len) {
        var nums = line.split("\\s+");
        assert nums.length == len : "Amount of numbers differ";
        return Arrays.stream(nums).mapToInt(Integer::parseInt).toArray();
    }

    private static Matrix matrixFromNumber(int number) {
        var matrix = IntStream.range(0, N)
                .mapToObj((i) -> vectorFromNumber(number).data)
                .toArray(int[][]::new);
        return new Matrix(matrix);
    }

    private static Matrix matrixFromRandom() {
        var matrix = IntStream.range(0, N)
                .mapToObj((i) -> vectorFromRandom().data)
                .toArray(int[][]::new);
        return new Matrix(matrix);
    }

    enum MethodType {
        Manual(0), File(1), AllElementsSame(2), Random(3);
        public final int value;

        MethodType(int type) {
            value = type;
        }

        public static MethodType fromValue(int value) {
            assert value >= 0 && value <= 3 : "Invalid input type";
            for (MethodType method : values()) {
                if (method.value == value) {
                    return method;
                }
            }
            return MethodType.Manual;
        }
    }

    public static class Vector {
        private final int[] data;

        public Vector(int[] data) {
            this.data = data;
        }

        public Vector sum(Vector other) {
            assert this.data.length == other.data.length : "Can't sum different len vectors";
            var sum = IntStream.range(0, N)
                    .map(i -> data[i] + other.data[i])
                    .toArray();
            return new Vector(sum);
        }

        public int min() {
            assert this.data.length > 0 : "Min on empty array";
            return Arrays.stream(this.data).min().getAsInt();
        }

        public Vector scalarMultiply(int scalar) {
            var mult = Arrays.stream(this.data)
                    .map(datum -> datum * scalar)
                    .toArray();
            return new Vector(mult);
        }

        public Matrix toMatrix() {
            var matrix = new int[1][data.length];
            matrix[0] = this.data;
            return new Matrix(matrix);
        }
    }

    public static class Matrix {
        private final int[][] data;

        Matrix(int[][] data) {
            this.data = data;
        }

        Matrix multiply(Matrix other) {
            var m = data.length;
            var n = data[0].length;
            var p = other.data[0].length;
            assert n == other.data.length : "Can't multiply matrices";

            var newMatrix = new int[m][p];

            for (var i = 0; i < m; i++) {
                for (var j = 0; j < n; j++) {
                    for (var k = 0; k < p; k++) {
                        newMatrix[i][k] += data[i][j] * other.data[j][k];
                    }
                }
            }

            return new Matrix(newMatrix);
        }

        public Matrix subtract(Matrix other) {
            var matrix = IntStream.range(0, data.length)
                    .mapToObj(i -> IntStream.range(0, data[i].length)
                            .map(j -> data[i][j] - other.data[i][j])
                            .toArray())
                    .toArray(int[][]::new);
            return new Matrix(matrix);
        }

        public Matrix sort() {
            var sorted = Arrays.stream(data)
                    .map(v -> Arrays.stream(v).sorted().toArray())
                    .toArray(int[][]::new);
            return new Matrix(sorted);
        }


        @Override
        public String toString() {
            return Arrays.stream(data)
                    .map(row -> Arrays.stream(row)
                            .mapToObj(String::valueOf)
                            .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining("\n"));
        }
    }
}

class RunT1 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started", Data.THREAD_NAME_1);

        // Початок вводу даних
        var A = Data.vectorGet("A", Data.THREAD_NAME_1);
        var B = Data.vectorGet("B", Data.THREAD_NAME_1);
        var C = Data.vectorGet("C", Data.THREAD_NAME_1);
        var MA = Data.matrixGet("MA", Data.THREAD_NAME_1);
        var MD = Data.matrixGet("MD", Data.THREAD_NAME_1);
        // Обрахунок формули F1
        var res = Data.calculateF1(A, B, C, MA, MD);
        // Вивід результату обрахунків
        System.out.printf("\n[%s] Result of F1 is \n%s;", Data.THREAD_NAME_1,
                res.toString());

        System.out.printf("Thread [%s] is finished", Data.THREAD_NAME_1);
    }
}

class RunT2 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started", Data.THREAD_NAME_2);

        // Початок вводу даних
        var MF = Data.matrixGet("MF", Data.THREAD_NAME_2);
        var MH = Data.matrixGet("MH", Data.THREAD_NAME_2);
        var MK = Data.matrixGet("MK", Data.THREAD_NAME_2);
        // Обрахунок формули F2
        var res = Data.calculateF2(MF, MH, MK);
        // Вивід результату обрахунків
        System.out.printf("\n[%s] Result of F2 is \n%s;", Data.THREAD_NAME_2,
                res.toString());

        System.out.printf("Thread [%s] is finished", Data.THREAD_NAME_2);
    }
}

class RunT3 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started", Data.THREAD_NAME_3);

        // Початок вводу даних
        var O = Data.vectorGet("O", Data.THREAD_NAME_3);
        var P = Data.vectorGet("P", Data.THREAD_NAME_3);
        var V = Data.vectorGet("V", Data.THREAD_NAME_3);
        var MR = Data.matrixGet("MR", Data.THREAD_NAME_3);
        var MS = Data.matrixGet("MS", Data.THREAD_NAME_3);
        // Обрахунок формули F3
        var res = Data.calculateF3(O, P, V, MR, MS);
        // Вивід результату обрахунків
        System.out.printf("\n[%s] Result of F2 is \n%s;", Data.THREAD_NAME_3,
                res.toString());

        System.out.printf("Thread [%s] is finished", Data.THREAD_NAME_3);
    }
}

public class Main {
    public static void Lab1() throws InterruptedException {
        Data.readInput();

        var T1 = new Thread(new RunT1());
        var T2 = new Thread(new RunT2());
        var T3 = new Thread(new RunT3());

        T1.start();
        T2.start();
        T3.start();

        T1.join();
        T2.join();
        T3.join();
    }

    public static void main(String[] args) throws InterruptedException {
        Lab1();
    }
}