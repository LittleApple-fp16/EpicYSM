package com.argorice.epicysm.client.ysm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.Deque;

import org.junit.jupiter.api.Test;

import com.argorice.epicysm.mixin.PoseStackAccess;
import com.mojang.blaze3d.vertex.PoseStack;

class PoseStacksTest {
    @Test
    void restoresCallerWhenRendererReturnsWithExtraFrames() throws Exception {
        TestStack stack = new TestStack();
        stack.pushPose();
        PoseStack.Pose caller = stack.last();
        stack.pushPose();
        PoseStack.Pose expected = stack.last();
        stack.pushPose();
        stack.pushPose();

        PoseStacks.restore(stack, caller, expected, null, "test_extra", this);

        assertSame(caller, stack.last());
        assertEquals(2, stack.epicysm$frames().size());
    }

    @Test
    void removesOnlyTheFrameOwnedByABalancedDraw() throws Exception {
        TestStack stack = new TestStack();
        stack.pushPose();
        PoseStack.Pose caller = stack.last();
        stack.pushPose();

        PoseStacks.restore(stack, caller, stack.last(), null, "test_balanced", this);

        assertSame(caller, stack.last());
        assertEquals(2, stack.epicysm$frames().size());
    }

    @Test
    void leavesEarlierFramesAloneIfTheRendererPoppedTheCaller() throws Exception {
        TestStack stack = new TestStack();
        stack.pushPose();
        PoseStack.Pose earlier = stack.last();
        stack.pushPose();
        PoseStack.Pose caller = stack.last();
        stack.popPose();

        PoseStacks.restore(stack, caller, caller, null, "test_missing", this);

        assertSame(earlier, stack.last());
        assertEquals(2, stack.epicysm$frames().size());
    }

    private static final class TestStack extends PoseStack implements PoseStackAccess {
        private final Deque<Pose> frames;

        @SuppressWarnings("unchecked")
        TestStack() throws ReflectiveOperationException {
            Field field = PoseStack.class.getDeclaredField("poseStack");
            field.setAccessible(true);
            this.frames = (Deque<Pose>) field.get(this);
        }

        @Override
        public Deque<Pose> epicysm$frames() {
            return this.frames;
        }
    }
}
