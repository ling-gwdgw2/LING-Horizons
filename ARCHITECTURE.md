# LING Horizons - Technical Architecture & System Specification

This document provides a comprehensive technical breakdown of the architecture, memory layouts, GPU-driven rendering pipelines, storage subsystems, and hardware acceleration techniques utilized in **LING Horizons**.

---

## 1. High-Level Architecture Overview

LING Horizons is built upon a fully decoupled, multi-threaded client-server architecture designed to eliminate CPU rendering bottlenecks by shifting mesh generation, occlusion culling, and draw call dispatching entirely to modern GPU compute hardware.

```
+-------------------------------------------------------------------------------+
|                             Minecraft World / Server                          |
+-------------------------------------------------------------------------------+
                                       | (Block Updates & Chunk Packets)
                                       v
+-------------------------------------------------------------------------------+
|                       Ingestion & Voxelization Pipeline                       |
|  - WorldConversionFactory: Converts 16x16x16 ChunkSections into 32x32x32 DAGs |
|  - SIMD Vector Processing: AVX2/AVX-512 accelerated bitwise packing          |
+-------------------------------------------------------------------------------+
                                       |
                                       v
+-------------------------------------------------------------------------------+
|                        Multi-Engine Storage Subsystem                         |
|  - RocksDB 10.2 / LMDB / SQLite: Key-Value persistent sparse storage          |
|  - Zstandard (ZSTD) compression with LRU off-heap section caching             |
+-------------------------------------------------------------------------------+
                                       |
                                       v
+-------------------------------------------------------------------------------+
|                        Hierarchical Traversal Subsystem                       |
|  - CPU-side Frustum & Section Octree Filtering                                |
|  - Asynchronous Node Cleaner & Visibility Sorter                              |
+-------------------------------------------------------------------------------+
                                       |
                                       v
+-------------------------------------------------------------------------------+
|                      GPU-Driven MDIC Rendering Pipeline                       |
|  - Hi-Z Depth Pyramid & Hierarchical Occlusion Culling (raster.comp)          |
|  - GPU Compute Indirect Command Generator (cmdgen.comp, prep.comp)            |
|  - Multi-Draw Elements Indirect (glMultiDrawElementsIndirect)                 |
|  - Spherical Earth Curvature & Forward Water Shading (quads3.vert, quads.frag)|
+-------------------------------------------------------------------------------+
                                       |
                                       v
+-------------------------------------------------------------------------------+
|                    Iris Shaderpack Interoperability Layer                     |
|  - Dynamic Depth Samplers (dhDepthTex0/1/Water, vxDepthTexOpaque/Trans)       |
|  - Uniform Matrix Feed (vxProj, vxViewProj, dhFarPlane, dhNearPlane)          |
|  - G-Buffer Water Attachment (colortex16) for Photon / Iris PBR Passes        |
+-------------------------------------------------------------------------------+
```

---

## 2. Core Subsystems

### 2.1 Ingestion and Voxelization Subsystem (`me.ling.horizons.common.voxelization`)
- **VoxelizedSection**: Represents a 32x32x32 block volume (8x larger than a standard 16x16x16 vanilla section).
- **Sparse Voxel DAG Construction**: Identical sub-volumes (e.g., solid air, solid stone, repetitive underground patterns) share memory pointers across the hierarchy, reducing storage footprint by up to 90%.
- **SIMD Acceleration (`jdk.incubator.vector`)**: Vectorized algorithms process 8 to 16 voxels per instruction cycle for face occlusion checks, lighting extraction, and biome palette lookups.

### 2.2 Storage Engine (`me.ling.horizons.common.world.SaveLoadSystem*`)
- **RocksDB 10.2 Engine**: Embedded LSM-tree storage optimized for high write throughput during fast player movement.
- **LMDB & SQLite Backends**: Memory-mapped zero-copy alternative engines selectable via configuration.
- **Off-Heap Memory Arenas (`AllocationArena`, `MemoryBuffer`)**: Eliminates Java Garbage Collection (GC) pauses by allocating off-heap native memory buffers for voxel staging and GPU DMA uploads.

### 2.3 Traversal and Occlusion Engine (`me.ling.horizons.client.core.rendering.hierachical`)
- **HierarchicalOcclusionTraverser**: Traverses the global octree of sections from nearest to furthest.
- **Visibility Sorting**: Employs parallel radix sort and subgroup prefix sums (`prefixsum/inital3.comp`) on the GPU to order draw calls front-to-back, maximizing early-Z rejection.

---

## 3. GPU-Driven Rendering Pipeline (OpenGL 4.6 MDIC)

