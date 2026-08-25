# Credits & Attribution for LING Horizons

## Original Work & Attribution
**LING Horizons** is built upon and inspired by the revolutionary voxel Level-of-Detail (LOD) architecture developed by **Cortex** ([MCRcortex](https://github.com/MCRcortex)) in the [Voxy](https://github.com/MCRcortex/voxy) project.

- **Original Project**: Voxy (by Cortex / MCRcortex)
- **Original License**: GNU Lesser General Public License v3.0 (LGPL-3.0)
- **Repository**: https://github.com/MCRcortex/voxy

We express our deepest gratitude to Cortex and the Voxy contributors for their groundbreaking research in sparse voxel DAGs, GPU compute culling, and real-time Minecraft world geometry streaming.

---

## LING Horizons Development & Enhancements
**LING Horizons** by **LING** introduces major enhancements, modernization, and dedicated ecosystem bridges:

1. **Modernized NeoForge 1.21.1 Architecture**:
   - Complete native port to **NeoForge 1.21.1** with Java 21, SIMD Vector API (`jdk.incubator.vector`), and Parchment mappings.
   - Independent namespace and modular architecture under `me.ling.horizons`.
2. **Advanced Shaderpack Interoperability & Reverse Engineering**:
   - Native compatibility bridge for **Iris 1.8+** and modern shaderpacks (**Photon Shader**, **Complementary**, **Solas**, **Bliss**).
   - Dedicated Forward Water Shading pipelines, `colortex16` G-buffer integration, and DH sampler emulation (`dhWaterDepthTex`, `dhTerrainDepthTex`).
3. **Planetary Curvature & Atmospheric Visuals**:
   - Real-time spherical Earth curvature vertex shader transforms.
   - Smooth Linear Distance Fog scaling and dynamic Screen-Space Water Reflections (SSR).
4. **Enhanced Sodium / NeoForge UI & Commands**:
   - Modernized UI configuration menu with complete English localization.
   - Built-in administrative and pregeneration commands (`/ling` & `/ling_horizons`).

---

## License Notice
In compliance with the **GNU Lesser General Public License v3.0 (LGPL-3.0)**, LING Horizons is distributed under the terms of the LGPL-3.0. You may find a copy of the license in the `LICENSE` file.
