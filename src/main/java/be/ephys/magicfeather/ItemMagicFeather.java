package be.ephys.magicfeather;

import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemMagicFeather extends Item {

    public static final String NAME = "magicfeather";
    private static final WeakHashMap<EntityPlayer, MagicFeatherData> playerData = new WeakHashMap<>();

    public ItemMagicFeather() {
        super();

        setMaxStackSize(1);
        setUnlocalizedName(MagicFeatherMod.MODID + ":" + NAME);
        setRegistryName(NAME);
        setCreativeTab(CreativeTabs.MISC);
    }

    public int getEntityLifespan(ItemStack itemStack, World world) {
        return Integer.MAX_VALUE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            if (!isInBeaconRange(player)) {
                tooltip.add(I18n.format("magicfeather.gui.out_of_beacon_range"));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void registerModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(World world, Entity entity, ItemStack itemstack) {
        entity.setEntityInvulnerable(true);

        return null;
    }

    private static boolean isInBeaconRange(EntityPlayer player) {
        World world = player.getEntityWorld();

        List<TileEntity> tileEntities = world.loadedTileEntityList;
        for (TileEntity t : tileEntities) {
            if (!(t instanceof TileEntityBeacon)) {
                continue;
            }

            TileEntityBeacon beacon = (TileEntityBeacon) t;

            int level = beacon.getField(0);
            int radius = (level * 10 + 10);

            BlockPos pos = beacon.getPos();
            int x = pos.getX();
            int z = pos.getZ();

            if (player.posX < (x - radius) || player.posX > (x + radius)) {
                continue;
            }

            if (player.posZ < (z - radius) || player.posZ > (z + radius)) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static void setMayFly(EntityPlayer player, boolean mayFly) {
        if (player.capabilities.allowFlying == mayFly) {
            return;
        }

        if (!mayFly) {
            // force the player on the ground then remove ability to fly
            // this prevent crashing the the ground and dying
            // when you accidentally get out of the beacon range
            player.capabilities.isFlying = false;

            if (player.onGround && player.fallDistance < 1F) {
                player.capabilities.allowFlying = false;
            }
        } else {
            player.capabilities.allowFlying = true;
        }

        player.sendPlayerAbilities();
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (!world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;

        MagicFeatherData data = ItemMagicFeather.playerData.get(player);
        if (data == null) {
            data = new MagicFeatherData(player);
            ItemMagicFeather.playerData.put(player, data);
        }

        data.onTick();
    }

    private static boolean hasItem(EntityPlayer player, Item item) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (item == stack.getItem()) {
                return true;
            }
        }

        return false;
    }

    private static class MagicFeatherData {
        private final EntityPlayer player;
        private boolean hasItem = false;

        private long checkTick;
        private boolean beaconInRangeCache;

        public MagicFeatherData(EntityPlayer player) {
            this.player = player;
            this.beaconInRangeCache = player.capabilities.allowFlying;
        }

        public void onTick() {
            if (player.isSpectator()) {
                return;
            }

            boolean hasItem = hasItem(player, ModItems.magicFeather);
            if (hasItem != this.hasItem) {
                if (hasItem) {
                    this.onAdd();
                }

                if (!hasItem) {
                    this.onRemove();
                }

                this.hasItem = hasItem;
                return;
            }

            boolean mayFly = player.capabilities.isCreativeMode || (hasItem && checkBeaconInRange(player));
            setMayFly(player, mayFly);
        }

        private void onAdd() {
            if (!ItemMagicFeather.isInBeaconRange(player)) {
                return;
            }

            setMayFly(player, true);
        }

        private void onRemove() {
            if (player.capabilities.isCreativeMode) {
                return;
            }

            setMayFly(player, false);
        }

        private boolean checkBeaconInRange(EntityPlayer player) {
            if (checkTick++ % 40 != 0) {
                return beaconInRangeCache;
            }

            beaconInRangeCache = ItemMagicFeather.isInBeaconRange(player);

            return beaconInRangeCache;
        }
    }
}
