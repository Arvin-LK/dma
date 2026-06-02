"""Convert PNG icon to multi-resolution .ico for Windows"""
from PIL import Image

img = Image.open('dma-desktop/src/main/resources/icon.png')
print(f'Source: {img.size}, mode={img.mode}')

# Ensure RGBA
if img.mode != 'RGBA':
    img = img.convert('RGBA')

# Standard Windows icon sizes
sizes = [(16,16), (24,24), (32,32), (48,48), (64,64), (128,128), (256,256)]
icons = []

for w, h in sizes:
    if w <= img.width and h <= img.height:
        # Downscale from source (if source is larger)
        r = img.resize((w, h), Image.LANCZOS)
    else:
        # Upscale (will be blurry but better than nothing)
        r = img.resize((w, h), Image.LANCZOS)
    icons.append(r)

# Save as multi-resolution .ico
icons[0].save(
    'dma-desktop/src/main/resources/icon.ico',
    format='ICO',
    sizes=sizes,
    append_images=icons[1:]
)

# Verify
import os
size_kb = os.path.getsize('dma-desktop/src/main/resources/icon.ico') / 1024
print(f'icon.ico created ({size_kb:.1f} KB) with {len(sizes)} resolutions: {[f"{w}x{h}" for w,h in sizes]}')
