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

Google's `litert-runtime` skill (Android skills v1.0.17, Apache-2.0) is vendored for the generated projects at `templates/android/.agents/skills/litert-runtime/`.
Read `templates/android/.agents/skills/litert-runtime/SKILL.md` and resolve every relative path in it from that folder.
When you change a template, also read the `litert-runtime` row of the "Google's Android skills" table in that template's `CLAUDE.md`:
it lists where the generated projects differ, and it wins wherever the two disagree.
