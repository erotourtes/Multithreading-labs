/**
 * Program header: «ПРОГРАМНЕ ЗАБЕЗПЕЧЕННЯ ВИСОКОПРОДУКТИВНИХ КОМП’ЮТЕРНИХ СИСТЕМ.»
 * Lab 2: «Програмування для комп’ютерних систем зі спільною пам’яттю»
 * Function:
 * - MU = (MD * MC) * d + max(Z) * MR
 * Name: Сірик Максим Олександрович
 * Date: 01.03.2025
 */

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Data {
    private static final Random random = new Random();

    private static int N;
    private static int H;

    private static final int AMOUNT_OF_THREADS = 4;

    public static final String THREAD_NAME_1 = "T1";
    public static final String THREAD_NAME_2 = "T2";
    public static final String THREAD_NAME_3 = "T3";
    public static final String THREAD_NAME_4 = "T4";

    public static AtomicInteger d = new AtomicInteger(0);
    public static Matrix MC, MR, MD;
    public static Vector Z;

    public static CyclicBarrier CB1 = new CyclicBarrier(4);
    public static ReentrantLock L1 = new ReentrantLock();
    public static Semaphore Sem1 = new Semaphore(-2);


    public static AtomicInteger z = new AtomicInteger(Integer.MIN_VALUE);

    public static void maxZ(int zi) {
        L1.lock();
        z.set(Math.max(z.get(), zi));
        L1.unlock();
    }

    public static void readInput() {
        var scanner = new Scanner(System.in);
        System.out.println("Enter N: ");
        N = scanner.nextInt();
        H = N / AMOUNT_OF_THREADS; // Floor round and distribute reminder to first k elements
        scanner.nextLine();
    }

    private static boolean isManualInput() {
        return N <= 3;
    }

    private static Range getRange(int partOneBased) {
        int reminder = N % AMOUNT_OF_THREADS;
        int i = partOneBased - 1;
        int start = i * H + Math.min(i, reminder);
        int end = start + H + (i < reminder ? 1 : 0);
        return new Range(start, end);
    }

    public static int scalarGet(String name, String threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            System.out.printf("\n[%s] Enter input for the scalar(%s): ", threadName, name);
            return scanner.nextInt();
        }

        return random.nextInt();
    }

    public static Vector vectorGet(String name, String threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            if (name != null) {
                System.out.printf("\n[%s] Enter input for the vector(%s): ", threadName, name);
            }
            var vector = vectorFromString(scanner.nextLine());
            return new Vector(vector);
        }

        return vectorFromRandom();
    }

    private static Vector vectorFromRandom() {
        var vector = IntStream.range(0, N)
                .map((i) -> random.nextInt())
                .toArray();
        return new Vector(vector);
    }

    public static Matrix matrixGet(String name, String threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            System.out.printf("\n[%s] Enter input for the matrix(%s): ", threadName, name);
            var vector = vectorFromString(scanner.nextLine(), N * N);
            var matrix = IntStream.range(0, N)
                    .mapToObj(i -> Arrays.copyOfRange(vector, i * N, (i + 1) * N))
                    .toArray(int[][]::new);

            return new Matrix(matrix);
        }

        return matrixFromRandom();
    }

    private static int[] vectorFromString(String line) {
        return vectorFromString(line, N);
    }

    private static int[] vectorFromString(String line, int len) {
        var nums = line.split("\\s+");
        assert nums.length == len : "Amount of numbers differ";
        return Arrays.stream(nums).mapToInt(Integer::parseInt).toArray();
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

        public int max(int partOneBased) {
            var range = Data.getRange(partOneBased);
            return this.max(range.startIncl, range.endExcl);
        }

        public int max(int startInc, int endExcl) {
            assert this.data.length > 0 : "Max on empty array";
            assert startInc >= 0 && endExcl <= this.data.length : "Wrong boundaries";
            return Arrays.stream(this.data, startInc, endExcl).max().getAsInt();
        }
    }

    public static class Matrix {
        private final int[][] data;

        Matrix(int[][] data) {
            this.data = data;
        }

        Matrix multiply(Matrix other, int partOneBased) {
            var m = data.length;
            var n = data[0].length;

            var range = Data.getRange(partOneBased);
            int offset = range.startIncl;
            int p = range.endExcl - range.startIncl;
            assert n == other.data.length : "Can't multiply matrices";

            var newMatrix = new int[m][p];

            for (var i = 0; i < m; i++) {
                for (var j = 0; j < n; j++) {
                    for (var k = 0; k < p; k++) {
                        newMatrix[i][k] += data[i][j] * other.data[j][k + offset];
                    }
                }
            }

            return new Matrix(newMatrix);
        }

        Matrix scalarMultiply(int scalar) {
            return this.scalarMultiply(scalar, -1);
        }

        Matrix scalarMultiply(int scalar, int partOneBased) {
            var matrix = Arrays.stream(data).map(datum -> {
                var range = Data.getRange(partOneBased);
                var isWholeArray = partOneBased < 1;
                var stream = isWholeArray ? Arrays.stream(datum) : Arrays.stream(datum, range.startIncl, range.endExcl);
                return stream.map(k -> k * scalar).toArray();
            }).toArray(int[][]::new);
            return new Matrix(matrix);
        }

        public Matrix add(Matrix other) {
            assert data.length == other.data.length : "Mismatch in len " + other.data.length + " Expected " + data.length;
            assert data[0].length == other.data[0].length : "Mismatch in len2 " + other.data[0].length + " Expected " + data[0].length;

            var matrix = IntStream.range(0, data.length)
                    .mapToObj(i -> IntStream.range(0, data[i].length)
                            .map(j -> data[i][j] + other.data[i][j])
                            .toArray())
                    .toArray(int[][]::new);
            return new Matrix(matrix);
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

    record Range(int startIncl, int endExcl) {
    }
}

class RunT1 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started\n", Data.THREAD_NAME_1);

        try {
            // Початок вводу даних
            Data.d.set(Data.scalarGet("d", Data.THREAD_NAME_1));
            // Сигнал про введення
            // Чекати на введення
            Data.CB1.await();
            // Обчислення zi
            int zi = Data.Z.max(1);
            // Обчислення Z
            Data.maxZ(zi);
            // Сигнал про обчислення
            Data.CB1.await();
            // Копіювання d
            int d1 = Data.d.get();
            // Копіювання z
            int z1 = Data.z.get();
            // Обрахунок MUh
            Data.Matrix MUh = Data.MD.multiply(Data.MC, 1)
                    .scalarMultiply(d1)
                    .add(Data.MR.scalarMultiply(z1, 1));
            // Чекати завершення обрахунку MUh
            Data.Sem1.acquire();
            // Вивід результату обрахунків
            System.out.printf("\n[%s] Result is \n%s;", Data.THREAD_NAME_1, MUh.toString());
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        System.out.printf("Thread [%s] is finished\n", Data.THREAD_NAME_1);
    }
}

