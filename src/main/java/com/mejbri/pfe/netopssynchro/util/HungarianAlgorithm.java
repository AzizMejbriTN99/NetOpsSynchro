package com.mejbri.pfe.netopssynchro.util;

import java.util.Arrays;

public final class HungarianAlgorithm {

    private HungarianAlgorithm() {}

    public static int[] solve(double[][] cost) {
        int rows = cost.length;
        int cols = cost[0].length;
        int n    = Math.max(rows, cols);

        // Pad to square matrix with large values so padding assignments are never chosen
        double[][] c = new double[n][n];
        double BIG   = 1e9;
        for (double[] row : c) Arrays.fill(row, BIG);
        for (int r = 0; r < rows; r++)
            for (int cc = 0; cc < cols; cc++)
                c[r][cc] = cost[r][cc];

        double[] u = new double[n + 1];
        double[] v = new double[n + 1];
        int[]  p   = new int[n + 1]; // p[j] = row assigned to column j
        int[]  way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minVal = new double[n + 1];
            boolean[] used  = new boolean[n + 1];
            Arrays.fill(minVal, Double.MAX_VALUE);

            do {
                used[j0] = true;
                int i0 = p[j0], j1 = -1;
                double delta = Double.MAX_VALUE;
                for (int j = 1; j <= n; j++) {
                    if (!used[j]) {
                        double cur = c[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minVal[j]) {
                            minVal[j] = cur;
                            way[j] = j0;
                        }
                        if (minVal[j] < delta) {
                            delta = minVal[j];
                            j1 = j;
                        }
                    }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta; }
                    else          { minVal[j] -= delta; }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] result = new int[rows];
        Arrays.fill(result, -1);
        for (int j = 1; j <= n; j++) {
            int row = p[j] - 1;
            if (row < rows && j - 1 < cols)
                result[row] = j - 1;
        }
        return result;
    }
}
