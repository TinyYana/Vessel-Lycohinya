package org.maboroshi.vessel.handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.maboroshi.vessel.config.objects.effects.EffectGroup;
import org.maboroshi.vessel.config.objects.effects.ParticleEffect;
import org.maboroshi.vessel.config.objects.effects.SoundEffect;
import org.maboroshi.vessel.util.Log;

public class EffectHandler {
    private final Plugin plugin;

    public EffectHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void playEffects(EffectGroup group, Location location, boolean globalSound) {
        if (group == null) return;

        if (group.sounds != null && !group.sounds.isEmpty()) {
            for (SoundEffect sound : group.sounds.values()) {
                playSound(sound, location, globalSound);
            }
        }

        if (location == null || location.getWorld() == null) return;

        if (group.particles != null && !group.particles.isEmpty()) {
            for (ParticleEffect particle : group.particles.values()) {
                playParticle(particle, location);
            }
        }
    }

    private void playSound(SoundEffect sound, Location location, boolean globalSound) {
        if (sound.type == null || sound.type.isEmpty()) return;

        try {
            if (globalSound) {
                // Folia: each online player may be owned by a different region thread than the one
                // calling this method, so the sound must be scheduled onto each player's own thread
                // rather than played directly from here.
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.getScheduler()
                            .run(
                                    plugin,
                                    task -> p.playSound(p.getLocation(), sound.type, sound.volume, sound.pitch),
                                    null);
                }
            } else {
                if (location != null && location.getWorld() != null) {
                    location.getWorld().playSound(location, sound.type, sound.volume, sound.pitch);
                }
            }
        } catch (Exception e) {
            Log.debug("Failed to play sound: " + sound.type);
        }
    }

    private void playParticle(ParticleEffect particleData, Location location) {
        String particleType = particleData.type;
        if (particleType == null || particleType.isEmpty()) return;

        try {
            Particle particle = Particle.valueOf(particleType.toUpperCase());

            double speed = particleData.speed;
            double offX = 0.5;
            double offY = 0.5;
            double offZ = 0.5;

            if (particleData.offset != null) {
                offX = particleData.offset.x;
                offY = particleData.offset.y;
                offZ = particleData.offset.z;
            }

            location.getWorld()
                    .spawnParticle(
                            particle, location.clone().add(0, 1.0, 0), particleData.count, offX, offY, offZ, speed);
        } catch (IllegalArgumentException e) {
            Log.error("Invalid particle type in config: " + particleType);
        }
    }
}
