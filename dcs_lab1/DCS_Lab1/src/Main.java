import kotlin.Triple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Function;
import java.util.function.Supplier;

interface IMatrix {
    /**
     * ```
     * | a11 a12 a13 |   | b11 b12 |   | c11 c12 |
     * | a21 a22 a23 | * | b21 b22 | =
     * | a31 a32 a33 |   | b31 b32 |
     * ```
     * mXn * nXp = mXp
     */
    static Matrix multiply(IMatrix MA, IMatrix MB, int rowStart, int rowEnd, int colStart, int colEnd) {
        if (rowEnd == -1) rowEnd = MA.size(0) - 1;
        if (colEnd == -1) colEnd = MB.size(1) - 1;

        int m = MA.size(0);
        int n = MA.size(1);
        int p = MA.size(1);

        assert n == MB.size(0) : "Can't multiply matrices";
        assert rowEnd >= rowStart : "rowEnd must be greater than or equal to rowStart";
        assert rowEnd < m : "rowEnd must be less than the number of rows in MA";
        assert colEnd >= colStart : "colEnd must be greater than or equal to colStart";
        assert colEnd < p : "colEnd must be less than the number of columns in MB";

        int numRows = rowEnd - rowStart + 1;
        int numCols = colEnd - colStart + 1;
        double[][] result = new double[numRows][numCols];

        for (int i = rowStart; i <= rowEnd; i++) {
            for (int k = colStart; k <= colEnd; k++) {
                final int ii = i;
                final int kk = k;
                var sum = Summator.kahanSum(
                        0, n - 1,
                        (j) -> MA.get(ii, j) * MB.get(j, kk)
                );
                result[i - rowStart][k - colStart] = sum;
// Equivalent to:
//                for (int j = 0; j < n; j++) {
//                    result[i - rowStart][k - colStart] += MA.get(i, j) * MB.get(j, k);
//                }
            }
        }

        return new Matrix(result);
    }

    static Matrix multiplyScalar(IMatrix MA, double scalar, int rowStart, int rowEnd, int colStart, int colEnd) {
        if (rowEnd == -1) rowEnd = MA.size(0) - 1;
        if (colEnd == -1) colEnd = MA.size(1) - 1;

        assert rowEnd >= rowStart : "rowEnd must be greater than or equal to rowStart";
        assert rowEnd < MA.size(0) : "rowEnd must be less than the number of rows in MA";
        assert colEnd >= colStart : "colEnd must be greater than or equal to colStart";
        assert colEnd < MA.size(1) : "colEnd must be less than the number of columns in MA";

        int m = rowEnd - rowStart + 1;
        int n = colEnd - colStart + 1;

        double[][] result = new double[m][n];

        for (int i = rowStart; i <= rowEnd; i++) {
            for (int j = colStart; j <= colEnd; j++) {
                result[i - rowStart][j - colStart] = MA.get(i, j) * scalar;
            }
        }

        return new Matrix(result);
    }

    static Matrix sum(IMatrix MA, IMatrix MB) {
        return sum(MA, MB, 1);
    }

    static Matrix diff(IMatrix MA, IMatrix MB) {
        return sum(MA, MB, -1);
    }

    private static Matrix sum(IMatrix MA, IMatrix MB, int sign) {
        sign = Integer.signum(sign);

        int m = MA.size(0);
        int n = MA.size(1);

        assert m == MB.size(0) : "Can't sum matrices";
        assert n == MB.size(1) : "Can't sum matrices";

        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double[] values = {MA.get(i, j), sign * MB.get(i, j)};
                result[i][j] = Arrays.stream(values).sorted().sum();
            }
        }

        return new Matrix(result);
    }

    static double min(IMatrix A, int rowStart, int rowEnd, int colStart, int colEnd) {
        if (rowEnd == -1) rowEnd = A.size(0) - 1;
        if (colEnd == -1) colEnd = A.size(1) - 1;

        assert rowEnd >= rowStart : "rowEnd must be greater than or equal to rowStart";
        assert rowEnd < A.size(0) : "rowEnd must be less than the number of rows in A";
        assert colEnd >= colStart : "colEnd must be greater than or equal to colStart";
        assert colEnd < A.size(1) : "colEnd must be less than the number of columns in A";

        double minValue = Double.MAX_VALUE;

        for (int i = rowStart; i <= rowEnd; i++) {
            for (int j = colStart; j <= colEnd; j++) {
                double value = A.get(i, j);
                minValue = Math.min(minValue, value);
            }
        }

        return minValue;
    }

    double get(int i, int j);

    int size(int dim);

    class TransposeView implements IMatrix {
        private final IMatrix MA;

        public TransposeView(IMatrix MA) {
            this.MA = MA;
        }

        @Override
        public double get(int i, int j) {
            return MA.get(j, i);
        }

        @Override
        public int size(int dim) {
            if (dim == 0) return MA.size(1);
            else if (dim == 1) return MA.size(0);
            else
                throw new IllegalArgumentException("Invalid dimension: " + dim);
        }
    }

    class OffsetView implements IMatrix {
        private final IMatrix MA;
        private final int colOffset;
        private final int colLen;

        public OffsetView(IMatrix MA, int colOffset, int colLen) {
            this.MA = MA;
            this.colOffset = colOffset;
            this.colLen = colLen;
        }

        @Override
        public double get(int i, int j) {
            return MA.get(i, j + colOffset);
        }

        @Override
        public int size(int dim) {
            if (dim == 0) return MA.size(0);
            else if (dim == 1) return this.colLen;
            else
                throw new IllegalArgumentException("Invalid dimension: " + dim);
        }
    }
}

