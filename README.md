# Spawners+ (fork)

This fork adds a generic mob soul system to Spawners+. Rather than each soul being its own hand-made item, a single `mob_soul` item programmatically covers every mob the game can detect: all vanilla mobs, plus every mob added by other mods, with no per-mob code or assets. On a modded server such as BCM4, that means every creature from every installed mod gets a working soul the moment the game loads.

This started as a family project. I wanted to build an enderman farm on the server I play with my son and stepson, and was sad to find the mod's souls stopped short of endermen. Extending it to one mob turned into extending it to every mob, from any mod, which the kids love.

- Souls take on each mob's spawn-egg color, with a neutral gray for mobs that have no egg
- Any soul's generated look can be replaced with hand-drawn art by adding a single PNG at `assets/spawnersplus/textures/item/<mob>_soul.png`, in the mod or in any resource pack, with no code changes
- Souls drop from mobs killed with the Soul Stealing enchantment, with drop rates configurable globally, per mod, and per individual mob
- Every mob's soul appears in the creative tab and creative search
- Admin commands: `/spawnersplus give <players> <mob>` and `/spawnersplus droprate ...`

This branch contains the full change set, which is being prepared as a pull request to the upstream mod.

## About Spawners+

Spawners+ is a Fabric mod by [NathanTheCraziest](https://github.com/NathanTheCraziest) that makes mob spawners part of survival gameplay: broken spawners drop fragments, fragments craft into an inactive spawner, and using a mob soul on it activates it for that mob. The original mod ships 12 hand-crafted souls (zombie, skeleton, spider, cave spider, blaze, magma cube, silverfish, stray, wither skeleton, husk, drowned, and creeper). This fork is deliberately additive: those 12 items, their art, and their drop behavior are untouched and always take precedence, with the generic system covering everything beyond them. The original mod is on [CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/spawners-plus). Huge thanks to NathanTheCraziest for making a mod awesome enough to be worth building on.
