/**
 * Program header: «ПРОГРАМНЕ ЗАБЕЗПЕЧЕННЯ ВИСОКОПРОДУКТИВНИХ КОМП’ЮТЕРНИХ СИСТЕМ.»
 * Lab 4: «розробка програми для ПКС зі СП»
 * Function:
 * - V = sort(d*B + Z*MM) * (MX*MT) + (B*Z)*B
 * Name: Сірик Максим Олександрович
 * Date: 12.04.2025
 */

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

class Data {
    public static final int THREAD_1 = 1;
    public static final int THREAD_2 = 2;
    public static final int THREAD_3 = 3;
    public static final int THREAD_4 = 4;

    public static Matrix MM, MX, MT = null;
    public static Vector Z, B, O = null;
    public static Vector V = null;
    public static Monitor.SyncValue d = new Monitor.SyncValue();
    public static Monitor.SyncValue a = new Monitor.SyncValue();

    public static final Monitor monitor = new Monitor();

    private static int N;
    private static final int AMOUNT_OF_THREADS = 4;

    private static int getFillNumber() {
        return 1;
    }

    private static final ReentrantLock L1 = new ReentrantLock(); // For syncronous input

    public static void calculate(int a, int partOneBased) {
        O.multiply(MX.multiplyTransposed(MT, partOneBased))
                .sum(B.scalarMultiply(a, partOneBased), V, partOneBased);
    }

    public static void readInput() {
        var scanner = new Scanner(System.in);
        System.out.println("Enter N: ");
        N = scanner.nextInt();
        V = new Vector(new int[N]);
        O = new Vector(new int[N]);
        scanner.nextLine();
    }

    private static boolean isManualInput() {
        return N <= 0;
    }

    private static Range getRange(int partOneBased) {
        return getRange(partOneBased, AMOUNT_OF_THREADS);
    }

    private static Range getRange(int taskIndexOneBased, int amountOfTasks) {
        int reminder = N % amountOfTasks;
        int H = N / amountOfTasks; // Floor round and distribute reminder to first k elements
        int i = taskIndexOneBased - 1;
        int start = i * H + Math.min(i, reminder);
        int end = start + H + (i < reminder ? 1 : 0);
        return new Range(start, end);
    }

    public static int scalarGet(String name, int threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            try {
                L1.lock();
                System.out.printf("\n[T%s] Enter input for the scalar(%s): ", threadName, name);
                return scanner.nextInt();
            } finally {
                L1.unlock();
            }
        }

