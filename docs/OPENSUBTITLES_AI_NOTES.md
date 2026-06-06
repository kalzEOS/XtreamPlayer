# OpenSubtitles AI Integration Notes

Created by ChatGPT branch.

Goal:
- Add subtitle translation for downloaded subtitles.
- Later add AI transcription when no subtitles exist.

Suggested API surface:
- translateSubtitle(fileId, targetLanguage)
- translateSubtitleBytes(content, sourceLanguage, targetLanguage)

Reasoning:
Your existing subtitle search/download flow is already solid. AI translation can be layered on top without changing the player architecture.
