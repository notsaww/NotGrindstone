package ru.notsaww.notgrindstone.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires when enchantments are stripped at a grindstone, with the amount of removed enchantments
 * as the only condition.
 */
public final class DisenchantCriterion extends SimpleCriterionTrigger<DisenchantCriterion.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int removed) {
        trigger(player, instance -> instance.matches(removed));
    }

    public record Instance(int minRemoved) implements SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("count", 1).forGetter(Instance::minRemoved)
        ).apply(instance, Instance::new));

        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }

        public boolean matches(int removed) {
            return removed >= minRemoved;
        }
    }
}
