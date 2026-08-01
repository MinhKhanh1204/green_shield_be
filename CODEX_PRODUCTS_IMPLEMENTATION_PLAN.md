# GreenShield Product Catalog Implementation Plan

## Current state

- The React application already provides landing, design, AI, QR, map, order, chat, and admin modules.
- The Spring Boot application has minimal `Product` and `ProductImage` entities but no product API or management workflow.
- Product source images live locally in `product_images`: five folders, five PNG files per product, all 1448x1086.
- Cloudinary is used by existing image features, but it does not yet provide product deletion metadata or a local fallback.

## Delivery scope

1. Restore frontend lint/build and isolated backend tests.
2. Replace the minimal product model with bilingual catalog entities, enums, DTOs, validation, and service invariants.
3. Add Cloudinary and local product image storage behind one interface.
4. Generate normalized WebP seed assets without modifying or upscaling source files.
5. Seed five products and their images idempotently when explicitly enabled.
6. Add public catalog/detail APIs and authenticated admin CRUD/image APIs.
7. Add `/products`, `/products/:slug`, and `/admin/dashboard/products` to the React application.
8. Add public exhibition/detail experiences, admin management, i18n, SEO, accessibility, tests, and documentation.

## Storage and image rules

- Source PNG files remain outside Git and unchanged.
- Seed assets use lowercase ASCII kebab-case names.
- Display images never exceed the 1448 px source width; thumbnails target 640 px.
- `PRODUCT_IMAGE_STORAGE=local|cloudinary` selects the implementation at startup.
- Local files are served from `/uploads/products/**`; Cloudinary records both URL and public ID.
- Uploads accept verified JPEG, PNG, or WebP files up to 5 MB and eight images per product.

## Commit plan

1. `docs: add product showcase implementation plan`
2. `fix: restore frontend and backend quality gates`
3. `feat: add product catalog domain and public API`
4. `feat: add product image storage and seed importer`
5. `feat: add admin product management APIs`
6. `feat: add product exhibition and detail pages`
7. `feat: add admin product management UI`
8. `test: cover product catalog behavior`
9. `docs: document product catalog operations`
10. `fix: resolve responsive and regression issues`

## Completion criteria

- Five active products are available from the backend with five sorted images each.
- Public responses never expose internal cost data or inactive products.
- Admin product and image routes require the existing authenticated session.
- Local and Cloudinary storage support upload and deletion.
- Frontend lint/build and backend tests pass.
- Product pages are verified at 1440, 1024, 768, and 390 px without regressions in existing modules.
