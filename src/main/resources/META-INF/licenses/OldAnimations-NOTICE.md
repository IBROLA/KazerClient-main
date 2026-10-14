# Sk1er OldAnimations animation code

Upstream: https://github.com/Sk1erLLC/OldAnimations
Revision: ab10fa4879bf54a899bba777d7a0bb70c946e4fb
Authors: Sk1erLLC and OldAnimations contributors (upstream authorship retained).
License: GNU Lesser General Public License version 3 (LGPL-3.0-only).
The full LGPL-3.0 and GPL-3.0 texts accompany this notice in this directory.
This code is provided WITHOUT ANY WARRANTY; see those licenses for details.

Derived files in cn/kazer/client189/animation:
OldSwingState.java, OldAnimationsAdapter.java, BlockHitTransformer.java.
Adapted from AnimationHandler.java, SneakHandler.java and MixinItemRenderer,
MixinMinecraft, ModelBipedMixin, MixinRenderItem, MixinEntityRenderer,
LayerArmorBaseMixin and GuiIngameForgeMixin.

Kazer modifications (2026-10-08): Forge ASM bridges replace Mixin/Essential;
existing Kazer options and config migration replace Vigilance; reset on world
or module lifecycle changes; suppress new override starts while menus are open;
block-removal guard only during real item use; allocation-free option bitmask
updated on settings changes instead of repeated per-frame settings lookups. Core swing interpolation,
first-person transforms, bow pullback, third-person blocking, item switching,
sneaking, red armor and health-flash algorithms retain the upstream behavior.
No upstream Essential account UI, updater, debug or TAB replacement is imported.
No Microsoft/Mojang login tokens, game directories or proprietary Minecraft /
OptiFine binaries are part of the corresponding-source archive.

Corresponding source and rebuild/relink instructions ship in launcher assets:
assets/kazer-client-1.8.9-source.zip (README-RELINK.md and build.gradle included).
The archive contains the entire independent extension source, including the
derived files, their tests and packaged resources. Copy it separately from the
launcher resources/app.asar with an ASAR extraction tool, or obtain the archive
alongside the installer. Redistribution must retain this archive and notices.

Users may modify and replace these components and reverse-engineer the combined
work for debugging their modifications, as allowed by LGPL section 4. The root
MIT license does not replace LGPL coverage on these derived files or grant
rights to unrelated third-party remapped code/assets (see THIRD_PARTY_NOTICE.md).
Build using Java 8, Gradle 2.7, ForgeGradle 2.1, Forge 1.8.9 #1902, stable_22.
The rebuilt extension can be relinked by placing the resulting JAR into an
independent Forge #1902 game directory's mods folder, alongside unmodified
official OptiFine HD U M5 obtained separately. Launch that Forge installation
independently, not through Kazer's hash-enforcing production launcher. Remove
the old extension JAR first to avoid duplicate modules/core plugins.
