# GreenShield Product Catalog

## Overview

The product catalog is split across the existing GreenShield applications:

- React frontend: public exhibition, product detail, landing teaser, and Ant Design admin management.
- Spring Boot backend: product domain, public/admin REST APIs, validation, image storage, and optional seed import.
- FastAPI classifier: unchanged; it remains an independent Plant Disease service.

The frontend never hard-codes product records or gallery files. Product copy, price, sale mode, active state, display order, and image metadata come from the Spring Boot API.

## Data model

`Product` uses a UUID string ID and stores bilingual names, descriptions, materials, benefits, applications, and specifications. Prices use `BigDecimal`. Category, sale mode, and image type are enums.

`ProductImage` stores the display URL, thumbnail URL, storage key, optional Cloudinary public ID, bilingual alt text, type, sort order, main-image state, and creation time. Service operations ensure that setting a main image clears the previous main image.

Public DTOs do not include production cost or storage deletion metadata.

## Public API

| Method | Endpoint | Notes |
| --- | --- | --- |
| `GET` | `/api/v1/products` | Active products only; supports `category`, `featured`, `search`, and `active`. `active=false` returns an empty list. |
| `GET` | `/api/v1/products/{slug}` | Active product detail, sorted gallery, and up to four related active products. |

The frontend selects Vietnamese or English fields using the current i18n language.

## Admin API

All routes under `/api/v1/admin/**` require the existing authenticated Spring Security session.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/products` | List all products, including inactive products. |
| `GET` | `/api/v1/admin/products/{id}` | Get one product. |
| `POST` | `/api/v1/admin/products` | Create a product. |
| `PUT` | `/api/v1/admin/products/{id}` | Replace editable product data. |
| `DELETE` | `/api/v1/admin/products/{id}` | Delete product metadata and stored images. |
| `PATCH` | `/api/v1/admin/products/{id}/status` | Update `active`. |
| `PATCH` | `/api/v1/admin/products/{id}/featured` | Update `featured`. |
| `PATCH` | `/api/v1/admin/products/{id}/display-order` | Update non-negative display order. |
| `POST` | `/api/v1/admin/products/{id}/images` | Multipart upload using repeated `files` fields. |
| `PUT` | `/api/v1/admin/products/{id}/images/{imageId}` | Update alt text and image type. |
| `DELETE` | `/api/v1/admin/products/{id}/images/{imageId}` | Delete metadata and the local/Cloudinary resource. |
| `PATCH` | `/api/v1/admin/products/{id}/images/reorder` | Submit every image ID once in the intended order. |
| `PATCH` | `/api/v1/admin/products/{id}/images/{imageId}/main` | Select the single main image. |

Uploads are inspected by file signature, not trusted client MIME. JPEG, PNG, and WebP are accepted up to 5 MB per file and eight images per product.

## Environment variables

Backend product settings:

```dotenv
PRODUCT_SEED_ENABLED=false
PRODUCT_IMAGE_STORAGE=local
PRODUCT_UPLOAD_DIR=uploads/products
PRODUCT_PUBLIC_BASE_URL=/uploads/products
```

Use `PRODUCT_IMAGE_STORAGE=cloudinary` only when all three Cloudinary variables are configured:

```dotenv
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

The first backend start also needs `ADMIN_PASSWORD` unless an admin record already exists. The seeded login email is `admin@gmail.com`.

Frontend configuration:

```dotenv
VITE_API_BASE=http://localhost:8080
```

When `VITE_API_BASE` is empty, requests use the current origin. Admin requests always include cookies with `credentials: include`.

## Seed and image import

Set `PRODUCT_SEED_ENABLED=true` for a startup that should upsert the five catalog records. The importer is idempotent by slug and imports images only when a product has no images.

The source PNGs are not modified or committed. The checked-in manifest is:

`src/main/resources/product-seed/products-images.manifest.json`

Image mapping:

| Product slug | Source folder | Seed result |
| --- | --- | --- |
| `hop-dung-trai-cay` | `product_images/hop_trai_cay` | 5 display WebP + 5 thumbnails |
| `dia-la-sen` | `product_images/dia_la_sen` | 5 display WebP + 5 thumbnails |
| `chen-la-luc-binh` | `product_images/chen_la_lb` | 5 display WebP + 5 thumbnails |
| `lot-ly-luc-binh` | `product_images/lot_ly` | 5 display WebP + 5 thumbnails |
| `tui-dan-bao-ve-trai-cay` | `product_images/Tui_dung_trai_cay` | 5 display WebP + 5 thumbnails |

Web filenames are lowercase ASCII kebab-case. Display images preserve the 1448 px source width instead of upscaling; thumbnails target 640 px. Every product has one `HERO`, one `DETAIL`, one `APPLICATION`, and two `GALLERY` images.

## Storage behavior

Local storage is the default. Files are written below `PRODUCT_UPLOAD_DIR/{slug}` and served through `/uploads/products/**`. Generated upload names use UUIDs; prepared seed names use sanitized manifest names. Path segments are validated to prevent traversal.

Cloudinary storage uses the folder `greenshield/products/{slug}`, records the public ID, and generates delivery URLs for 1448 px display and 640 px thumbnail variants. Deleting an image calls the matching storage provider.

## Run locally

Backend from `D:\Project_Greenshield\BE\green_shield_be`:

```powershell
.\gradlew.bat bootRun
```

Frontend from `D:\Project_Greenshield\green_shield`:

```powershell
npm install
npm run dev
```

Public pages are `/products` and `/products/{slug}`. Admin management is `/admin/dashboard/products` after logging in through the existing admin screen.

Quality checks:

```powershell
.\gradlew.bat test
npm run lint
npm run build
```

## Technical decisions and limitations

- The project does not currently use Flyway. Schema changes still rely on `spring.jpa.hibernate.ddl-auto=update`; adding versioned production migrations remains technical debt.
- Existing databases created from the former minimal `Product` schema should be backed up and tested before deployment because the catalog adds required columns and collection tables.
- Uploaded WebP files are retained directly for both display and thumbnail in local mode; prepared seed images have separate optimized thumbnails.
- Product-specific QR records do not exist, so the detail page links to the existing traceability/map experience instead of fabricating QR data.
- A separate bundle entity was not added. Chén and dĩa pages display their individual backend combo data without inventing a combined price.
- The global `MaterialDataProvider` remains part of the current frontend shell and can still initiate unrelated material/map loading on catalog routes.