class Summator {
    static double kahanSum(int start, int end, Function<Integer, Double> doubleProvider) {
        double sum = 0;
        double err = 0;
        for (int i = start; i <= end; i++) {
            var corrected = doubleProvider.apply(i) - err;
            var newSum = sum + corrected;
            err = (newSum - sum) - corrected;
            sum = newSum;
        }
        return sum;
    }
}

class Matrix implements IMatrix {
    private final double[][] MA;

    public Matrix(double[][] MA) {
        this.MA = MA;
    }

    static Matrix fromVector(double[] vector) {
        var matrix = new double[1][vector.length];
        matrix[0] = vector;
        return new Matrix(matrix);
    }

    public IMatrix getTransposeView() {
        return new TransposeView(this);
    }

    public IMatrix getOffsetView(int start, int end) {
        return new OffsetView(this, start, end - start + 1);
    }

    public Matrix copy() {
        var copy = new double[MA.length][MA[0].length];
        for (int i = 0; i < MA.length; i++) {
            copy[i] = Arrays.copyOf(MA[i], MA[i].length);
        }
        return new Matrix(copy);
    }

    @Override
    public double get(int i, int j) {
        return MA[i][j];
    }

    @Override
    public int size(int dim) {
        if (dim == 0) return MA.length;
        else if (dim == 1) return MA[0].length;
        else throw new IllegalArgumentException("Invalid dimension: " + dim);
    }
}

record Data(
        Matrix MB,
        Matrix MT,
        Matrix MC,
        Matrix MM,

        Matrix D,
        Matrix C
) {
    Data deepCopy() {
        return new Data(
                MB.copy(),
                MT.copy(),
                MC.copy(),
                MM.copy(),
                D.copy(),
                C.copy()
        );
    }
}

record Conf(
        int N,
        int amountOfThreads
) {
    public Conf {
        assert N % amountOfThreads == 0 : "N must be divisible by amountOfThreads";
    }
}

class FileUtils {
    static String baseDir = "data/";

    static String getInputFileName(int N) {
        return N + "_input.txt";
    }

    static String getOutputFileName(int N) {
        return N + "_output.txt";
    }

