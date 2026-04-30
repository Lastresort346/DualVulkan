package net.vulkanmod.vulkan.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import static org.lwjgl.vulkan.VK10.*;

public final class MultiGpuInterop {
    private MultiGpuInterop() {}

    /**
     * Records an image copy intended for secondary->primary merge passes.
     * This is the baseline transfer path used for offloaded shadow/volumetric targets.
     */
    public static void recordShadowTargetCopy(
            MemoryStack stack,
            VkCommandBuffer commandBuffer,
            long srcImage,
            long dstImage,
            int width,
            int height) {
        transitionForTransfer(stack, commandBuffer, srcImage, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        transitionForTransfer(stack, commandBuffer, dstImage, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

        VkImageCopy.Buffer copyRegion = VkImageCopy.calloc(1, stack);
        copyRegion.get(0)
                .srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1);
        copyRegion.get(0)
                .dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1);
        copyRegion.get(0).extent().set(width, height, 1);

        vkCmdCopyImage(
                commandBuffer,
                srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                dstImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                copyRegion);

        transitionForTransfer(stack, commandBuffer, srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        transitionForTransfer(stack, commandBuffer, dstImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    }

    private static void transitionForTransfer(
            MemoryStack stack,
            VkCommandBuffer commandBuffer,
            long image,
            int oldLayout,
            int newLayout) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout);
        barrier.newLayout(newLayout);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);

        vkCmdPipelineBarrier(
                commandBuffer,
                VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                0,
                null,
                null,
                barrier);
    }
}
