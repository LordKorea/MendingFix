package nge.lk.mods.mendingfix;

import com.google.common.collect.Lists;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod(modid = "mendingfix", version = "1.0.1-1.10.2.2511")
public class MendingFix {

    /**
     * Vanilla value: 2 Durability per XP.
     */
    private static final int DURABILITY_PER_XP = 2;

    /**
     * Finds a damaged, enchanted item in the player's inventory and equipment.
     *
     * @param ench The enchantment which is required.
     * @param player The player.
     *
     * @return The first item that matches the description or the empty item {@code null}.
     */
    private static ItemStack getDamagedEnchantedItem(Enchantment ench, EntityPlayer player) {
        List<ItemStack> possible = Lists.newArrayList(ench.getEntityEquipment(player));
        if (possible.isEmpty()) {
            // No viable equipment items.
            return null;
        } else {
            // Filter viable equipment items.
            List<ItemStack> choices = Lists.newArrayList();
            for (ItemStack itemstack : possible) {
                if (itemstack != null && itemstack.isItemDamaged() && EnchantmentHelper.getEnchantmentLevel(ench, itemstack) > 0) {
                    choices.add(itemstack);
                }
            }

            // Pick one choice at random.
            if (choices.isEmpty()) {
                return null;
            }
            return choices.get(player.getRNG().nextInt(choices.size()));
        }
    }

    @EventHandler
    public void onInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onXP(PlayerPickupXpEvent e) {
        // Rewrite this event's handling.
        e.setCanceled(true);

        EntityPlayer player = e.getEntityPlayer();
        EntityXPOrb xp = e.getOrb();
        ItemStack item = getDamagedEnchantedItem(Enchantments.MENDING, player);

        // See EntityXPOrb#onCollideWithPlayer for details.
        // All requirements for picking up XP are met at this point.

        // -> EntityPlayer#xpCooldown is set to 2.
        player.xpCooldown = 2;

        // -> Play sound
        xp.worldObj.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_EXPERIENCE_ORB_TOUCH, SoundCategory.PLAYERS, 0.1F,
                0.5F * ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.8F));

        // -> EntityPlayer#onItemPickup is called with the xp orb and 1 (quantity).
        player.onItemPickup(xp, 1);

        // -> The mending effect is applied and the xp value is recalculated.
        if (item != null && item.isItemDamaged()) {
            int realRepair = Math.min(xp.xpValue * DURABILITY_PER_XP, item.getItemDamage());
            xp.xpValue -= realRepair / DURABILITY_PER_XP;
            item.setItemDamage(item.getItemDamage() - realRepair);
        }

        // -> The XP are added to the player's experience.
        if (xp.xpValue > 0) {
            player.addExperience(xp.xpValue);
        }

        // -> The XP orb is killed.
        xp.setDead();
    }
}
