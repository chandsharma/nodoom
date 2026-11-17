# Google Play assets

Final upload-ready assets:

- `graphics/app-icon-512.png` — 512×512 store icon derived from the app's committed launcher art.
- `graphics/feature-graphic-1024x500.png` — 1024×500 feature graphic.
- `phone-screenshots/*.png` — 1080×1920 phone screenshots with accurate feature text and no personal Instagram content.
- `listing/en-US/` — app name, short description, full description and release notes.

The feature graphic was generated with OpenAI ImageGen and then mechanically resized/cropped to the Play-required 1024×500 dimensions. Prompt:

> Create a polished Google Play feature graphic for an Android digital wellbeing app called “No Doom”, exactly 1024×500 landscape. Premium editorial minimalism. Warm ivory background (#FFF8F1), deep charcoal typography, restrained vermilion accent (#B9362C) and emerald green (#147554). On the right: an elegant abstract broken infinity loop transforming into a calm focused human mind / leaf, representing stopping endless scrolling while keeping social connection. On the left, use crisp exact text only: large headline “One Reel. Then you choose.” and small label “NO DOOM”. No phone mockup, no Instagram logo, no Meta marks, no gradients that reduce legibility, no extra words, no misspellings. High-end, calm, trustworthy, lots of breathing room, accessible contrast.

The phone screenshots are deterministic HTML/CSS compositions. Screenshot 1 embeds only the non-personal, still-accurate top of a real Pixel capture of No Doom; screenshots 2 and 3 render the exact disclosure/payment copy from version 1.0.0. Their source files are kept in `source/` for reproducibility.
