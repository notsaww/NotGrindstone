package ru.notsaww.notgrindstone.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fires when enchantments are stripped at a grindstone, with the amount of removed enchantments
 * as the only condition.
 */
public final class DisenchantCriterion implements Criterion<DisenchantCriterion.Instance> {

    private final Map<PlayerAdvancementTracker, Set<ConditionsContainer<Instance>>> players = new IdentityHashMap<>();

    @Override
    public Codec<Instance> getConditionsCodec() {
        return Instance.CODEC;
    }

    @Override
    public void beginTrackingCondition(PlayerAdvancementTracker tracker, ConditionsContainer<Instance> container) {
        players.computeIfAbsent(tracker, key -> new HashSet<>()).add(container);
    }

    @Override
    public void endTrackingCondition(PlayerAdvancementTracker tracker, ConditionsContainer<Instance> container) {
        Set<ConditionsContainer<Instance>> tracked = players.get(tracker);
        if (tracked == null) {
            return;
        }
        tracked.remove(container);
        if (tracked.isEmpty()) {
            players.remove(tracker);
        }
    }

    @Override
    public void endTracking(PlayerAdvancementTracker tracker) {
        players.remove(tracker);
    }

    public void trigger(ServerPlayerEntity player, int removed) {
        PlayerAdvancementTracker tracker = player.getAdvancementTracker();
        Set<ConditionsContainer<Instance>> tracked = players.get(tracker);
        if (tracked == null) {
            return;
        }

        for (ConditionsContainer<Instance> container : tracked) {
            if (container.conditions().matches(removed)) {
                container.grant(tracker);
            }
        }
    }

    public record Instance(int minRemoved) implements CriterionConditions {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("count", 1).forGetter(Instance::minRemoved)
        ).apply(instance, Instance::new));

        public boolean matches(int removed) {
            return removed >= minRemoved;
        }

        @Override
        public void validate(LootContextPredicateValidator validator) {
        }
    }
}
