package net.vulkanmod.vulkan.device;

/**
 * Lightweight handshake surface for Epik library integration.
 * Other mods can query this class without depending on renderer internals.
 */
public final class SecondaryGpuCapability {
    private SecondaryGpuCapability() {}

    public static boolean isSecondaryGpuAvailable() {
        return DeviceManager.hasSecondaryDevice();
    }

    public static String getSecondaryGpuName() {
        return DeviceManager.hasSecondaryDevice() ? DeviceManager.secondaryDevice.deviceName : "none";
    }

    public static int getSecondaryGpuVendorId() {
        return DeviceManager.hasSecondaryDevice() ? DeviceManager.secondaryDevice.properties.vendorID() : -1;
    }
}