Traditional LOD mods generate vertex buffers on the CPU and issue thousands of individual draw calls. LING Horizons utilizes **GPU-Driven Multi-Draw Indirect with Compute Shaders (MDIC)**:

```
[ CPU ] Uploads Section Metadata & Viewport Matrices
   │
   ▼
[ GPU Compute: prep.comp ] Initializes command counters and memory barriers
   │
   ▼
[ GPU Compute: cmdgen.comp ] Evaluates Screen-Space Bounding Box & Hi-Z Culling
   │  ├── Visible Sections ──> Writes DrawArraysIndirectCommand into Draw Buffer
   │  └── Occluded Sections ─> Discarded instantly in GPU registers
   ▼
[ GPU Compute: buildtranslucents.comp ] Separates and sorts water/translucent quads
   │
   ▼
[ GPU Raster: quads3.vert & quads.frag ]
   ├── glMultiDrawElementsIndirect(GL_TRIANGLES)
   ├── Spherical Planet Curvature Vertex Transformation
   └── PBR / Biome Tinting / Water Normal Synthesis
```

### 3.1 Shader Stages
1. **`cmdgen.comp`**: Evaluates section bounding boxes against the view frustum and the Hierarchical Z-Buffer (Hi-Z). Generates OpenGL indirect draw parameters directly in VRAM.
2. **`prep.comp`**: Resets atomic counters and parameter buffers across frames without CPU round-trips.
3. **`buildtranslucents.comp`**: Extracts translucent faces (water, ice, stained glass) into a secondary indirect draw buffer for ordered alpha blending.
4. **`quads3.vert`**: Decodes compact 64-bit packed vertex attributes, applies viewport matrices, and computes spherical planetary curvature transforms.
5. **`quads.frag`**: Evaluates PBR lighting, normal mapping, Screen-Space Reflections (SSR), and forwards G-buffer parameters to shaderpacks.

---

## 4. Iris & Shaderpack Interoperability Architecture

LING Horizons includes a dedicated compatibility bridge for **Iris 1.8+** and modern shaderpacks (**Photon**, **Complementary**, **Bliss**, **Solas**):

```
+-------------------+---------------------------------------------------------+
| Interface         | Implementation & Functionality                          |
+-------------------+---------------------------------------------------------+
| Macro Defines     | Defines VOXY, voxy, LING_HORIZONS, ling_horizons,       |
|                   | and DISTANT_HORIZONS in Iris StandardMacros.            |
+-------------------+---------------------------------------------------------+
| Dynamic Samplers  | Exposes vxDepthTexOpaque, vxDepthTexTrans, dhDepthTex0, |
|                   | dhDepthTex1, dhWaterDepthTex, and dhTerrainDepthTex.    |
+-------------------+---------------------------------------------------------+
| G-Buffer Water    | Allocates colortex16 (RGBA16) for Photon Shader         |
|                   | forward water shading and physical depth absorption.    |
+-------------------+---------------------------------------------------------+
| Uniform Streaming | Provides vxProj, vxProjInv, vxViewProj, vxModelView,     |
|                   | dhNearPlane, dhFarPlane, and linear fog distances.      |
+-------------------+---------------------------------------------------------+
```

---

## 5. Threading and Concurrency Model

LING Horizons maintains an asynchronous execution model across dedicated thread pools:

- **Main Render Thread**: Exclusively handles lightweight GPU state binding, buffer swapping, and indirect draw dispatches (sub-millisecond execution time).
- **`UnifiedServiceThreadPool`**: Background worker pool managing multi-threaded voxelization, noise sampling, and RocksDB disk I/O.
- **Off-Heap Cleaner Thread**: Performs non-blocking memory reclamation for unloaded world sections without invoking the JVM Garbage Collector.

---

## 6. Directory and Package Layout

```
me.ling.horizons
├── client
│   ├── config                  # Sodium & NeoForge Configuration GUI
│   ├── core                    # Render Pipelines, Viewports, Model Stores
│   │   ├── gl                  # OpenGL 4.6 Wrappers (FBO, VAO, SSBO, Texture)
│   │   ├── model               # Block Model Baker, Texture Atlases, MipGen
│   │   └── rendering           # MDIC Backends, Hierarchical Octree, Post-Blit
│   ├── iris                    # Iris Shaderpack Bridge, Uniforms, Samplers
│   └── mixin                   # Client Minecraft, Sodium, and Iris Mixins
├── common
│   ├── thread                  # Unified Threadpool & Context Executors
│   ├── util                    # Native Memory Buffers, SIMD CPU Layouts
│   ├── voxelization            # Sparse Voxel Octree DAG & Conversion Factory
│   └── world                   # RocksDB / LMDB Save-Load Engines
└── commonImpl                  # Common Bootstrap & Data Importers
```
