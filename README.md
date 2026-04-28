# Spray Problem Generator

[日本語版はこちら](./README.ja.md)

Spray Problem Generator is an Android app for registering climbing hold information from a bouldering wall photo and generating problems based on that data.

The app allows users to register hold positions, hold attributes, scores, and reach reference settings on a wall image. The registered hold data can then be used to create spray wall problems.

## Overview

This app is designed for spray walls in bouldering gyms, home walls, and similar climbing walls.

Main use cases include:

- Registering a wall image
- Automatically detecting hold candidates
- Manually editing hold information
- Setting a reach reference
- Assigning start and goal candidate attributes
- Setting scores for each hold
- Generating problems from registered holds

The automatic detection feature is intended as a support tool, not as a fully automatic final detection system.  
Detected holds are expected to be reviewed and adjusted manually by the user.

## Features

### Wall Registration

Users can add a new wall from the wall list screen.

The registration flow is as follows:

1. Take a photo or select an image from the device
2. Crop the wall image
3. Automatically detect hold candidates
4. Manually edit holds
5. Set reach reference
6. Set hold attributes
7. Set hold scores
8. Save the wall

### Automatic Hold Detection

The user manually selects wall color samples on the image.  
The app uses those colors as the background reference and extracts hold candidates based on the difference from the background.

Adjustable parameters include:

- Color range
- Brightness range
- Minimum saturation
- Background difference threshold

After automatic detection, users can manually add, remove, or edit holds.

### Hold Editing

The hold editing feature supports the following operations:

- Add holds
- Add to existing holds
- Erase parts of holds
- Delete holds

Adding and appending are handled in the same editing mode.  
When the user draws over an existing hold, the app can either append the drawn area to that hold or create a new hold, depending on the selected setting.

If erasing a hold splits it into multiple separate regions, each separated region is kept as an individual hold.

### Reach Reference

Users can select two points on the image and enter the real-world distance between them.  
This setting is used as a reference for distance and reach calculations during problem generation.

### Hold Attributes

Each hold can be assigned the following attributes:

- Start candidate
- Goal candidate

These attributes can be used when selecting start and goal holds during problem generation.

### Hold Scores

Each hold can be assigned a score.

Scores are used for filtering, difficulty adjustment, and problem generation settings.

### Problem Generation

Users can generate problems from registered wall data.

Problem generation supports the following modes:

- Specify start and goal holds
- Let the app choose start and goal holds

Common generation settings include:

- Hold selection range
- Score range
- Maximum number of holds
- Additional route generation parameters

Generated problems are highlighted on the wall image, and users can regenerate problems using the same settings.

### Display Color Settings

Users can customize app-wide display colors.

Configurable colors include:

- Hold outline color
- Selection color
- Range selection color
- Start / goal color

These settings are shared across the entire app and are preserved after restarting the app.

## Screen Flow

The main screen flow is as follows:

```text
Wall List
 ├─ Add New Wall
 │   └─ Select Image / Take Photo
 │       └─ Crop Image
 │           └─ Automatic Hold Detection
 │               └─ Hold Editing
 │                   └─ Reach Reference
 │                       └─ Hold Attributes
 │                           └─ Hold Scores
 │                               └─ Save
 │
 ├─ Edit Existing Wall
 │   ├─ Hold Editing
 │   ├─ Reach Reference
 │   ├─ Hold Attributes
 │   └─ Hold Scores
 │
 ├─ Generate Problem
 │   └─ Common Settings
 │       ├─ Specify Start / Goal
 │       └─ Auto Select Start / Goal
 │
 └─ Display Color Settings
```

## Saved Data

### Data Saved Per Wall

- Wall image
- Hold shapes
- Hold attributes
- Hold scores
- Reach reference settings

### App-Wide Saved Data

- Display color settings

## Tech Stack

- Android
- Kotlin
- Jetpack Compose
- MVVM architecture

## Development Policy

This app prioritizes manual adjustability over fully automatic hold recognition.

Automatic hold detection is designed to create initial candidates only.  
Final hold data is expected to be refined through manual editing.

Because the app requires detailed editing on wall images, screens such as hold editing, reach reference setting, and problem generation support image zooming.

## Possible Future Features

- Saving generated problems
- Problem history management
- Improved difficulty estimation
- Improved automatic hold detection
- Chalk and shadow correction
- Hold color-based detection settings

## License

This repository is intended to be released under the Unlicense.

See the `LICENSE` file for details.