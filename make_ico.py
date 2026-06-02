"""Design professional DMA icon from scratch"""
import struct, io, math
from PIL import Image, ImageDraw, ImageFont

SIZES = [16, 24, 32, 48, 64, 128, 256]

def create_icon(size):
    """Create a DMA icon at the given size"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    margin = max(1, size // 16)
    r = size // 8  # corner radius

    # ── Background: rounded square with gradient ──
    # Draw rounded rectangle
    draw.rounded_rectangle(
        [margin, margin, size - margin, size - margin],
        radius=r,
        fill=(26, 115, 232, 255)  # #1a73e8
    )

    # ── Text: "DMA" ──
    # Calculate font size proportional to icon size
    font_size = int(size * 0.52)

    # Try to use a clean system font
    font = None
    font_paths = [
        "C:/Windows/Fonts/segoeui.ttf",      # Segoe UI
        "C:/Windows/Fonts/segoeuib.ttf",      # Segoe UI Bold
        "C:/Windows/Fonts/arialbd.ttf",        # Arial Bold
        "C:/Windows/Fonts/arial.ttf",          # Arial
        "C:/Windows/Fonts/calibrib.ttf",       # Calibri Bold
        "C:/Windows/Fonts/calibri.ttf",        # Calibri
    ]
    for fp in font_paths:
        try:
            font = ImageFont.truetype(fp, font_size)
            break
        except Exception:
            continue
    if font is None:
        font = ImageFont.load_default()

    text = "DMA"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = (size - tw) // 2 - bbox[0]
    ty = (size - th) // 2 - bbox[1] - int(size * 0.02)

    # Subtle text shadow (only for larger sizes)
    if size >= 48:
        draw.text((tx + max(1, size//64), ty + max(1, size//64)),
                  text, font=font, fill=(13, 71, 161, 80))  # dark blue shadow

    # White text
    draw.text((tx, ty), text, font=font, fill=(255, 255, 255, 255))

    return img


# ── Generate all sizes ──
png_data_list = []
for size in SIZES:
    img = create_icon(size)
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    png_data_list.append(buf.getvalue())
    print(f'  {size}x{size} OK')

# ── Save 256x256 as icon.png ──
create_icon(256).save('dma-desktop/src/main/resources/icon.png')
print(f'icon.png saved')

# ── Save 32x32 as icon-32.png ──
create_icon(32).save('dma-desktop/src/main/resources/icon-32.png')
print(f'icon-32.png saved')

# ── Write multi-resolution .ico ──
with open('dma-desktop/src/main/resources/icon.ico', 'wb') as f:
    f.write(struct.pack('<HHH', 0, 1, len(SIZES)))
    header_size = 6 + len(SIZES) * 16
    offset = header_size
    offsets = []
    for data in png_data_list:
        offsets.append(offset)
        offset += len(data)
    for i, s in enumerate(SIZES):
        f.write(struct.pack('<BBBBHHII',
            s if s < 256 else 0, s if s < 256 else 0,
            0, 0, 1, 32, len(png_data_list[i]), offsets[i]))
    for data in png_data_list:
        f.write(data)

import os
ico_size = os.path.getsize('dma-desktop/src/main/resources/icon.ico')
print(f'icon.ico: {ico_size/1024:.1f} KB with {len(SIZES)} resolutions')
print('DONE!')
