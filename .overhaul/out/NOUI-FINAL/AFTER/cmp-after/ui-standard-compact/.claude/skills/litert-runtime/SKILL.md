---
name: litert-runtime
description: Configures and implements LiteRT (.tflite) on-device inference on Android
  in Kotlin using the CompiledModel API on CPU and GPU. Use this skill for any Android
  LiteRT or .tflite task, including Gradle setup, image classification or vision/audio/embedding
  helpers, TensorBuffer reuse, Bitmap preprocessing, AndroidViewModel GPU-to-CPU fallback
  with warm-up on a single-thread dispatcher, migrating from legacy org.tensorflow.lite.Interpreter
  or GpuDelegate, and latency benchmarking. Don't use for running generative text
  LLMs with .litertlm files or for Python model conversion.
---

Google's `litert-runtime` skill is vendored at `.agents/skills/litert-runtime/` (Android skills v1.0.17, Apache-2.0).
First read the `litert-runtime` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/litert-runtime/SKILL.md` and resolve every
relative path in it from that folder.
