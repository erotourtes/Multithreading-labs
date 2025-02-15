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

    public static Vector vectorGet(String name) {
        var scanner = new Scanner(System.in);
        switch (methodType) {
            case Manual -> {
                if (name != null) {
                    System.out.printf("\nEnter input for the vector(%s): ",
                            name);
                }
                var vector = vectorFromString(scanner.nextLine());
                return new Vector(vector);
            }
            case File -> {
                System.out.printf("\nEnter a file path with vector(%s): ",
                        name);
                var path = scanner.nextLine();
                return vectorFromFile(path);
            }
            case AllElementsSame -> {
                System.out.printf("\nEnter a number to fill the vector(%s): "
                        , name);
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
        var vector = IntStream.range(0, N).map((i) -> random.nextInt()).toArray();
        return new Vector(vector);
    }

    public static Matrix matrixGet(String name) {
        var scanner = new Scanner(System.in);
        switch (methodType) {
            case Manual -> {
                System.out.printf("\nEnter input for the matrix(%s): ", name);
                var vector = vectorFromString(scanner.nextLine(), N * N);
                var matrix = IntStream.range(0, N)
                        .mapToObj(i -> Arrays.copyOfRange(vector, i * N,
                                (i + 1) * N))
                        .toArray(int[][]::new);

                return new Matrix(matrix);
            }
            case File -> {
                System.out.printf("\nEnter a file path with matrix(%s): ",
                        name);
                var path = scanner.nextLine();
                return matrixFromFile(path);
            }
            case AllElementsSame -> {
                System.out.println("Enter a number to fill the matrix\n");
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

    public static class Vector {
        private final int[] data;

        public Vector(int[] data) {
            this.data = data;
        }

        public Vector sum(Vector other) {
            assert this.data.length == other.data.length : "Can't sum " +
                    "different len vectors";
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

class T1 implements Runnable {
    @Override
    public void run() {
        var res = F1();
        System.out.printf("\nResult of F1 is \n%s;", res.toString());
    }

    private Data.Matrix F1() {
        var A = Data.vectorGet("A");
        var B = Data.vectorGet("B");
        var C = Data.vectorGet("C");
        var MA = Data.matrixGet("MA");
        var MD = Data.matrixGet("MD");
        return B.sum(C)
                .scalarMultiply(A.sum(B).min())
                .toMatrix()
                .multiply(MA.multiply(MD));
    }
}

class T2 implements Runnable {
    @Override
    public void run() {
        var res = F2();
        System.out.printf("\nResult of F2 is \n%s;", res.toString());
    }

    private Data.Matrix F2() {
        var MF = Data.matrixGet("MF");
        var MH = Data.matrixGet("MH");
        var MK = Data.matrixGet("MK");
        return MF.subtract(MH.multiply(MK)).sort();
    }
}

class T3 implements Runnable {
    @Override
    public void run() {
        var res = F2();
        System.out.printf("\nResult of F2 is \n%s;", res.toString());
    }

    private Data.Matrix F2() {
        var O = Data.vectorGet("O");
        var P = Data.vectorGet("P");
        var V = Data.vectorGet("V");
        var MR = Data.matrixGet("MR");
        var MS = Data.matrixGet("MS");
        return O.sum(P).sum(V).toMatrix().multiply(MR.multiply(MS));
    }
}

public class Main {
    public static void Lab1() throws InterruptedException {
        Data.readInput();

        var t1 = new Thread(new T1());
        var t2 = new Thread(new T2());
        var t3 = new Thread(new T3());

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();
    }

    public static void main(String[] args) throws InterruptedException {
        Lab1();
    }
}