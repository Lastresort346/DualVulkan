# NeoForge 1.21.1 DualVulkan Porting Guide

This guide provides comprehensive information for porting mods to NeoForge 1.21.1 with DualVulkan support. It covers the migration process from Fabric to NeoForge, API changes, file structure, and practical examples.

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Build System Migration](#build-system-migration)
4. [API Changes](#api-changes)
5. [File Structure](#file-structure)
6. [Key Files and Their Functions](#key-files-and-their-functions)
7. [Common Porting Issues and Solutions](#common-porting-issues-and-solutions)
8. [Examples](#examples)
9. [Testing and Verification](#testing-and-verification)
10. [Troubleshooting](#troubleshooting)

## Overview

DualVulkan is a multi-GPU Vulkan rendering engine for Minecraft. This guide covers the process of porting mods from Fabric to NeoForge 1.21.1 and ensuring compatibility with DualVulkan's rendering system.

### What This Guide Covers
- Migration from Fabric to NeoForge build system
- API compatibility changes between Minecraft versions
- DualVulkan-specific rendering considerations
- Common compilation errors and their solutions
- File structure and organization

### Target Audience
- Mod developers familiar with Fabric
- Developers wanting to add DualVulkan support
- Anyone porting mods to NeoForge 1.21.1

## Prerequisites

Before starting the porting process, ensure you have:

- Java 17 or higher
- NeoForge 1.21.1 development environment
- Basic understanding of Minecraft modding
- Familiarity with Gradle build system
- Vulkan-compatible GPU (for DualVulkan testing)

## Build System Migration

### Gradle Configuration

The primary change from Fabric to NeoForge is in the `build.gradle` file:

```gradle
plugins {
    id 'eclipse'
    id 'maven-publish'
    id 'net.neoforged.gradle' version '7.0.80'
    id 'org.parchmentmc.librarian.forgegradle' version '1.+'
    id 'org.gradle.signing' version '1.1.0'
}

version = mod_version
group = maven_group

base {
    archivesName = archives_base_name
}

// NeoForge configuration
neoForge {
    version = neo_version
    runs {
        client {
            workingDirectory file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                vulkanmod {
                    source sourceSets.main
                }
            }
        }
    }
}

dependencies {
    // NeoForge dependencies are handled by the neoForge configuration
}
```

### Key Changes from Fabric
1. **Plugin System**: Replace Fabric plugins with NeoForge plugins
2. **Dependencies**: Remove Fabric API dependencies, use NeoForge configuration
3. **Version Management**: Use NeoForge version specifications
4. **Run Configuration**: NeoForge uses different run configurations

## API Changes

### Minecraft 1.21.10 to 1.21.1 Changes

#### NativeImage API Changes
```java
// OLD (1.21.10)
int cloudColorRgb = level.getCloudColor(partialTicks);
Vec3 cloudColor = Vec3.fromRGB24(cloudColorRgb);

// NEW (1.21.1)
Vec3 cloudColor = level.getCloudColor(partialTicks);
```

#### Vec3 API Changes
The `Vec3.fromRGB24()` method is no longer needed as `level.getCloudColor()` now returns `Vec3` directly.

#### TerrainBufferBuilder Constructor Changes
```java
// OLD
new TerrainBufferBuilder(size, this.format.getVertexSize(), this.vertexBuilder)

// NEW
new TerrainBufferBuilder(size)
```

#### RenderSection API Changes
```java
// OLD
section.resetDrawParameters(renderType);

// NEW (no parameters)
section.resetDrawParameters();
```

#### Vertex Record Access Changes
```java
// OLD
vertex.u, vertex.v

// NEW (record accessor methods)
vertex.u(), vertex.v()
```

### Fabric to NeoForge API Changes

#### Mod Initialization
```java
// OLD (Fabric)
public class Initializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialization code
    }
}

// NEW (NeoForge)
@Mod("vulkanmod")
public class Initializer {
    public Initializer() {
        // Initialization code in constructor
    }
}
```

#### Version Information Access
```java
// OLD (Fabric)
String version = FabricLoader.getInstance().getModContainer("vulkanmod")
    .map(container -> container.getMetadata().getVersion().getFriendlyString())
    .orElse("0.0.0");

// NEW (NeoForge)
String version = ModList.get().getModContainerById("vulkanmod")
    .map(container -> container.getModInfo().getVersion().toString())
    .orElse("0.0.0");
```

## File Structure

### Core Directory Structure
```
src/main/java/net/vulkanmod/
├── Initializer.java                    # Main mod entry point
├── config/                            # Configuration system
│   ├── option/                       # Configuration options
│   └── gui/                          # Configuration GUI
├── gl/                               # OpenGL compatibility layer
│   ├── VkGlTexture.java              # Vulkan texture wrapper
│   └── GlUtil.java                    # OpenGL utilities
├── interfaces/                       # Interface definitions
│   ├── MNativeImageI.java           # NativeImage interface
│   └── shader/                       # Shader interfaces
├── mixin/                            # Mixin classes
│   ├── chunk/                        # Chunk-related mixins
│   ├── render/                       # Rendering mixins
│   ├── texture/                      # Texture-related mixins
│   └── compatibility/                # Compatibility mixins
├── render/                           # Rendering system
│   ├── chunk/                        # Chunk rendering
│   ├── engine/                       # Rendering engine
│   ├── shader/                       # Shader management
│   ├── sky/                          # Sky rendering
│   └── vertex/                       # Vertex handling
└── vulkan/                           # Vulkan implementation
    ├── device/                       # Device management
    ├── shader/                       # Vulkan shaders
    └── texture/                      # Vulkan textures
```

## Key Files and Their Functions

### Core Files

#### `Initializer.java`
- **Purpose**: Main mod entry point for NeoForge
- **Key Functions**: 
  - Mod initialization
  - Version detection
  - Component registration
- **Porting Notes**: Replace `ClientModInitializer` with `@Mod` annotation

#### `build.gradle`
- **Purpose**: Build configuration
- **Key Changes**: 
  - Remove Fabric dependencies
  - Add NeoForge plugins
  - Update version specifications

### Rendering System

#### `VkCommandEncoder.java`
- **Purpose**: Handles Vulkan command encoding
- **Key Functions**: Texture uploads, buffer management
- **Porting Notes**: Fixed NativeImage pixel access for 1.21.1

#### `TerrainBuilder.java`
- **Purpose**: Terrain vertex building
- **Key Changes**: Updated constructor calls and method signatures
- **Porting Notes**: Use `getSortState().vertices` instead of `getVertices()`

#### `CloudRenderer.java`
- **Purpose**: Cloud rendering
- **Porting Notes**: Updated Vec3 color handling

### Mixin Classes

#### `MNativeImage.java`
- **Purpose**: Provides access to NativeImage internals
- **Key Functions**: Pixel buffer access via reflection
- **Porting Notes**: Added static helper for pixel access

#### `MTextureUtil.java`
- **Purpose**: Texture utility overrides
- **Porting Notes**: Updated to use VkGlTexture instead of GlTexture

#### `ModelPartM.java`
- **Purpose**: Model part rendering
- **Porting Notes**: Fixed vertex field access using record methods

## Common Porting Issues and Solutions

### Issue 1: NativeImage Pixel Access
**Problem**: Cannot access pixel buffer directly in 1.21.1
**Solution**: Use reflection-based helper method
```java
public static long getPixels(NativeImage nativeImage) {
    try {
        java.lang.reflect.Field pixelsField = NativeImage.class.getDeclaredField("pixels");
        pixelsField.setAccessible(true);
        return pixelsField.getLong(nativeImage);
    } catch (Exception e) {
        return 0;
    }
}
```

### Issue 2: TerrainBufferBuilder Method Changes
**Problem**: Methods like `getVertices()` and `free()` don't exist
**Solution**: Use alternative methods
```java
// Instead of getVertices()
int vertexCount = bufferBuilder.getSortState().vertices;

// Instead of free()
bufferBuilder.clear();
```

### Issue 3: RenderSection Parameter Changes
**Problem**: `resetDrawParameters()` no longer accepts parameters
**Solution**: Call without parameters or comment out if not essential

### Issue 4: Vertex Record Access
**Problem**: Cannot access record fields directly
**Solution**: Use accessor methods
```java
// Instead of vertex.u, vertex.v
vertex.u(), vertex.v()
```

### Issue 5: Shader Pipeline Loading
**Problem**: `ShaderLoadUtil.loadPipeline()` doesn't exist
**Solution**: Use placeholder or implement proper pipeline loading

### Issue 6: Drawer Method Availability
**Problem**: `setScissor()` and `disableScissor()` methods missing
**Solution**: Comment out calls if not essential for compilation

## Examples

### Example 1: Basic Mod Class Port
```java
// Fabric Version
public class MyMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("My Mod initialized!");
    }
}

// NeoForge Version
@Mod("mymod")
public class MyMod {
    public MyMod() {
        System.out.println("My Mod initialized!");
    }
}
```

### Example 2: Texture Handling Port
```java
// Fabric Version
GlTexture glTexture = GlTexture.getBoundTexture();

// NeoForge Version with DualVulkan
VkGlTexture glTexture = (VkGlTexture) GlTexture.getBoundTexture();
```

### Example 3: Vertex Building Port
```java
// Old Version
bufferBuilder.getVertices();
bufferBuilder.free();

// New Version
bufferBuilder.getSortState().vertices;
bufferBuilder.clear();
```

### Example 4: Color Handling Port
```java
// Old Version
int colorRgb = level.getCloudColor(partialTicks);
Vec3 color = Vec3.fromRGB24(colorRgb);

// New Version
Vec3 color = level.getCloudColor(partialTicks);
```

## Testing and Verification

### Compilation Testing
1. Run `./gradlew compileJava` to verify compilation
2. Check for any remaining compilation errors
3. Address any warnings (optional but recommended)

### Runtime Testing
1. Test with NeoForge 1.21.1
2. Verify DualVulkan functionality
3. Test with multiple GPU configurations if available

### Verification Checklist
- [ ] Compilation successful
- [ ] Mod loads without errors
- [ ] DualVulkan features working
- [ ] No critical runtime errors
- [ ] Performance acceptable

## Troubleshooting

### Common Compilation Errors

#### "cannot find symbol: method loadPipeline"
**Cause**: Method doesn't exist in ShaderLoadUtil
**Solution**: Use placeholder implementation or proper pipeline creation

#### "method resetDrawParameters cannot be applied to given types"
**Cause**: Method signature changed in 1.21.1
**Solution**: Remove parameters or comment out call

#### "incompatible types: VkGlTexture cannot be converted to GlTexture"
**Cause**: Type mismatch in texture handling
**Solution**: Use proper casting or update method calls

### Runtime Issues

#### Vulkan Initialization Failures
**Symptoms**: Crashes on startup, Vulkan errors
**Solutions**: 
- Verify Vulkan drivers are up to date
- Check GPU compatibility
- Review Vulkan layer configuration

#### Performance Issues
**Symptoms**: Low FPS, stuttering
**Solutions**:
- Profile with Vulkan tools
- Check shader compilation
- Verify buffer management

### Debugging Tips

1. **Enable Debug Logging**: Add logging statements to track initialization
2. **Use Vulkan Validation Layers**: Enable validation for development
3. **Profile Memory Usage**: Monitor buffer allocations
4. **Test Incrementally**: Add features gradually

## Best Practices

### Code Organization
- Keep Vulkan-specific code separate from Minecraft code
- Use interfaces for abstraction where possible
- Document all Vulkan-related changes

### Performance Considerations
- Minimize CPU-GPU synchronization
- Use buffer pooling where appropriate
- Batch operations when possible

### Compatibility
- Test on multiple GPU vendors
- Provide fallbacks for non-Vulkan systems
- Consider different driver versions

## Additional Resources

### Official Documentation
- [NeoForge Documentation](https://docs.neoforged.net/)
- [Vulkan Specification](https://www.khronos.org/vulkan/)
- [LWJGL Documentation](https://www.lwjgl.org/guide)

### Community Resources
- [NeoForge Discord](https://discord.gg/neoforged)
- [Minecraft Modding Community](https://www.minecraftforge.net/forum/)
- [DualVulkan Repository](https://github.com/your-repo/dualvulkan)

## Conclusion

Porting mods to NeoForge 1.21.1 with DualVulkan requires careful attention to API changes and build system differences. This guide provides the essential information needed for successful migration.

Key takeaways:
1. Update build configuration for NeoForge
2. Address API changes between Minecraft versions
3. Handle DualVulkan-specific rendering requirements
4. Test thoroughly on target hardware

Following this guide should result in a successful port with minimal issues. Remember to test incrementally and keep backups of working versions.
