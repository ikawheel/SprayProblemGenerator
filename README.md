# Spray Problem Generator

[日本語版はこちら](./README.ja.md)

Spray Problem Generator is an Android app that lets you register hold information from a spray wall photo and automatically generate climbing problems from that data.

It is designed for spray walls such as gym spray walls and home walls. You can register holds on a wall image, then generate problems based on conditions such as start and goal holds or the number of holds to use.  
Generated problems can be saved and reviewed later from a list or a detail screen.

https://github.com/user-attachments/assets/ab455a4d-bdb2-4269-8319-6d0531f99a5f

https://github.com/user-attachments/assets/4a26d5af-e9c3-43cd-98bf-5c071fc32b0a

## Main Features

- Register wall images
  - Take a photo with the camera
  - Select an image from the device
- Crop wall images
- Automatically extract hold candidates
- Manually adjust hold shapes
- Set a reference used to estimate wall size
- Set start / goal candidates
- Set hold difficulty scores
- Generate problems from saved walls
- Save generated problems
- View saved problems in a list and detail screen
- Save problem images to the device
- Customize display colors
- View licenses

## Basic Usage

### 1. Register a Wall

Take a photo with the camera or select an image from the device to register a wall image for problem generation.

You can crop the image as needed to adjust the area used as the wall.

### 2. Register Holds

Register holds on the wall image.

After generating candidates with automatic extraction, you can add, modify, or remove holds manually as needed.  
Small holds or holds that are difficult to extract can also be registered through manual adjustment.

### 3. Set Problem-Generation Information

You can set supporting information used for problem generation.

- Reference for wall-size estimation
- Start candidates
- Goal candidates
- Difficulty score for each hold

Start / goal candidates and hold difficulty scores are optional.  
Problems can still be generated even if they are not set.

### 4. Generate Problems

Generate problems using the registered wall data.

You can either specify the start and goal yourself, or let the app choose them automatically.

Generated problems can be saved and reviewed later.

## Screen Structure

The main screen flow is as follows:

```text
Wall List
 ├─ Add Wall
 │   └─ Select Image / Take Photo
 │       └─ Crop Image
 │           └─ Hold Registration / Adjustment
 │               └─ Problem-Generation Settings
 │                   └─ Save Wall
 │
 ├─ Edit Wall
 │   ├─ Hold Adjustment
 │   ├─ Wall Size Settings
 │   ├─ Start / Goal Settings
 │   └─ Hold Difficulty Settings
 │
 ├─ Problem Generation
 ├─ Problem List
 │   └─ Problem Details
 │
 ├─ Display Settings
 └─ Licenses
```

## Data That Can Be Saved

This app mainly saves the following kinds of data.

### Wall Data

- Wall image
- Hold information
- Reference for wall-size estimation
- Start / goal candidates
- Hold difficulty scores

### Problem Data

- Wall used for the problem
- Start / goal
- Holds used in the problem
- Hold order
- Creation timestamp

### App Settings

- Hold display color
- Start / goal display color
- Display settings such as stroke width

## Tech Stack

- Android
- Kotlin
- Jetpack Compose
- MVVM

## License

See `LICENSE` for details.