class RunT2 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started\n", Data.THREAD_NAME_2);

        try {
            // Чекати на введення
            Data.CB1.await();
            // Обчислення zi
            int zi = Data.Z.max(2);
            // Обчислення Z
            Data.maxZ(zi);
            // Сигнал про обчислення
            Data.CB1.await();
            // Копіювання d
            int d2 = Data.d.get();
            // Копіювання z
            int z2 = Data.z.get();
            // Обрахунок MUh
//            Data.Matrix MUh = Data.MD.multiply(Data.MC, 2)
//                    .scalarMultiply(d2, -1)
//                    .add(Data.MR.scalarMultiply(z2, 2));
            // Сигнал про завершення обчислення MUн
            Data.Sem1.release();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        System.out.printf("Thread [%s] is finished\n", Data.THREAD_NAME_2);
    }
}

class RunT3 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started\n", Data.THREAD_NAME_3);

        try {
            // Початок вводу даних
            Data.MC = Data.matrixGet("MC", Data.THREAD_NAME_3);
            Data.MR = Data.matrixGet("MR", Data.THREAD_NAME_3);
            // Сигнал про введення
            // Чекати на введення
            Data.CB1.await();
            // Обчислення zi
            int zi = Data.Z.max(3);
            // Обчислення Z
            Data.maxZ(zi);
            // Сигнал про обчислення
            Data.CB1.await();
            // Копіювання d
            int d3 = Data.d.get();
            // Копіювання z
            int z3 = Data.z.get();
            // Обрахунок MUh
//            Data.Matrix MUh = Data.MD.multiply(Data.MC, 3)
//                    .scalarMultiply(d3, -1)
//                    .add(Data.MR.scalarMultiply(z3, 3));
            // Сигнал про завершення обчислення MUн
            Data.Sem1.release();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        System.out.printf("Thread [%s] is finished\n", Data.THREAD_NAME_3);
    }
}

class RunT4 implements Runnable {
    @Override
    public void run() {
        System.out.printf("Thread [%s] is started\n", Data.THREAD_NAME_4);

        try {
            // Початок вводу даних
            Data.Z = Data.vectorGet("Z", Data.THREAD_NAME_4);
            Data.MD = Data.matrixGet("MD", Data.THREAD_NAME_4);
            // Сигнал про введення
            // Чекати на введення
            Data.CB1.await();
            // Обчислення zi
            int zi = Data.Z.max(4);
            // Обчислення Z
            Data.maxZ(zi);
            // Сигнал про обчислення
            Data.CB1.await();
            // Копіювання d
            int d4 = Data.d.get();
            // Копіювання z
            int z4 = Data.z.get();
            // Обрахунок MUh
//            Data.Matrix MUh = Data.MD.multiply(Data.MC, 4)
//                    .scalarMultiply(d4, -1)
//                    .add(Data.MR.scalarMultiply(z4, 4));
            // Сигнал про завершення обчислення MUн
            Data.Sem1.release();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        System.out.printf("Thread [%s] is finished\n", Data.THREAD_NAME_4);
    }
}

public class Main {
    public static void Lab1() throws InterruptedException {
        Data.readInput();

        var start = System.currentTimeMillis();

        var T1 = new Thread(new RunT1());
        var T2 = new Thread(new RunT2());
        var T3 = new Thread(new RunT3());
        var T4 = new Thread(new RunT4());

        T1.start();
        T2.start();
        T3.start();
        T4.start();

        T1.join();
        T2.join();
        T3.join();
        T4.join();

        var end = System.currentTimeMillis();
        System.out.println();
        System.out.println(end - start);
    }

    public static void main(String[] args) throws InterruptedException {
        Lab1();
    }
}