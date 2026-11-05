package com.argorice.epicysm.mixin;

import java.util.Deque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(PoseStack.class)
public interface PoseStackAccess {
    @Accessor("poseStack")
    Deque<PoseStack.Pose> epicysm$frames();
}
