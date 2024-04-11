package mod.acgaming.universaltweaks.tweaks.performance.entityradiuscheck;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.registries.RegistryManager;

import com.google.common.collect.ImmutableSet;
import mod.acgaming.universaltweaks.UniversalTweaks;
import mod.acgaming.universaltweaks.config.UTConfigTweaks;

public class UTEntityRadiusCheck
{
    public static Set<Class<? extends Entity>> searchTargets = Collections.emptySet();
    public static Set<Class<? extends Entity>> collisionTargets = Collections.emptySet();

    public static void onLoadComplete()
    {
        if (UTConfigTweaks.PERFORMANCE.ENTITY_RADIUS_CHECK.utReduceSearchSizeToggle) initSearchTargets();
        if (UTConfigTweaks.PERFORMANCE.ENTITY_RADIUS_CHECK.utLessCollisionsToggle) initCollisionTargets();
    }

    private static void initSearchTargets()
    {
        final ResourceLocation playerId = new ResourceLocation("player");
        Set<Class<? extends Entity>> out = new HashSet<>();
        // Read config for classes
        for (String entityId : UTConfigTweaks.PERFORMANCE.ENTITY_RADIUS_CHECK.utReduceSearchSizeTargets)
        {
            // Special case of player
            if (entityId.equals(playerId.toString()))
            {
                out.add(EntityPlayer.class);
                continue;
            }
            Class<? extends Entity> entityClazz = EntityList.getClass(new ResourceLocation(entityId));
            if (entityClazz == null)
            {
                UniversalTweaks.LOGGER.warn("UTEntityRadiusCheck ::: Invalid entity id " + entityId + "in \"[3] Reduce Search Size Targets\"! Skipping this entry.");
                continue;
            }
            out.add(entityClazz);
        }
        if (!out.isEmpty()) searchTargets = out;
    }

    private static void initCollisionTargets()
    {
        // Conditions taken from RLTweaker's JsonConfigLessCollisions.
        // (1) No combat allies or offensive tools
        // (2) No mountables, except for pigs.
        // (3) No projectiles of any kind
        // (4) Caution with entities that may become the owner of explosions
        Predicate<Class<? extends Entity>> livingPredicate = new Predicate<Class<? extends Entity>>()
        {
            @Override
            public boolean test(Class<? extends Entity> entityClazz)
            {
                return EntityLivingBase.class.isAssignableFrom(entityClazz) &&
                    !(entityClazz == EntityWolf.class ||                        // (1)
                        AbstractHorse.class.isAssignableFrom(entityClazz) ||    // (2)
                        entityClazz == EntityPlayer.class ||
                        entityClazz == EntityDragon.class ||                    // (4)
                        entityClazz == EntityWither.class                       // (4)
                    );
            }
        };
        Predicate<Class<? extends Entity>> miscPredicate = new Predicate<Class<? extends Entity>>()
        {
            @Override
            public boolean test(Class<? extends Entity> entityClazz)
            {
                Collection<Class<? extends Entity>> allowed = ImmutableSet.of(
                    EntityItem.class,
                    EntityItemFrame.class,
                    EntityPainting.class,
                    EntityXPOrb.class
                );
                return allowed.contains(entityClazz);
            }
        };
        // TODO: Add custom predicate reading entities from config?
        Collection<?> vanillaEntities = RegistryManager.VANILLA.getRegistry(new ResourceLocation("entities")).getValuesCollection();
        collisionTargets = vanillaEntities.stream()
            .map(entry -> ((EntityEntry) entry).getEntityClass())
            .filter(livingPredicate.or(miscPredicate))
            .collect(Collectors.toCollection(HashSet::new));
    }
}