from __future__ import annotations

import json
import re
import unicodedata
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "product_images"
OUTPUT_ROOT = ROOT / "src" / "main" / "resources" / "product-seed" / "images"
MANIFEST_PATH = ROOT / "src" / "main" / "resources" / "product-seed" / "products-images.manifest.json"

FOLDERS = {
    "hop-dung-trai-cay": "hop_trai_cay",
    "dia-la-sen": "dia_la_sen",
    "chen-la-luc-binh": "chen_la_lb",
    "lot-ly-luc-binh": "lot_ly",
    "tui-dan-bao-ve-trai-cay": "Tui_dung_trai_cay",
}

ALT_TEXT = {
    "hop-dung-trai-cay": ("Hộp đựng trái cây từ sợi lục bình", "Water hyacinth fiber fruit box"),
    "dia-la-sen": ("Dĩa lá sen ép định hình", "Molded lotus leaf plate"),
    "chen-la-luc-binh": ("Chén lá lục bình ép định hình", "Molded water hyacinth leaf bowl"),
    "lot-ly-luc-binh": ("Lót ly từ sợi lục bình", "Water hyacinth fiber coaster"),
    "tui-dan-bao-ve-trai-cay": ("Túi đan bảo vệ trái cây", "Woven fruit protection bag"),
}

IMAGE_TYPES = ["HERO", "GALLERY", "DETAIL", "APPLICATION", "GALLERY"]


def resize(image: Image.Image, max_width: int) -> Image.Image:
    if image.width <= max_width:
        return image.copy()
    height = round(image.height * max_width / image.width)
    return image.resize((max_width, height), Image.Resampling.LANCZOS)


def safe_slug(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    normalized = re.sub(r"[^a-zA-Z0-9]+", "-", normalized).strip("-").lower()
    return normalized or "product-image"


def main() -> None:
    manifest: dict[str, list[dict[str, object]]] = {}
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

    for slug, folder_name in FOLDERS.items():
        source_dir = SOURCE_ROOT / folder_name
        files = sorted(path for path in source_dir.iterdir() if path.is_file())
        if not files:
            raise RuntimeError(f"No source images found for {slug}: {source_dir}")

        output_dir = OUTPUT_ROOT / slug
        output_dir.mkdir(parents=True, exist_ok=True)
        entries: list[dict[str, object]] = []
        alt_vi, alt_en = ALT_TEXT[slug]

        for index, source in enumerate(files[:5], start=1):
            base_name = safe_slug(f"{slug}-{index:02d}")
            display_name = f"{base_name}.webp"
            thumbnail_name = f"{base_name}-thumb.webp"
            display_path = output_dir / display_name
            thumbnail_path = output_dir / thumbnail_name

            with Image.open(source) as image:
                rgb = image.convert("RGB")
                resize(rgb, 1448).save(display_path, "WEBP", quality=86, method=6)
                resize(rgb, 640).save(thumbnail_path, "WEBP", quality=82, method=6)

            entries.append(
                {
                    "source": str(source.relative_to(ROOT)).replace("\\", "/"),
                    "fileName": base_name,
                    "displayResource": f"product-seed/images/{slug}/{display_name}",
                    "thumbnailResource": f"product-seed/images/{slug}/{thumbnail_name}",
                    "sortOrder": index - 1,
                    "mainImage": index == 1,
                    "imageType": IMAGE_TYPES[index - 1],
                    "altTextVi": f"{alt_vi} - ảnh {index}",
                    "altTextEn": f"{alt_en} - image {index}",
                    "seed": True,
                }
            )

        manifest[slug] = entries

    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
