#ifndef TUPAZ_GPU_BENCHMARK_H_
#define TUPAZ_GPU_BENCHMARK_H_

#include <cstdint>

namespace tupaz {
namespace benchmark {

struct BenchmarkResult {
    double fp16_tflops;
    double vulkan_megapixels_per_sec;
    double memory_bandwidth_gbps;
    double total_time_ms;
};

/**
 * @brief Run fast native micro-tests (<4s execution) measuring GPU capabilities.
 * @return BenchmarkResult containing performance metrics.
 */
BenchmarkResult run_gpu_benchmark();

} // namespace benchmark
} // namespace tupaz

#endif // TUPAZ_GPU_BENCHMARK_H_
