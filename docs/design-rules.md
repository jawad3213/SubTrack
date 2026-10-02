# SubTrack Design System & UI Rules

## 1. BRAND & COLOR PALETTE

**Primary color: Blue**
- Blue 50  → `#E6F1FB`  (lightest fill, hover backgrounds)
- Blue 100 → `#B5D4F4`  (light fill, selected states)
- Blue 200 → `#85B7EB`  (borders on active elements)
- Blue 400 → `#378ADD`  (mid tone)
- Blue 600 → `#185FA5`  ★ PRIMARY — buttons, links, focus rings, active states
- Blue 800 → `#0C447C`  (hover on primary button, dark text on blue fill)
- Blue 900 → `#042C53`  (darkest, headings on colored bg)

**Neutral / UI:**
- Page background     → `#F1EFE8`  (gray-50, full viewport)
- Card background     → `#FFFFFF`
- Input background    → `#F5F5F3`
- Border default      → `rgba(0,0,0,0.15)`  (0.5px)
- Border hover        → `rgba(0,0,0,0.30)`
- Border focus        → `#185FA5`  (Blue 600)
- Text primary        → `#1A1A1A`
- Text secondary      → `#5F5E5A`
- Text placeholder    → `#B4B2A9`

**Semantic:**
- Error background    → `#FCEBEB`  /  Error text   → `#791F1F`  /  Error border → `#E24B4A`
- Warning background  → `#FAEEDA`  /  Warning text → `#633806`  /  Warning border → `#BA7517`
- Success background  → `#EAF3DE`  /  Success text → `#27500A`  /  Success border → `#639922`

## 2. TYPOGRAPHY
**Font family:** `system-ui, -apple-system, 'Segoe UI', Arial, sans-serif`
- Page title: 20px / weight 500 / color `#1A1A1A`
- Subtitle: 13px / weight 400 / color `#5F5E5A`
- Field label: 12px / weight 500 / color `#5F5E5A` / margin-bottom: 4px
- Input text: 14px / weight 400 / color `#1A1A1A`
- Input placeholder: 14px / weight 400 / color `#B4B2A9`
- Primary button label: 14px / weight 500 / color `#FFFFFF`
- Helper / error text: 11px / weight 400 / color `#791F1F`
- Footer link: 12px / color `#5F5E5A` + link color `#185FA5` / weight 500
- Brand logo name: 17px / weight 500 / color `#1A1A1A`

## 3. COMPONENT IMPLEMENTATION RULES

Auth pages are STANDALONE — **no template, no sidebar.**
All auth pages must use `auth.css` and use plain JSF tags for layout (`h:inputText`, `h:commandButton`) instead of `p:inputText` components to apply specific raw CSS design values, unless PrimeFaces strictly matches the spec.

Password Toggle requires pure JS logic (`type='text'` toggle) or plain styling.

### Auth Wrapper Layout
- `display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #F1EFE8`
- Auth card width: 380px, max-width calc(100vw - 2rem). Padding: 32px. Radius: 12px. Border: 0.5px solid rgba(0,0,0,0.15).
