package fastcamera.benchmark;

import fastcamera.CameraDevice;
import fastcamera.FastCamera;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark {

    @org.openjdk.jmh.annotations.Benchmark
    public List<CameraDevice> benchmarkEnumerateDevices() {
        try {
            return FastCamera.enumerateDevices();
        } catch (Throwable t) {
            return null;
        }
    }
}
