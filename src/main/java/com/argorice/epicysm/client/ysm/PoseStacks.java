package com.argorice.epicysm.client.ysm;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.argorice.epicysm.EpicYsm;
import com.argorice.epicysm.client.ClientDiagnostics;
import com.argorice.epicysm.mixin.PoseStackAccess;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.LivingEntity;

public final class PoseStacks {
    private static final Set<String> reported = new HashSet<>();

    private PoseStacks() {
    }

    /**
     * Removes this draw's frames without touching its caller's frames. A renderer
     * may leave extra frames on a normal return as well as on an exception.
     */
    public static void restore(PoseStack stack, PoseStack.Pose caller, PoseStack.Pose expected,
                               LivingEntity entity, String route, Object renderer) {
        Deque<PoseStack.Pose> frames = ((PoseStackAccess) stack).epicysm$frames();
        if (frames.peekLast() == expected) {
            if (expected != caller) {
                stack.popPose();
            }
            return;
        }

        int removed = 0;
        boolean callerPresent = frames.contains(caller);
        if (callerPresent) {
            while (frames.peekLast() != caller) {
                stack.popPose();
                removed++;
            }
        }

        String rendererClass = renderer.getClass().getName();
        if (reported.add(route + ":" + rendererClass)) {
            EpicYsm.LOGGER.warn("Unbalanced pose stack in {} via {}. Removed {} frame(s); caller frame present: {}",
                    route, rendererClass, removed, callerPresent);
            ClientDiagnostics.event("render_pose_stack_unbalanced", ClientDiagnostics.fields(
                    "route", route, "renderer", rendererClass, "removed_frames", removed, "caller_present", callerPresent));
            ClientDiagnostics.problem("render_pose_stack_" + route, entity, null);
        }
    }
}
