# Icon Guide for TCraft Client

The installer script supports custom icons for each platform. If you want to add custom icons to your installers, follow these instructions:

## Required Icon Formats

- **macOS (.dmg)**: `assets/icon.icns` (ICNS format)
- **Windows (.exe)**: `assets/icon.ico` (ICO format)
- **Linux (.deb)**: `assets/icon.png` (PNG format, recommended: 512x512px)

## Creating Icons from Existing Images

### Option 1: Use Online Tools

1. **IconConverter** (https://iconverticons.com/online/)
   - Upload your PNG image
   - Select target format (ICNS, ICO, PNG)
   - Download and place in `assets/` folder

2. **CloudConvert** (https://cloudconvert.com/)
   - Supports conversion to ICNS, ICO, and other formats

### Option 2: Use Command Line Tools

#### For macOS (.icns):

```bash
# Install ImageMagick (if not installed)
brew install imagemagick

# Create icon set directory
mkdir icon.iconset

# Generate different sizes
for size in 16 32 128 256 512; do
    sips -z $size $size assets/minecraft_title.png --out icon.iconset/icon_${size}x${size}.png
done

# Create ICNS file
iconutil -c icns icon.iconset -o assets/icon.icns

# Clean up
rm -rf icon.iconset
```

#### For Windows (.ico):

```bash
# Using ImageMagick
convert assets/minecraft_title.png -resize 256x256 assets/icon.ico
```

#### For Linux (.png):

```bash
# Simply resize to 512x512 if needed
convert assets/minecraft_title.png -resize 512x512 assets/icon.png
```

## Quick Setup

If you have a source image (e.g., `minecraft_title.png`), you can use this script:

```bash
#!/bin/bash

# Create all icon formats from a single PNG image
SOURCE_IMAGE="assets/minecraft_title.png"

# For macOS
mkdir icon.iconset
for size in 16 32 128 256 512; do
    sips -z $size $size $SOURCE_IMAGE --out icon.iconset/icon_${size}x${size}.png
done
iconutil -c icns icon.iconset -o assets/icon.icns
rm -rf icon.iconset

# For Linux
convert $SOURCE_IMAGE -resize 512x512 assets/icon.png

# For Windows (requires ImageMagick)
convert $SOURCE_IMAGE -resize 256x256 assets/icon.ico

echo "✓ All icons created successfully!"
```

## Current Status

Currently, the installer builds **without custom icons** and uses the default Java application icon. The installer will automatically use custom icons once they are placed in the `assets/` directory with the correct filenames.