        return getFillNumber();
    }

    public static Vector vectorGet(String name, int threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            try {
                L1.lock();
                if (name != null) {
                    System.out.printf("\n[T%s] Enter input for the vector(%s): ", threadName, name);
                }
                var vector = vectorFromString(scanner.nextLine());
                return new Vector(vector);
            } finally {
                L1.unlock();
            }
        }

        return vectorFromFill();
    }

    private static Vector vectorFromFill() {
        var vector = IntStream.range(0, N)
                .map((i) -> getFillNumber())
                .toArray();
        return new Vector(vector);
    }

    public static void printStart(int thread) {
        System.out.printf("Thread [T%s] is started\n", thread);
    }

    public static void printEnd(int thread) {
        System.out.printf("Thread [T%s] is finished\n", thread);
    }

    public static Matrix matrixGet(String name, int threadName) {
        var scanner = new Scanner(System.in);
        if (isManualInput()) {
            try {
                L1.lock();
                System.out.printf("\n[T%s] Enter input for the matrix(%s): ", threadName, name);
                var vector = vectorFromString(scanner.nextLine(), N * N);
                var matrix = IntStream.range(0, N)
                        .mapToObj(i -> Arrays.copyOfRange(vector, i * N, (i + 1) * N))
                        .toArray(int[][]::new);

                return new Matrix(matrix);
            } finally {
                L1.unlock();
            }
        }

        return matrixFromFill();
    }

    private static int[] vectorFromString(String line) {
        return vectorFromString(line, N);
    }

    private static int[] vectorFromString(String line, int len) {
        var nums = line.split("\\s+");
        assert nums.length == len : "Amount of numbers differ";
        return Arrays.stream(nums).mapToInt(Integer::parseInt).toArray();
    }

    private static Matrix matrixFromFill() {
        var matrix = IntStream.range(0, N)
                .mapToObj((i) -> vectorFromFill().data)
                .toArray(int[][]::new);
        return new Matrix(matrix);
    }

    public static void calculateO(int partOneBased) {
        var Z_MM = Z.toMatrix().multiply(MM, partOneBased);
        var d = Data.d.get();
        var range = getRange(partOneBased);
        for (int i = range.startIncl; i < range.endExcl; i++) {
            Data.O.data[i] = d * B.data[i] + Z_MM.data[0][i];
        }
    }

    public static class Vector {
        private final int[] data;

        public Vector(int[] data) {
            this.data = data;
        }

        public int dotProduct(Vector vector, int partOneBased) {
            assert getLength() == vector.getLength() : "Should have same amount of elements";
            return getRange(partOneBased).stream()
                    .map(i -> data[i] * vector.data[i])
                    .sum();
        }

        /**
         * Multiplies in place
         */
        public Vector scalarMultiply(int scalar, int partOneBased) {
            scalarMultiply(scalar, this, partOneBased);
            return this;
        }

        public void scalarMultiply(int scalar, Vector out, int partOneBased) {
            var range = getRange(partOneBased);
            range.stream().forEach((i) -> out.data[i] = data[i] * scalar);
        }

        public int getLength() {
            return data.length;
        }


        public void sort() {
            Arrays.sort(this.data);
        }

        public void sort(int partOneBased) {
            var range = getRange(partOneBased);
            Arrays.sort(this.data, range.startIncl, range.endExcl);
        }

        public void sortHalf(int partOneBased) {
            var range = getRange(partOneBased, AMOUNT_OF_THREADS / 2);
            Arrays.sort(data, range.startIncl, range.endExcl);
        }

        public Vector multiply(Matrix matrix) {
            assert data.length == matrix.data.length;
            var n = data.length;
            var p = matrix.data[0].length;
            var newVector = new int[p];
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    newVector[k] += data[j] * matrix.data[j][k];
                }
            }

            return new Vector(newVector);
        }

        public Matrix toMatrix() {
            var matrix = new int[1][N];
            matrix[0] = data;
            return new Matrix(matrix);
        }

        public void sum(Vector vector, Vector out, int partOneBased) {
            var range = getRange(partOneBased);
            range.stream().forEach((i) -> {
                var sum = this.data[i - range.startIncl] + vector.data[i];
                out.data[i] = sum;
            });
        }

        @Override
        public String toString() {
            return Arrays.toString(data);
        }
    }

    public static class Matrix {
        private final int[][] data;

        Matrix(int[][] data) {
            this.data = data;
        }


        /**
         * If second matrix split into 3 parts
         * | * * * | * | * * * | = | * |
         * | * * * |   | * * * |   | * |
         * | * * * |
         */
        Matrix multiplyTransposed(Matrix other, int partOneBased) {
            var range = Data.getRange(partOneBased);
            int offset = range.startIncl;
            int m = data.length;
            var n = data[0].length;
            int p = range.endExcl - range.startIncl;

            assert n == other.data.length : "Can't multiply matrices";

            var newMatrix = new int[n][p];

            for (var i = 0; i < m; i++) {
                for (var j = 0; j < n; j++) {
                    for (var k = 0; k < p; k++) {
                        newMatrix[i][k] += data[i][j] * other.data[j][k + offset];
                    }
                }
            }

            return new Matrix(newMatrix);
        }

        Matrix multiply(Matrix other, int partOneBased) {
            var range = Data.getRange(partOneBased);
            int m = range.endExcl - range.startIncl;
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

        public Vector toVector() {
            var row = data[0];
            return new Vector(row);
        }

        public Matrix add(Matrix other) {
            /* If number of tasks is smaller than `AMOUNT_OF_THREADS` */
            var isThreadIdle = data.length == 0;
            if (isThreadIdle) {
                return new Matrix(new int[0][]);
            }

            assert data.length == other.data.length : "Mismatch in len " + other.data.length + " Expected " + data.length;
            assert data[0].length == other.data[0].length : "Mismatch in len2 " + other.data[0].length + " Expected " + data[0].length;

            var matrix = IntStream.range(0, data.length)
                    .mapToObj(i -> IntStream.range(0, data[i].length)
                            .map(j -> data[i][j] + other.data[i][j])
                            .toArray())
                    .toArray(int[][]::new);
            return new Matrix(matrix);
        }
    }

    record Range(int startIncl, int endExcl) {
        IntStream stream() {
            return IntStream.range(startIncl, endExcl);
        }
    }

    public static class Monitor {
        public CountDown in = new CountDown(AMOUNT_OF_THREADS - 1);
        public CountDown sortFirstHalf = new CountDown(1);
        public CountDown sortSecondHalf = new CountDown(1);
        public CountDown sortSecondPart = new CountDown(1);
        public CountDown sortDone = new CountDown(1);
        public CountDown a = new CountDown(AMOUNT_OF_THREADS);
        public CountDown out = new CountDown(AMOUNT_OF_THREADS - 1);

        public static class CountDown {
            private int count;

            CountDown(int count) {
                this.count = count;
            }

            public synchronized void done() {
                assert count >= 0 : "Count should be greater or equal than 0";
                // TODO[question]: is the order matter?
                count--;
                notifyAll();
            }

            public synchronized void waitFor() {
                try {
                    while (count > 0) wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public static class SyncValue {
            private int value = 0;

            public synchronized void addSync(int delta) {
                value += delta;
            }

            public synchronized void setSync(int value) {
                this.value = value;
            }

            public int get() {
                return value;
            }
        }
    }

    private static Monitor.CountDown cd = new Monitor.CountDown(4);

    public static void setDummyValues() {
        d.setSync(5);
        B = new Vector(new int[]{6, 7, 6, 8});
        Z = new Vector(new int[]{0, 9, 0, 5});
        MM = new Matrix(new int[][]{{9, 4, 7, 7,}, {2, 4, 1, 0,}, {8, 2, 3, 2,}, {7, 7, 2, 2},});
        MX = new Matrix(new int[][]{{3, 8, 5, 0,}, {5, 5, 0, 8,}, {1, 4, 6, 2,}, {2, 9, 7, 6,},});
        MT = new Matrix(new int[][]{{8, 7, 3, 1,}, {3, 5, 4, 5,}, {3, 4, 7, 0,}, {2, 3, 5, 5,},});
        cd.done();
        cd.waitFor();
    }

}

class RunT1 implements Runnable {
    @Override
    public void run() {
        Data.printStart(Data.THREAD_1);

        try {
            // Початок вводу даних
            Data.MM = Data.matrixGet("MM", Data.THREAD_1);
            Data.MX = Data.matrixGet("MX", Data.THREAD_1);
            Data.B = Data.vectorGet("B", Data.THREAD_1);
            // Сигнал про введення
            Data.monitor.in.done();
            // Чекати на введення
            Data.monitor.in.waitFor();
            Data.setDummyValues();
            // Обчислення O1
            Data.calculateO(Data.THREAD_1);
            // Сортування O1
            Data.O.sort(Data.THREAD_1);
            // Чекати завершення сортування O2
            Data.monitor.sortFirstHalf.waitFor();
            // Обчислення O21
            Data.O.sortHalf(1);
            // Чекати завершення сортування O23
            Data.monitor.sortSecondPart.waitFor();
            // Сортування O
            Data.O.sort();
            // Сигнал завершення сортування O	
            Data.monitor.sortDone.done();
            // Обчислення a1
            var a1 = Data.B.dotProduct(Data.Z, Data.THREAD_1);
            // Сумування a (КД1)
            Data.a.addSync(a1);
            // Сигнал завершення сумування
            Data.monitor.a.done();
            // Чекати завершення сумування
            Data.monitor.a.waitFor();
            // Обчислення V1
            Data.calculate(Data.a.get(), Data.THREAD_1);
            // Сигнал завершення обчислень
            Data.monitor.out.done();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        Data.printEnd(Data.THREAD_1);
    }
}

class RunT2 implements Runnable {
    @Override
    public void run() {
        Data.printStart(Data.THREAD_2);

        try {
            // Початок вводу даних
            Data.Z = Data.vectorGet("Z", Data.THREAD_2);
            // Сигнал про введення
            Data.monitor.in.done();
            // Чекати на введення
            Data.monitor.in.waitFor();
            Data.setDummyValues();
            // Обчислення O2
            Data.calculateO(Data.THREAD_2);
            // Сортування O2
            Data.O.sort(Data.THREAD_2);
            // Сигнал про закінчення сортування
            Data.monitor.sortFirstHalf.done();
            // Чекати завершення сортування O
            Data.monitor.sortDone.waitFor();
            // Обчислення a1
            var a2 = Data.B.dotProduct(Data.Z, Data.THREAD_2);
            // Сумування a (КД1)
            Data.a.addSync(a2);
            // Сигнал завершення сумування
            Data.monitor.a.done();
            // Чекати завершення сумування
            Data.monitor.a.waitFor();
            // Обчислення V2
            Data.calculate(Data.a.get(), Data.THREAD_2);
            // Чекати завершення обчислень
            Data.monitor.out.waitFor();
            // Вивід результату обрахунків
            System.out.printf("\n[T%s] Result is \n%s;\n\n", Data.THREAD_2, Data.V.toString());
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        Data.printEnd(Data.THREAD_2);
    }
}

class RunT3 implements Runnable {
    @Override
    public void run() {
        Data.printStart(Data.THREAD_3);

        try {
            // Чекати на введення
            Data.monitor.in.waitFor();
            Data.setDummyValues();
            // Обчислення O3
            Data.calculateO(Data.THREAD_3);
            // Сортування O3
            Data.O.sort(Data.THREAD_3);
            // Чекати завершення сортування O4
            Data.monitor.sortSecondHalf.waitFor();
            // Обчислення O23
            Data.O.sortHalf(2);
            // Сигнал завершення сортування O23
            Data.monitor.sortSecondPart.done();
            // Чекати завершення сортування
            Data.monitor.sortDone.waitFor();
            // Обчислення a3
            var a3 = Data.B.dotProduct(Data.Z, Data.THREAD_3);
            // Сумування a (КД1)
            Data.a.addSync(a3);
            // Сигнал завершення сумування
            Data.monitor.a.done();
            // Чекати завершення сумування
            Data.monitor.a.waitFor();
            // Обчислення V3
            Data.calculate(Data.a.get(), Data.THREAD_3);
            // Сигнал завершення обчислень
            Data.monitor.out.done();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        Data.printEnd(Data.THREAD_3);
    }
}

class RunT4 implements Runnable {
    @Override
    public void run() {
        Data.printStart(Data.THREAD_4);

        try {
            // Початок вводу даних
            Data.MT = Data.matrixGet("MT", Data.THREAD_4);
            Data.d.setSync(Data.scalarGet("d", Data.THREAD_4));
            // Сигнал про введення
            Data.monitor.in.done();
            // Чекати на введення
            Data.monitor.in.waitFor();
            Data.setDummyValues();
            // Обчислення O4
            Data.calculateO(Data.THREAD_4);
            // Сортування O4
            Data.O.sort(Data.THREAD_4);
            // Сигнал про закінчення сортування  O4
            Data.monitor.sortSecondHalf.done();
            // Чекати завершення сортування
            Data.monitor.sortDone.waitFor();
            // Обчислення a4
            var a4 = Data.B.dotProduct(Data.Z, Data.THREAD_4);
            // Сумування a (КД1)
            Data.a.addSync(a4);
            // Сигнал завершення сумування
            Data.monitor.a.done();
            // Чекати завершення сумування
            Data.monitor.a.waitFor();
            // Обчислення V4
            Data.calculate(Data.a.get(), Data.THREAD_4);
            // Сигнал завершення обчислень
            Data.monitor.out.done();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        Data.printEnd(Data.THREAD_4);
    }
}

public class Main {
    public static void Lab2() throws InterruptedException {
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
        System.out.printf("Time: %sms", end - start);
    }

    public static void main(String[] args) throws InterruptedException {
        Lab2();
    }
}