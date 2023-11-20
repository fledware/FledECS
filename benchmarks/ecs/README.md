# ecs benchmarks

```
Benchmark                          (entityCount)   Mode  Cnt      Score      Error  Units
AshleyBenchmark.baseline                    4096  thrpt    5  21234.810 ±  315.440  ops/s
AshleyBenchmark.baseline                   16384  thrpt    5   5426.821 ± 1499.220  ops/s
AshleyBenchmark.baseline                   65536  thrpt    5    726.956 ±   23.169  ops/s
AshleyBenchmark.baseline                  262144  thrpt    5     97.238 ±    1.131  ops/s
AshleyBenchmark.baseline                 1048576  thrpt    5     24.285 ±    0.137  ops/s
FledEcsBenchmark.baseline                   4096  thrpt    5  17597.418 ± 2379.521  ops/s
FledEcsBenchmark.baseline                  16384  thrpt    5   5940.391 ± 2402.456  ops/s
FledEcsBenchmark.baseline                  65536  thrpt    5   1481.796 ±  182.751  ops/s
FledEcsBenchmark.baseline                 262144  thrpt    5    270.792 ±    4.805  ops/s
FledEcsBenchmark.baseline                1048576  thrpt    5     48.309 ±    0.221  ops/s
FledEcsExecutorBenchmark.baseline           4096  thrpt    5  16645.252 ± 2015.462  ops/s
FledEcsExecutorBenchmark.baseline          16384  thrpt    5   6996.322 ± 1620.204  ops/s
FledEcsExecutorBenchmark.baseline          65536  thrpt    5   3709.586 ±  489.573  ops/s
FledEcsExecutorBenchmark.baseline         262144  thrpt    5   1287.309 ±   20.613  ops/s
FledEcsExecutorBenchmark.baseline        1048576  thrpt    5    296.060 ±    4.638  ops/s
FledEcsForkJoinBenchmark.baseline           4096  thrpt    5  16937.044 ± 1596.430  ops/s
FledEcsForkJoinBenchmark.baseline          16384  thrpt    5   7899.669 ± 2712.194  ops/s
FledEcsForkJoinBenchmark.baseline          65536  thrpt    5   4359.957 ±  550.060  ops/s
FledEcsForkJoinBenchmark.baseline         262144  thrpt    5   1381.765 ±   81.109  ops/s
FledEcsForkJoinBenchmark.baseline        1048576  thrpt    5    323.250 ±   39.113  ops/s
```
