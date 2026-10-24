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

Google's `litert-lm` skill (Android skills v1.0.17, Apache-2.0) is vendored for the generated projects at `templates/android/.agents/skills/litert-lm/`.
Read `templates/android/.agents/skills/litert-lm/SKILL.md` and resolve every relative path in it from that folder.
When you change a template, also read the `litert-lm` row of the "Google's Android skills" table in that template's `CLAUDE.md`:
it lists where the generated projects differ, and it wins wherever the two disagree.
