# LING Horizons - ประวัติการอัปเดต (Changelog)

เอกสารรวบรวมประวัติการพัฒนา การแก้ไขบั๊ก และการปรับปรุงประสิทธิภาพของ **LING Horizons** (GPU-driven Voxel LOD Engine สำหรับ Minecraft NeoForge 1.21.1) ตั้งแต่เวอร์ชันเริ่มต้นจนถึงเวอร์ชันปัจจุบัน

---

## เวอร์ชัน 1.0.3 (เวอร์ชันปัจจุบัน)

### การแก้ไขข้อผิดพลาดสำคัญ (Bug Fixes)
- **แก้ไขปัญหาฐานล่างเกาะ The End และมิติพิกัด Y ติดลบขาดหาย (End Island Bottom Cutoff)**:
  - แก้ไขการเลื่อนบิตการคำนวณขอบเขตแนวตั้งใน `LingRenderSystem` จาก `>> 5` เป็น `>> 4` (`level.getMinSection() >> 4`) ให้สอดคล้องกับโครงสร้าง TopLevel Octree Node Level 4 ($2^4 = 16$ Sections = 256 บล็อก) ทำให้พื้นที่ระดับ $Y < 0$ และ $Y > 256$ ในมิติต่างๆ ถูกติดตามและเรนเดอร์ครบถ้วน 100%
- **ปรับปรุงการดึง Chunk Sections เข้าสู่ระบบ (Chunk Section Ingestion)**:
  - ขยายเงื่อนไขการรับ Section ใน `MixinRenderSectionManager` เป็น `(this.cachedChunkStatus & 1) != 0 || this.cachedChunkStatus == 3` รองรับการ Ingest ทั้ง Chunk ปกติและ Chunk ในเงามืด (Dark Sections / Boundary Chunks) อย่างสมบูรณ์
- **แก้ไขอาการเกมค้างขณะออกจากโลก (Disconnect Freeze / Deadlock)**:
  - ปรับระบบ Lifecycle ของ `SectionSavingService`, `ClientLodStreamManager`, และ `LingInstance` ให้หยุดการทำงานและ Flush ข้อมูลแบบ Non-blocking เคลียร์เธรดเบื้องหลังได้อย่างหมดจด

### การปรับปรุงประสิทธิภาพและเสถียรภาพ (Performance & Stability)
- **ระบบ Lock-Free Mapper Array**:
  - เปลี่ยนโครงสร้าง Biome/Block Mapper เป็นแบบ Lock-Free ป้องกันอาการเธรดชนกัน (Thread Contention) ขณะบินสำรวจโลกด้วยความเร็วสูง
- **จำกัดคิวคำขอขึ้น GPU (Bounded GPU Request Queue)**:
  - จำกัดคิวคำขอข้อมูล GPU ไว้ที่สูงสุด 1024 รายการ ป้องกันปัญหาหน่วยความจำ Heap และ Native Buffer เต็ม (Out of Memory)
- **การจัดการหน่วยความจำปลอดภัย (Native Memory Buffer Safety)**:
  - เพิ่มการคืนหน่วยความจำ Native Pointer (`buffer.free()`) และปลดล็อก Section (`section.release()`) ในบล็อก `finally` เสมอ

---

## เวอร์ชัน 1.0.2

### ฟีเจอร์ใหม่ (New Features)
- **ระบบสตรีมมิ่งข้อมูลข้ามเครือข่าย (Network LOD Streaming Architecture)**:
  - เพิ่มระบบสตรีมมิ่งก้อน LOD จากฝั่ง Dedicated Server ไปยัง Client แบบ Real-time ผ่าน Custom Packets (`LingLodDataPayload`, `LingLodRequestPayload`)
  - บีบอัดข้อมูลด้วย Zstandard (Zstd) ช่วยประหยัดแบนด์วิดท์เครือข่ายได้มากกว่า 70%
- **การเชื่อมต่อกับ Chunky Pregenerator (Native Chunky Integration)**:
  - รองรับการสร้างก้อนข้อมูล LOD อัตโนมัติในเบื้องหลังขณะรันคำสั่ง Pregen ของม็อด Chunky

### การปรับปรุงระบบ
- เพิ่มระบบ Heartbeat Auto-Recovery ตรวจจับสถานะการเชื่อมต่อเซิร์ฟเวอร์และรีเซ็ตคิวอัตโนมัติเมื่อ Reconnect

---

## เวอร์ชัน 1.0.1

### การแก้ไขข้อผิดพลาด (Bug Fixes)
- **แก้ไขปัญหารอยต่อทะลุระหว่าง Chunk ปกติกับก้อน LOD (Chunk Boundary Gaps)**:
  - พัฒนาระบบ Conservative Inset Bounding ปิดรอยต่อผิวน้ำและพื้นดินระหว่าง Vanilla Render Distance กับ LOD ให้แนบสนิท
- **แก้ไขอาการจอกะพริบและก้อน LOD กะพริบ (Terrain & Screen Flickering)**:
  - ปรับระบบสร้างคำสั่งเรนเดอร์ใน `cmdgen.comp` ให้ใช้ Hardware SSBO Atomics พร้อมผูก DMA Memory Barriers (`GL_FRAMEBUFFER_BARRIER_BIT | GL_PIXEL_BUFFER_BARRIER_BIT`)

### การปรับปรุงความสวยงาม (Visual Refinements)
- ปรับปรุงการเกลี่ยแสง Lightmap (Voxy 0.2.9 Lighting Refinements) ให้ความสว่างของแสงบล็อกและแสงท้องฟ้าเชื่อมต่อกับ Vanilla Chunk อย่างเป็นธรรมชาติ

---

## เวอร์ชัน 1.0.0 (Initial Release)

### ฟีเจอร์หลัก (Core Features)
- **GPU-Driven Voxel LOD Engine**:
  - สถาปัตยกรรมเรนเดอร์ LOD ยุคใหม่ ขับเคลื่อนด้วย Compute Shaders และ Hierarchical Octree Traversal บน OpenGL 4.6
- **Hi-Z Screen-Space Occlusion Culling**:
  - ระบบตัดก้อนบล็อกที่ถูกบดบังใน Screen Space ระดับพิกเซล เพิ่มอัตราเฟรมเรต (FPS) อย่างมหาศาล
- **Dual Pipeline Architecture**:
  - `NormalRenderPipeline`: รองรับการแสดงผลระยะหมอก Vanilla 3D Fog และ SSAO คุณภาพสูง
  - `IrisLingRenderPipeline`: รองรับการเชื่อมต่อกับ Iris Shaders อย่างเต็มรูปแบบ
