package gg.kite.util;

import gg.kite.manager.StorageHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    private static final NamespacedKey TRANSACTION_KEY = new NamespacedKey("nexostorage", "transaction_id");

    public static byte @NotNull [] serializeItems(@Nullable ItemStack[] items) {
        if (items == null) return new byte[0];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (ItemStack item : items) {
                byte[] itemBytes = item != null && !item.getType().isAir() ? item.serializeAsBytes() : new byte[0];
                baos.write(ByteBuffer.allocate(4).putInt(itemBytes.length).array());
                baos.write(itemBytes);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize items", e);
        }
    }

    public static ItemStack @NotNull [] deserializeItems(@Nullable byte[] data) {
        if (data == null || data.length == 0) return new ItemStack[0];
        List<ItemStack> items = new ArrayList<>();
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) {
                if (buffer.remaining() < 4) break;
                int length = buffer.getInt();
                if (length < 0 || length > buffer.remaining()) {
                    throw new RuntimeException("Invalid item data length: " + length);
                }
                if (length == 0) {
                    items.add(null);
                    continue;
                }
                byte[] itemBytes = new byte[length];
                buffer.get(itemBytes);
                ItemStack item = ItemStack.deserializeBytes(itemBytes);
                items.add(item);
            }
            return items.toArray(new ItemStack[0]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }
    }

    public static void setTransactionId(@NotNull Inventory inv, @NotNull String transactionId) {
        if (!(inv.getHolder() instanceof StorageHolder holder)) {
            throw new IllegalArgumentException("Inventory holder must be a StorageHolder");
        }
        holder.getPersistentDataContainer().set(TRANSACTION_KEY, PersistentDataType.STRING, transactionId);
    }

    public static @Nullable String getTransactionId(@NotNull Inventory inv) {
        if (!(inv.getHolder() instanceof StorageHolder holder)) {
            return null;
        }
        return holder.getPersistentDataContainer().get(TRANSACTION_KEY, PersistentDataType.STRING);
    }

    public static void invalidateTransactionId(@NotNull Inventory inv) {
        if (!(inv.getHolder() instanceof StorageHolder holder)) {
            return;
        }
        holder.getPersistentDataContainer().remove(TRANSACTION_KEY);
    }
}