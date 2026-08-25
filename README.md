# LING Horizons

**LING Horizons** is a next-generation, high-performance GPU-driven **Voxel Level-of-Detail (LOD)** rendering engine for **Minecraft 1.21.1 on NeoForge**.

Featuring **Sparse Voxel DAGs**, **SIMD Vector Acceleration**, **OpenGL 4.6 Multi-Draw Indirect with Compute Shaders (MDIC)**, **Planetary Curvature**, and seamless compatibility with modern shaderpacks (**Photon**, **Complementary**, **Bliss**, **Solas**).

---

## Features

- **GPU-Driven MDIC Pipeline**: Near-zero CPU overhead by delegating occlusion culling, command generation, and meshlet processing directly to OpenGL 4.6 compute shaders.
- **Java 21 SIMD Vector Acceleration**: Hardware-accelerated voxel processing, face culling, and bitwise manipulations using `jdk.incubator.vector`.
- **Planetary Spherical Curvature**: Real-time vertex shader world curvature transformation for realistic spherical horizons.
- **Seamless Shaderpack & Water Pipeline**:
  - Full compatibility with **Iris 1.8+**
  - Dedicated **Photon Shader** G-buffer (`colortex16`), physical ocean absorption, and dynamic reflections.
  - Native and fallback shaderpatch resolution (`ling_horizons.json` / `voxy.json`).
- **Full 3D Voxel World**: Unlike traditional 2.5D heightmap LODs, LING Horizons accurately renders complex overhangs, floating islands, deep caverns, and multi-layered water surfaces.
- **Multi-Engine Storage Architecture**: High-speed chunk persistence powered by **RocksDB 10.2**, **LMDB**, or **SQLite**.
- **In-Game Management**:
  - Sodium Video Settings Integration
  - NeoForge Mod Config Spec Screen
  - Administrative and pregeneration commands: `/ling` & `/ling_horizons`

---

## Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge 21.1.0+
- **Java**: Java 21 (JDK 21) with `--add-modules jdk.incubator.vector`
- **GPU**: OpenGL 4.6 compatible GPU (NVIDIA GTX 900+ / AMD GCN 2+ / Intel Arc)
- **Companion Mods**: Sodium 0.8+ / Iris 1.8+ (Recommended)

---

## Building from Source

To build LING Horizons, clone the repository and run Gradle:

```bash
git clone https://github.com/ling-gwdgw2/LING-Horizons.git
cd LING-Horizons
./gradlew build
```

The compiled mod JAR will be located in `build/libs/lingHorizons-V1-1.0.0.jar`.

---

## Credits & License

- **LING Horizons** is authored by **LING**.
- Built upon and inspired by the voxel LOD research from the **Voxy** project by **Cortex** ([MCRcortex](https://github.com/MCRcortex)).
- Distributed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**. See [LICENSE](LICENSE) and [CREDITS.md](CREDITS.md) for full details.
