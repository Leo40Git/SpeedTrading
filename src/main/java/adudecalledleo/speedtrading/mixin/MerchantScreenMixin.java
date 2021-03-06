package adudecalledleo.speedtrading.mixin;

import adudecalledleo.speedtrading.config.ModConfig;
import adudecalledleo.speedtrading.duck.MerchantScreenHooks;
import adudecalledleo.speedtrading.gui.SpeedTradeButton;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static adudecalledleo.speedtrading.util.PlayerInventoryUtil.playerCanAcceptStack;
import static adudecalledleo.speedtrading.util.PlayerInventoryUtil.playerHasStack;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> implements MerchantScreenHooks {
    @SuppressWarnings("FieldMayBeFinal")
    @Shadow private int selectedIndex;

    @Shadow protected abstract void syncRecipeIndex();

    @Unique private SpeedTradeButton speedTradeButton;

    public MerchantScreenMixin() {
        super(null, null, null);
        throw new RuntimeException("Mixin constructor called?!");
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void addSpeedTradeButton(CallbackInfo ci) {
        addButton(speedTradeButton = new SpeedTradeButton(x + 247, y + 36, this));
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderSpeedTradeButtonTooltip(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        speedTradeButton.renderToolTip(matrices, mouseX, mouseY);
    }

    @Override
    public State getState() {
        if (client == null || client.currentScreen != this)
            return State.CLOSED;
        TradeOffer offer = getCurrentTradeOffer();
        if (offer == null)
            return State.NO_SELECTION;
        ItemStack sellItem = offer.getMutableSellItem();
        ModConfig.TradeBlockBehavior tradeBlockBehavior = ModConfig.get().tradeBlockBehavior;
        switch (tradeBlockBehavior) {
        case DAMAGEABLE:
            if (sellItem.isDamageable())
                return State.BLOCKED;
            break;
        case UNSTACKABLE:
            if (!sellItem.isStackable())
                return State.BLOCKED;
        default:
            break;
        }
        if (handler.getSlot(2).hasStack())
            return State.CAN_PERFORM;
        if (offer.isDisabled())
            return State.OUT_OF_STOCK;
        if (!playerCanAcceptStack(playerInventory, sellItem))
            return State.NO_ROOM_FOR_SELL_ITEM;
        if (playerHasStack(playerInventory, offer.getAdjustedFirstBuyItem()) && playerHasStack(playerInventory, offer.getSecondBuyItem()))
            return State.CAN_PERFORM;
        return State.NOT_ENOUGH_BUY_ITEMS;
    }

    @Override
    public TradeOffer getCurrentTradeOffer() {
        TradeOfferList tradeOffers = handler.getRecipes();
        if (selectedIndex < 0 || selectedIndex >= tradeOffers.size())
            return null;
        return tradeOffers.get(selectedIndex);
    }

    @Override
    public void autofillSellSlots() {
        syncRecipeIndex();
    }

    @Override
    public void performTrade() {
        if (getState() == State.CAN_PERFORM)
            onMouseClick(handler.slots.get(2), -1, 0, SlotActionType.QUICK_MOVE);
    }

    @Override
    public void clearSellSlots() {
        onMouseClick(handler.slots.get(0), -1, 0, SlotActionType.QUICK_MOVE);
        onMouseClick(handler.slots.get(1), -1, 0, SlotActionType.QUICK_MOVE);
    }

    @Override
    public void callRenderTooltip(MatrixStack matrixStack, List<Text> text, int mouseX, int mouseY) {
        renderTooltip(matrixStack, text, mouseX, mouseY);
    }

    @Override
    public void tick() {
        super.tick();
        speedTradeButton.tick();
    }
}
