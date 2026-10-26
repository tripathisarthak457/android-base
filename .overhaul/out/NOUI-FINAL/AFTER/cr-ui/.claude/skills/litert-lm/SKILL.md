---
name: litert-lm
description: Builds an Android app that runs an open text LLM on the device, on the
  CPU or the GPU, with the LiteRT-LM Kotlin API (com.google.ai.edge.litertlm:litertlm-android).
  Use this skill when creating a chat, summarization, or extraction feature with a
  .litertlm model from Hugging Face litert-community (Gemma, Qwen, Llama, Phi), including
  the Gradle dependency, the GPU manifest entries, getting the model file onto the
  device, Engine initialization off the main thread, a streamed multi-turn Conversation
  with sendMessageAsync, and the AndroidViewModel and Compose screen. Don't use for
  running .tflite vision, audio, or embedding models with the CompiledModel API, for
  image or audio input, tool calling, or NPU backends, or for converting models.
---

Google's `litert-lm` skill is vendored at `.agents/skills/litert-lm/` (Android skills v1.0.17, Apache-2.0).
First read the `litert-lm` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/litert-lm/SKILL.md` and resolve every
relative path in it from that folder.