    public static void writeResult(int N, Triple<Long, Matrix[], Matrix[]> result) {
        long time = result.getFirst();
        Matrix[] MG_i = result.getSecond();
        Matrix[] B_i = result.getThird();
        var file = new File(baseDir, getOutputFileName(N));
        try (var fileWriter = new FileWriter(file)) {
            fileWriter.write("Time: " + time + " ms\n");
            fileWriter.write("\nMG:\n");
            writeMatrixColParts(fileWriter, MG_i);
            fileWriter.write("\nB:\n");
            writeMatrixRowParts(fileWriter, B_i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeMatrixColParts(FileWriter fileWriter, Matrix[] matrices) throws IOException {
        for (int k = 0; k < matrices[0].size(0); k++) {
            for (Matrix matrix : matrices) {
                for (int j = 0; j < matrix.size(1); j++) {
                    var result = matrix.get(k, j) + " ";
                    fileWriter.write(result);
                }
            }
            fileWriter.write("\n");
        }
    }

    private static void writeMatrixRowParts(FileWriter fileWriter, Matrix[] matrices) throws IOException {
        for (Matrix matrix : matrices) {
            for (int k = 0; k < matrices[0].size(0); k++) {
                for (int j = 0; j < matrix.size(1); j++) {
                    var result = matrix.get(k, j) + " ";
                    fileWriter.write(result);
                }
                fileWriter.write("\n");
            }
        }
    }

    public static Data readData(int N) {
        var file = new File(baseDir, getInputFileName(N));
        if (!file.exists()) {
            generateRandomData(N, file);
        }

        try (var scanner = new Scanner(file)) {
            double[][] MB = new double[N][N];
            double[][] MT = new double[N][N];
            double[][] MC = new double[N][N];
            double[][] MM = new double[N][N];
            double[] C = new double[N];
            double[] D = new double[N];

            for (var matrix : new double[][][]{MB, MT, MC, MM}) {
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        matrix[i][j] = scanner.nextDouble();
                    }
                }
            }

            for (var vector : new double[][]{C, D}) {
                for (int i = 0; i < N; i++) {
                    vector[i] = scanner.nextDouble();
                }
            }

            return new Data(
                    new Matrix(MB),
                    new Matrix(MT),
                    new Matrix(MC),
                    new Matrix(MM),
                    Matrix.fromVector(D),
                    Matrix.fromVector(C)
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateRandomData(int N, File file) {
        var random = new Random(42);
        var getDigit = (Supplier<Double>) () -> {
            int digits = random.nextInt(10) + 1;
            double result = Math.round(random.nextDouble() * Math.pow(10, digits)) / Math.pow(10, digits);
            return result;
        };

        try (var fileWriter = new FileWriter(file)) {
            for (int t = 0; t < 4; t++) {
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        var num = getDigit.get();
                        fileWriter.write(num + " ");
                    }
                    fileWriter.write("\n");
                }
                fileWriter.write("\n");
            }

            for (int t = 0; t < 2; t++) {
                for (int i = 0; i < N; i++) {
                    var num = getDigit.get();
                    fileWriter.write(num + "\n");
                }
                fileWriter.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * =============================================================================
 * =============================================================================
 * =============================================================================
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            var times = new ArrayList<Long>();
            for (int N = 500; N <= 1000; N += 50) {
                var conf = new Conf(N, Thread.activeCount());
                var data = FileUtils.readData(conf.N());
                var result = Runner.run(conf, data, false);
//                var result = Runner.run(conf, data, true);
                FileUtils.writeResult(conf.N(), result);
                System.out.println("N: " + conf.N() + ", time: " + result.getFirst() + " ms");
                times.add(result.getFirst());
            }

            System.out.println(times);
        }
    }
}


class Runner {

    public static Triple<Long, Matrix[], Matrix[]> run(Conf conf, Data data, boolean isUsingGlobals) throws InterruptedException {
        var ts = new T[conf.amountOfThreads()];
        var threads = new Thread[conf.amountOfThreads()];
        var context = new MinContext(conf.amountOfThreads());
        for (int i = 0; i < conf.amountOfThreads(); i++) {
            int diapason = conf.N() / conf.amountOfThreads();
            int rowStart = i * diapason;
            int rowEnd = (i + 1) * diapason - 1;
            Data fullCopy = isUsingGlobals ? data : data.deepCopy();
            ts[i] = new T(fullCopy, rowStart, rowEnd, context);
            threads[i] = new Thread(ts[i]);
        }

        long startTime = System.currentTimeMillis();

        for (var thread : threads) {
            thread.start();
        }

        for (var thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        Matrix[] result1 = new Matrix[conf.amountOfThreads()];
        Matrix[] result2 = new Matrix[conf.amountOfThreads()];
        for (int i = 0; i < ts.length; i++) {
            result1[i] = ts[i].MG_i;
            result2[i] = ts[i].B_i;
        }

        return new Triple<>(elapsedTime, result1, result2);
    }

    private static class T implements Runnable {
        private final int start;
        private final int end;
        private final Data data;
        private final MinContext minContext;
        Matrix MG_i;
        Matrix B_i;

        public T(Data data, int start, int end, MinContext minContext) {
            this.data = data;
            this.start = start;
            this.end = end;
            this.minContext = minContext;
        }

        private void calc1() {
            var MB_MM_i = IMatrix.multiply(
                    data.MB(),
                    data.MM(),
                    0, -1,
                    start, end
            );
            var MB_MM_MT_i = IMatrix.sum(
                    MB_MM_i,
                    data.MT().getOffsetView(start, end)
            );
            var MC_MB_MM_MT_i =
                    IMatrix.multiply(
                            data.MC(),
                            MB_MM_MT_i,
                            0, -1,
                            0, -1
                    );
            var MB_MT_i = IMatrix.multiply(
                    data.MB(),
                    data.MT(),
                    0, -1,
                    start, end
            );
            MG_i = IMatrix.sum(
                    MB_MT_i,
                    MC_MB_MM_MT_i
            );
        }

        private void calc2() throws BrokenBarrierException, InterruptedException {
            var min_D_i = IMatrix.min(data.D(), 0, -1, start, end);
            var min_D = minContext.compareAndGet(min_D_i);
            var min_d_C = IMatrix.multiplyScalar(data.C(), min_D, 0, -1, start, end);
            var MT_D_i = IMatrix.multiply(
                    data.MT(),
                    data.D().getTransposeView(),
                    start, end,
                    0, -1
            );
            B_i = IMatrix.diff(
                    MT_D_i,
                    min_d_C.getTransposeView()
            );
        }

        @Override
        public void run() {
            try {
                calc1();
                calc2();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class MinContext {
    private final CyclicBarrier cb;
    private double min = Double.MAX_VALUE;

    public MinContext(int amountOfThreads) {
        cb = new CyclicBarrier(amountOfThreads);
    }

    public double compareAndGet(double min) throws BrokenBarrierException, InterruptedException {
        synchronized (this) {
            this.min = Math.min(this.min, min);
        }
        cb.await();
        return this.min;
    }
}

