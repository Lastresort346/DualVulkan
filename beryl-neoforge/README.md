# Beryl NeoForge with Dual GPU Support

## Overview

This is a NeoForge 1.21.1 port of Beryl with enhanced dual GPU rendering capabilities, integrated with DualVulkan's multi-GPU workload distribution system. This version enables true multi-GPU shader rendering, allowing complex shader packs like Eclipse to leverage multiple GPUs for improved performance.

## Features

### Dual GPU Rendering
- **Shadow Mapping**: Distributed shadow rendering across primary and secondary GPUs
- **Terrain Rendering**: Parallel terrain processing with workload splitting
- **Translucent Rendering**: Offloaded translucent effects to secondary GPU
- **Entity Rendering**: Distributed entity shader processing
- **Post-Processing**: Parallel bloom and atmospheric effects
- **Cloud Rendering**: GPU-accelerated cloud rendering with workload distribution

### Eclipse Shader Integration
- **Enhanced Compatibility**: Full Eclipse shader pack support with dual GPU optimization
- **Workload Distribution**: Intelligent distribution of Eclipse shader effects across GPUs
- **Performance Optimization**: Reduced frame times through parallel processing
- **Memory Management**: Optimized VRAM usage across multiple GPUs

### NeoForge 1.21.1 Support
- **Modern Architecture**: Built for NeoForge 1.21.1 with updated APIs
- **Compatibility**: Maintains compatibility with existing Beryl shader features
- **Performance**: Optimized for modern Minecraft versions

## Requirements

- **NeoForge 1.21.1**: Latest stable version
- **DualVulkan**: Multi-GPU Vulkan rendering backend
- **Java 21**: Required for NeoForge 1.21.1
- **Multiple GPUs**: At least 2 GPUs for dual GPU rendering
- **VRAM**: Minimum 4GB per GPU recommended for Eclipse shader pack

## Installation

1. **Install NeoForge 1.21.1**
   ```bash
   # Download and install NeoForge 1.21.1
   ```

2. **Install DualVulkan**
   ```bash
   # Install DualVulkan as prerequisite
   # Place DualVulkan jar in mods folder
   ```

3. **Install Beryl NeoForge**
   ```bash
   # Place beryl-neoforge-1.21.1-0.1.3-alpha+1.jar in mods folder
   ```

4. **Install Eclipse Shader Pack**
   ```bash
   # Place Eclipse shader pack in shaderpacks folder
   # Copy to: minecraft/shaderpacks/Eclipse-Shader-Unstable/
   ```

## Configuration

### Beryl Configuration
Edit `config/beryl_settings.json`:
```json
{
  "shadersOn": true,
  "shadowResolution": 4096,
  "shadowRenderDistance": 144,
  "bloomIntensity": 1.0,
  "atmFogIntensity": 1.0,
  "dualGPUEnabled": true,
  "workloadDistribution": {
    "shadows": "primary",
    "terrain": "secondary",
    "translucent": "secondary",
    "entities": "secondary",
    "postProcessing": "secondary"
  }
}
```

### Eclipse Shader Configuration
Edit `shaderpacks/Eclipse-Shader-Unstable/shaders/shaders.properties`:
```properties
# Enable dual GPU optimizations
dual_gpu.enabled=true
dual_gpu.shadow_quality=ULTRA
dual_gpu.lighting_quality=ULTRA
dual_gpu.volumetric_quality=HIGH
```

## Building from Source

### Prerequisites
- Java 21 JDK
- Gradle 8.0+
- DualVulkan source code

### Build Commands
```bash
# Clone the repository
git clone <repository-url>
cd beryl-neoforge

# Build DualVulkan dependency first
cd ../DualVulkan
./gradlew build

# Build Beryl NeoForge
cd ../beryl-neoforge
./gradlew build

# The built jar will be in build/libs/
```

### Development Setup
```bash
# Setup development environment
./gradlew genNeoForgeRuns
./gradlew eclipse  # or idea for IntelliJ

# Run in development mode
./gradlew runClient
```

## Dual GPU Architecture

### Workload Distribution Strategy

The system uses DualVulkan's `MultiGPUWorkloadDistributor` to intelligently split rendering workloads:

1. **Primary GPU**: Handles shadow mapping and core terrain rendering
2. **Secondary GPU**: Handles translucent effects, entities, and post-processing
3. **Memory Management**: Shared uniform buffers and texture synchronization
4. **Frame Synchronization**: Ensures proper frame timing across GPUs

### Rendering Pipeline

```
┌─────────────────┐    ┌─────────────────┐
│   Primary GPU   │    │  Secondary GPU  │
├─────────────────┤    ├─────────────────┤
│ Shadow Mapping  │    │ Translucent     │
│ Terrain Base    │    │ Entity Shaders  │
│ Depth Buffers   │    │ Post-Processing │
│ Main Render     │    │ Atmospheric     │
└─────────────────┘    └─────────────────┘
         │                       │
         └──────────┬──────────┘
                    │
            ┌─────────────┐
            │  Frame Sync  │
            └─────────────┘
```

## Performance Optimization

### Memory Usage
- **VRAM Distribution**: Balances VRAM usage across GPUs
- **Texture Streaming**: Optimized texture loading for multi-GPU
- **Buffer Management**: Efficient uniform buffer sharing

### Frame Rate Optimization
- **Parallel Processing**: Reduces frame time through parallel rendering
- **GPU Utilization**: Maximizes usage of available GPU resources
- **Load Balancing**: Dynamic workload adjustment based on GPU capabilities

## Troubleshooting

### Common Issues

1. **Dual GPU Not Working**
   - Verify DualVulkan is properly installed
   - Check GPU driver compatibility
   - Ensure both GPUs support Vulkan

2. **Eclipse Shader Issues**
   - Verify shader pack compatibility
   - Check VRAM availability
   - Reduce shader quality if needed

3. **Performance Problems**
   - Monitor GPU utilization
   - Check workload distribution settings
   - Adjust shadow resolution

### Debug Information

Enable debug logging in `config/beryl_settings.json`:
```json
{
  "debugLogging": true,
  "gpuInfo": true,
  "workloadStats": true
}
```

## Contributing

### Development Guidelines
1. Follow NeoForge 1.21.1 modding standards
2. Maintain dual GPU compatibility
3. Test with multiple GPU configurations
4. Ensure Eclipse shader compatibility

### Pull Request Process
1. Fork the repository
2. Create feature branch
3. Test dual GPU functionality
4. Submit pull request with testing results

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

- **Beryl Team**: Original Beryl shader mod
- **Eclipse Team**: Eclipse shader pack
- **DualVulkan Team**: Multi-GPU Vulkan backend
- **NeoForge Team**: NeoForge modding framework

## Support

For support and issues:
- **GitHub Issues**: Report bugs and feature requests
- **Discord**: Community support and discussion
- **Documentation**: Additional guides and tutorials
