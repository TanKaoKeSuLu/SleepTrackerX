# Changelog

All notable changes to this project will be documented in this file.


# v0.6.0 - Streaming Audio Architecture

## Added

- Implemented streaming audio analysis architecture.
- Added real-time sound event detection during recording.
- Added RingBuffer based PCM processing.
- Added EventClipWriter for event audio segment generation.
- Added local audio volume analysis utility.


## Changed

- Removed full-night PCM offline analysis workflow.
- Changed recording pipeline from file-first processing to stream-first processing.
- Improved SleepRecordService lifecycle management.
- Improved audio event broadcast flow.
- Improved dark/light theme color adaptation.
- Optimized bottom navigation appearance.


## Fixed

- Fixed navigation stack duplication issue.
- Fixed sleep record database transaction problems.
- Fixed short recording producing invalid files.
- Fixed audio event persistence issues.