package widecat.itemframedupe.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ItemFrameDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("The max distance to search for blocks.")
        .min(1)
        .sliderMin(1)
        .defaultValue(5)
        .sliderMax(6)
        .max(6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between placements.")
        .defaultValue(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Whether or not to rotate when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Whether or not to swap back to the previous held item after placing.")
        .defaultValue(true)
        .build()
    );

    public ItemFrameDupe() {
        super(Categories.Misc, "item-frame-dupe", "Assists with the item frame dupe by placing item frames on block surfaces.");
    }

    private int timer;
    private final ArrayList<BlockPos> positions = new ArrayList<>();
    private static final ArrayList<BlockPos> blocks = new ArrayList<>();

    @Override
    public void onActivate() {
        timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (timer > 0) {
            timer--;
            return;
        }
        else timer = delay.get();

        FindItemResult itemResult = InvUtils.findInHotbar(Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME);
        if (!itemResult.found()) {
            error("No item frames found in hotbar.");
            toggle();
            return;
        }

        for (BlockPos blockPos : getSphere(mc.player.getBlockPos(), distance.get(), distance.get())) {
            Block block = mc.world.getBlockState(blockPos).getBlock();
            if (block != Blocks.AIR) {
                if (shouldPlace(blockPos)) positions.add(blockPos);
            }
        }

        for (BlockPos blockPos : positions) {
            Block block = mc.world.getBlockState(blockPos).getBlock();
            if (block == Blocks.AIR) {
                positions.remove(blockPos);
                return;
            }

            for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos placePos = getBlockPosFromDirection(direction, blockPos);
                BlockUtils.place(placePos, itemResult, rotate.get(), 50, true, true, swapBack.get());
            }

            if (delay.get() != 0) {
                positions.clear();
                break;
            }
        }
    }

    private boolean shouldPlace(BlockPos blockPos) {
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos placePos = getBlockPosFromDirection(direction, blockPos);
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemFrameEntity) {
                    BlockPos entityPos = new BlockPos(Math.floor(entity.getPos().x), Math.floor(entity.getPos().y), Math.floor(entity.getPos().z));
                    if (placePos.equals(entityPos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        blocks.clear();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    private static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    private BlockPos getBlockPosFromDirection(Direction direction, BlockPos originalPos) {
        return switch (direction) {
            case UP -> originalPos.up();
            case DOWN -> originalPos.down();
            case EAST -> originalPos.east();
            case WEST -> originalPos.west();
            case NORTH -> originalPos.north();
            case SOUTH -> originalPos.south();
        };
    }
}